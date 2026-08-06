import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders } from '../workflows/utils';

/**
 * Proxy /api/bidding requests to the workflow service.
 *
 * Handles:
 * - GET /api/bidding/wallet/balance - Get bid wallet balance
 * - GET /api/bidding/wallet/transactions - Get bid transaction history
 * - POST /api/bidding/wallet/allocate - Allocate bids to a user wallet (admin)
 * - GET /api/bidding/wallet/user - Get wallet by user ID (admin)
 * - POST /api/bidding/submissions - Submit a vendor bid
 * - GET /api/bidding/submissions - Get bids for a tender
 */

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const path = searchParams.get('_path') || '';

    // Extract userId from session token
    const { cookies } = await import('next/headers');
    const cookieStore = await cookies();
    const token = cookieStore.get('cerp_session')?.value;

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

    const DEFAULT_TENANT_CODE = 'default';
    let userId: string | null = null;
    let tenantCode = DEFAULT_TENANT_CODE;
    let jwtRole: string | null = null;

    if (token) {
      const payload = decodeJwtPayload(token);
      if (payload) {
        userId = payload.sub && typeof payload.sub === 'string' ? payload.sub : null;
        if (payload.tenantCode && typeof payload.tenantCode === 'string' && payload.tenantCode.trim()) {
          tenantCode = payload.tenantCode;
        }
        const rolesField = payload.roles;
        if (typeof rolesField === 'string') {
          const roleList = rolesField
            .split(',')
            .map((r) => r.trim().toLowerCase())
            .filter(Boolean);
          jwtRole = roleList.includes('vendor') ? 'vendor' : (roleList[0] ?? null);
        }
      }
    }

    // Build the target URL to the workflow service
    const targetUrl = new URL(`/api/v1/bidding/${path}`, WORKFLOW_SERVICE_URL);

    // Forward query parameters (but not _path)
    searchParams.forEach((value, key) => {
      if (key !== '_path') {
        targetUrl.searchParams.set(key, value);
      }
    });

    // Auto-fill entityType from JWT role when not provided by the client
    if (!targetUrl.searchParams.has('entityType') && jwtRole) {
      targetUrl.searchParams.set('entityType', jwtRole === 'vendor' ? 'VENDOR' : 'PROJECT_OWNER');
    }

    // Pass userId as query parameter for backend access control
    if (userId) {
      targetUrl.searchParams.set('userId', userId);
    }

    const headers = getBackendHeaders(request);
    (headers as Record<string, string>)['X-Tenant-Id'] = tenantCode;

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
    const body = await request.json() as Record<string, unknown>;
    const { searchParams } = new URL(request.url);
    const path = searchParams.get('_path') || 'submissions';

    // Extract tenantId and userId from session
    const { cookies } = await import('next/headers');
    const cookieStore = await cookies();
    const token = cookieStore.get('cerp_session')?.value;

    const DEFAULT_TENANT_CODE = 'default';
    let tenantCode = DEFAULT_TENANT_CODE;
    let sessionUserId: string | null = null;

    if (token) {
      try {
        const parts = token.split('.');
        if (parts.length === 3 && parts[1]) {
          const payload = parts[1];
          const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
          const json = Buffer.from(base64, 'base64').toString('utf-8');
          const decoded = JSON.parse(json) as Record<string, unknown>;
          if (decoded.tenantCode && typeof decoded.tenantCode === 'string' && decoded.tenantCode.trim()) {
            tenantCode = decoded.tenantCode;
          }
          if (decoded.sub && typeof decoded.sub === 'string') {
            sessionUserId = decoded.sub;
          }
        }
      } catch {
        // Use default tenant code
      }
    }

    const headers = getBackendHeaders(request);
    (headers as Record<string, string>)['X-Tenant-Id'] = tenantCode;

    // For wallet/allocate: auto-inject session userId if the caller didn't provide one
    const effectiveBody =
      path === 'wallet/allocate' && !body.userId && sessionUserId
        ? { ...body, userId: sessionUserId }
        : body;

    const res = await fetch(`${WORKFLOW_SERVICE_URL}/api/v1/bidding/${path}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(effectiveBody),
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