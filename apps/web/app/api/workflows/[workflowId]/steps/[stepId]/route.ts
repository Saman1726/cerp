import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getAuthHeaders, resolveUserIdByEmail } from '../../../utils';

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string; stepId: string }> }
) {
  try {
    const { workflowId, stepId } = await params;
    const body = await request.json();
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/steps/${stepId}`;

    // Resolve assignee: if it looks like an email, look up the UUID via identity service
    const assigneeInput = body?.assigneeUserId ?? body?.assignedTo;
    if (assigneeInput && typeof assigneeInput === 'string') {
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
      if (!uuidRegex.test(assigneeInput)) {
        const resolvedId = await resolveUserIdByEmail(assigneeInput);
        if (!resolvedId) {
          return NextResponse.json(
            { message: `User not found with email: ${assigneeInput}` },
            { status: 400 }
          );
        }
        body.assigneeUserId = resolvedId;
        body.assignedTo = resolvedId;
      }
    }

    const res = await fetch(url, {
      method: 'PATCH',
      headers: getAuthHeaders(request),
      body: JSON.stringify(body),
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