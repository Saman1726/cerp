import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders } from '../workflows/utils';

/**
 * Decode the payload of a JWT-like token (base64-encoded JSON, no signature verification).
 * Supports two formats:
 * - Legacy: {"sub":"admin@cerp.dev","role":"admin","exp":...}
 * - New:    {"sub":"userId","roles":"role1,role2","tenantId":"uuid","exp":...}
 */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1];
    if (!payload) return null;
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = Buffer.from(base64, 'base64').toString('utf-8');
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/**
 * Default tenant ID used when the JWT doesn't contain a tenantId.
 * Matches the vendor tenant default from AuthController.makeDevJwt.
 */
const DEFAULT_TENANT_ID = '00000000-0000-0000-0000-000000000001';

/**
 * Extract user info from the session cookie.
 */
async function extractUserFromToken(): Promise<{ userId: string | null; tenantId: string }> {
  const { cookies } = await import('next/headers');
  const cookieStore = await cookies();
  const token = cookieStore.get('cerp_session')?.value;
  if (!token) return { userId: null, tenantId: DEFAULT_TENANT_ID };
  const payload = decodeJwtPayload(token);
  if (!payload) return { userId: null, tenantId: DEFAULT_TENANT_ID };

  // userId: from "sub" field
  const userId =
    payload.sub && typeof payload.sub === 'string'
      ? payload.sub
      : null;

  // tenantId: prefer "tenantId", fall back to default
  let tenantId = DEFAULT_TENANT_ID;
  if (payload.tenantId && typeof payload.tenantId === 'string' && payload.tenantId.trim()) {
    tenantId = payload.tenantId;
  }

  return { userId, tenantId };
}

/**
 * Proxy /api/sagas (base endpoint) requests to the workflow service.
 *
 * Handles GET and POST to the sagas list/create endpoint.
 * Specific saga operations are handled by [...path]/route.ts.
 */

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);

    // Extract userId from session token for access control
    const { userId } = await extractUserFromToken();

    const targetUrl = new URL('/api/v1/sagas', WORKFLOW_SERVICE_URL);
    // Forward query parameters
    searchParams.forEach((value, key) => {
      targetUrl.searchParams.set(key, value);
    });
    // Pass userId as query parameter for backend access control
    if (userId) {
      targetUrl.searchParams.set('userId', userId);
    }

    const headers = getBackendHeaders(request);

    const res = await fetch(targetUrl.toString(), {
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json({ message: body?.message ?? res.statusText }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    // Inject userId and tenantId for saga creation (same logic as [...path]/route.ts).
    const { userId, tenantId } = await extractUserFromToken();
    console.log('[sagas/route POST] extractUserFromToken:', { userId, tenantId });
    console.log('[sagas/route POST] original body:', JSON.stringify(body).slice(0, 500));
    if (body && typeof body === 'object') {
      // Always override tenantId (the form sends tenantId="" which is falsy/empty).
      (body as Record<string, unknown>).tenantId = tenantId;
      // Inject userId into data if not already present.
      if (userId) {
        const data = (body.data as Record<string, unknown> | undefined) ?? {};
        if (!data.userId) {
          data.userId = userId;
          body.data = data;
        }
      }
    }

    const headers = getBackendHeaders(request);
    // Inject X-Tenant-Id header so the workflow service's TenantRequestFilter sets TenantContext.
    (headers as Record<string, string>)['X-Tenant-Id'] = tenantId;

    console.log('[sagas/route POST] final body:', JSON.stringify(body).slice(0, 500));

    const res = await fetch(`${WORKFLOW_SERVICE_URL}/api/v1/sagas`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}