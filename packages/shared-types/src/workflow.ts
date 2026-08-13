import { z } from 'zod';

/**
 * Shared types for workflow saga orchestration (Section 7 Enhancement Plan).
 *
 * These types are for the saga-based workflow orchestration layer,
 * distinct from the construction workflow types in construction-engineering.ts.
 *
 * @see docs/workflows/section7-enhancement-plan.md
 */

// ── Saga Types ──

/**
 * Represents a running saga instance in the workflow orchestration system.
 */
export const sagaStatusSchema = z.enum([
  'RUNNING',
  'PAUSED',
  'COMPLETED',
  'FAILED',
  'COMPENSATING',
  'COMPENSATED',
  'REQUIRES_MANUAL_INTERVENTION',
]);
export type SagaStatus = z.infer<typeof sagaStatusSchema>;

/**
 * A single step executed within a saga.
 */
export const sagaStepSchema = z.object({
  stepCode: z.string(),
  stepName: z.string(),
  order: z.number(),
  status: z.enum(['PENDING', 'EXECUTING', 'COMPLETED', 'FAILED', 'COMPENSATING', 'COMPENSATED']),
  startedAt: z.string().optional(),
  completedAt: z.string().optional(),
  errorMessage: z.string().optional(),
  compensationErrorMessage: z.string().optional(),
});
export type SagaStep = z.infer<typeof sagaStepSchema>;

/**
 * Full saga context including all steps and data.
 */
export const sagaSchema = z.object({
  sagaId: z.string(),
  sagaType: z.string(),
  sagaTypeName: z.string(),
  aggregateId: z.string().optional(),
  status: sagaStatusSchema,
  currentStep: z.string().optional(),
  tenantId: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
  steps: z.array(sagaStepSchema),
  data: z.record(z.string(), z.any()),
  errorDetails: z.record(z.string(), z.any()).optional(),
  /** Missing/undefined means 0 (hide the attachments affordance), not "unknown" - same convention as WorkflowInstanceStep.attachmentCount. */
  noteCount: z.number().optional(),
  attachmentCount: z.number().optional(),
});
export type Saga = z.infer<typeof sagaSchema>;

// ── Saga Notes ──

export const sagaNoteTypeSchema = z.enum(['GENERAL', 'RISK', 'ISSUE', 'DECISION']);
export type SagaNoteType = z.infer<typeof sagaNoteTypeSchema>;

export const sagaNoteSchema = z.object({
  id: z.string(),
  sagaId: z.string(),
  authorId: z.string(),
  note: z.string(),
  type: sagaNoteTypeSchema,
  createdAt: z.string(),
});
export type SagaNote = z.infer<typeof sagaNoteSchema>;

export interface SagaNoteCreateRequest {
  note: string;
  /** Defaults to 'GENERAL' server-side when omitted. */
  type?: SagaNoteType;
}

// ── Orchestration Workflow Template Types ──

export const orchestrationStepTypeSchema = z.enum(['task', 'approval', 'auto', 'wait', 'parallel']);
export type OrchestrationStepType = z.infer<typeof orchestrationStepTypeSchema>;

export const orchestrationTemplateStatusSchema = z.enum(['Draft', 'Active', 'Deprecated']);
export type OrchestrationTemplateStatus = z.infer<typeof orchestrationTemplateStatusSchema>;

export const orchestrationTemplateStepSchema = z.object({
  stepId: z.string(),
  order: z.number(),
  code: z.string(),
  name: z.string(),
  type: orchestrationStepTypeSchema,
  assigneeRole: z.string().optional(),
  requiredFields: z.array(z.string()).optional(),
  requiredEvidence: z.array(z.string()).optional(),
  slaHours: z.number().optional(),
  autoForward: z.boolean().optional(),
  nextSteps: z.array(z.string()),
  onApprove: z.array(z.string()).optional(),
  onReject: z.array(z.string()).optional(),
});
export type OrchestrationTemplateStep = z.infer<typeof orchestrationTemplateStepSchema>;

export const orchestrationConditionSchema = z.object({
  name: z.string(),
  expression: z.string(),
  then: z.array(z.string()),
  else: z.array(z.string()).optional(),
});
export type OrchestrationCondition = z.infer<typeof orchestrationConditionSchema>;

export const orchestrationTemplateSchema = z.object({
  templateId: z.string(),
  code: z.string(),
  name: z.string(),
  description: z.string(),
  version: z.number(),
  status: orchestrationTemplateStatusSchema,
  steps: z.array(orchestrationTemplateStepSchema),
  conditions: z.array(orchestrationConditionSchema).optional(),
  metadata: z.object({
    createdBy: z.string(),
    createdAt: z.string(),
    updatedAt: z.string(),
    applicableCategories: z.array(z.string()).optional(),
    applicableRegions: z.array(z.string()).optional(),
  }),
});
export type OrchestrationTemplate = z.infer<typeof orchestrationTemplateSchema>;

// ── Command/Event Types ──

export const sagaCommandSchema = z.object({
  command: z.string(),
  aggregateId: z.string(),
  tenantId: z.string(),
  data: z.record(z.string(), z.any()).optional(),
  idempotencyKey: z.string().optional(),
});
export type SagaCommand = z.infer<typeof sagaCommandSchema>;

export const sagaEventSchema = z.object({
  eventId: z.string(),
  eventType: z.string(),
  sagaType: z.string(),
  sagaId: z.string(),
  aggregateId: z.string().optional(),
  tenantId: z.string(),
  timestamp: z.string(),
  data: z.record(z.string(), z.any()),
});
export type SagaEvent = z.infer<typeof sagaEventSchema>;

// ── Saga Query Types ──

export const sagaQuerySchema = z.object({
  sagaType: z.string().optional(),
  status: sagaStatusSchema.optional(),
  tenantId: z.string().optional(),
  vendorId: z.string().optional(),
  aggregateId: z.string().optional(),
  page: z.number().optional(),
  pageSize: z.number().optional(),
  sortBy: z.enum(['createdAt', 'updatedAt', 'status']).optional(),
  sortOrder: z.enum(['asc', 'desc']).optional(),
});
export type SagaQuery = z.infer<typeof sagaQuerySchema>;

export const sagaQueryResponseSchema = z.object({
  items: z.array(sagaSchema),
  total: z.number(),
  page: z.number(),
  pageSize: z.number(),
});
export type SagaQueryResponse = z.infer<typeof sagaQueryResponseSchema>;

// ── Workflow Action Types ──

export const sagaActionSchema = z.object({
  sagaId: z.string(),
  action: z.enum([
    'resume',
    'pause',
    'start',
    'compensate',
    'escalate',
    'complete_step',
    'insert_step',
    'remove_step',
  ]),
  stepCode: z.string().optional(),
  data: z.record(z.string(), z.any()).optional(),
  reason: z.string().optional(),
});
export type SagaAction = z.infer<typeof sagaActionSchema>;

// ── Workflow Diagram Types ──

export const sequenceDiagramNodeSchema = z.object({
  id: z.string(),
  label: z.string(),
  type: z.enum(['participant', 'actor', 'boundary', 'entity', 'collection', 'dollar']),
  participantType: z.string().optional(),
});
export type SequenceDiagramNode = z.infer<typeof sequenceDiagramNodeSchema>;

export const sequenceDiagramMessageSchema = z.object({
  from: z.string(),
  to: z.string(),
  label: z.string(),
  type: z.enum(['sync', 'async', 'reply', 'self']),
});
export type SequenceDiagramMessage = z.infer<typeof sequenceDiagramMessageSchema>;

export const sequenceDiagramSchema = z.object({
  title: z.string(),
  description: z.string().optional(),
  workflowType: z.string(),
  nodes: z.array(sequenceDiagramNodeSchema),
  messages: z.array(sequenceDiagramMessageSchema),
  notes: z
    .array(
      z.object({
        participant: z.string(),
        text: z.string(),
      }),
    )
    .optional(),
});
export type SequenceDiagram = z.infer<typeof sequenceDiagramSchema>;

// ── Mermaid Types ──

export const mermaidDiagramSchema = z.object({
  id: z.string(),
  title: z.string(),
  type: z.enum([
    'sequenceDiagram',
    'stateDiagram-v2',
    'flowChart',
    'gantt',
    'classDiagram',
    'erDiagram',
  ]),
  content: z.string(),
  workflowType: z.string(),
  description: z.string().optional(),
});
export type MermaidDiagram = z.infer<typeof mermaidDiagramSchema>;

// ── State Machine Types ──

export const stateTransitionSchema = z.object({
  from: z.string(),
  to: z.string(),
  event: z.string(),
  guard: z.string().optional(),
  action: z.string().optional(),
});
export type StateTransition = z.infer<typeof stateTransitionSchema>;

export const stateMachineSchema = z.object({
  aggregate: z.string(),
  states: z.array(z.string()),
  transitions: z.array(stateTransitionSchema),
  initial: z.string(),
  final: z.array(z.string()),
});
export type StateMachine = z.infer<typeof stateMachineSchema>;

// ── KPI Types ──

export const workflowKpiSchema = z.object({
  code: z.string(),
  name: z.string(),
  description: z.string(),
  measurementFormula: z.string(),
  workflowSteps: z.array(z.string()),
  target: z.string(),
  dataSource: z.string(),
});
export type WorkflowKpi = z.infer<typeof workflowKpiSchema>;

// ── API Request/Response Schemas ──

export const createSagaRequestSchema = z.object({
  sagaType: z.string(),
  aggregateId: z.string().optional(),
  tenantId: z.string(),
  data: z.record(z.string(), z.any()),
});
export type CreateSagaRequest = z.infer<typeof createSagaRequestSchema>;

export const executeSagaStepRequestSchema = z.object({
  command: z.string(),
  data: z.record(z.string(), z.any()).optional(),
  idempotencyKey: z.string().optional(),
});
export type ExecuteSagaStepRequest = z.infer<typeof executeSagaStepRequestSchema>;

export const createOrchestrationTemplateRequestSchema = z.object({
  code: z.string(),
  name: z.string(),
  description: z.string(),
  steps: z.array(orchestrationTemplateStepSchema),
  conditions: z.array(orchestrationConditionSchema).optional(),
  applicableCategories: z.array(z.string()).optional(),
  applicableRegions: z.array(z.string()).optional(),
});
export type CreateOrchestrationTemplateRequest = z.infer<
  typeof createOrchestrationTemplateRequestSchema
>;

export const updateOrchestrationTemplateRequestSchema =
  createOrchestrationTemplateRequestSchema.extend({
    version: z.number().optional(),
    status: orchestrationTemplateStatusSchema.optional(),
  });
export type UpdateOrchestrationTemplateRequest = z.infer<
  typeof updateOrchestrationTemplateRequestSchema
>;
