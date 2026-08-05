import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders, extractSessionUser } from '../../workflows/utils';

const CHANNEL = 'open_tenders';

async function buildHeaders(request: NextRequest) {
  const { tenantId } = await extractSessionUser();
  const headers = getBackendHeaders(request) as Record<string, string>;
  headers['X-Tenant-Id'] = tenantId;
  return headers;
}

/** GET /api/open-tenders/subscription — check whether the current user is subscribed */
export async function GET(request: NextRequest) {
  try {
    const headers = await buildHeaders(request);
    const res = await fetch(
      `${WORKFLOW_SERVICE_URL}/api/v1/bidding/channels/${CHANNEL}/subscription`,
      { headers, cache: 'no-store' }
    );
    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json({ message: body?.message ?? res.statusText }, { status: res.status });
    }
    return NextResponse.json(await res.json());
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

/** POST /api/open-tenders/subscription — subscribe to the open tenders channel */
export async function POST(request: NextRequest) {
  try {
    const headers = await buildHeaders(request);
    const res = await fetch(
      `${WORKFLOW_SERVICE_URL}/api/v1/bidding/channels/${CHANNEL}/subscription`,
      { method: 'POST', headers, cache: 'no-store' }
    );
    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json({ message: body?.message ?? res.statusText }, { status: res.status });
    }
    return NextResponse.json(await res.json());
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

/** DELETE /api/open-tenders/subscription — unsubscribe from the open tenders channel */
export async function DELETE(request: NextRequest) {
  try {
    const headers = await buildHeaders(request);
    const res = await fetch(
      `${WORKFLOW_SERVICE_URL}/api/v1/bidding/channels/${CHANNEL}/subscription`,
      { method: 'DELETE', headers, cache: 'no-store' }
    );
    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json({ message: body?.message ?? res.statusText }, { status: res.status });
    }
    return NextResponse.json(await res.json());
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}
