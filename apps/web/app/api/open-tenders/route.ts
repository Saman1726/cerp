import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders, extractSessionUser } from '../workflows/utils';

/** GET /api/open-tenders — list all active open tenders in the channel */
export async function GET(request: NextRequest) {
  try {
    const { tenantId } = await extractSessionUser();
    const headers = getBackendHeaders(request) as Record<string, string>;
    headers['X-Tenant-Id'] = tenantId;

    const res = await fetch(`${WORKFLOW_SERVICE_URL}/api/v1/bidding/open-tenders`, {
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: body?.message ?? res.statusText },
        { status: res.status }
      );
    }

    return NextResponse.json(await res.json());
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}
