import { apiClient } from '../lib/api-client';
import type {
  Saga,
  SagaQuery,
  SagaQueryResponse,
  SagaEvent,
  SagaAction,
  CreateSagaRequest,
  ExecuteSagaStepRequest,
  OrchestrationTemplate,
  CreateOrchestrationTemplateRequest,
  UpdateOrchestrationTemplateRequest,
  MermaidDiagram,
  SequenceDiagram,
  StateMachine,
  WorkflowKpi,
} from '@cerp/shared-types';

// Saga endpoints use /api/sagas (proxied by Next.js API route to backend /api/v1/sagas)
const SAGA_API_BASE = '/api/sagas';

// Other workflow endpoints use /api/workflows (gateway routes to workflow-service)
const WORKFLOW_API_BASE = '/api/workflows';

/**
 * Saga orchestration API client.
 *
 * Provides methods for managing saga instances and orchestration templates.
 */
export const workflowApi = {
  // ── Saga CRUD ──

  /**
   * List sagas with filtering and pagination.
   */
  listSagas: async (query: SagaQuery): Promise<SagaQueryResponse> => {
    const params = new URLSearchParams();
    if (query.sagaType) params.set('sagaType', query.sagaType);
    if (query.status) params.set('status', query.status);
    if (query.tenantId) params.set('tenantId', query.tenantId);
    if (query.vendorId) params.set('vendorId', query.vendorId);
    if (query.aggregateId) params.set('aggregateId', query.aggregateId);
    if (query.page) params.set('page', String(query.page));
    if (query.pageSize) params.set('pageSize', String(query.pageSize));
    if (query.sortBy) params.set('sortBy', query.sortBy);
    if (query.sortOrder) params.set('sortOrder', query.sortOrder);

    return apiClient.get<SagaQueryResponse>(`${SAGA_API_BASE}?${params}`);
  },

  /**
   * Get a single saga by ID.
   */
  getSaga: async (sagaId: string): Promise<Saga> => {
    return apiClient.get<Saga>(`${SAGA_API_BASE}/${sagaId}`);
  },

  /**
   * Get all events for a saga.
   */
  getSagaEvents: async (sagaId: string): Promise<SagaEvent[]> => {
    return apiClient.get<SagaEvent[]>(`${SAGA_API_BASE}/${sagaId}/events`);
  },

  /**
   * Create a new saga instance.
   */
  createSaga: async (request: CreateSagaRequest): Promise<Saga> => {
    return apiClient.post<CreateSagaRequest, Saga>(SAGA_API_BASE, request);
  },

  /**
   * Update an existing saga's data without changing its state.
   */
  updateSaga: async (sagaId: string, data: Record<string, unknown>): Promise<Record<string, unknown>> => {
    return apiClient.put<Record<string, unknown>, Record<string, unknown>>(`${SAGA_API_BASE}/${sagaId}`, data);
  },

  // ── Saga Actions ──

  /**
   * Execute the next step in a saga.
   */
  executeStep: async (sagaId: string, request: ExecuteSagaStepRequest): Promise<Saga> => {
    const response = await apiClient.post<ExecuteSagaStepRequest, Record<string, unknown>>(`${SAGA_API_BASE}/${sagaId}/execute`, request);
    // Transform the backend response into a Saga object
    const contextData = (response.context as Record<string, unknown>) || {};
    return {
      sagaId: response.sagaId as string,
      sagaType: (response.sagaType as string) || (contextData.sagaType as string) || '',
      sagaTypeName: (response.sagaTypeName as string) || (contextData.sagaType as string) || '',
      status: (response.status as Saga['status']) || 'RUNNING',
      currentStep: (response.currentStep as string) || (contextData.currentStep as string) || '',
      tenantId: (response.tenantId as string) || '',
      createdAt: (response.createdAt as string) || new Date().toISOString(),
      updatedAt: (response.updatedAt as string) || new Date().toISOString(),
      steps: (response.steps as unknown[][]) || [],
      data: {
        ...(response.data as Record<string, unknown>) || {},
        ...contextData,
      },
    } as unknown as Saga;
  },

  /**
   * Perform a workflow action (resume, compensate, escalate, etc.).
   */
  performAction: async (sagaId: string, action: SagaAction): Promise<Saga> => {
    return apiClient.post<SagaAction, Saga>(`${SAGA_API_BASE}/${sagaId}/action`, action);
  },

  /**
   * Manually compensate a saga (rollback).
   */
  compensateSaga: async (sagaId: string, reason?: string): Promise<Saga> => {
    const response = await apiClient.post<SagaAction, Record<string, unknown>>(`${SAGA_API_BASE}/${sagaId}/compensate`, {
      action: 'compensate',
      reason,
    } as SagaAction);
    const contextData = (response.context as Record<string, unknown>) || {};
    return {
      sagaId: response.sagaId as string,
      sagaType: (response.sagaType as string) || (contextData.sagaType as string) || '',
      sagaTypeName: (response.sagaTypeName as string) || (contextData.sagaType as string) || '',
      status: (response.status as Saga['status']) || 'COMPLETED',
      currentStep: (response.currentStep as string) || (contextData.currentStep as string) || '',
      tenantId: (response.tenantId as string) || '',
      createdAt: (response.createdAt as string) || new Date().toISOString(),
      updatedAt: (response.updatedAt as string) || new Date().toISOString(),
      steps: (response.steps as unknown[][]) || [],
      data: {
        ...(response.data as Record<string, unknown>) || {},
        ...contextData,
      },
    } as unknown as Saga;
  },

  /**
   * Pause a running saga.
   */
  pauseSaga: async (sagaId: string): Promise<Record<string, unknown>> => {
    return apiClient.post(`${SAGA_API_BASE}/${sagaId}/pause`, {});
  },

  /**
   * Start/resume a paused saga (transition from PAUSED to RUNNING).
   */
  startSaga: async (sagaId: string): Promise<Record<string, unknown>> => {
    return apiClient.post(`${SAGA_API_BASE}/${sagaId}/start`, {});
  },

  /**
   * Resume a paused saga (legacy resume for REQUIRES_MANUAL_INTERVENTION sagas).
   */
  resumeSaga: async (sagaId: string, stepCode?: string): Promise<Saga> => {
    return apiClient.post<SagaAction, Saga>(`${SAGA_API_BASE}/${sagaId}/resume`, {
      action: 'resume',
      stepCode,
    } as SagaAction);
  },

  /**
   * Escalate a saga step.
   */
  escalateSaga: async (sagaId: string, reason?: string): Promise<Saga> => {
    return apiClient.post<SagaAction, Saga>(`${SAGA_API_BASE}/${sagaId}/escalate`, {
      action: 'escalate',
      reason,
    } as SagaAction);
  },

  // ─ Orchestration Templates ──

  /**
   * List all orchestration templates.
   * Note: The backend returns templates without steps in the list endpoint.
   * Steps are fetched lazily when viewing template details.
   */
  listTemplates: async (): Promise<Omit<OrchestrationTemplate, 'steps'>[]> => {
    const templates = await apiClient.get<Record<string, unknown>[]>(`${WORKFLOW_API_BASE}/templates`);
    return templates.map((t) => ({
      templateId: (t.id as string) ?? (t.templateId as string) ?? '',
      code: (t.code as string) ?? '',
      name: (t.name as string) ?? '',
      description: (t.description as string) ?? '',
      version: (t.version as number) ?? 1,
      status: (t.status as OrchestrationTemplate['status']) ?? 'Draft',
      steps: [] as OrchestrationTemplate['steps'],
      metadata: (t.metadata as OrchestrationTemplate['metadata']) ?? {
        createdBy: '',
        createdAt: '',
        updatedAt: '',
      },
    }));
  },

  /**
   * Get a single template by code with all steps.
   */
  getTemplateWithSteps: async (templateCode: string): Promise<OrchestrationTemplate> => {
    const result = await apiClient.get<{ template: Record<string, unknown>; steps: Record<string, unknown>[] }>(
      `${WORKFLOW_API_BASE}/templates/${templateCode}`
    );
    const { template, steps } = result;
    return {
      templateId: (template.id as string) ?? '',
      code: (template.code as string) ?? '',
      name: (template.name as string) ?? '',
      description: (template.description as string) ?? '',
      version: (template.version as number) ?? 1,
      status: (template.status as OrchestrationTemplate['status']) ?? 'Draft',
      steps: (steps as OrchestrationTemplate['steps']) ?? [],
      metadata: (template.metadata as OrchestrationTemplate['metadata']) ?? {
        createdBy: '',
        createdAt: '',
        updatedAt: '',
      },
    };
  },

  /**
   * Get a single template by code.
   */
  getTemplate: async (templateCode: string): Promise<OrchestrationTemplate> => {
    return apiClient.get<OrchestrationTemplate>(
      `${WORKFLOW_API_BASE}/templates/${templateCode}`
    );
  },

  /**
   * Create a new orchestration template.
   */
  createTemplate: async (request: CreateOrchestrationTemplateRequest): Promise<OrchestrationTemplate> => {
    return apiClient.post<CreateOrchestrationTemplateRequest, OrchestrationTemplate>(
      `${WORKFLOW_API_BASE}/templates`,
      request
    );
  },

  /**
   * Update an existing orchestration template (uses PATCH).
   */
  updateTemplate: async (templateCode: string, request: UpdateOrchestrationTemplateRequest): Promise<OrchestrationTemplate> => {
    return apiClient.patch<UpdateOrchestrationTemplateRequest, OrchestrationTemplate>(
      `${WORKFLOW_API_BASE}/templates/${templateCode}`,
      request
    );
  },

  /**
   * Delete an orchestration template.
   */
  deleteTemplate: async (templateCode: string): Promise<void> => {
    await apiClient.delete(`${WORKFLOW_API_BASE}/templates/${templateCode}`);
  },

  // ── Diagrams ──

  /**
   * Get all Mermaid diagrams for workflows.
   */
  listDiagrams: async (workflowType?: string): Promise<MermaidDiagram[]> => {
    const params = workflowType ? `?workflowType=${workflowType}` : '';
    return apiClient.get<MermaidDiagram[]>(`${WORKFLOW_API_BASE}/diagrams${params}`);
  },

  /**
   * Get a specific Mermaid diagram.
   */
  getDiagram: async (diagramId: string): Promise<MermaidDiagram> => {
    return apiClient.get<MermaidDiagram>(`${WORKFLOW_API_BASE}/diagrams/${diagramId}`);
  },

  /**
   * Get all sequence diagrams.
   */
  listSequenceDiagrams: async (): Promise<SequenceDiagram[]> => {
    return apiClient.get<SequenceDiagram[]>(`${WORKFLOW_API_BASE}/sequence-diagrams`);
  },

  // ── State Machines ──

  /**
   * Get all state machine definitions.
   */
  listStateMachines: async (): Promise<StateMachine[]> => {
    return apiClient.get<StateMachine[]>(`${WORKFLOW_API_BASE}/state-machines`);
  },

  /**
   * Get a specific state machine.
   */
  getStateMachine: async (aggregate: string): Promise<StateMachine> => {
    return apiClient.get<StateMachine>(`${WORKFLOW_API_BASE}/state-machines/${aggregate}`);
  },

  // ── KPIs ──

  /**
   * Get all workflow KPIs.
   */
  listKpis: async (): Promise<WorkflowKpi[]> => {
    return apiClient.get<WorkflowKpi[]>(`${WORKFLOW_API_BASE}/kpis`);
  },

  // ── Health ──

  /**
   * Check workflow service health.
   */
  health: async (): Promise<{ healthy: boolean; services: Record<string, boolean> }> => {
    return apiClient.get(`${WORKFLOW_API_BASE}/health`);
  },
};