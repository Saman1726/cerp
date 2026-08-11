import { NextRequest, NextResponse } from 'next/server';
import { DOCUMENT_SERVICE_URL } from '@cerp/core-web/lib/env';
import type { SiteCaptureSubmission, SiteCaptureSubmissionResponse } from '@cerp/shared-types';

const SESSION_COOKIE = 'cerp_session';
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

// Forwards the lightweight capture metadata (now including the object key
// from the presign step) to document-service. The image binary itself never
// passes through here - it already went straight from the browser to storage.
export async function POST(request: NextRequest) {
  const body = (await request.json().catch(() => null)) as SiteCaptureSubmission | null;

  if (!body?.clientId || !body?.projectId || !body?.kind || !body?.objectKey) {
    return NextResponse.json(
      { error: 'clientId, projectId, kind and objectKey are required' },
      { status: 400 },
    );
  }

  const headers = new Headers({ 'Content-Type': 'application/json' });
  const token = extractSessionToken(request);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  headers.set('X-Tenant-Id', request.headers.get('x-tenant-id') ?? DEFAULT_TENANT_ID);

  try {
    const response = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/site-captures`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      // clientId makes retries idempotent on the backend, so a long timeout
      // here is safe - worst case is a duplicate-safe retry from the queue.
      signal: AbortSignal.timeout(15000),
    });

    if (!response.ok) {
      const details = await response.text();
      return NextResponse.json({ error: 'Failed to submit capture', details }, { status: response.status });
    }

    const data = (await response.json()) as SiteCaptureSubmissionResponse;
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy site-capture submission:', error);
    return NextResponse.json({ error: 'Failed to connect to the capture service' }, { status: 503 });
  }
}
