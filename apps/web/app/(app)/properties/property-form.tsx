'use client';

import { useActionState } from 'react';
import { createPropertyAction, type CreatePropertyActionState } from './actions';

const initialState: CreatePropertyActionState = {};

export function PropertyForm() {
  const [state, formAction, pending] = useActionState(createPropertyAction, initialState);

  return (
    <form action={formAction} style={{ display: 'flex', gap: 8, margin: '16px 0' }}>
      <input name="name" placeholder="Property name" required />
      <input name="address" placeholder="Address" required />
      <button type="submit" disabled={pending}>
        {pending ? 'Adding...' : 'Add property'}
      </button>
      {state.error ? <span style={{ color: 'crimson' }}>{state.error}</span> : null}
    </form>
  );
}
