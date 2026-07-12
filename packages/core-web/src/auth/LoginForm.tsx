'use client';

import { useActionState } from 'react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import type { LoginActionState } from './types';

const initialState: LoginActionState = {};

export interface LoginFormProps {
  action: (state: LoginActionState, formData: FormData) => Promise<LoginActionState>;
}

export function LoginForm({ action }: LoginFormProps) {
  const [state, formAction, pending] = useActionState(action, initialState);

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <div className="grid gap-2">
        <Label htmlFor="tenantSlug">Organization</Label>
        <Input id="tenantSlug" name="tenantSlug" placeholder="acme" required />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="email">Email</Label>
        <Input id="email" name="email" type="email" required />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="password">Password</Label>
        <Input id="password" name="password" type="password" required minLength={8} />
      </div>
      {state.error ? <p className="text-sm text-destructive">{state.error}</p> : null}
      <Button type="submit" disabled={pending} className="mt-2">
        {pending ? 'Signing in...' : 'Sign in'}
      </Button>
    </form>
  );
}
