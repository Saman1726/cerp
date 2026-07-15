import { redirect } from 'next/navigation';
import { loginRequestSchema, type LoginResponse } from '@cerp/shared-types';
import { setSession } from '../lib/session';
import { API_URL } from '../lib/env';
import type { LoginActionState } from './types';

export interface PerformLoginOptions {
  /** Where to redirect on success. Defaults to '/properties'. */
  redirectTo?: string;
}

// Deliberately NOT a 'use server' function - see properties/property-logic.ts
// for why. redirect() still works fine called from a plain function below
// the real 'use server' boundary - it's throw-based, unrelated to the RSC
// reference machinery that makes cross-package Server Actions unreliable.
export async function performLogin(
  formData: FormData,
  options: PerformLoginOptions = {},
): Promise<LoginActionState> {
  const parsed = loginRequestSchema.safeParse({
    username: formData.get('username'),
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
    return { error: body?.message ?? 'Invalid username or password' };
  }

  const data: LoginResponse = await res.json();
  await setSession({
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
  });

  redirect(options.redirectTo ?? '/properties');
}
