export { proxy as middleware } from '../proxy';

export const config = {
  matcher: [
    '/dashboard/:path*',
    '/properties/:path*',
    '/construction/:path*',
    '/procurement/:path*',
    '/tenants/:path*',
    '/partners/:path*',
    '/vendors/:path*',
    '/settings/:path*',
    '/workflows/:path*',
    '/login',
  ],
};
