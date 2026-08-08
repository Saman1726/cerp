import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getAuthHeaders } from '../../utils';

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string }> }
) {
  try {
    const { workflowId } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/complete`;

    const res = await fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const workflow = await res.json();
    return NextResponse.json(workflow);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}
