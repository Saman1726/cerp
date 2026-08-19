import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const SESSION_COOKIE = 'cerp_session';

const VENDOR_SERVICE_URL = (process.env.VENDOR_SERVICE_URL || process.env.NEXT_PUBLIC_VENDOR_SERVICE_URL || 'http://localhost:8085')
  .replace(/\/$/, '');

export async function GET() {
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  let token: string | null = null;

  const cookieStore = await cookies();
  token = cookieStore.get(SESSION_COOKIE)?.value ?? null;

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const targetUrl = `${VENDOR_SERVICE_URL}/vendors/categories`;

  try {
    const response = await fetch(targetUrl, { headers, signal: AbortSignal.timeout(30000) });

    if (!response.ok) {
      const errorText = await response.text();
      return NextResponse.json(
        { error: 'Failed to fetch categories', details: errorText },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy categories request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to vendor service' },
      { status: 503 },
    );
  }
}