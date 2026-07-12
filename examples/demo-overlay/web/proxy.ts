import { createAuthProxy } from '@cerp/core-web/proxy';

export const proxy = createAuthProxy({ authenticatedPrefixes: ['/properties', '/procurement'] });

// Must stay an inline literal here - Next's build-time config extractor only
// accepts literal values for this export, and silently drops an unresolvable
// (computed/imported) matcher rather than failing the build.
export const config = {
  matcher: ['/properties/:path*', '/procurement/:path*', '/login'],
};
