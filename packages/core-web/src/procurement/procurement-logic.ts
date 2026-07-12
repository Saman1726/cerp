import { recordGoodsReceiptRequestSchema, raisePurchaseOrderRequestSchema } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import type { RaisePurchaseOrderActionState, RecordGoodsReceiptActionState } from './types';

// Deliberately NOT a 'use server' function - see properties/property-logic.ts.
// Each consuming app wraps these in its own thin 'use server' actions.
export async function raisePurchaseOrder(formData: FormData): Promise<RaisePurchaseOrderActionState> {
  const parsed = raisePurchaseOrderRequestSchema.safeParse({
    projectId: formData.get('projectId'),
    vendorPartnerId: formData.get('vendorPartnerId'),
    description: formData.get('description'),
  });

  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? 'Invalid input' };
  }

  try {
    await apiClient.post('/purchase-orders', parsed.data);
  } catch {
    return { error: 'Failed to raise purchase order' };
  }

  return {};
}

export async function recordGoodsReceipt(formData: FormData): Promise<RecordGoodsReceiptActionState> {
  const purchaseOrderId = String(formData.get('purchaseOrderId') ?? '');
  if (!purchaseOrderId) {
    return { error: 'Missing purchase order id' };
  }

  const parsed = recordGoodsReceiptRequestSchema.safeParse({
    notes: (formData.get('notes') as string | null) ?? undefined,
  });

  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? 'Invalid input' };
  }

  try {
    await apiClient.post(`/purchase-orders/${purchaseOrderId}/receipts`, parsed.data);
  } catch {
    return { error: 'Failed to record goods receipt' };
  }

  return {};
}