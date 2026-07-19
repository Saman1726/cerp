import { NextRequest } from 'next/server';

const SESSION_COOKIE = 'cerp_session';

export function getAuthHeaders(request: NextRequest): HeadersInit {
  const headers: HeadersInit = { 'Content-Type': 'application/json' };

  // Forward Authorization header if present (from client-side token)
  const authHeader = request.headers.get('authorization');
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }

  // Forward cookie-based auth (session cookie pattern used in the app)
  const sessionCookie = request.cookies.get(SESSION_COOKIE)?.value;
  if (sessionCookie) {
    headers['Cookie'] = `${SESSION_COOKIE}=${sessionCookie}`;
  }

  return headers;
}
