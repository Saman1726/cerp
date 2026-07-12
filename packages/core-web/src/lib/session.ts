import { cookies } from 'next/headers';
import { SESSION_COOKIE, REFRESH_COOKIE, TENANT_COOKIE, sessionCookieOptions } from './session-cookies';

export interface Session {
  accessToken: string;
  refreshToken: string;
  tenantSlug: string;
}

// The browser only ever talks to this Next.js app, never to the NestJS API
// directly - these cookies are Next's own session, not something the API set.
export async function setSession(session: Session): Promise<void> {
  const store = await cookies();
  store.set(SESSION_COOKIE, session.accessToken, sessionCookieOptions);
  store.set(REFRESH_COOKIE, session.refreshToken, sessionCookieOptions);
  store.set(TENANT_COOKIE, session.tenantSlug, sessionCookieOptions);
}

export async function getSession(): Promise<Session | null> {
  const store = await cookies();
  const accessToken = store.get(SESSION_COOKIE)?.value;
  const refreshToken = store.get(REFRESH_COOKIE)?.value;
  const tenantSlug = store.get(TENANT_COOKIE)?.value;

  if (!accessToken || !refreshToken || !tenantSlug) {
    return null;
  }
  return { accessToken, refreshToken, tenantSlug };
}

export async function clearSession(): Promise<void> {
  const store = await cookies();
  store.delete(SESSION_COOKIE);
  store.delete(REFRESH_COOKIE);
  store.delete(TENANT_COOKIE);
}
