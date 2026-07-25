import { cookies } from 'next/headers';
import { SESSION_COOKIE, REFRESH_COOKIE, sessionCookieOptions } from './session-cookies';

export interface Session {
  accessToken: string;
  refreshToken: string;
}

// The browser only ever talks to this Next.js app, never to the NestJS API
// directly - these cookies are Next's own session, not something the API set.
export async function setSession(session: Session): Promise<void> {
  const store = await cookies();
  store.set(SESSION_COOKIE, session.accessToken, sessionCookieOptions);
  store.set(REFRESH_COOKIE, session.refreshToken, sessionCookieOptions);
}

export async function getSession(): Promise<Session | null> {
  const store = await cookies();
  const accessToken = store.get(SESSION_COOKIE)?.value;
  const refreshToken = store.get(REFRESH_COOKIE)?.value;

  if (!accessToken || !refreshToken) {
    return null;
  }
  return { accessToken, refreshToken };
}

export async function clearSession(): Promise<void> {
  const store = await cookies();
  const expired = new Date(0); // Expire immediately (Jan 1, 1970)
  store.set(SESSION_COOKIE, '', { ...sessionCookieOptions, expires: expired });
  store.set(REFRESH_COOKIE, '', { ...sessionCookieOptions, expires: expired });
}
