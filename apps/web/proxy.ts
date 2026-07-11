import { NextRequest, NextResponse } from 'next/server';

const AUTHENTICATED_PREFIXES = ['/properties'];

// Cookie-presence check only - real enforcement is the API returning 401,
// which Server Components/Actions surface as an ApiClientError. This just
// avoids an obviously-logged-out user rendering a page that will fail.
export function proxy(request: NextRequest): NextResponse {
  const hasSession = request.cookies.has('cerp_session');
  const { pathname } = request.nextUrl;

  const isAuthenticatedRoute = AUTHENTICATED_PREFIXES.some((prefix) => pathname.startsWith(prefix));

  if (isAuthenticatedRoute && !hasSession) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  if (pathname === '/login' && hasSession) {
    return NextResponse.redirect(new URL('/properties', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/properties/:path*', '/login'],
};
