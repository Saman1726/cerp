import type { TenantContext } from '../../tenancy/interfaces/tenant-context.interface';
import type { WorkOrder } from '../entities/work-order.entity';

export const WORK_ORDER_ISSUED_EVENT = 'work-order.issued';
export const WORK_ORDER_COMPLETED_EVENT = 'work-order.completed';

export interface WorkOrderIssuedEvent {
  workOrder: WorkOrder;
  tenantContext: TenantContext;
}

export interface WorkOrderCompletedEvent {
  workOrder: WorkOrder;
  tenantContext: TenantContext;
}
