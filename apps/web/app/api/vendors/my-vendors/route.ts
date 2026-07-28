import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const SESSION_COOKIE = 'cerp_session';

const VENDOR_SERVICE_URL = (process.env.VENDOR_SERVICE_URL || process.env.NEXT_PUBLIC_VENDOR_SERVICE_URL || 'http://localhost:8085')
  .replace(/\/$/, '');

export async function GET(request: NextRequest) {
  const targetUrl = `${VENDOR_SERVICE_URL}/vendors/my-vendors`;

  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  let token: string | null = null;

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

  try {
    const response = await fetch(targetUrl, { headers });

    if (!response.ok) {
      const errorText = await response.text();
      return NextResponse.json(
        { error: 'Failed to fetch my vendors', details: errorText },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy my-vendors request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to vendor service' },
      { status: 503 }
    );
  }
}