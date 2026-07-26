import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const SESSION_COOKIE = 'cerp_session';

// Use VENDOR_SERVICE_URL for server-side (docker-compose sets this to Docker service name)
// Fall back to NEXT_PUBLIC_ variant for local development
const VENDOR_SERVICE_URL = (process.env.VENDOR_SERVICE_URL || process.env.NEXT_PUBLIC_VENDOR_SERVICE_URL || 'http://localhost:8085')
  .replace(/\/$/, '');  // Remove trailing slash if present

export async function GET(request: NextRequest) {
  const url = new URL(request.url);
  const queryString = url.search;
  const targetUrl = `${VENDOR_SERVICE_URL}/vendors${queryString}`;

  // Build headers to forward the authenticated request
  const headers = new Headers();
  headers.set('Content-Type', 'application/json');

  // Extract the session token from the cookie and forward it to the backend.
  // The browser automatically sends httpOnly cookies on same-site requests,
  // but NextRequest.headers.get('cookie') returns them as a raw string.
  let token: string | null = null;

  // Method 1: Parse the raw Cookie header from the incoming request
  const cookieHeader = request.headers.get('cookie');
  if (cookieHeader) {
    const cookies = cookieHeader.split(';').map(c => c.trim());
    for (const cookie of cookies) {
      const [key, ...valueParts] = cookie.split('=');
      if (key === SESSION_COOKIE && valueParts.length > 0) {
        token = valueParts.join('='); // Handle tokens containing '='
        // Decode URL-encoded cookie values
        try {
          token = decodeURIComponent(token);
        } catch {
          // Token might not be URL-encoded, use as-is
        }
        break;
      }
    }
  }

  // Method 2: Fall back to Next.js cookies() API if raw parsing didn't work
  if (!token) {
    try {
      const cookieStore = await cookies();
      const rawToken = cookieStore.get(SESSION_COOKIE)?.value;
      if (rawToken) {
        token = rawToken;
      }
    } catch {
      // cookies() is not available in all server contexts, skip silently
    }
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
    console.log('[vendor-proxy] Forwarding auth with token sub:', token.slice(0, 20) + '...');
  } else {
    console.log('[vendor-proxy] WARNING: No session token found');
  }

  try {
    const response = await fetch(targetUrl, {
      method: 'GET',
      headers,
    });

    if (!response.ok) {
      const errorText = await response.text();
      return NextResponse.json(
        { error: 'Failed to fetch vendors', details: errorText },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Failed to proxy vendor request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to vendor service' },
      { status: 503 }
    );
  }
}