import { BadRequestException, Inject, Injectable, NotFoundException } from '@nestjs/common';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { TenancyService } from '../tenancy/tenancy.service';
import { WorkOrder } from './entities/work-order.entity';
import { IssueWorkOrderDto } from './dto/issue-work-order.dto';
import { PARTY_REGISTRY_GATEWAY } from '../party-registry/party-registry.tokens';
import type { PartyRegistryGateway } from '../party-registry/party-registry-gateway.interface';
import { WORK_ORDER_COMPLETED_EVENT, WORK_ORDER_ISSUED_EVENT } from './events/work-order-events';

@Injectable()
export class ConstructionEngineeringService {
  constructor(
    private readonly tenancyService: TenancyService,
    @Inject(PARTY_REGISTRY_GATEWAY) private readonly partyRegistry: PartyRegistryGateway,
    private readonly events: EventEmitter2,
  ) {}

  async findAll(): Promise<WorkOrder[]> {
    const manager = await this.tenancyService.getEntityManager();
    return manager.find(WorkOrder, { order: { issuedAt: 'DESC' } });
  }

  async findOne(id: string): Promise<WorkOrder> {
    const manager = await this.tenancyService.getEntityManager();
    const workOrder = await manager.findOne(WorkOrder, { where: { id } });
    if (!workOrder) {
      throw new NotFoundException(`Work order ${id} not found`);
    }
    return workOrder;
  }

  async issue(dto: IssueWorkOrderDto): Promise<WorkOrder> {
    const tenantContext = this.tenancyService.requireTenantContext();

    // Contractor identity is resolved (never trusted from the client as
    // freeform text) against the real Party Registry - this is the same
    // gateway the Partners page reads from, just called from a second
    // context now, exactly the Open Host Service relationship documented
    // for Party Registry.
    const contractor = await this.partyRegistry.findById(dto.contractorPartnerId);
    if (!contractor) {
      throw new BadRequestException('Contractor not found in the partner directory');
    }
    if (contractor.status !== 'active') {
      throw new BadRequestException('Contractor is not active');
    }

    const manager = await this.tenancyService.getEntityManager();
    const workOrder = await manager.save(WorkOrder, {
      projectId: dto.projectId,
      title: dto.title,
      description: dto.description ?? null,
      contractorPartnerId: contractor.id,
      contractorPartnerName: contractor.name,
      status: 'issued' as const,
    });

    await this.events.emitAsync(WORK_ORDER_ISSUED_EVENT, { workOrder, tenantContext });
    return workOrder;
  }

  async complete(id: string): Promise<WorkOrder> {
    const tenantContext = this.tenancyService.requireTenantContext();
    const existing = await this.findOne(id);
    if (existing.status === 'completed') {
      return existing;
    }

    const manager = await this.tenancyService.getEntityManager();
    await manager.update(WorkOrder, { id }, { status: 'completed', completedAt: new Date() });

    const workOrder = await this.findOne(id);
    await this.events.emitAsync(WORK_ORDER_COMPLETED_EVENT, { workOrder, tenantContext });
    return workOrder;
  }
}
