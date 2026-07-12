'use client';

import { useActionState, type ReactNode } from 'react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import type { CreatePropertyActionState } from './types';

const initialState: CreatePropertyActionState = {};

export interface PropertyFormProps {
  action: (state: CreatePropertyActionState, formData: FormData) => Promise<CreatePropertyActionState>;
  /** Slot for a consumer's own extra fields - native inputs rendered here are
   * automatically part of the submitted FormData, no wiring needed. */
  extraFields?: ReactNode;
}

export function PropertyForm({ action, extraFields }: PropertyFormProps) {
  const [state, formAction, pending] = useActionState(action, initialState);

  return (
    <form action={formAction} className="flex flex-wrap items-start gap-3">
      <Input name="name" placeholder="Property name" required className="w-56" />
      <Input name="address" placeholder="Address" required className="w-64" />
      {extraFields}
      <Button type="submit" disabled={pending}>
        {pending ? 'Adding...' : 'Add property'}
      </Button>
      {state.error ? <p className="w-full text-sm text-destructive">{state.error}</p> : null}
    </form>
  );
}
