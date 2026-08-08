import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL, IDENTITY_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders } from '../workflows/utils';

// Bid wallets only distinguish PROJECT_OWNER vs VENDOR (BidWallet.EntityType); every
// non-vendor account type (USER/COMPANY) maps to PROJECT_OWNER.
// GET /users/{id} requires authentication (SecurityConfig: `GET /users/**` -> authenticated()),
// unlike `GET /users?email=`, so the caller's own bearer token must be forwarded.
async function resolveEntityType(userId: string, accessToken: string): Promise<'VENDOR' | 'PROJECT_OWNER' | null> {
  try {
    const res = await fetch(`${IDENTITY_SERVICE_URL}/users/${encodeURIComponent(userId)}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
      cache: 'no-store',
    });
    if (!res.ok) return null;
    const data = (await res.json()) as Record<string, unknown>;
    return data.type === 'VENDOR' ? 'VENDOR' : 'PROJECT_OWNER';
  } catch {
    return null;
  }
}

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

    if (token) {
      const payload = decodeJwtPayload(token);
      if (payload) {
        // The session cookie holds a real Keycloak access token: its own "sub" is
        // Keycloak's internal id, which never matches identity.user_accounts.id (see
        // AuthController.refresh in identity-service-java). "internal_user_id" is the
        // custom claim identity-service maps for exactly this purpose.
        userId =
          payload.internal_user_id && typeof payload.internal_user_id === 'string'
            ? payload.internal_user_id
            : null;
        if (payload.tenantCode && typeof payload.tenantCode === 'string' && payload.tenantCode.trim()) {
          tenantCode = payload.tenantCode;
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

    // Auto-fill entityType from the user's account type when not provided by the
    // client - the Keycloak token carries no app-level role/type claim to derive
    // this from.
    if (!targetUrl.searchParams.has('entityType') && userId && token) {
      const entityType = await resolveEntityType(userId, token);
      if (entityType) {
        targetUrl.searchParams.set('entityType', entityType);
      }
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
          if (decoded.internal_user_id && typeof decoded.internal_user_id === 'string') {
            sessionUserId = decoded.internal_user_id;
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