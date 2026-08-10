'use server';

import { revalidatePath } from 'next/cache';
import { apiClient } from '@cerp/core-web/lib/api-client';
import type {
  ProjectCreateRequest,
  ProjectResponse,
  SubProjectResponse,
  WorkflowTemplate,
  Workflow,
  ProjectUserAssignment,
  ProjectAssignedUser,
} from '@cerp/shared-types';

// ── Project Actions ──

export async function createProjectActionWrapper(formData: FormData): Promise<void> {
  const result = await createProjectActionCore(formData);
  if (result.error) {
    console.error('Failed to create project:', result.error);
    throw new Error(result.error);
  }
}

// Default organization ID for development/testing when no specific org is provided.
// This matches the seeded organization in V2__seed_default_organization.sql
const DEFAULT_ORG_ID = '00000000-0000-0000-0000-000000000001';

async function createProjectActionCore(formData: FormData): Promise<{ error?: string }> {
  try {
    const code = formData.get('code')?.toString()?.trim() ?? '';
    const name = formData.get('name')?.toString()?.trim() ?? '';
    // Use provided orgId or fall back to the default seeded organization
    const orgId = formData.get('orgId')?.toString()?.trim() || DEFAULT_ORG_ID;

    if (!code || !name) {
      return { error: 'Project code and name are required' };
    }

    const request: ProjectCreateRequest = {
      code,
      name,
      orgId: orgId || DEFAULT_ORG_ID,
      description: formData.get('description')?.toString()?.trim() || undefined,
      constructionSiteId: formData.get('constructionSiteId')?.toString()?.trim() || undefined,
      addressLine1: formData.get('addressLine1')?.toString()?.trim() || undefined,
      addressLine2: formData.get('addressLine2')?.toString()?.trim() || undefined,
      postalCode: formData.get('postalCode')?.toString()?.trim() || undefined,
      city: formData.get('city')?.toString()?.trim() || undefined,
      countryCode: formData.get('countryCode')?.toString()?.trim() || undefined,
      projectType: (formData.get('type')?.toString()?.trim() || undefined) as ProjectCreateRequest['projectType'],
      status: (formData.get('status')?.toString()?.trim() || undefined) as ProjectCreateRequest['status'],
      startDate: formData.get('startDate')?.toString()?.trim() || undefined,
      endDate: formData.get('endDate')?.toString()?.trim() || undefined,
      budget: formData.get('budget') ? Number(formData.get('budget')) : undefined,
      currencyCode: formData.get('currencyCode')?.toString()?.trim() || 'EUR',
    };

    await apiClient.post<ProjectCreateRequest, ProjectResponse>('/api/v1/projects', request);
    revalidatePath('/construction');
    return {};
  } catch (error: unknown) {
    const errorMessage = error instanceof Error && error.message ? error.message : 'Failed to create project';
    console.error('Failed to create project:', error);
    return { error: errorMessage };
  }
}

export async function deleteProjectActionWrapper(projectId: string): Promise<void> {
  const result = await deleteProjectActionCore(projectId);
  if (result.error) {
    console.error('Failed to delete project:', result.error);
  }
}

async function deleteProjectActionCore(projectId: string): Promise<{ error?: string }> {
  try {
    if (!projectId) {
      return { error: 'Missing project ID' };
    }

    await apiClient.delete(`/api/v1/projects/${projectId}`);
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to delete project:', error);
    return { error: 'Failed to delete project' };
  }
}

  export async function fetchProjects(): Promise<ProjectResponse[]> {
    try {
      return await apiClient.get<ProjectResponse[]>('/api/v1/projects');
    } catch {
    return [];
  }
}


// ── Project Assignment Actions ──

/**
 * Assign users to a project with roles.
 * @param projectId - The project ID
 * @param assignments - Array of { userId, role } where role is VIEWER, EDITOR, or ADMIN
 */
export async function assignUsersToProjectAction(projectId: string, assignments: ProjectUserAssignment[]): Promise<{ error?: string }> {
  try {
    if (!projectId || !assignments || assignments.length === 0) {
      return { error: 'Project ID and user assignments are required' };
    }

    await apiClient.post(`/api/v1/projects/${projectId}/assign`, { assignments });
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to assign users to project:', error);
    return { error: error instanceof Error ? error.message : 'Failed to assign users to project' };
  }
}

export async function removeUserFromProjectAction(projectId: string, userIdToRemove: string): Promise<{ error?: string }> {
  try {
    if (!projectId || !userIdToRemove) {
      return { error: 'Project ID and user ID are required' };
    }

    await apiClient.delete(`/api/v1/projects/${projectId}/assign/${userIdToRemove}`);
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to remove user from project:', error);
    return { error: error instanceof Error ? error.message : 'Failed to remove user from project' };
  }
}

/**
 * Assigned users enriched with their emails, for display (assignedUserIds itself is UUID-keyed
 * and not human-readable on its own). Fetched on demand when the Assign Users popup opens rather
 * than baked into fetchProjects(), to avoid an identity-service round trip per assignee on every
 * project list load.
 */
export async function getProjectAssignedUsers(projectId: string): Promise<ProjectAssignedUser[]> {
  try {
    if (!projectId) {
      return [];
    }
    return await apiClient.get<ProjectAssignedUser[]>(`/api/v1/projects/${projectId}/assigned-users`);
  } catch (error) {
    console.error('Failed to fetch assigned users:', error);
    return [];
  }
}

// ── SubProject Actions ──

export async function createSubProjectActionWrapper(formData: FormData): Promise<void> {
  const result = await createSubProjectActionCore(formData);
  if (result.error) {
    console.error('Failed to create subProject:', result.error);
    throw new Error(result.error);
  }
}

async function createSubProjectActionCore(formData: FormData): Promise<{ error?: string; subProjectId?: string }> {
  try {
    const projectId = formData.get('projectId')?.toString();
    if (!projectId) {
      return { error: 'Missing project ID' };
    }

    const buildingId = formData.get('buildingId')?.toString()?.trim();
    const requestBody: Record<string, unknown> = {
      code: formData.get('code')?.toString()?.trim() ?? '',
      name: formData.get('name')?.toString()?.trim() ?? '',
      description: formData.get('description')?.toString()?.trim() || undefined,
      buildingType: formData.get('buildingType')?.toString()?.trim() || 'OTHER',
      constructionPhase: formData.get('constructionPhase')?.toString()?.trim() || 'PRE_CONSTRUCTION',
      buildingLevel: formData.get('buildingLevel')?.toString()?.trim() || 'WHOLE',
      buildingId: buildingId || undefined,
      floorIds: formData.get('floors') ? JSON.parse(formData.get('floors')?.toString() ?? '[]') : [],
      partIds: formData.get('buildingParts') ? JSON.parse(formData.get('buildingParts')?.toString() ?? '[]') : [],
      startDate: formData.get('startDate')?.toString()?.trim() || undefined,
      endDate: formData.get('endDate')?.toString()?.trim() || undefined,
      progress: formData.get('progress') ? Number(formData.get('progress')) : undefined,
      budget: formData.get('budget') ? Number(formData.get('budget')) : undefined,
      currencyCode: formData.get('currencyCode')?.toString()?.trim() || 'EUR',
    };

    console.log('[SubProject] requestBody:', JSON.stringify(requestBody, null, 2));
    
    if (!requestBody.code || !requestBody.name) {
      return { error: 'SubProject code and name are required' };
    }
    
    if (!requestBody.buildingId) {
      return { error: 'Building ID is required' };
    }

    const created = await apiClient.post<typeof requestBody, SubProjectResponse>(
      `/api/v1/projects/${projectId}/subprojects`,
      requestBody
    );

    // Initialize default workflows if workflow initialization is enabled
    const initWorkflows = formData.get('initWorkflows')?.toString();
    if (initWorkflows === 'true') {
      await initializeSubProjectWorkflows(created.id, projectId);
    }

    revalidatePath('/construction');
    return { error: undefined, subProjectId: created.id };
  } catch (error) {
    const errorMessage = error instanceof Error && error.message ? error.message : 'Failed to create subProject';
    console.error('Failed to create subProject:', error);
    return { error: errorMessage };
  }
}

export async function deleteSubProjectActionWrapper(formData: FormData): Promise<void> {
  const result = await deleteSubProjectActionCore(formData);
  if (result.error) {
    console.error('Failed to delete subProject:', result.error);
  }
}

async function deleteSubProjectActionCore(formData: FormData): Promise<{ error?: string }> {
  try {
    const projectId = formData.get('projectId')?.toString();
    const subProjectId = formData.get('subProjectId')?.toString();
    if (!projectId || !subProjectId) {
      return { error: 'Missing project or subProject ID' };
    }

    await apiClient.delete(`/api/v1/projects/${projectId}/subprojects/${subProjectId}`);
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to delete subProject:', error);
    return { error: 'Failed to delete subProject' };
  }
}

// ── Workflow Template Actions ──

export async function fetchWorkflowTemplates(): Promise<WorkflowTemplate[]> {
  try {
    return await apiClient.get<WorkflowTemplate[]>('/api/v1/workflow-templates?all=true');
  } catch {
    return [];
  }
}

export async function createWorkflowTemplateAction(formData: FormData): Promise<{ error?: string }> {
  try {
    const request: Record<string, unknown> = {
      code: formData.get('code')?.toString()?.trim() ?? '',
      name: formData.get('name')?.toString()?.trim() ?? '',
      description: formData.get('description')?.toString()?.trim() || undefined,
      category: formData.get('category')?.toString()?.trim() || 'CONSTRUCTION_PHASE',
      scopeLevel: formData.get('scopeLevel')?.toString()?.trim() || 'WHOLE',
      steps: formData.get('steps') ? JSON.parse(formData.get('steps')?.toString() ?? '[]') : [],
    };

    if (!request.code || !request.name) {
      return { error: 'Template code and name are required' };
    }

    await apiClient.post<Record<string, unknown>, unknown>('/api/v1/workflow-templates', request);
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to create workflow template:', error);
    return { error: 'Failed to create workflow template' };
  }
}

export async function activateWorkflowTemplateAction(templateCode: string): Promise<{ error?: string }> {
  try {
    const code = templateCode?.trim();
    if (!code) {
      return { error: 'Template code is required' };
    }

    await apiClient.post<Record<string, never>, unknown>(`/api/v1/workflow-templates/${code}/activate`, {});
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to activate workflow template:', error);
    return { error: 'Failed to activate workflow template' };
  }
}

export async function deprecateWorkflowTemplateAction(templateCode: string): Promise<{ error?: string }> {
  try {
    const code = templateCode?.trim();
    if (!code) {
      return { error: 'Template code is required' };
    }

    await apiClient.post<Record<string, never>, unknown>(`/api/v1/workflow-templates/${code}/deprecate`, {});
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to deprecate workflow template:', error);
    return { error: 'Failed to deprecate workflow template' };
  }
}

// ── Workflow Instance Actions ──

export async function createWorkflowAction(formData: FormData): Promise<{ error?: string }> {
  try {
    const request: Record<string, unknown> = {
      templateId: formData.get('templateId')?.toString()?.trim() ?? '',
      projectId: formData.get('projectId')?.toString()?.trim() ?? '',
      subProjectId: formData.get('subProjectId')?.toString()?.trim() || undefined,
      scopeLevel: formData.get('scopeLevel')?.toString()?.trim() || 'WHOLE',
      floorIds: formData.get('floorIds') ? JSON.parse(formData.get('floorIds')?.toString() ?? '[]') : [],
      partIds: formData.get('partIds') ? JSON.parse(formData.get('partIds')?.toString() ?? '[]') : [],
      startDate: formData.get('startDate')?.toString()?.trim() || undefined,
      endDate: formData.get('endDate')?.toString()?.trim() || undefined,
      dueDate: formData.get('dueDate')?.toString()?.trim() || undefined,
      description: formData.get('description')?.toString()?.trim() || undefined,
    };

    if (!request.templateId || !request.projectId) {
      return { error: 'Template and project are required' };
    }

    await apiClient.post<Record<string, unknown>, unknown>('/api/v1/workflows/instantiate', request);
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to create workflow:', error);
    return { error: 'Failed to create workflow' };
  }
}

export async function startWorkflowAction(formData: FormData): Promise<{ error?: string }> {
  try {
    const workflowId = formData.get('workflowId')?.toString()?.trim();
    if (!workflowId) {
      return { error: 'Workflow ID is required' };
    }

    await apiClient.post<Record<string, never>, unknown>(`/api/v1/workflows/${workflowId}/start`, {});
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to start workflow:', error);
    return { error: 'Failed to start workflow' };
  }
}

export async function cancelWorkflowAction(formData: FormData): Promise<{ error?: string }> {
  try {
    const workflowId = formData.get('workflowId')?.toString()?.trim();
    if (!workflowId) {
      return { error: 'Workflow ID is required' };
    }

    await apiClient.post<Record<string, never>, unknown>(`/api/v1/workflows/${workflowId}/cancel`, {});
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to cancel workflow:', error);
    return { error: 'Failed to cancel workflow' };
  }
}

export async function completeWorkflowAction(formData: FormData): Promise<{ error?: string }> {
  try {
    const workflowId = formData.get('workflowId')?.toString()?.trim();
    if (!workflowId) {
      return { error: 'Workflow ID is required' };
    }

    await apiClient.post<Record<string, never>, unknown>(`/api/v1/workflows/${workflowId}/complete`, {});
    revalidatePath('/construction');
    return {};
  } catch (error) {
    console.error('Failed to complete workflow:', error);
    return { error: 'Failed to complete workflow' };
  }
}

export async function fetchWorkflows(projectId?: string): Promise<Workflow[]> {
  try {
    if (projectId) {
      return await apiClient.get<Workflow[]>(`/api/v1/workflows?projectId=${projectId}`);
    }
    return await apiClient.get<Workflow[]>('/api/v1/workflows');
  } catch {
    return [];
  }
}

// ── Workflow Initialization Helper ──

async function initializeSubProjectWorkflows(subProjectId: string, projectId: string): Promise<void> {
  try {
    // Get the tenant ID from cookies or header
    const { cookies } = await import('next/headers');
    const store = await cookies();
    const tenantId = store.get('x-tenant-id')?.value || 'default';
    const token = store.get('set-token')?.value || '';

    // Get orgId from the project
    const project = await apiClient.get<{ orgId: string }>(`/api/v1/projects/${projectId}`);
    const orgId = project?.orgId;

    if (!orgId) {
      console.warn('Cannot auto-generate workflows: orgId not found for project');
      return;
    }

    // Get the API base URL from env
    const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

    // Call the auto-generate endpoint which handles everything via REST (query params)
    const apiUrl = `${API_URL}/api/v1/${tenantId}/workflows/auto-generate?orgId=${orgId}&subprojectId=${subProjectId}&projectId=${projectId}&buildingType=RESIDENTIAL`;
    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      console.warn(`Failed to auto-generate workflows: ${response.status} ${errorBody.message || response.statusText}`);
      return;
    }

    console.log(`Auto-generated workflows for subProject: ${subProjectId}`);
  } catch (error) {
    console.warn('Failed to auto-generate workflows for subProject:', error);
  }
}
