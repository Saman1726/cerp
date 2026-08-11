import { NextRequest } from 'next/server';

const SESSION_COOKIE = 'cerp_session';
// Every registration lands on this shared tenant when no cookie is set yet - see the other /api routes.
const DEFAULT_TENANT_ID = '00000000-0000-0000-0000-000000000001';

function extractSessionToken(request: NextRequest): string | null {
  const cookieHeader = request.headers.get('cookie');
  if (!cookieHeader) return null;
  for (const cookie of cookieHeader.split(';').map((c) => c.trim())) {
    const [key, ...rest] = cookie.split('=');
    if (key === SESSION_COOKIE && rest.length > 0) {
      const raw = rest.join('=');
      try {
        return decodeURIComponent(raw);
      } catch {
        return raw;
      }
    }
  }
  return null;
}

/** Auth/tenant headers for calls to document-service (a different auth convention than workflow-service's getAuthHeaders). */
export function getDocumentServiceHeaders(request: NextRequest): Headers {
  const headers = new Headers({ 'Content-Type': 'application/json' });
  const token = extractSessionToken(request);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  headers.set('X-Tenant-Id', request.headers.get('x-tenant-id') ?? DEFAULT_TENANT_ID);
  return headers;
}
