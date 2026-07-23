import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders } from '../../utils';

// Map Java WorkflowStatus to frontend WorkflowStatus
function mapWorkflowStatus(status: string): string {
  const mapping: Record<string, string> = {
    PLANNED: 'ACTIVE',
    IN_PROGRESS: 'ACTIVE',
    BLOCKED: 'PAUSED',
    ON_HOLD: 'PAUSED',
    COMPLETED: 'COMPLETED',
    APPROVED: 'COMPLETED',
    REJECTED: 'ACTIVE',
    CANCELED: 'CANCELLED',
  };
  return mapping[status] ?? status;
}

// Map Java StepStatus to frontend WorkflowStepStatus
function mapStepStatus(status: string): string {
  const mapping: Record<string, string> = {
    NOT_STARTED: 'PENDING',
    IN_PROGRESS: 'IN_PROGRESS',
    BLOCKED: 'PENDING',
    COMPLETED: 'COMPLETED',
    APPROVED: 'COMPLETED',
    REJECTED: 'FAILED',
  };
  return mapping[status] ?? status;
}

// Helper to get a field from a record, checking both camelCase and snake_case
function getField(obj: Record<string, unknown>, camelCase: string, snakeCase: string): unknown {
  return obj[camelCase] ?? obj[snakeCase];
}

// Transform Java Workflow to frontend Workflow format
function transformWorkflow(javaWorkflow: Record<string, unknown>, javaSteps: Record<string, unknown>[]): Record<string, unknown> {
  const completedSteps = javaSteps.filter(
    (s: Record<string, unknown>) => {
      const sStatus = getField(s, 'status', 'status') as string;
      return sStatus === 'COMPLETED' || sStatus === 'APPROVED';
    }
  ).length as unknown as number;
  const totalSteps = javaSteps.length as unknown as number;

  const progressPercent = getField(javaWorkflow, 'progressPercent', 'progress_percent');
  let progress = 0;
  if (progressPercent !== undefined && progressPercent !== null) {
    if (typeof progressPercent === 'object' && 'toNumber' in progressPercent) {
      progress = (progressPercent as { toNumber: () => number }).toNumber();
    } else {
      progress = Number(progressPercent);
    }
  }

  const startDate = getField(javaWorkflow, 'startDate', 'start_date');
  const endDate = getField(javaWorkflow, 'endDate', 'end_date');

  return {
    id: javaWorkflow.id ?? getField(javaWorkflow, 'id', 'id'),
    code: javaWorkflow.code ?? getField(javaWorkflow, 'code', 'code'),
    name: javaWorkflow.name ?? getField(javaWorkflow, 'name', 'name'),
    description: javaWorkflow.description ?? getField(javaWorkflow, 'description', 'description'),
    subprojectIds: javaWorkflow.subprojectId ? [(javaWorkflow.subprojectId as string) ?? ''] : [],
    templateId: javaWorkflow.templateId ? String(javaWorkflow.templateId) : null,
    templateCode: null,
    status: mapWorkflowStatus(String(getField(javaWorkflow, 'status', 'status') ?? '')),
    phaseType: '',
    currentStepId: null,
    currentStepOrder: null,
    progress,
    completedSteps,
    totalSteps,
    parameters: null,
    startedAt: startDate ? String(startDate) : null,
    completedAt: endDate ? String(endDate) : null,
    createdAt: javaWorkflow.createdAt ?? getField(javaWorkflow, 'createdAt', 'created_at'),
    updatedAt: javaWorkflow.updatedAt ?? getField(javaWorkflow, 'updatedAt', 'updated_at'),
  };
}

// Transform Java WorkflowStep to frontend WorkflowInstanceStep format
function transformStep(javaStep: Record<string, unknown>, javaWorkflow: Record<string, unknown>): Record<string, unknown> {
  const evidence = getField(javaStep, 'evidence', 'evidence');
  let evidenceUrls: string[] = [];
  if (evidence !== undefined && evidence !== null) {
    if (typeof evidence === 'string') {
      evidenceUrls = evidence.split(',').map((u: string) => u.trim()).filter((u: string) => u.length > 0);
    } else {
      evidenceUrls = [String(evidence)];
    }
  }

  const stepOrder = getField(javaStep, 'order', 'step_order');
  const statusStr = getField(javaStep, 'status', 'status') as string;

  return {
    id: javaStep.id ?? getField(javaStep, 'id', 'id'),
    workflowId: javaWorkflow.id ?? getField(javaWorkflow, 'id', 'id'),
    templateStepId: getField(javaStep, 'templateStepId', 'template_step_id') ? String(getField(javaStep, 'templateStepId', 'template_step_id')!) : null,
    code: javaStep.code ?? getField(javaStep, 'code', 'code'),
    name: javaStep.name ?? getField(javaStep, 'name', 'name'),
    description: (getField(javaStep, 'notes', 'notes') as string | undefined) ?? null,
    type: 'TASK',
    order: stepOrder !== undefined && stepOrder !== null ? Number(stepOrder) : 0,
    status: mapStepStatus(statusStr ?? ''),
    assignedTo: getField(javaStep, 'assigneeUserId', 'assignee_user_id') ? String(getField(javaStep, 'assigneeUserId', 'assignee_user_id')!) : null,
    assignedToName: null,
    requiresEvidence: false,
    requiresApproval: false,
    evidenceUrls,
    approverId: getField(javaStep, 'approvedBy', 'approved_by') ? String(getField(javaStep, 'approvedBy', 'approved_by')!) : null,
    approverName: null,
    approvalNotes: (getField(javaStep, 'notes', 'notes') as string | undefined) ?? null,
    startAt: (getField(javaStep, 'startedAt', 'started_at') as string | undefined) ?? null,
    dueAt: (getField(javaStep, 'dueAt', 'due_at') as string | undefined) ?? null,
    startedAt: (getField(javaStep, 'startedAt', 'started_at') as string | undefined) ?? null,
    completedAt: (getField(javaStep, 'completedAt', 'completed_at') as string | undefined) ?? null,
    createdAt: (getField(javaStep, 'createdAt', 'created_at') as string | undefined) ?? new Date().toISOString(),
    updatedAt: (getField(javaStep, 'updatedAt', 'updated_at') as string | undefined) ?? new Date().toISOString(),
  };
}

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ subprojectId: string }> }
) {
  try {
    const { subprojectId } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/subprojects/${subprojectId}`;

    const res = await fetch(url, {
      headers: getBackendHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const rawData = await res.text();
    console.log('[API workflows/subprojects/[subprojectId]] Raw response:', rawData.substring(0, 2000));
    
    let data: unknown;
    try {
      data = JSON.parse(rawData);
    } catch (parseError) {
      console.error('[API workflows/subprojects/[subprojectId]] JSON parse error:', parseError);
      return NextResponse.json({ message: 'Invalid JSON response from workflow service' }, { status: 502 });
    }

    // The Java backend returns { workflows: [...], steps: [...] }
    // Transform to match frontend expectations
    const obj = data as Record<string, unknown>;
    const javaWorkflows = (obj.workflows as Record<string, unknown>[]) ?? [];
    const javaSteps = (obj.steps as Record<string, unknown>[]) ?? [];
    
    console.log('[API workflows/subprojects/[subprojectId]] Workflows count:', javaWorkflows.length);
    console.log('[API workflows/subprojects/[subprojectId]] Steps count:', javaSteps.length);
    if (javaWorkflows.length > 0 && javaWorkflows[0]) {
      console.log('[API workflows/subprojects/[subprojectId]] First workflow keys:', Object.keys(javaWorkflows[0] as object));
    }
    if (javaSteps.length > 0 && javaSteps[0]) {
      console.log('[API workflows/subprojects/[subprojectId]] First step keys:', Object.keys(javaSteps[0] as object));
    }

    // Build a map of workflowId -> steps
    const workflowStepsMap: Record<string, Record<string, unknown>[]> = {};
    for (const step of javaSteps) {
      const wfId = getField(step, 'workflowId', 'workflow_id') as string | undefined;
      if (wfId) {
        if (!workflowStepsMap[wfId]) {
          workflowStepsMap[wfId] = [];
        }
        workflowStepsMap[wfId].push(step);
      }
    }

    // Transform workflows with their steps
    const workflows = javaWorkflows.map((wf: Record<string, unknown>) => {
      const wfId = getField(wf, 'id', 'id') as string;
      const steps = workflowStepsMap[wfId] ?? [];
      return transformWorkflow(wf, steps);
    });

    // Transform steps
    const transformedSteps = javaSteps.map((step: Record<string, unknown>) => {
      const stepWfId = getField(step, 'workflowId', 'workflow_id') as string | undefined;
      const workflow = stepWfId ? javaWorkflows.find((wf: Record<string, unknown>) => String(getField(wf, 'id', 'id')!) === stepWfId) : undefined;
      if (workflow) {
        return transformStep(step, workflow);
      }
      return null;
    }).filter((item: Record<string, unknown> | null): item is Record<string, unknown> => item !== null);

    return NextResponse.json({
      workflows,
      steps: transformedSteps,
    });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}