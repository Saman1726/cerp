import { BadRequestException, Inject, Injectable, NotFoundException } from '@nestjs/common';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { TenancyService } from '../tenancy/tenancy.service';
import { PurchaseOrder } from './entities/purchase-order.entity';
import { GoodsReceipt } from './entities/goods-receipt.entity';
import { RaisePurchaseOrderDto } from './dto/raise-purchase-order.dto';
import { RecordGoodsReceiptDto } from './dto/record-goods-receipt.dto';
import { PARTY_REGISTRY_GATEWAY } from '../party-registry/party-registry.tokens';
import type { PartyRegistryGateway } from '../party-registry/party-registry-gateway.interface';
import { GOODS_RECEIVED_EVENT, PURCHASE_ORDER_RAISED_EVENT } from './events/procurement-events';

export interface PurchaseOrderWithReceipts extends PurchaseOrder {
  receipts: GoodsReceipt[];
}

@Injectable()
export class ProcurementService {
  constructor(
    private readonly tenancyService: TenancyService,
    @Inject(PARTY_REGISTRY_GATEWAY) private readonly partyRegistry: PartyRegistryGateway,
    private readonly events: EventEmitter2,
  ) {}

  async findAll(): Promise<PurchaseOrderWithReceipts[]> {
    const manager = await this.tenancyService.getEntityManager();
    const purchaseOrders = await manager.find(PurchaseOrder, { order: { orderedAt: 'DESC' } });
    return Promise.all(purchaseOrders.map((po) => this.withReceipts(po)));
  }

  async findOne(id: string): Promise<PurchaseOrderWithReceipts> {
    const purchaseOrder = await this.findPurchaseOrderOrThrow(id);
    return this.withReceipts(purchaseOrder);
  }

  async raise(dto: RaisePurchaseOrderDto): Promise<PurchaseOrder> {
    const tenantContext = this.tenancyService.requireTenantContext();

    // Vendor identity is resolved against the real Party Registry, never
    // trusted from the client as freeform text - same pattern Construction
    // Engineering uses to resolve a contractor.
    const vendor = await this.partyRegistry.findById(dto.vendorPartnerId);
    if (!vendor) {
      throw new BadRequestException('Vendor not found in the partner directory');
    }
    if (vendor.status !== 'active') {
      throw new BadRequestException('Vendor is not active');
    }

    const manager = await this.tenancyService.getEntityManager();
    const purchaseOrder = await manager.save(PurchaseOrder, {
      projectId: dto.projectId,
      vendorPartnerId: vendor.id,
      vendorPartnerName: vendor.name,
      description: dto.description,
      status: 'ordered' as const,
    });

    await this.events.emitAsync(PURCHASE_ORDER_RAISED_EVENT, { purchaseOrder, tenantContext });
    return purchaseOrder;
  }

  async recordGoodsReceipt(purchaseOrderId: string, dto: RecordGoodsReceiptDto): Promise<PurchaseOrderWithReceipts> {
    const tenantContext = this.tenancyService.requireTenantContext();
    await this.findPurchaseOrderOrThrow(purchaseOrderId); // 404s before writing if the id is bad

    const manager = await this.tenancyService.getEntityManager();
    const goodsReceipt = await manager.save(GoodsReceipt, {
      purchaseOrderId,
      notes: dto.notes ?? null,
    });
    await manager.update(PurchaseOrder, { id: purchaseOrderId }, { status: 'received' });

    const updatedPurchaseOrder = await this.findPurchaseOrderOrThrow(purchaseOrderId);
    await this.events.emitAsync(GOODS_RECEIVED_EVENT, {
      purchaseOrder: updatedPurchaseOrder,
      goodsReceipt,
      tenantContext,
    });

    return this.withReceipts(updatedPurchaseOrder);
  }

  private async findPurchaseOrderOrThrow(id: string): Promise<PurchaseOrder> {
    const manager = await this.tenancyService.getEntityManager();
    const purchaseOrder = await manager.findOne(PurchaseOrder, { where: { id } });
    if (!purchaseOrder) {
      throw new NotFoundException(`Purchase order ${id} not found`);
    }
    return purchaseOrder;
  }

  private async withReceipts(purchaseOrder: PurchaseOrder): Promise<PurchaseOrderWithReceipts> {
    const manager = await this.tenancyService.getEntityManager();
    const receipts = await manager.find(GoodsReceipt, {
      where: { purchaseOrderId: purchaseOrder.id },
      order: { receivedAt: 'ASC' },
    });
    return { ...purchaseOrder, receipts };
  }
}
