import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import type { WorkflowNoteCreateRequest } from '@cerp/shared-types';
import { getAuthHeaders, extractSessionUser, toFrontendNote } from '../../../../utils';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string; stepId: string }> },
) {
  try {
    const { workflowId, stepId } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/notes?stepId=${stepId}`;

    const res = await fetch(url, {
      headers: getAuthHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const notes = await res.json();
    return NextResponse.json(
      notes.map((note: Parameters<typeof toFrontendNote>[0]) => toFrontendNote(note, workflowId)),
    );
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string; stepId: string }> },
) {
  try {
    const { workflowId, stepId } = await params;
    const body = (await request.json()) as WorkflowNoteCreateRequest;
    const { userId } = await extractSessionUser();
    if (!userId) {
      return NextResponse.json({ message: 'Could not resolve the current user' }, { status: 401 });
    }

    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/notes`;
    const res = await fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(request),
      // Backend expects { userId, stepId, note, type }, not the frontend's { content, type? }.
      body: JSON.stringify({ userId, stepId, note: body.content, type: body.type ?? 'GENERAL' }),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const note = await res.json();
    return NextResponse.json(toFrontendNote(note, workflowId));
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}
