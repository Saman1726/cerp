import { NextRequest, NextResponse } from 'next/server';
import { DOCUMENT_SERVICE_URL } from '@cerp/core-web/lib/env';
import type { SiteCapturePresignRequest, SiteCapturePresignResponse } from '@cerp/shared-types';
import { getDocumentServiceHeaders } from '../utils';

// Issues a short-lived S3 presigned PUT URL so the compressed photo blob
// uploads straight from the browser to object storage - this route only ever
// sees the tiny JSON request/response, never the image itself. Shared by
// Site Capture and by workflow step attachments (both just need a
// projectId-scoped upload URL).
export async function POST(request: NextRequest) {
  const body = (await request.json().catch(() => null)) as SiteCapturePresignRequest | null;

  if (!body?.projectId || !body?.kind || !body?.fileName || !body?.contentType) {
    return NextResponse.json(
      { error: 'projectId, kind, fileName and contentType are required' },
      { status: 400 },
    );
  }

  const headers = getDocumentServiceHeaders(request);

  try {
    const response = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/media/presign`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(15000),
    });

    if (!response.ok) {
      const details = await response.text();
      return NextResponse.json(
        { error: 'Failed to obtain an upload URL', details },
        { status: response.status },
      );
    }

    const data = (await response.json()) as SiteCapturePresignResponse;
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy presign request:', error);
    return NextResponse.json({ error: 'Failed to connect to the upload service' }, { status: 503 });
  }
}
