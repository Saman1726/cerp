import { NextRequest, NextResponse } from 'next/server';

const SESSION_COOKIE = 'cerp_session';

const PROJECT_SERVICE_URL = (process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL || process.env.PROJECT_SERVICE_URL || 'http://localhost:8085')
  .replace(/\/$/, '');

export async function GET(request: NextRequest) {
  const { searchParams } = new URL(request.url);
  const path = searchParams.toString()
    ? `/api/v1/projects?${searchParams.toString()}`
    : '/api/v1/projects';
  const targetUrl = `${PROJECT_SERVICE_URL}${path}`;

  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  // Get tenant ID from cookie or default
  const tenantId = request.headers.get('x-tenant-id') ?? 'default';
  headers.set('X-Tenant-Id', tenantId);

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

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  try {
    const response = await fetch(targetUrl, { headers, signal: AbortSignal.timeout(30000) });

    if (!response.ok) {
      const errorText = await response.text();
      return NextResponse.json(
        { error: 'Failed to fetch projects', details: errorText },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy projects request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to project service' },
      { status: 503 }
    );
  }
}