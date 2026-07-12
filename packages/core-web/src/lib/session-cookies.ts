export const SESSION_COOKIE = 'cerp_session';
export const REFRESH_COOKIE = 'cerp_refresh';
export const TENANT_COOKIE = 'cerp_tenant';

export const sessionCookieOptions = {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',
  sameSite: 'lax' as const,
  path: '/',
};
