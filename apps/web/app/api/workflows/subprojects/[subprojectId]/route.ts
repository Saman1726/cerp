import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getAuthHeaders } from '../../utils';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ subprojectId: string }> }
) {
  try {
    const { subprojectId } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows?subprojectId=${subprojectId}`;

    const res = await fetch(url, {
      headers: getAuthHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const workflows = await res.json();
    return NextResponse.json(workflows);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}