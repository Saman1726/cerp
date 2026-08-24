import { NextRequest, NextResponse } from 'next/server';

const SESSION_COOKIE = 'cerp_session';
const DEFAULT_TENANT_ID = '00000000-0000-0000-0000-000000000001';

const PROJECT_SERVICE_URL = (process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL || process.env.PROJECT_SERVICE_URL || 'http://localhost:8085')
  .replace(/\/$/, '');

export async function GET(request: NextRequest) {
  const targetUrl = `${PROJECT_SERVICE_URL}/api/v1/construction-sites`;
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  const tenantId = request.headers.get('x-tenant-id') ?? DEFAULT_TENANT_ID;
  headers.set('X-Tenant-Id', tenantId);

  let token: string | null = null;
  const cookieHeader = request.headers.get('cookie');
  if (cookieHeader) {
    for (const cookie of cookieHeader.split(';').map(c => c.trim())) {
      const [key, ...valueParts] = cookie.split('=');
      if (key === SESSION_COOKIE && valueParts.length > 0) {
        token = valueParts.join('=');
        try { token = decodeURIComponent(token); } catch {}
        break;
      }
    }
  }
  if (!token) {
    const cookieStore = await (await import('next/headers')).cookies();
    token = cookieStore.get(SESSION_COOKIE)?.value ?? null;
  }
  if (token) headers.set('Authorization', `Bearer ${token}`);

  try {
    const res = await fetch(targetUrl, { headers, signal: AbortSignal.timeout(30000) });
    if (!res.ok) {
      const details = await res.text().catch(() => '');
      return NextResponse.json({ error: 'Failed to fetch construction sites', details }, { status: res.status });
    }
    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('[construction-sites proxy] GET failed:', error);
    return NextResponse.json({ error: 'Failed to connect to project service' }, { status: 503 });
  }
}

export async function POST(request: NextRequest) {
  const targetUrl = `${PROJECT_SERVICE_URL}/api/v1/construction-sites`;
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  const tenantId = request.headers.get('x-tenant-id') ?? DEFAULT_TENANT_ID;
  headers.set('X-Tenant-Id', tenantId);

  let token: string | null = null;
  const cookieHeader = request.headers.get('cookie');
  if (cookieHeader) {
    for (const cookie of cookieHeader.split(';').map(c => c.trim())) {
      const [key, ...valueParts] = cookie.split('=');
      if (key === SESSION_COOKIE && valueParts.length > 0) {
        token = valueParts.join('=');
        try { token = decodeURIComponent(token); } catch {}
        break;
      }
    }
  }
  if (!token) {
    const cookieStore = await (await import('next/headers')).cookies();
    token = cookieStore.get(SESSION_COOKIE)?.value ?? null;
  }
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const body = await request.text();

  try {
    const res = await fetch(targetUrl, { method: 'POST', headers, body, signal: AbortSignal.timeout(30000) });
    if (!res.ok) {
      const details = await res.text().catch(() => '');
      return NextResponse.json({ error: 'Failed to create construction site', details }, { status: res.status });
    }
    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('[construction-sites proxy] POST failed:', error);
    return NextResponse.json({ error: 'Failed to connect to project service' }, { status: 503 });
  }
}