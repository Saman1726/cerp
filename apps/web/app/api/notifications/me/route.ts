import { NextRequest, NextResponse } from 'next/server';

const NOTIFICATION_SERVICE_URL = process.env.NOTIFICATION_SERVICE_URL || 'http://notification-service-java:8082';

function getHeaders(req: NextRequest): Record<string, string> {
  const headers: Record<string, string> = {};
  const tenantId = req.headers.get('X-Tenant-Id') || 'default';
  headers['X-Tenant-Id'] = tenantId;
  return headers;
}

export async function GET(req: NextRequest) {
  const url = new URL(req.url);
  const tenantId = req.headers.get('X-Tenant-Id') || 'default';
  const status = url.searchParams.get('status') || undefined;
  const page = url.searchParams.get('page') || '0';
  const size = url.searchParams.get('size') || '20';

  const params = new URLSearchParams({ page, size });
  if (status) params.set('status', status);

  try {
    const response = await fetch(
      `${NOTIFICATION_SERVICE_URL}/api/notifications/me?${params}`,
      {
        method: 'GET',
        headers: { 'X-Tenant-Id': tenantId },
        // Don't forward cookies/authorization to avoid CORS issues
        // The notification service uses its own auth
      }
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
  const tenantId = req.headers.get('X-Tenant-Id') || 'default';

  try {
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
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-Id': tenantId,
      },
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