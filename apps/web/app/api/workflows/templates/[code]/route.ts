import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getAuthHeaders } from '../../utils';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  try {
    const { code } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates/${code}`;

    const res = await fetch(url, {
      headers: getAuthHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const template = await res.json();
    const stepsUrl = `${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates/${code}/steps`;
    let steps: unknown[] = [];
    try {
      const stepsRes = await fetch(stepsUrl, {
        headers: getAuthHeaders(request),
        cache: 'no-store',
      });
      if (stepsRes.ok) {
        steps = await stepsRes.json();
      }
    } catch {
      // Ignore step fetch errors
    }
    return NextResponse.json({ template, steps });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}