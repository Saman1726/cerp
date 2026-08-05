import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders, extractSessionUser } from '../../workflows/utils';

/**
 * Proxy /api/sagas/* requests to the workflow service.
 *
 * The frontend apiClient routes /api/sagas to WORKFLOW_SERVICE_URL
 * (http://workflow-service-java:8089 in Docker). Since NEXT_PUBLIC_
 * variables are baked into the browser bundle, the browser can't
 * resolve Docker service names. This route acts as a server-side
 * proxy so the browser only needs to fetch from the relative path.
 */

function proxyPath(request: NextRequest): string {
  const url = new URL(request.url);
  const path = url.pathname;
  // Remove /api/sagas prefix to get the saga-specific path
  // e.g., /api/sagas?page=1 → /api/v1/sagas (base endpoint with query params)
  // e.g., /api/sagas/abc-123 → /api/v1/sagas/abc-123
  // e.g., /api/sagas/abc-123/events → /api/v1/sagas/abc-123/events
  const suffix = path.replace(/^\/api\/sagas/, '');
  return `/api/v1/sagas${suffix}`;
}

export async function GET(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const { searchParams } = new URL(request.url);

    const targetUrl = new URL(sagaPath, WORKFLOW_SERVICE_URL);
    // Forward query parameters
    searchParams.forEach((value, key) => {
      targetUrl.searchParams.set(key, value);
    });

    const headers = getBackendHeaders(request);

    const res = await fetch(targetUrl.toString(), {
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json({ message: body?.message ?? res.statusText }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const body = await request.json();

    // Inject userId and tenantId into the request body for saga creation.
    // The backend SagaContext expects userId and projectId inside `data`,
    // and tenantId at the top level of StartSagaRequest.
    const { userId, tenantId } = await extractSessionUser();

    // Always override tenantId (the form passes tenantId="" which is falsy)
    if (body && typeof body === 'object') {
      (body as Record<string, unknown>).tenantId = tenantId;
      // Inject userId into data if not already present
      if (userId) {
        const data = (body.data as Record<string, unknown> | undefined) ?? {};
        if (!data.userId) {
          data.userId = userId;
          body.data = data;
        }
      }
    }

    const headers = getBackendHeaders(request);
    // Inject X-Tenant-Id header so the workflow service's TenantRequestFilter sets TenantContext
    (headers as Record<string, string>)['X-Tenant-Id'] = tenantId;

    const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

export async function PUT(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const body = await request.json();
    const headers = getBackendHeaders(request);

    const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const headers = getBackendHeaders(request);

    const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
      method: 'DELETE',
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    return new NextResponse(null, { status: res.status });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}