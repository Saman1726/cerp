import { z } from 'zod';

export interface GoodsReceipt {
  id: string;
  purchaseOrderId: string;
  notes: string | null;
  receivedAt: string;
}

export type PurchaseOrderStatus = 'ordered' | 'received';

export interface PurchaseOrder {
  id: string;
  projectId: string;
  vendorPartnerId: string;
  vendorPartnerName: string;
  description: string;
  status: PurchaseOrderStatus;
  orderedAt: string;
  updatedAt: string;
}

export type PurchaseOrderWithReceipts = PurchaseOrder & {
  receipts: GoodsReceipt[];
};

export const raisePurchaseOrderRequestSchema = z.object({
  projectId: z.string().uuid(),
  vendorPartnerId: z.string().min(1),
  description: z.string().min(1).max(500),
});
export type RaisePurchaseOrderRequest = z.infer<typeof raisePurchaseOrderRequestSchema>;

export const recordGoodsReceiptRequestSchema = z.object({
  notes: z.string().max(2000).optional(),
});
export type RecordGoodsReceiptRequest = z.infer<typeof recordGoodsReceiptRequestSchema>;