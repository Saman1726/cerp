import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';

async function workflowFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const headersInit = new Headers(init?.headers);
  headersInit.set('Content-Type', 'application/json');

  const fullUrl = `${WORKFLOW_SERVICE_URL}${path}`;

  const res = await fetch(fullUrl, { ...init, headers: headersInit, cache: 'no-store' });

  if (!res.ok) {
    const body = await res.json().catch(() => undefined);
    throw new Error(body?.message ?? res.statusText);
  }

  return res.json();
}

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const templateCode = searchParams.get('templateCode');
    const projectId = searchParams.get('projectId');
    const subprojectId = searchParams.get('subprojectId');

    const url = new URL('/api/v1/workflows', WORKFLOW_SERVICE_URL);
    if (templateCode) url.searchParams.set('templateCode', templateCode);
    if (projectId) url.searchParams.set('projectId', projectId);
    if (subprojectId) url.searchParams.set('subprojectId', subprojectId);

    const res = await fetch(url.toString(), {
      headers: { 'Content-Type': 'application/json' },
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
    const body = await request.json();
    const res = await fetch(`${WORKFLOW_SERVICE_URL}/api/v1/workflows`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
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