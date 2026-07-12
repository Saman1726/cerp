'use server';

import { revalidatePath } from 'next/cache';
import { raisePurchaseOrder, recordGoodsReceipt } from '@cerp/core-web/procurement';
import type {
  RaisePurchaseOrderActionState,
  RecordGoodsReceiptActionState,
} from '@cerp/core-web/procurement';

export async function raisePurchaseOrderAction(
  _prevState: RaisePurchaseOrderActionState,
  formData: FormData,
): Promise<RaisePurchaseOrderActionState> {
  const result = await raisePurchaseOrder(formData);
  if (!result.error) {
    revalidatePath('/procurement');
  }
  return result;
}

export async function recordGoodsReceiptAction(
  _prevState: RecordGoodsReceiptActionState,
  formData: FormData,
): Promise<RecordGoodsReceiptActionState> {
  const result = await recordGoodsReceipt(formData);
  if (!result.error) {
    revalidatePath('/procurement');
  }
  return result;
}