import { NextRequest } from 'next/server';

const SESSION_COOKIE = 'cerp_session';

export function getAuthHeaders(request: NextRequest): HeadersInit {
  const headers: HeadersInit = { 'Content-Type': 'application/json' };

  // Forward Authorization header if present (from client-side token)
  const authHeader = request.headers.get('authorization');
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }

  // Forward cookie-based auth (session cookie pattern used in the app)
  const sessionCookie = request.cookies.get(SESSION_COOKIE)?.value;
  if (sessionCookie) {
    headers['Cookie'] = `${SESSION_COOKIE}=${sessionCookie}`;
  }

  return headers;
}

/**
 * Get headers for backend API calls, including tenant ID and auth headers.
 * 
 * <p>
 * The X-Tenant-Id header is used by Java services to set the TenantContext,
 * which controls data access in multi-tenant deployments.
 * </p>
 */
export function getBackendHeaders(request: NextRequest): HeadersInit {
  const headers: HeadersInit = { 'Content-Type': 'application/json' };

  // Forward Authorization header if present
  const authHeader = request.headers.get('authorization');
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }

  // Forward cookie-based auth
  const sessionCookie = request.cookies.get(SESSION_COOKIE)?.value;
  if (sessionCookie) {
    headers['Cookie'] = `${SESSION_COOKIE}=${sessionCookie}`;
  }

  // Forward tenant ID from URL query param or header
  const searchParams = request.nextUrl.searchParams;
  const tenantId = searchParams.get('tenantId') || request.headers.get('x-tenant-id');
  if (tenantId) {
    headers['X-Tenant-Id'] = tenantId;
  }

  return headers;
}
