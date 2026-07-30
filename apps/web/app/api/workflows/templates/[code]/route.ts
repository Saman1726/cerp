import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getAuthHeaders, getBackendHeaders } from '../../utils';

// Map Java TemplateStatus to frontend WorkflowTemplateStatus
function mapTemplateStatus(status: string): string {
  const mapping: Record<string, string> = {
    DRAFT: 'DRAFT',
    ACTIVE: 'ACTIVE',
    DEPRECATED: 'ARCHIVED',
  };
  return mapping[status] ?? status;
}

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  try {
    const { code } = await params;
    // Use the new /by-code/{code} endpoint that returns template + steps in one request
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates/by-code/${code}`;

    const res = await fetch(url, {
      headers: getAuthHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const javaData = await res.json();

    // The response includes template and steps together from the /by-code/{code} endpoint
    const javaTemplate = javaData.template ?? javaData;
    const javaSteps: Record<string, unknown>[] = javaData.steps ?? [];

    // Transform template steps to frontend format
    const transformedSteps = javaSteps.map((step: Record<string, unknown>) => ({
      id: step.id,
      templateId: step.templateId ?? javaTemplate.id,
      code: step.code,
      name: step.name,
      description: (step.description as string | undefined) ?? (step.notes as string | undefined) ?? null,
      type: 'TASK' as const,
      order: (step.order as number | undefined) ?? (step.stepOrder as number | undefined) ?? 0,
      weight: (step.weight as number | undefined) ?? null,
      requiresEvidence: (step.requiresEvidence as boolean | undefined) ?? false,
      requiresApproval: (step.requiresApproval as boolean | undefined) ?? false,
      approverRole: (step.approverRole as string | undefined) ?? null,
      slaHours: (step.slaHours as number | undefined) ?? null,
      checklists: (step.checklists as unknown[] | undefined) ?? null,
      gatingCondition: (step.gatingCondition as string | undefined) ?? null,
      createdAt: (step.createdAt as string | undefined) ?? new Date().toISOString(),
      updatedAt: (step.updatedAt as string | undefined) ?? new Date().toISOString(),
    }));

    // Transform the template to match frontend format
    const transformedTemplate: Record<string, unknown> = {
      id: javaTemplate.id,
      code: javaTemplate.code,
      name: javaTemplate.name,
      description: (javaTemplate.description as string | undefined) ?? null,
      phaseType: javaTemplate.phaseType ?? '',
      status: mapTemplateStatus(String(javaTemplate.status ?? '')),
      parametersSchema: (javaTemplate.parametersSchema as string | undefined) ?? null,
      requiredDocuments: Array.isArray(javaTemplate.requiredDocuments)
        ? (javaTemplate.requiredDocuments as string[])
        : [],
      steps: transformedSteps,
      createdAt: javaTemplate.createdAt,
      updatedAt: javaTemplate.updatedAt,
    };

    return NextResponse.json({
      template: transformedTemplate,
      steps: transformedSteps,
    });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ code: string }> }
) {
  try {
    const { code } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates/${code}`;

    const body = await request.json();
    console.log('[API workflows/templates]', code, 'PATCH request body:', JSON.stringify(body, null, 2));

    const res = await fetch(url, {
      method: 'PATCH',
      headers: {
        ...getBackendHeaders(request as any),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    console.log('[API workflows/templates]', code, 'PATCH response status:', res.status, res.statusText);

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      console.error('[API workflows/templates]', code, 'PATCH error:', res.status, data);
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
