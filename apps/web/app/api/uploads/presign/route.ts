import { NextRequest, NextResponse } from 'next/server';
import { DOCUMENT_SERVICE_URL } from '@cerp/core-web/lib/env';
import type { SiteCapturePresignRequest, SiteCapturePresignResponse } from '@cerp/shared-types';

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

// Issues a short-lived S3 presigned PUT URL so the compressed photo blob
// uploads straight from the browser to object storage - this route only ever
// sees the tiny JSON request/response, never the image itself.
export async function POST(request: NextRequest) {
  const body = (await request.json().catch(() => null)) as SiteCapturePresignRequest | null;

  if (!body?.projectId || !body?.kind || !body?.fileName || !body?.contentType) {
    return NextResponse.json(
      { error: 'projectId, kind, fileName and contentType are required' },
      { status: 400 },
    );
  }

  const headers = new Headers({ 'Content-Type': 'application/json' });
  const token = extractSessionToken(request);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  headers.set('X-Tenant-Id', request.headers.get('x-tenant-id') ?? DEFAULT_TENANT_ID);

  try {
    const response = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/media/presign`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(15000),
    });

    if (!response.ok) {
      const details = await response.text();
      return NextResponse.json({ error: 'Failed to obtain an upload URL', details }, { status: response.status });
    }

    const data = (await response.json()) as SiteCapturePresignResponse;
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy presign request:', error);
    return NextResponse.json({ error: 'Failed to connect to the upload service' }, { status: 503 });
  }
}
