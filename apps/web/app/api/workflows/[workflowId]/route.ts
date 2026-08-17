import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders, resolveUserLookup, type WorkflowStepData } from '../utils';

// Map Java WorkflowStatus to frontend WorkflowStatus
function mapWorkflowStatus(status: string): string {
  const mapping: Record<string, string> = {
    PLANNED: 'PLANNED',
    IN_PROGRESS: 'ACTIVE',
    BLOCKED: 'PAUSED',
    ON_HOLD: 'PAUSED',
    COMPLETED: 'COMPLETED',
    APPROVED: 'APPROVED',
    REJECTED: 'REJECTED',
    CANCELED: 'CANCELLED',
  };
  return mapping[status] ?? status;
}

// Map Java StepStatus to frontend WorkflowStepStatus
function mapStepStatus(status: string): string {
  const mapping: Record<string, string> = {
    NOT_STARTED: 'PENDING',
    IN_PROGRESS: 'IN_PROGRESS',
    BLOCKED: 'BLOCKED',
    COMPLETED: 'COMPLETED',
    APPROVED: 'APPROVED',
    REJECTED: 'REJECTED',
  };
  return mapping[status] ?? status;
}

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string }> },
) {
  try {
    const { workflowId } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/detail`;

    const res = await fetch(url, {
      headers: getBackendHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const data = await res.json();

    // Transform the workflow and steps to match frontend expectations
    const javaWorkflow = data.workflow as Record<string, unknown> | undefined;
    const javaSteps = (data.steps as Record<string, unknown>[]) ?? [];
    const javaNotes = (data.notes as Record<string, unknown>[]) ?? [];

    if (!javaWorkflow) {
      return NextResponse.json({ message: 'Workflow not found' }, { status: 404 });
    }

    const completedSteps = javaSteps.filter(
      (s: Record<string, unknown>) => s.status === 'COMPLETED' || s.status === 'APPROVED',
    ).length as number;
    const totalSteps = javaSteps.length as number;

    const progressPercent = javaWorkflow.progressPercent;
    let progress = 0;
    if (progressPercent !== undefined && progressPercent !== null) {
      if (typeof progressPercent === 'object' && 'toNumber' in progressPercent) {
        progress = (progressPercent as { toNumber: () => number }).toNumber();
      } else {
        progress = Number(progressPercent);
      }
    }

    const transformedWorkflow: Record<string, unknown> = {
      id: javaWorkflow.id,
      code: javaWorkflow.code,
      name: javaWorkflow.name,
      description: javaWorkflow.description,
      subprojectIds: javaWorkflow.subprojectId ? [(javaWorkflow.subprojectId as string) ?? ''] : [],
      templateId: javaWorkflow.templateId ? String(javaWorkflow.templateId) : null,
      templateCode: null,
      status: mapWorkflowStatus(String(javaWorkflow.status ?? '')),
      phaseType: '',
      currentStepId: null,
      currentStepOrder: null,
      progress,
      completedSteps,
      totalSteps,
      parameters: null,
      startedAt: javaWorkflow.startDate ? String(javaWorkflow.startDate) : null,
      completedAt: javaWorkflow.endDate ? String(javaWorkflow.endDate) : null,
      createdAt: javaWorkflow.createdAt,
      updatedAt: javaWorkflow.updatedAt,
    };

    const transformedSteps = javaSteps.map((step: Record<string, unknown>) => {
      const evidence = step.evidence;
      let evidenceUrls: string[] = [];
      if (evidence !== undefined && evidence !== null) {
        if (typeof evidence === 'string') {
          evidenceUrls = evidence
            .split(',')
            .map((u: string) => u.trim())
            .filter((u: string) => u.length > 0);
        } else {
          evidenceUrls = [String(evidence)];
        }
      }

      return {
        id: step.id,
        workflowId: javaWorkflow.id,
        templateStepId: step.templateStepId ? String(step.templateStepId) : null,
        code: step.code,
        name: step.name,
        description: (step.notes as string | undefined) ?? null,
        type: 'TASK',
        order: (step.stepOrder as number | undefined) ?? (step.order as number | undefined) ?? 0,
        status: mapStepStatus(String(step.status ?? '')),
        assignedTo: step.assigneeUserId ? String(step.assigneeUserId) : null,
        assignedToName: (step.assigneeName as string | undefined) ?? null,
        assigneeEmail: (step.assigneeEmail as string | undefined) ?? null,
        assigneeName: (step.assigneeName as string | undefined) ?? null,
        requiresEvidence: false,
        requiresApproval: false,
        evidenceUrls,
        approverId: step.approvedBy ? String(step.approvedBy) : null,
        approverName: null,
        approvalNotes: (step.notes as string | undefined) ?? null,
        startAt: (step.startedAt as string | undefined) ?? null,
        dueAt: (step.dueAt as string | undefined) ?? null,
        startedAt: (step.startedAt as string | undefined) ?? null,
        completedAt: (step.completedAt as string | undefined) ?? null,
        createdAt: (step.createdAt as string | undefined) ?? new Date().toISOString(),
        updatedAt: (step.updatedAt as string | undefined) ?? new Date().toISOString(),
        attachmentCount: step.attachmentCount as number | undefined,
      };
    });

    const transformedNotes = javaNotes.map((note: Record<string, unknown>) => ({
      id: note.id,
      workflowId: javaWorkflow.id,
      content: (note.content as string | undefined) ?? (note.note as string | undefined) ?? '',
      createdBy:
        (note.createdBy as string | undefined) ?? (note.createdByUser as string | undefined) ?? '',
      createdAt: note.createdAt,
    }));

    // Batch-resolve assignee/approver names from identity service (fallback for old data)
    const userIds = transformedSteps.flatMap((s: WorkflowStepData) => [
      s.assignedTo,
      s.approverId,
    ]).filter((id): id is string => id != null) as string[];
    const userLookup = await resolveUserLookup(userIds);
    for (const rawS of transformedSteps) {
      const s = rawS as WorkflowStepData;
      if (s.assignedTo && userLookup[s.assignedTo]) {
        const assignedUser = userLookup[s.assignedTo]!;
        // Populate missing assignee fields for backwards compatibility
        if (!s.assigneeEmail) s.assigneeEmail = assignedUser.email;
        if (!s.assigneeName) s.assigneeName = assignedUser.name;
        if (!s.assignedToName) s.assignedToName = assignedUser.name;
      }
      if (s.approverId && userLookup[s.approverId]) {
        const approverUser = userLookup[s.approverId]!;
        s.approverName = approverUser.name;
      }
    }

    return NextResponse.json({
      workflow: transformedWorkflow,
      steps: transformedSteps,
      notes: transformedNotes,
    });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string }> },
) {
  try {
    const { workflowId } = await params;
    const body = await request.json();
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}`;

    const res = await fetch(url, {
      method: 'PATCH',
      headers: getBackendHeaders(request),
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const javaWorkflow = await res.json();

    // Transform the workflow to match frontend expectations
    const transformedWorkflow = {
      id: javaWorkflow.id,
      code: javaWorkflow.code,
      name: javaWorkflow.name,
      description: javaWorkflow.description,
      subprojectIds: javaWorkflow.subprojectId ? [javaWorkflow.subprojectId.toString()] : [],
      templateId: javaWorkflow.templateId?.toString() ?? null,
      templateCode: null,
      status: mapWorkflowStatus(String(javaWorkflow.status ?? '')),
      phaseType: '',
      currentStepId: null,
      currentStepOrder: null,
      progress:
        javaWorkflow.progressPercent?.toNumber?.() ??
        (javaWorkflow.progressPercent as number | null) ??
        0,
      completedSteps: 0,
      totalSteps: 0,
      parameters: null,
      startedAt: javaWorkflow.startDate ? new javaWorkflow.startDate.toISOString() : null,
      completedAt: javaWorkflow.endDate ? new javaWorkflow.endDate.toISOString() : null,
      createdAt: javaWorkflow.createdAt,
      updatedAt: javaWorkflow.updatedAt,
    };

    return NextResponse.json(transformedWorkflow);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}
