import { cookies } from 'next/headers';

const isProd = process.env.NODE_ENV === 'production';
const cookieOptions = {
  httpOnly: true,
  secure: isProd,
  sameSite: 'lax' as const,
  path: '/',
};

export interface Session {
  accessToken: string;
  refreshToken: string;
  tenantSlug: string;
}

// The browser only ever talks to this Next.js app, never to the NestJS API
// directly - these cookies are Next's own session, not something the API set.
export async function setSession(session: Session): Promise<void> {
  const store = await cookies();
  store.set('cerp_session', session.accessToken, cookieOptions);
  store.set('cerp_refresh', session.refreshToken, cookieOptions);
  store.set('cerp_tenant', session.tenantSlug, cookieOptions);
}

export async function getSession(): Promise<Session | null> {
  const store = await cookies();
  const accessToken = store.get('cerp_session')?.value;
  const refreshToken = store.get('cerp_refresh')?.value;
  const tenantSlug = store.get('cerp_tenant')?.value;

  if (!accessToken || !refreshToken || !tenantSlug) {
    return null;
  }
  return { accessToken, refreshToken, tenantSlug };
}

export async function clearSession(): Promise<void> {
  const store = await cookies();
  store.delete('cerp_session');
  store.delete('cerp_refresh');
  store.delete('cerp_tenant');
}
