import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { extractSessionUser } from '../../workflows/utils';

const SESSION_COOKIE = 'cerp_session';

/**
 * GET /api/vendors/me
 *
 * Returns the vendor profile of the currently authenticated user.
 * The vendor ID is resolved from the session user ID, then proxied to the
 * vendor service's /vendors/profile endpoint.
 */
export async function GET(request: NextRequest) {
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  let token: string | null = null;

  // Extract token from cookie
  const cookieHeader = request.headers.get('cookie');
  if (cookieHeader) {
    const cookiesArr = cookieHeader.split(';').map(c => c.trim());
    for (const cookie of cookiesArr) {
      const [key, ...valueParts] = cookie.split('=');
      if (key === SESSION_COOKIE && valueParts.length > 0) {
        token = valueParts.join('=');
        try {
          token = decodeURIComponent(token);
        } catch {
          // Token might not be URL-encoded, use as-is
        }
        break;
      }
    }
  }

  if (!token) {
    try {
      const cookieStore = await cookies();
      token = cookieStore.get(SESSION_COOKIE)?.value ?? null;
    } catch {
      // skip
    }
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const { userId, tenantId } = await extractSessionUser();
  headers.set('X-Tenant-Id', tenantId);
  if (userId) {
    headers.set('X-User-Id', userId);
  }

  try {
    const vendorServiceUrl = (process.env.NEXT_PUBLIC_VENDOR_SERVICE_URL || process.env.VENDOR_SERVICE_URL || 'http://vendor-service-java:8085').replace(/\/$/, '');
    const vendorProfileRes = await fetch(`${vendorServiceUrl}/vendors/profile`, { headers });

    if (!vendorProfileRes.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch vendor profile', details: await vendorProfileRes.text() },
        { status: vendorProfileRes.status }
      );
    }

    const vendorProfile = await vendorProfileRes.json();
    return NextResponse.json(vendorProfile);
  } catch (error) {
    console.error('Failed to proxy vendor/me request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to vendor service' },
      { status: 503 }
    );
  }
}