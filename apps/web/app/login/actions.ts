'use server';

import { redirect } from 'next/navigation';
import { loginRequestSchema, type LoginResponse } from '@cerp/shared-types';
import { setSession } from '@/lib/session';
import { API_URL } from '@/lib/env';

export interface LoginActionState {
  error?: string;
}

export async function loginAction(
  _prevState: LoginActionState,
  formData: FormData,
): Promise<LoginActionState> {
  const parsed = loginRequestSchema.safeParse({
    tenantSlug: formData.get('tenantSlug'),
    email: formData.get('email'),
    password: formData.get('password'),
  });

  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? 'Invalid input' };
  }

  // Calls the API directly rather than through lib/api-client - there's no
  // session yet for the client to attach.
  const res = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(parsed.data),
    cache: 'no-store',
  });

  if (!res.ok) {
    const body = await res.json().catch(() => undefined);
    return { error: body?.message ?? 'Invalid organization, email or password' };
  }

  const data: LoginResponse = await res.json();
  await setSession({
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
    tenantSlug: data.tenant.slug,
  });

  redirect('/properties');
}
