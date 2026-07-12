import { createAuthProxy } from '@cerp/core-web/proxy';

export const proxy = createAuthProxy({
  authenticatedPrefixes: ['/dashboard', '/properties', '/construction', '/procurement', '/tenants', '/partners', '/settings'],
  postLoginRedirect: '/dashboard',
});

// Must stay an inline literal here - Next's build-time config extractor only
// accepts literal values for this export, and silently drops an unresolvable
// (computed/imported) matcher rather than failing the build.
export const config = {
  matcher: [
    '/dashboard/:path*',
    '/properties/:path*',
    '/construction/:path*',
    '/procurement/:path*',
    '/tenants/:path*',
    '/partners/:path*',
    '/settings/:path*',
    '/login',
  ],
};
