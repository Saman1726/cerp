import { NextRequest, NextResponse } from 'next/server';

export interface CreateAuthProxyOptions {
  authenticatedPrefixes: string[];
  loginPath?: string;
  postLoginRedirect?: string;
}

// Cookie-presence check only - real enforcement is the API returning 401,
// which Server Components/Actions surface as an ApiClientError. This just
// avoids an obviously-logged-out user rendering a page that will fail.
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

  return function proxy(request: NextRequest): NextResponse {
    const hasSession = request.cookies.has('cerp_session');
    const { pathname } = request.nextUrl;

    const isAuthenticatedRoute = options.authenticatedPrefixes.some((prefix) => pathname.startsWith(prefix));

    if (isAuthenticatedRoute && !hasSession) {
      return NextResponse.redirect(new URL(loginPath, request.url));
    }

    if (pathname === loginPath && hasSession) {
      return NextResponse.redirect(new URL(postLoginRedirect, request.url));
    }

    return NextResponse.next();
  };
}
