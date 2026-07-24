import { NextRequest, NextResponse } from 'next/server';
import { WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getBackendHeaders } from '../utils';

// Map Java TemplateStatus to frontend WorkflowTemplateStatus
function mapTemplateStatus(status: string): string {
  const mapping: Record<string, string> = {
    DRAFT: 'DRAFT',
    ACTIVE: 'ACTIVE',
    DEPRECATED: 'ARCHIVED',
  };
  return mapping[status] ?? status;
}

// Helper to get a field from a record, checking both camelCase and snake_case
function getField(obj: Record<string, unknown>, camelCase: string, snakeCase: string): unknown {
  return obj[camelCase] ?? obj[snakeCase];
}

// Transform Java WorkflowTemplate to frontend WorkflowTemplate format
function transformTemplate(javaTemplate: Record<string, unknown>, javaSteps: Record<string, unknown>[]): Record<string, unknown> {
  const transformedSteps = javaSteps.map((step: Record<string, unknown>) => {
    return {
      id: getField(step, 'id', 'id'),
      templateId: getField(step, 'templateId', 'template_id') ?? getField(javaTemplate, 'id', 'id'),
      code: getField(step, 'code', 'code'),
      name: getField(step, 'name', 'name'),
      description: (getField(step, 'description', 'description') as string | undefined) ?? (getField(step, 'notes', 'notes') as string | undefined) ?? null,
      type: 'TASK' as const,
      order: getField(step, 'order', 'step_order') !== undefined && getField(step, 'order', 'step_order') !== null
        ? Number(getField(step, 'order', 'step_order'))
        : 0,
      weight: getField(step, 'weight', 'weight') as number | undefined,
      requiresEvidence: (getField(step, 'requiresEvidence', 'requires_evidence') as boolean | undefined) ?? false,
      requiresApproval: (getField(step, 'requiresApproval', 'requires_approval') as boolean | undefined) ?? false,
      approverRole: getField(step, 'approverRole', 'approver_role') as string | undefined,
      slaHours: getField(step, 'slaHours', 'sla_hours') as number | undefined,
      checklists: getField(step, 'checklists', 'checklists') as unknown[] | undefined,
      gatingCondition: getField(step, 'gatingCondition', 'gating_condition') as string | undefined,
      createdAt: (getField(step, 'createdAt', 'created_at') as string | undefined) ?? new Date().toISOString(),
      updatedAt: (getField(step, 'updatedAt', 'updated_at') as string | undefined) ?? new Date().toISOString(),
    };
  });

  return {
    id: javaTemplate.id ?? getField(javaTemplate, 'id', 'id'),
    code: javaTemplate.code ?? getField(javaTemplate, 'code', 'code'),
    name: javaTemplate.name ?? getField(javaTemplate, 'name', 'name'),
    description: (getField(javaTemplate, 'description', 'description') as string | undefined) ?? null,
    phaseType: (getField(javaTemplate, 'phaseType', 'phase_type') ?? '') as string,
    status: mapTemplateStatus(String(getField(javaTemplate, 'status', 'status') ?? '')),
    parametersSchema: getField(javaTemplate, 'parametersSchema', 'parameters_schema') as string | undefined,
    requiredDocuments: Array.isArray(getField(javaTemplate, 'requiredDocuments', 'required_documents'))
      ? (getField(javaTemplate, 'requiredDocuments', 'required_documents') as string[])
      : [],
    steps: transformedSteps,
    createdAt: javaTemplate.createdAt ?? getField(javaTemplate, 'createdAt', 'created_at'),
    updatedAt: javaTemplate.updatedAt ?? getField(javaTemplate, 'updatedAt', 'updated_at'),
  };
}

export async function GET(request: NextRequest) {
  try {
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates`;
    console.log('[API workflows/templates] Fetching from workflow service:', url);

    const res = await fetch(url, {
      headers: getBackendHeaders(request),
      cache: 'no-store',
    });

    console.log('[API workflows/templates] Workflow service response status:', res.status, res.statusText);

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      console.error('[API workflows/templates] Backend error:', res.status, data?.message ?? res.statusText);
      return NextResponse.json({ message: data?.message ?? res.statusText }, { status: res.status });
    }

    const contentType = res.headers.get('content-type');
    if (!contentType || !contentType.includes('application/json')) {
      console.error('[API workflows/templates] Unexpected content type:', contentType);
      const text = await res.text();
      console.error('[API workflows/templates] Response body:', text.substring(0, 1000));
      return NextResponse.json({ message: `Unexpected content type: ${contentType}` }, { status: 502 });
    }

    const rawData = await res.text();
    console.log('[API workflows/templates] Raw response:', rawData.substring(0, 2000));

    if (!rawData.trim()) {
      console.error('[API workflows/templates] Empty response body from workflow service');
      return NextResponse.json({ message: 'Empty response from workflow service' }, { status: 502 });
    }

    let data: unknown[];
    try {
      data = JSON.parse(rawData);
    } catch (parseError) {
      console.error('[API workflows/templates] JSON parse error:', parseError);
      return NextResponse.json({ message: 'Invalid JSON response from workflow service' }, { status: 502 });
    }

    if (!Array.isArray(data)) {
      console.error('[API workflows/templates] Response is not an array:', typeof data);
      return NextResponse.json({ message: 'Expected array response from workflow service' }, { status: 502 });
    }

    console.log('[API workflows/templates] Templates count:', data.length);

    // Transform each template
    const templates = data.map((value: unknown) => {
      const item = value as Record<string, unknown>;
      const javaSteps = (getField(item, 'steps', 'steps') as Record<string, unknown>[]) ?? [];
      return transformTemplate(item, javaSteps);
    });

    return NextResponse.json(templates);
  } catch (error) {
    console.error('[API workflows/templates] Unexpected error:', error);
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    );
  }
}
