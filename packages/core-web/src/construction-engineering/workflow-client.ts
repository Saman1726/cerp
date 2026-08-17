/**
 * Workflow API client for subproject workflow management.
 * Provides methods to create, query, and update workflows and their steps.
 */

import type {
  Workflow,
  WorkflowResponse,
  WorkflowInstanceStep,
  WorkflowTemplate,
  WorkflowTemplateResponse,
  WorkflowNote,
  WorkflowCreateRequest,
  WorkflowInstanceStepUpdateRequest,
  WorkflowStepCompleteRequest,
  WorkflowGenerationRequest,
  OrganisationWorkflowConfig,
  OrganisationWorkflowConfigRequest,
  TemplatePhaseMapping,
  TemplatePhaseMappingRequest,
  TemplatePhaseMappingFilter,
  SubProjectWorkflowResponse,
} from '@cerp/shared-types';

// Use relative path to proxy through Next.js API routes
// This handles authentication via middleware and avoids CORS issues
const API_BASE = '';

async function workflowFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const headersInit = new Headers(init?.headers);
  headersInit.set('Content-Type', 'application/json');

  const fullUrl = `${API_BASE}${path}`;

  const res = await fetch(fullUrl, {
    ...init,
    headers: headersInit,
    credentials: 'same-origin',
    cache: 'no-store',
  });

  if (!res.ok) {
    const body = await res.json().catch(() => undefined);
    throw new Error(body?.message ?? res.statusText);
  }

  return res.json();
}

// ── Workflow Instance Methods ──

/**
 * Get all workflows for a subproject.
 */
export async function getSubprojectWorkflows(subprojectId: string): Promise<SubProjectWorkflowResponse> {
  return workflowFetch<SubProjectWorkflowResponse>(`/api/workflows/subprojects/${subprojectId}`);
}

/**
 * Get a single workflow by ID with its steps and notes.
 */
export async function getWorkflow(workflowId: string): Promise<WorkflowResponse> {
  return workflowFetch<WorkflowResponse>(`/api/workflows/${workflowId}`);
}

/**
 * Get workflows by template code.
 */
export async function getWorkflowsByTemplate(templateCode: string): Promise<Workflow[]> {
  return workflowFetch<Workflow[]>(`/api/workflows?templateCode=${templateCode}`);
}

/**
 * Create a new workflow from a template for a subproject.
 */
export async function createWorkflow(request: WorkflowCreateRequest): Promise<Workflow> {
  return workflowFetch<Workflow>('/api/workflows', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Generate workflows for a subproject from selected templates.
 */
export async function generateWorkflows(request: WorkflowGenerationRequest): Promise<Workflow[]> {
  return workflowFetch<Workflow[]>('/api/workflows/generate', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Update a workflow step (status, assignment, evidence, approval).
 */
export async function updateWorkflowStep(
  workflowId: string,
  stepId: string,
  request: WorkflowInstanceStepUpdateRequest,
): Promise<WorkflowInstanceStep> {
  return workflowFetch<WorkflowInstanceStep>(`/api/workflows/${workflowId}/steps/${stepId}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

/**
 * Complete a workflow step with evidence and optional approval notes.
 */
export async function completeWorkflowStep(
  workflowId: string,
  stepId: string,
  request: WorkflowStepCompleteRequest,
): Promise<WorkflowInstanceStep> {
  return workflowFetch<WorkflowInstanceStep>(`/api/workflows/${workflowId}/steps/${stepId}/complete`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Add a note to a workflow.
 */
export async function addWorkflowNote(
  workflowId: string,
  content: string,
): Promise<WorkflowNote> {
  return workflowFetch<WorkflowNote>(`/api/workflows/${workflowId}/notes`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });
}

/**
 * Get notes for a workflow.
 */
export async function getWorkflowNotes(workflowId: string): Promise<WorkflowNote[]> {
  return workflowFetch<WorkflowNote[]>(`/api/workflows/${workflowId}/notes`);
}

/**
 * Pause a workflow.
 */
export async function pauseWorkflow(workflowId: string): Promise<Workflow> {
  return workflowFetch<Workflow>(`/api/workflows/${workflowId}/pause`, {
    method: 'POST',
  });
}

/**
 * Resume a paused workflow.
 */
export async function resumeWorkflow(workflowId: string): Promise<Workflow> {
  return workflowFetch<Workflow>(`/api/workflows/${workflowId}/resume`, {
    method: 'POST',
  });
}

/**
 * Cancel a workflow.
 */
export async function cancelWorkflow(workflowId: string): Promise<Workflow> {
  return workflowFetch<Workflow>(`/api/workflows/${workflowId}/cancel`, {
    method: 'POST',
  });
}

// ── Workflow Template Methods ──

/**
 * Get all active workflow templates.
 */
export async function getActiveTemplates(): Promise<WorkflowTemplate[]> {
  return workflowFetch<WorkflowTemplate[]>('/api/templates');
}

/**
 * Get a single template by code.
 */
export async function getTemplateByCode(code: string): Promise<WorkflowTemplateResponse> {
  return workflowFetch<WorkflowTemplateResponse>(`/api/templates/${code}`);
}

/**
 * Get available construction phase types.
 */
export async function getPhaseTypes(): Promise<string[]> {
  return workflowFetch<string[]>('/api/templates/phases');
}

/**
 * Get available building type groups for template-phase mapping.
 */
export async function getBuildingTypeGroups(): Promise<string[]> {
  return workflowFetch<string[]>('/api/templates/building-type-groups');
}

// ── Template Phase Mapping Methods ──

/**
 * Get template-phase mappings with optional filters.
 */
export async function getTemplatePhaseMappings(filter: TemplatePhaseMappingFilter = {}): Promise<TemplatePhaseMapping[]> {
  const params = new URLSearchParams();
  if (filter.templateCode) params.set('templateCode', filter.templateCode);
  if (filter.phaseType) params.set('phaseType', filter.phaseType);
  if (filter.scopeLevel) params.set('scopeLevel', filter.scopeLevel);
  if (filter.buildingTypeGroup) params.set('buildingTypeGroup', filter.buildingTypeGroup);
  const query = params.toString();
  return workflowFetch<TemplatePhaseMapping[]>(`/api/templates/mappings${query ? `?${query}` : ''}`);
}

/**
 * Create a new template-phase mapping.
 */
export async function createTemplatePhaseMapping(request: TemplatePhaseMappingRequest): Promise<TemplatePhaseMapping> {
  return workflowFetch<TemplatePhaseMapping>('/api/templates/mappings', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

/**
 * Delete a template-phase mapping.
 */
export async function deleteTemplatePhaseMapping(mappingId: string): Promise<void> {
  await workflowFetch<void>(`/api/templates/mappings/${mappingId}`, {
    method: 'DELETE',
  });
}

// ── Organisation Workflow Config Methods ──

/**
 * Get organisation workflow configuration.
 */
export async function getOrganisationWorkflowConfig(orgId: string): Promise<OrganisationWorkflowConfig | null> {
  try {
    return workflowFetch<OrganisationWorkflowConfig>(`/api/organisation-workflow-config/${orgId}`);
  } catch {
    return null;
  }
}

/**
 * Update organisation workflow configuration.
 */
export async function updateOrganisationWorkflowConfig(
  orgId: string,
  request: OrganisationWorkflowConfigRequest,
): Promise<OrganisationWorkflowConfig> {
  return workflowFetch<OrganisationWorkflowConfig>(`/api/organisation-workflow-config/${orgId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

/**
 * Get workflows where the current user is the creator or has an assigned step.
 * Uses server-side tenant scoping for security.
 */
export async function getMyWorkflows(
  accessMode: 'created' | 'assigned',
): Promise<Workflow[]> {
  const params = new URLSearchParams({ accessMode });
  return workflowFetch<Workflow[]>(`/api/workflows?${params.toString()}`);
}

/**
 * Get workflows assigned to or created by a user (legacy alias).
 * @deprecated Use getMyWorkflows instead.
 */
export async function listWorkflowsByAccessMode(
  accessMode: 'created' | 'assigned',
  userId?: string,
): Promise<Workflow[]> {
  const params = new URLSearchParams({ accessMode });
  if (userId) params.set('userId', userId);
  return workflowFetch<Workflow[]>(`/api/workflows?${params.toString()}`);
}

/**
 * Get all available template codes for an organisation.
 */
export async function getAvailableTemplateCodes(): Promise<string[]> {
  return workflowFetch<string[]>('/api/templates/codes');
}