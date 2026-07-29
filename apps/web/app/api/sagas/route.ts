import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders } from '../workflows/utils';

/**
 * Proxy /api/sagas (base endpoint) requests to the workflow service.
 *
 * Handles GET and POST to the sagas list/create endpoint.
 * Specific saga operations are handled by [...path]/route.ts.
 */

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);

    const targetUrl = new URL('/api/v1/sagas', WORKFLOW_SERVICE_URL);
    // Forward query parameters
    searchParams.forEach((value, key) => {
      targetUrl.searchParams.set(key, value);
    });

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
    const headers = getBackendHeaders(request);

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