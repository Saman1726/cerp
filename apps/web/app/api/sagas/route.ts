import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders, extractSessionUser } from '../workflows/utils';

/**
 * Proxy /api/sagas (base endpoint) requests to the workflow service.
 *
 * Handles GET and POST to the sagas list/create endpoint.
 * Specific saga operations are handled by [...path]/route.ts.
 */

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);

    const { userId, tenantId, roles } = await extractSessionUser();

    const targetUrl = new URL('/api/v1/sagas', WORKFLOW_SERVICE_URL);
    searchParams.forEach((value, key) => {
      targetUrl.searchParams.set(key, value);
    });
    if (userId) {
      targetUrl.searchParams.set('userId', userId);
    }

    const headers = getBackendHeaders(request) as Record<string, string>;
    headers['X-Tenant-Id'] = tenantId;
    if (roles.length > 0) {
      headers['X-User-Roles'] = roles.join(',');
    }

    const res = await fetch(targetUrl.toString(), {
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json({ message: body?.message ?? res.statusText }, { status: res.status });
    }

    const data = await res.json();

    // Filter by vendorId if provided
    const vendorId = searchParams.get('vendorId');
    if (vendorId && data && typeof data === 'object' && 'items' in data) {
      const items = Array.isArray(data.items) ? data.items : [];
      console.log('[sagas] Filtering by vendorId:', vendorId, 'Total items:', items.length);
      const filtered = items.filter((item: Record<string, unknown>) => {
        // Vendor ID lives inside the saga's data object
        const sagaData = (item.data ?? {}) as Record<string, unknown>;
        const itemVendorId = sagaData.vendorId;
        if (items.indexOf(item) < 5) { // Log first 5 for debugging
          console.log('[sagas] Saga vendorId check:', { sagaId: item.sagaId, itemVendorId, match: itemVendorId === vendorId });
        }
        return itemVendorId === vendorId;
      });
      console.log('[sagas] After filter:', filtered.length);
      data.items = filtered;
      // Update total count if present
      if (typeof data.totalCount === 'number') {
        data.totalCount = filtered.length;
      }
    }

    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    const { userId, tenantId, roles } = await extractSessionUser();

    if (body && typeof body === 'object') {
      (body as Record<string, unknown>).tenantId = tenantId;
      if (userId) {
        const data = (body.data as Record<string, unknown> | undefined) ?? {};
        if (!data.userId) {
          data.userId = userId;
          body.data = data;
        }
      }
    }

    const headers = getBackendHeaders(request) as Record<string, string>;
    headers['X-Tenant-Id'] = tenantId;
    if (roles.length > 0) {
      headers['X-User-Roles'] = roles.join(',');
    }

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

    return NextResponse.json(await res.json());
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}
