import { NextRequest, NextResponse } from 'next/server';

const SESSION_COOKIE = 'cerp_session';
const DEFAULT_TENANT_ID = '00000000-0000-0000-0000-000000000001';

const PROJECT_SERVICE_URL = (process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL || process.env.PROJECT_SERVICE_URL || 'http://localhost:8085')
  .replace(/\/$/, '');

/**
 * Extract the Bearer token from the incoming request (cookie header + server-side cookies).
 */
async function resolveToken(request: NextRequest): Promise<string | null> {
  // Try cookie header first (browser requests)
  const cookieHeader = request.headers.get('cookie');
  if (cookieHeader) {
    for (const cookie of cookieHeader.split(';').map(c => c.trim())) {
      const [key, ...valueParts] = cookie.split('=');
      if (key === SESSION_COOKIE && valueParts.length > 0) {
        let token = valueParts.join('=');
        try { token = decodeURIComponent(token); } catch { /* ignore */ }
        return token;
      }
    }
  }
  // Fallback to server-side cookie store (server actions / RSC)
  try {
    const cookieStore = await (await import('next/headers')).cookies();
    const token = cookieStore.get(SESSION_COOKIE)?.value ?? null;
    if (token) return token;
  } catch { /* ignore */ }
  return null;
}

/**
 * Build forward headers from the incoming request.
 * Preserves Authorization, X-Tenant-Id, and sets Content-Type.
 */
function buildForwardHeaders(request: NextRequest, token: string | null): Headers {
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  const tenantId = request.headers.get('x-tenant-id') ?? DEFAULT_TENANT_ID;
  headers.set('X-Tenant-Id', tenantId);

  if (token) headers.set('Authorization', `Bearer ${token}`);

  return headers;
}

/**
 * Proxy any HTTP method to the project service, forwarding the full sub-path.
 * e.g. POST /api/construction-sites/{siteId}/ifc-files/{fileId}/preview
 *      → POST http://project-service-java:8085/api/v1/construction-sites/{siteId}/ifc-files/{fileId}/preview
 */
async function proxyRequest(method: string, request: NextRequest): Promise<NextResponse> {
  // Extract the path after /api/construction-sites
  const url = new URL(request.url);
  const remainingPath = url.pathname.replace(/^\/api\/construction-sites/, '');
  const query = url.search;
  const targetUrl = `${PROJECT_SERVICE_URL}/api/v1/construction-sites${remainingPath}${query}`;

  const token = await resolveToken(request);
  const headers = buildForwardHeaders(request, token);

  // Forward the body for non-GET methods
  let body: string | undefined;
  if (method !== 'GET' && method !== 'HEAD') {
    body = await request.text();
  }

  try {
    const res = await fetch(targetUrl, {
      method,
      headers,
      body: body ? body : undefined,
      signal: AbortSignal.timeout(30000),
    });

    const responseHeaders = new Headers(res.headers);
    // Remove hop-by-hop headers
    for (const hop of ['transfer-encoding', 'connection', 'keep-alive']) {
      responseHeaders.delete(hop);
    }

    return new NextResponse(res.body, {
      status: res.status,
      headers: responseHeaders,
    });
  } catch (error) {
    console.error('[construction-sites proxy] Request failed:', method, targetUrl, error);
    return NextResponse.json(
      { error: 'Failed to connect to project service' },
      { status: 503 },
    );
  }
}

export async function GET(request: NextRequest) {
  return proxyRequest('GET', request);
}

export async function POST(request: NextRequest) {
  return proxyRequest('POST', request);
}

export async function PUT(request: NextRequest) {
  return proxyRequest('PUT', request);
}

export async function PATCH(request: NextRequest) {
  return proxyRequest('PATCH', request);
}

export async function DELETE(request: NextRequest) {
  return proxyRequest('DELETE', request);
}