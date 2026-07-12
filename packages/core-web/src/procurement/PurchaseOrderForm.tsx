'use client';

import { useActionState } from 'react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import type { RaisePurchaseOrderActionState } from './types';

const initialState: RaisePurchaseOrderActionState = {};

export interface PurchaseOrderFormProps {
  action: (state: RaisePurchaseOrderActionState, formData: FormData) => Promise<RaisePurchaseOrderActionState>;
}

export function PurchaseOrderForm({ action }: PurchaseOrderFormProps) {
  const [state, formAction, pending] = useActionState(action, initialState);

  return (
    <form action={formAction} className="flex flex-wrap items-start gap-3">
      <Input
        name="projectId"
        placeholder="Project ID (UUID)"
        required
        className="w-64"
        pattern="[0-9a-fA-F-]{36}"
        title="UUID"
      />
      <Input
        name="vendorPartnerId"
        placeholder="Vendor Partner ID (e.g. fake-partner-active)"
        required
        className="w-72"
      />
      <Input name="description" placeholder="Description" required className="w-72" />
      <Button type="submit" disabled={pending}>
        {pending ? 'Submitting...' : 'Raise purchase order'}
      </Button>
      {state.error ? <p className="w-full text-sm text-destructive">{state.error}</p> : null}
    </form>
  );
}