import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getAuthHeaders, extractSessionUser } from '../../../../utils';

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string; stepId: string }> }
) {
  try {
    const { workflowId, stepId } = await params;
    const body = await request.json().catch(() => ({}));
    const { userId } = await extractSessionUser();
    if (!userId) {
      return NextResponse.json({ message: 'Could not resolve the current user' }, { status: 401 });
    }
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/steps/${stepId}/approve`;

    const res = await fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(request),
      body: JSON.stringify({ userId, notes: body.notes || undefined }),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const step = await res.json();
    return NextResponse.json(step);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}
