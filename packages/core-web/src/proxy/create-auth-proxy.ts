import { NextRequest, NextResponse } from 'next/server';
import type { LoginResponse } from '@cerp/shared-types';
import { IDENTITY_SERVICE_URL } from '../lib/env';
import { SESSION_COOKIE, REFRESH_COOKIE, sessionCookieOptions } from '../lib/session-cookies';

export interface CreateAuthProxyOptions {
  authenticatedPrefixes: string[];
  loginPath?: string;
  postLoginRedirect?: string;
}

// Refresh this long before the access token's real expiry so a request that's
// in flight while middleware decides doesn't lose the race against the API's
// own clock.
const REFRESH_SKEW_MS = 30_000;

// No signature verification here - this is only ever a UX optimization to
// avoid sending a request the API is about to reject with 401. The API is
// the real enforcement point regardless of what this decode returns.
function decodeJwtExpiryMs(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const claims = JSON.parse(atob(padded)) as { exp?: number };
    return typeof claims.exp === 'number' ? claims.exp * 1000 : null;
  } catch {
    return null;
  }
}

async function tryRefresh(refreshToken: string): Promise<LoginResponse | null> {
  try {
    const res = await fetch(`${IDENTITY_SERVICE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
      cache: 'no-store',
    });
    if (!res.ok) return null;
    return (await res.json()) as LoginResponse;
  } catch {
    return null;
  }
}

function redirectToLogin(request: NextRequest, loginPath: string): NextResponse {
  const response = NextResponse.redirect(new URL(loginPath, request.url));
  response.cookies.delete(SESSION_COOKIE);
  response.cookies.delete(REFRESH_COOKIE);
  return response;
}

// Proactively refreshes an expiring access token before the page/Server
// Component render ever runs, since Server Components can't set cookies
// themselves (only Server Actions, Route Handlers, and middleware can) - this
// is the one place in the request lifecycle that can durably fix an expired
// token before anything downstream sees it.
//
// Returns the handler function itself - the consumer's own proxy.ts must
// still declare `export const config = { matcher: [...] }` as an inline
// literal. Next's build-time config extractor only accepts literal values
// there (confirmed against Next's own source) and silently drops an
// unresolvable matcher rather than failing the build, so this helper
// deliberately doesn't try to own or compute it.
export function createAuthProxy(options: CreateAuthProxyOptions) {
  const loginPath = options.loginPath ?? '/login';
  const postLoginRedirect = options.postLoginRedirect ?? options.authenticatedPrefixes[0] ?? '/';

  return async function proxy(request: NextRequest): Promise<NextResponse> {
    const { pathname } = request.nextUrl;
    const accessToken = request.cookies.get(SESSION_COOKIE)?.value;
    const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;
    const hasSession = Boolean(accessToken && refreshToken);

    const isAuthenticatedRoute = options.authenticatedPrefixes.some((prefix) => pathname.startsWith(prefix));

    if (isAuthenticatedRoute && !hasSession) {
      return redirectToLogin(request, loginPath);
    }

    if (pathname === loginPath && hasSession) {
      return NextResponse.redirect(new URL(postLoginRedirect, request.url));
    }

    if (isAuthenticatedRoute && accessToken && refreshToken) {
      const expiresAtMs = decodeJwtExpiryMs(accessToken);
      const isExpiring = expiresAtMs === null || expiresAtMs - Date.now() < REFRESH_SKEW_MS;

      if (isExpiring) {
        const refreshed = await tryRefresh(refreshToken);
        if (!refreshed) {
          return redirectToLogin(request, loginPath);
        }

        const response = NextResponse.next();
        response.cookies.set(SESSION_COOKIE, refreshed.accessToken, sessionCookieOptions);
        response.cookies.set(REFRESH_COOKIE, refreshed.refreshToken, sessionCookieOptions);
        return response;
      }
    }

    return NextResponse.next();
  };
}
