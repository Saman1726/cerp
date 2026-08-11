import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders, extractSessionUser } from './utils';

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

function transformStep(
  javaStep: Record<string, unknown>,
  workflowId: unknown,
): Record<string, unknown> {
  const evidence = javaStep.evidence;
  let evidenceUrls: string[] = [];
  if (evidence !== undefined && evidence !== null) {
    if (typeof evidence === 'string') {
      evidenceUrls = evidence
        .split(',')
        .map((u) => u.trim())
        .filter((u) => u.length > 0);
    } else {
      evidenceUrls = [String(evidence)];
    }
  }

  return {
    id: javaStep.id,
    workflowId,
    templateStepId: javaStep.templateStepId ? String(javaStep.templateStepId) : null,
    code: javaStep.code,
    name: javaStep.name,
    description: (javaStep.notes as string | undefined) ?? null,
    type: 'TASK',
    order: (javaStep.stepOrder as number | undefined) ?? 0,
    status: mapStepStatus(String(javaStep.status ?? '')),
    assignedTo: javaStep.assigneeUserId ? String(javaStep.assigneeUserId) : null,
    assignedToName: null,
    requiresEvidence: false,
    requiresApproval: false,
    evidenceUrls,
    approverId: javaStep.approvedBy ? String(javaStep.approvedBy) : null,
    approverName: null,
    approvalNotes: (javaStep.notes as string | undefined) ?? null,
    startAt: (javaStep.startedAt as string | undefined) ?? null,
    dueAt: (javaStep.dueAt as string | undefined) ?? null,
    startedAt: (javaStep.startedAt as string | undefined) ?? null,
    completedAt: (javaStep.completedAt as string | undefined) ?? null,
    createdAt: (javaStep.createdAt as string | undefined) ?? new Date().toISOString(),
    updatedAt: (javaStep.updatedAt as string | undefined) ?? new Date().toISOString(),
    attachmentCount: javaStep.attachmentCount as number | undefined,
  };
}

function transformWorkflow(javaWorkflow: Record<string, unknown>): Record<string, unknown> {
  const javaSteps = (javaWorkflow.steps as Record<string, unknown>[] | undefined) ?? [];
  const completedSteps = javaSteps.filter(
    (s) => s.status === 'COMPLETED' || s.status === 'APPROVED',
  ).length;
  const totalSteps = javaSteps.length;

  const progressPercent = javaWorkflow.progressPercent;
  let progress = 0;
  if (progressPercent !== undefined && progressPercent !== null) {
    if (typeof progressPercent === 'object' && 'toNumber' in progressPercent) {
      progress = (progressPercent as { toNumber: () => number }).toNumber();
    } else {
      progress = Number(progressPercent);
    }
  }

  return {
    id: javaWorkflow.id,
    code: javaWorkflow.code,
    name: javaWorkflow.name,
    description: javaWorkflow.description,
    projectId: javaWorkflow.projectId ? String(javaWorkflow.projectId) : null,
    subprojectId: javaWorkflow.subprojectId ? String(javaWorkflow.subprojectId) : null,
    subprojectIds: javaWorkflow.subprojectId ? [String(javaWorkflow.subprojectId)] : [],
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
    steps: javaSteps.map((step) => transformStep(step, javaWorkflow.id)),
  };
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

    // These proxy routes call workflow-service-java directly, bypassing the API
    // gateway that would normally inject X-Tenant-Id from the JWT - getBackendHeaders
    // only forwards a tenant header the *incoming* request already carried (as a
    // query param or header), which a plain client-side fetch() never sends. Listing
    // with no project/subproject filter relies entirely on the tenant header (there's
    // no other filter to fall back on), so resolve it from the session directly.
    const headers = getBackendHeaders(request) as Record<string, string>;
    if (!headers['X-Tenant-Id']) {
      const { tenantId } = await extractSessionUser();
      headers['X-Tenant-Id'] = tenantId;
    }

    const res = await fetch(url.toString(), {
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: body?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const javaWorkflows = (await res.json()) as Record<string, unknown>[];
    const workflows = javaWorkflows.map(transformWorkflow);
    return NextResponse.json(workflows);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
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
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}
