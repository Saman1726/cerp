import { NextRequest, NextResponse } from 'next/server';
import { DOCUMENT_SERVICE_URL } from '@cerp/core-web/lib/env';
import type { MediaPresignDownloadRequest, MediaPresignDownloadResponse } from '@cerp/shared-types';
import { getDocumentServiceHeaders } from '../utils';

// Issues a short-lived S3 presigned GET URL so an already-uploaded object can be viewed/
// downloaded. Symmetric to /api/uploads/presign (which only ever mints upload URLs) - neither
// route touches the object's bytes, only the presigned URL.
export async function POST(request: NextRequest) {
  const body = (await request.json().catch(() => null)) as MediaPresignDownloadRequest | null;

  if (!body?.objectKey) {
    return NextResponse.json({ error: 'objectKey is required' }, { status: 400 });
  }

  const headers = getDocumentServiceHeaders(request);

  try {
    const response = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/media/presign-download`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(15000),
    });

    if (!response.ok) {
      const details = await response.text();
      return NextResponse.json(
        { error: 'Failed to obtain a download URL', details },
        { status: response.status },
      );
    }

    const data = (await response.json()) as MediaPresignDownloadResponse;
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy presign-download request:', error);
    return NextResponse.json({ error: 'Failed to connect to the upload service' }, { status: 503 });
  }
}
