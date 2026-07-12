import type { TenantContext } from '../../tenancy/interfaces/tenant-context.interface';
import type { PurchaseOrder } from '../entities/purchase-order.entity';
import type { GoodsReceipt } from '../entities/goods-receipt.entity';

export const PURCHASE_ORDER_RAISED_EVENT = 'purchase-order.raised';
export const GOODS_RECEIVED_EVENT = 'goods.received';

export interface PurchaseOrderRaisedEvent {
  purchaseOrder: PurchaseOrder;
  tenantContext: TenantContext;
}

export interface GoodsReceivedEvent {
  purchaseOrder: PurchaseOrder;
  goodsReceipt: GoodsReceipt;
  tenantContext: TenantContext;
}
