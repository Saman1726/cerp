import { NextRequest, NextResponse } from 'next/server';
import { extractSessionUser } from '../../workflows/utils';

const NOTIFICATION_SERVICE_URL = process.env.NOTIFICATION_SERVICE_URL || 'http://localhost:8091';

async function getHeaders(req: NextRequest): Promise<Record<string, string>> {
  const { userId, tenantId } = await extractSessionUser();
  console.log('[notifications] session resolved: userId=%s, tenantId=%s', userId, tenantId);
  const headers: Record<string, string> = {
    'X-Tenant-Id': tenantId,
  };
  if (userId) {
    headers['X-User-Id'] = userId;
  }
  return headers;
}

export async function GET(req: NextRequest) {
  const url = new URL(req.url);
  const status = url.searchParams.get('status') || undefined;
  const page = url.searchParams.get('page') || '0';
  const size = url.searchParams.get('size') || '20';

  const params = new URLSearchParams({ page, size });
  if (status) params.set('status', status);

  try {
    const headers = await getHeaders(req);
    const response = await fetch(
      `${NOTIFICATION_SERVICE_URL}/api/notifications/me?${params}`,
      { method: 'GET', headers }
    );

    if (!response.ok) {
      const errorText = await response.text();
      console.error('[notifications proxy] Backend error:', response.status, errorText);
      return NextResponse.json(
        { error: 'Notification service error', details: errorText },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('[notifications proxy] Failed to proxy request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to notification service' },
      { status: 502 }
    );
  }
}

export async function POST(req: NextRequest) {
  const url = new URL(req.url);
  const pathSegments = url.pathname.split('/').filter(Boolean); // ['api', 'notifications', 'me', ...]

  // Extract notification ID from path: /api/notifications/me/{id}/ack, /api/notifications/me/{id}/action
  const notificationId = pathSegments[3];
  const action = pathSegments[4]; // 'ack' or 'action'

  if (!notificationId || !action) {
    return NextResponse.json(
      { error: 'Missing notification ID or action' },
      { status: 400 }
    );
  }

  const body = await req.json().catch(() => ({}));

  try {
    const headers = await getHeaders(req);
    headers['Content-Type'] = 'application/json';

    let endpoint: string;
    let method: string;

    if (action === 'ack') {
      endpoint = `${NOTIFICATION_SERVICE_URL}/api/notifications/me/${notificationId}/ack`;
      method = 'POST';
    } else if (action === 'action') {
      endpoint = `${NOTIFICATION_SERVICE_URL}/api/notifications/me/${notificationId}/action`;
      method = 'POST';
    } else {
      return NextResponse.json(
        { error: 'Unknown action' },
        { status: 400 }
      );
    }

    const response = await fetch(endpoint, {
      method,
      headers,
      body: action === 'action' ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error('[notifications proxy] Backend error:', response.status, errorText);
      return NextResponse.json(
        { error: 'Notification service error', details: errorText },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('[notifications proxy] Failed to proxy request:', error);
    return NextResponse.json(
      { error: 'Failed to connect to notification service' },
      { status: 502 }
    );
  }
}
