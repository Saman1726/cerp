import { NextRequest, NextResponse } from 'next/server';
import { extractSessionUser } from '../../workflows/utils';

export const dynamic = 'force-dynamic';

const SESSION_COOKIE = 'cerp_session';

async function getAuthHeaders(request: NextRequest): Promise<Record<string, string>> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const authHeader = request.headers.get('authorization');
  if (authHeader) headers['Authorization'] = authHeader;
  let token: string | null = null;
  const cookieHeader = request.headers.get('cookie');
  if (cookieHeader) {
    for (const cookie of cookieHeader.split(';').map((c: string) => c.trim())) {
      const [key, ...valueParts] = cookie.split('=');
      if (key === SESSION_COOKIE && valueParts.length > 0) {
        token = valueParts.join('=');
        break;
      }
    }
  }
  if (!token) {
    const cookieStore = await (await import('next/headers')).cookies();
    token = cookieStore.get(SESSION_COOKIE)?.value ?? null;
  }
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const { userId, tenantId } = await extractSessionUser();
  headers['X-Tenant-Id'] = tenantId;
  if (userId) headers['X-User-Id'] = userId;
  return headers;
}

function matchDeliverySchedulesPath(pathParts: string[]) {
  // URL pathname: /api/vendors/<vendorId>/purchase-orders/<procurementSagaId>/delivery-schedules
  // pathParts after split+filter: ['api', 'vendors', <vendorId>, 'purchase-orders', <procurementSagaId>, 'delivery-schedules']
  if (pathParts.length < 6) return null;
  if (pathParts[0] !== 'api') return null;
  if (pathParts[1] !== 'vendors') return null;
  if (pathParts[3] !== 'purchase-orders') return null;
  if (pathParts[5] !== 'delivery-schedules') return null;
  return { vendorId: pathParts[2], procurementSagaId: pathParts[4] };
}

export async function GET(request: NextRequest) {
  try {
    const headers = await getAuthHeaders(request);
    const wfUrl = (process.env.NEXT_PUBLIC_WORKFLOW_SERVICE_URL || process.env.WORKFLOW_SERVICE_URL || 'http://workflow-service-java:8089').replace(/\/$/, '');
    const path = new URL(request.url).pathname;
    const target = path.startsWith('/api/vendors')
      ? '/api/v1/sagas'
      : path.replace(/^\/api\//, '/api/v1/');
    const res = await fetch(`${wfUrl}${target}`, {
      headers,
      signal: AbortSignal.timeout(30000),
    });
    if (!res.ok) {
      const data = await res.text().catch(() => '');
      return NextResponse.json({ error: 'Failed to fetch', details: data }, { status: res.status });
    }
    return NextResponse.json(await res.json());
  } catch (error) {
    console.error('[vendors proxy] GET failed:', error);
    return NextResponse.json({ error: 'Failed to connect' }, { status: 503 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const wfUrl = (process.env.WORKFLOW_SERVICE_URL || 'http://localhost:8089').replace(/\/$/, '');
    const headers = await getAuthHeaders(request);
    const body = await request.json();
    const url = new URL(request.url);
    const pathParts = url.pathname.split('/').filter(Boolean);
    const match = matchDeliverySchedulesPath(pathParts);
    if (!match) {
      return NextResponse.json(
        { error: 'Invalid path' },
        { status: 400 },
      );
    }
    const deliveryRequest = {
      sagaType: 'DELIVERY_WORKFLOW',
      data: {
        userId: headers['X-User-Id'],
        tenantId: headers['X-Tenant-Id'],
        assignedTo: headers['X-User-Id'],
        procurementSagaId: match.procurementSagaId,
        vendorId: match.vendorId,
        partyIds: [headers['X-User-Id'], match.vendorId],
        scheduledDate: body.scheduledDate,
        quantity: body.quantity,
        expectedArrivalTime: body.expectedArrivalTime,
        description: body.description,
      },
    };
    const res = await fetch(`${wfUrl}/api/v1/sagas`, {
      method: 'POST',
      headers,
      body: JSON.stringify(deliveryRequest),
      signal: AbortSignal.timeout(30000),
    });
    if (!res.ok) {
      const data = await res.text().catch(() => '');
      return NextResponse.json(
        { error: 'Failed to create delivery schedule', details: data.slice(0, 500) },
        { status: res.status },
      );
    }
    const data = JSON.parse(await res.text());
    return NextResponse.json({ success: true, deliverySagaId: data?.sagaId ?? data?.id });
  } catch (error) {
    console.error('[vendors proxy] POST failed:', error);
    return NextResponse.json({ error: 'Failed to create delivery schedule', details: String(error) }, { status: 503 });
  }
}

export async function PATCH(request: NextRequest) {
  try {
    const wfUrl = (process.env.WORKFLOW_SERVICE_URL || 'http://localhost:8089').replace(/\/$/, '');
    const headers = await getAuthHeaders(request);
    const body = await request.json();
    const url = new URL(request.url);
    const pathParts = url.pathname.split('/').filter(Boolean);

    // Match: /api/sagas/<sagaId>/execute
    if (pathParts.length >= 4 && pathParts[0] === 'api' && pathParts[1] === 'sagas' && pathParts[3] === 'execute') {
      const sagaId = pathParts[2];
      const executeRequest = {
        command: body.command,
        data: body.data,
        idempotencyKey: body.idempotencyKey,
      };
      const res = await fetch(`${wfUrl}/api/v1/sagas/${sagaId}/execute`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify(executeRequest),
        signal: AbortSignal.timeout(30000),
      });
      if (!res.ok) {
        const data = await res.text().catch(() => '');
        return NextResponse.json(
          { error: 'Failed to execute step', details: data.slice(0, 500) },
          { status: res.status },
        );
      }
      const data = await res.json();
      return NextResponse.json(data);
    }

    return NextResponse.json({ error: 'Invalid path' }, { status: 400 });
  } catch (error) {
    console.error('[vendors proxy] PATCH failed:', error);
    return NextResponse.json({ error: 'Failed to execute step', details: String(error) }, { status: 503 });
  }
}
