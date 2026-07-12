'use client';

import { useActionState } from 'react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import type { RecordGoodsReceiptActionState } from './types';

const initialState: RecordGoodsReceiptActionState = {};

export interface GoodsReceiptFormProps {
  purchaseOrderId: string;
  action: (state: RecordGoodsReceiptActionState, formData: FormData) => Promise<RecordGoodsReceiptActionState>;
}

export function GoodsReceiptForm({ purchaseOrderId, action }: GoodsReceiptFormProps) {
  const [state, formAction, pending] = useActionState(action, initialState);

  return (
    <form action={formAction} className="flex items-center gap-2">
      <input type="hidden" name="purchaseOrderId" value={purchaseOrderId} />
      <Input name="notes" placeholder="Notes (optional)" className="w-64" />
      <Button type="submit" size="sm" disabled={pending}>
        {pending ? 'Recording...' : 'Record receipt'}
      </Button>
      {state.error ? <span className="text-xs text-destructive">{state.error}</span> : null}
    </form>
  );
}