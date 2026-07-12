import { BadRequestException, NotFoundException } from '@nestjs/common';
import { ProcurementService } from './procurement.service';
import { PurchaseOrder } from './entities/purchase-order.entity';
import { GoodsReceipt } from './entities/goods-receipt.entity';
import { PURCHASE_ORDER_RAISED_EVENT, GOODS_RECEIVED_EVENT } from './events/procurement-events';
import type { TenantContext } from '../tenancy/interfaces/tenant-context.interface';
import type { PartyRegistryEntry } from '../party-registry/party-registry.types';

const FAKE_TENANT_CONTEXT: TenantContext = {
  tenantId: 'tenant-1',
  tenantSlug: 'acme',
  schemaName: 'tenant_acme',
};

const ACTIVE_VENDOR: PartyRegistryEntry = {
  id: 'vendor-1',
  code: 'ACME-SUPPLY',
  name: 'Acme Supply Co.',
  type: 'supplier',
  contactEmail: 'contact@acmesupply.test',
  status: 'active',
};

const INACTIVE_VENDOR: PartyRegistryEntry = { ...ACTIVE_VENDOR, id: 'vendor-2', status: 'inactive' };

function createMockManager() {
  return {
    save: jest.fn(),
    find: jest.fn(),
    findOne: jest.fn(),
    update: jest.fn(),
  };
}

function createService(manager: ReturnType<typeof createMockManager>) {
  const tenancyService = {
    requireTenantContext: jest.fn(() => FAKE_TENANT_CONTEXT),
    getEntityManager: jest.fn(async () => manager),
  };
  const partyRegistry = {
    findAll: jest.fn(),
    findById: jest.fn(),
    login: jest.fn(),
  };
  const events = { emitAsync: jest.fn() };

  // Constructor accepts the interfaces directly - no NestJS TestingModule
  // needed for a plain service with three constructor dependencies.
  const service = new ProcurementService(tenancyService as never, partyRegistry as never, events as never);
  return { service, tenancyService, partyRegistry, events };
}

describe('ProcurementService', () => {
  describe('raise', () => {
    it('rejects when the vendor is not found in the partner directory', async () => {
      const manager = createMockManager();
      const { service, partyRegistry } = createService(manager);
      partyRegistry.findById.mockResolvedValue(null);

      await expect(
        service.raise({ projectId: 'project-1', vendorPartnerId: 'unknown', description: 'Rebar' }),
      ).rejects.toBeInstanceOf(BadRequestException);
      expect(manager.save).not.toHaveBeenCalled();
    });

    it('rejects when the vendor is inactive', async () => {
      const manager = createMockManager();
      const { service, partyRegistry } = createService(manager);
      partyRegistry.findById.mockResolvedValue(INACTIVE_VENDOR);

      await expect(
        service.raise({ projectId: 'project-1', vendorPartnerId: INACTIVE_VENDOR.id, description: 'Rebar' }),
      ).rejects.toBeInstanceOf(BadRequestException);
      expect(manager.save).not.toHaveBeenCalled();
    });

    it('raises a purchase order against a valid active vendor and emits PurchaseOrderRaised', async () => {
      const manager = createMockManager();
      const { service, partyRegistry, events } = createService(manager);
      partyRegistry.findById.mockResolvedValue(ACTIVE_VENDOR);
      const saved = {
        id: 'po-1',
        projectId: 'project-1',
        vendorPartnerId: ACTIVE_VENDOR.id,
        vendorPartnerName: ACTIVE_VENDOR.name,
        description: 'Rebar',
        status: 'ordered',
      };
      manager.save.mockResolvedValue(saved);

      const result = await service.raise({
        projectId: 'project-1',
        vendorPartnerId: ACTIVE_VENDOR.id,
        description: 'Rebar',
      });

      expect(manager.save).toHaveBeenCalledWith(
        PurchaseOrder,
        expect.objectContaining({
          vendorPartnerId: ACTIVE_VENDOR.id,
          vendorPartnerName: ACTIVE_VENDOR.name, // denormalized from the registry, not trusted client input
          status: 'ordered',
        }),
      );
      expect(events.emitAsync).toHaveBeenCalledWith(
        PURCHASE_ORDER_RAISED_EVENT,
        expect.objectContaining({ purchaseOrder: saved, tenantContext: FAKE_TENANT_CONTEXT }),
      );
      expect(result).toBe(saved);
    });
  });

  describe('recordGoodsReceipt', () => {
    it('throws NotFoundException for an unknown purchase order', async () => {
      const manager = createMockManager();
      const { service } = createService(manager);
      manager.findOne.mockResolvedValue(null);

      await expect(service.recordGoodsReceipt('missing-po', { notes: 'n/a' })).rejects.toBeInstanceOf(
        NotFoundException,
      );
      expect(manager.save).not.toHaveBeenCalled();
    });

    it('records a receipt, marks the purchase order received, and emits GoodsReceived', async () => {
      const manager = createMockManager();
      const { service, events } = createService(manager);

      const existingPo = { id: 'po-1', status: 'ordered' };
      const receivedPo = { id: 'po-1', status: 'received' };
      const receipt = { id: 'receipt-1', purchaseOrderId: 'po-1', notes: 'Delivered in good condition' };

      manager.findOne
        .mockResolvedValueOnce(existingPo) // existence check before writing
        .mockResolvedValueOnce(receivedPo); // re-fetch after the status update
      manager.save.mockResolvedValue(receipt);
      manager.find.mockResolvedValue([receipt]); // withReceipts() lookup

      const result = await service.recordGoodsReceipt('po-1', { notes: 'Delivered in good condition' });

      expect(manager.save).toHaveBeenCalledWith(
        GoodsReceipt,
        expect.objectContaining({ purchaseOrderId: 'po-1', notes: 'Delivered in good condition' }),
      );
      expect(manager.update).toHaveBeenCalledWith(PurchaseOrder, { id: 'po-1' }, { status: 'received' });
      expect(events.emitAsync).toHaveBeenCalledWith(
        GOODS_RECEIVED_EVENT,
        expect.objectContaining({ purchaseOrder: receivedPo, goodsReceipt: receipt, tenantContext: FAKE_TENANT_CONTEXT }),
      );
      expect(result).toEqual({ ...receivedPo, receipts: [receipt] });
    });
  });

  describe('findAll', () => {
    it('enriches each purchase order with its own receipts', async () => {
      const manager = createMockManager();
      const { service } = createService(manager);

      const poA = { id: 'po-a' };
      const poB = { id: 'po-b' };
      const receiptA = { id: 'r-a', purchaseOrderId: 'po-a' };

      manager.find
        .mockResolvedValueOnce([poA, poB]) // the purchase-orders query
        .mockResolvedValueOnce([receiptA]) // receipts for po-a
        .mockResolvedValueOnce([]); // receipts for po-b

      const result = await service.findAll();

      expect(result).toEqual([
        { ...poA, receipts: [receiptA] },
        { ...poB, receipts: [] },
      ]);
    });
  });
});
