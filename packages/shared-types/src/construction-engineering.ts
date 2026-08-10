// ── Project Types ──

export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';

export type ProjectType =
  | 'NEW_CONSTRUCTION'
  | 'RENOVATION'
  | 'MAINTENANCE'
  | 'INFRASTRUCTURE'
  | 'COMMERCIAL'
  | 'RESIDENTIAL'
  | 'INDUSTRIAL';

// ── Project User Role Types ──

export type ProjectUserRole = 'OWNER' | 'MANAGER' | 'SUPERVISOR' | 'VIEWER';

/**
 * User assignment with role for project-based access control.
 * - VIEWER: Read-only access to the project
 * - SUPERVISOR: Oversight role; permission semantics beyond labeling/quota are not yet defined
 * - MANAGER: Can modify project content (work orders, subprojects, etc.)
 * - OWNER: Full control including user management and role assignment. The project's
 *   creator is automatically and permanently OWNER (see the backend's primaryOwnerId) -
 *   additional OWNER assignments beyond the creator are also possible, plan limits
 *   permitting.
 */
export interface ProjectUserAssignment {
  userId: string;
  role: ProjectUserRole;
}

/**
 * A project assignment enriched with the assignee's email, for display. userId is whatever is
 * actually stored on the backend (a UUID going forward, potentially a raw email for older
 * assignments); email is always the best available human-readable form.
 */
export interface ProjectAssignedUser {
  userId: string;
  email: string;
  role: ProjectUserRole;
}

export interface Project {
  id: string;
  code: string;
  name: string;
  description: string | null;
  constructionSiteId: string | null;
  propertyId: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  postalCode: string | null;
  city: string | null;
  countryCode: string | null;
  status: ProjectStatus;
  type: ProjectType;
  startDate: string | null;
  endDate: string | null;
  actualStartDate: string | null;
  actualEndDate: string | null;
  progress: number | null;
  budget: number | null;
  currencyCode: string | null;
  clientName: string | null;
  clientEmail: string | null;
  clientPhone: string | null;
  syncStatus: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  assignedUserIds: string[];
  /** User roles mapping: { "user@example.com": "VIEWER" } */
  assignedUserRoles: Record<string, ProjectUserRole>;
  subProjects: SubProject[];
}

export interface CreateProjectRequest {
  code: string;
  name: string;
  orgId: string;
  description?: string;
  constructionSiteId?: string;
  propertyId?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  status?: ProjectStatus;
  projectType?: ProjectType;
  startDate?: string;
  endDate?: string;
  budget?: number;
  currencyCode?: string;
  clientName?: string;
  clientEmail?: string;
  clientPhone?: string;
}

// Alias for backward compatibility with existing code
export type ProjectCreateRequest = CreateProjectRequest;

export interface ProjectResponse {
  id: string;
  code: string;
  name: string;
  orgId: string;
  description: string | null;
  constructionSiteId: string | null;
  propertyId: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  postalCode: string | null;
  city: string | null;
  countryCode: string | null;
  status: ProjectStatus;
  projectType: ProjectType;
  startDate: string | null;
  endDate: string | null;
  actualStartDate: string | null;
  actualEndDate: string | null;
  progress: number | null;
  budget: number | null;
  currencyCode: string | null;
  clientName: string | null;
  clientEmail: string | null;
  clientPhone: string | null;
  syncStatus: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  assignedUserIds: string[];
  /** User roles mapping: { "user@example.com": "VIEWER" } */
  assignedUserRoles: Record<string, ProjectUserRole>;
}

export interface UpdateProjectRequest {
  code?: string;
  name?: string;
  description?: string;
  constructionSiteId?: string;
  propertyId?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  status?: ProjectStatus;
  type?: ProjectType;
  startDate?: string;
  endDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  budget?: number;
  currencyCode?: string;
  clientName?: string;
  clientEmail?: string;
  clientPhone?: string;
  assignedUserIds?: string[];
}

export interface AddAssignUsersRequest {
  userIds: string[];
}

export interface ProjectUserAssignRequest {
  assignments: ProjectUserAssignment[];
}

// ── SubProject Types ──

// Note: SubProjectStatus and SubProjectBuildingType are already defined later in this file (lines ~232-254)
// Using the existing definitions to avoid duplicates

export interface SubProject {
  id: string;
  code: string;
  name: string;
  description: string | null;
  buildingType: SubProjectBuildingType;
  constructionPhase: SubProjectConstructionPhase | null;
  status: SubProjectStatus;
  projectId: string;
  startDate: string | null;
  endDate: string | null;
  actualStartDate: string | null;
  actualEndDate: string | null;
  progress: number | null;
  budget: number | null;
  currencyCode: string | null;
  buildingId: string | null;
  externalId: string | null;
  syncStatus: string | null;
  floors: SubProjectFloor[];
  buildingParts: SubProjectBuildingPart[];
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
}

export interface CreateSubProjectRequest {
  code: string;
  name: string;
  description?: string;
  projectId: string;
  buildingId?: string;
  buildingType?: SubProjectBuildingType;
  constructionPhase?: SubProjectConstructionPhase;
}

export interface UpdateSubProjectRequest {
  code?: string;
  name?: string;
  description?: string;
  buildingType?: SubProjectBuildingType;
  constructionPhase?: SubProjectConstructionPhase;
  status?: SubProjectStatus;
  startDate?: string;
  endDate?: string;
  budget?: number;
}

// ── Workflow Types ──

export type WorkflowStatus = 'PLANNED' | 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'APPROVED' | 'REJECTED' | 'CANCELLED';
export type WorkflowStepStatus = 'PENDING' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED' | 'APPROVED' | 'REJECTED';
export type WorkflowStepType = 'TASK' | 'MILESTONE' | 'INSPECTION' | 'APPROVAL';
export type WorkflowTemplateStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

// ── Workflow ──

export interface Workflow {
  id: string;
  code: string;
  name: string;
  description: string | null;
  subprojectIds: string[];
  templateId: string | null;
  templateCode: string | null;
  status: WorkflowStatus;
  phaseType: string;
  currentStepId: string | null;
  currentStepOrder: number | null;
  progress: number;
  completedSteps: number;
  totalSteps: number;
  parameters: Record<string, unknown> | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  steps:WorkflowInstanceStep[];
}

export interface WorkflowResponse {
  workflow: Workflow;
  steps: WorkflowInstanceStep[];
  notes: WorkflowNote[];
}

// ── Workflow Step ──

export interface WorkflowInstanceStep {
  id: string;
  templateStepId: string | null;
  code: string;
  name: string;
  description: string | null;
  type: WorkflowStepType;
  order: number;
  status: WorkflowStepStatus;
  assignedTo: string | null;
  assignedToName: string | null;
  requiresEvidence: boolean;
  requiresApproval: boolean;
  evidenceUrls: string[];
  approverId: string | null;
  approverName: string | null;
  approvalNotes: string | null;
  startAt: string | null;
  dueAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

// ── Workflow Template ──

export interface WorkflowTemplate {
  id: string;
  code: string;
  name: string;
  description: string | null;
  phaseType: string;
  status: WorkflowTemplateStatus;
  parametersSchema: string | null;
  requiredDocuments: string[];
  steps: WorkflowTemplateStep[];
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowTemplateStep {
  id: string;
  templateId: string;
  code: string;
  name: string;
  description: string | null;
  type: WorkflowStepType;
  order: number;
  weight: number | null;
  requiresEvidence: boolean;
  requiresApproval: boolean;
  approverRole: string | null;
  slaHours: number | null;
  checklists: unknown[] | null;
  gatingCondition: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowTemplateResponse {
  template: WorkflowTemplate;
  steps: WorkflowTemplateStep[];
}

// ── Workflow Note ──

export interface WorkflowNote {
  id: string;
  workflowId: string;
  content: string;
  createdBy: string;
  createdAt: string;
}

// ── Workflow Requests ──

export interface WorkflowCreateRequest {
  templateCode: string;
  subprojectId: string;
  phaseType: string;
  parameters?: Record<string, unknown>;
}

export interface WorkflowInstanceStepUpdateRequest {
  status: WorkflowStepStatus;
  assignedTo?: string;
  evidenceUrls?: string[];
  approvalNotes?: string;
}

export interface WorkflowStepCompleteRequest {
  status: 'COMPLETED' | 'SKIPPED';
  evidenceUrls?: string[];
  approvalNotes?: string;
}

export interface WorkflowGenerationRequest {
  subprojectId: string;
  templateCodes?: string[];
  phaseType?: string;
}

export interface OrganisationWorkflowConfig {
  id: string;
  tenantId: string;
  orgId: string;
  autoGenerateOnSubprojectCreation: boolean;
  selectedTemplateCodes: string[];
  customPhaseMappings: Record<string, string[]>;
  createdAt: string;
  updatedAt: string;
}

export interface OrganisationWorkflowConfigRequest {
  autoGenerateOnSubprojectCreation?: boolean;
  selectedTemplateCodes?: string[];
  customPhaseMappings?: Record<string, string[]>;
}

export interface TemplatePhaseMapping {
  id: string;
  templateId: string;
  templateCode: string;
  phaseType: string;
  scopeLevel: string | null;
  buildingTypeGroup: string;
  isStandard: boolean;
  sortOrder: number;
}

export interface TemplatePhaseMappingRequest {
  templateCode: string;
  phaseType: string;
  scopeLevel?: string;
  buildingTypeGroup: string;
  isStandard?: boolean;
  sortOrder?: number;
}

export interface TemplatePhaseMappingFilter {
  templateCode?: string;
  phaseType?: string;
  scopeLevel?: string;
  buildingTypeGroup?: string;
}

// ── Workflow SubProject Floor ──

export interface SubProjectFloor {
  id: string;
  code: string;
  name: string;
  description: string | null;
  level: number;
  area: number | null;
}

export interface SubProjectBuildingPart {
  id: string;
  code: string;
  name: string;
  description: string | null;
  scopeLevel: string;
}

export type SubProjectBuildingType =
  | 'RESIDENTIAL'
  | 'COMMERCIAL'
  | 'INDUSTRIAL'
  | 'MIXED_USE'
  | 'INFRASTRUCTURE'
  | 'OTHER';

export type SubProjectConstructionPhase =
  | 'PRE_CONSTRUCTION'
  | 'SITE_PREP'
  | 'FOUNDATION'
  | 'BASEMENT'
  | 'SUPERSTRUCTURE'
  | 'ROOF'
  | 'BUILDING_ENVELOPE'
  | 'MEP_ROUGHIN'
  | 'INTERIOR_FINISH'
  | 'EXTERIOR_WORKS'
  | 'HANDOVER';

export type SubProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';

export interface SubProjectResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  buildingType: SubProjectBuildingType;
  constructionPhase: SubProjectConstructionPhase | null;
  status: SubProjectStatus;
  projectId: string;
  startDate: string | null;
  endDate: string | null;
  actualStartDate: string | null;
  actualEndDate: string | null;
  progress: number | null;
  budget: number | null;
  currencyCode: string | null;
  externalId: string | null;
  syncStatus: string | null;
  floors: SubProjectFloor[];
  buildingParts: SubProjectBuildingPart[];
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
}

export interface SubProjectWorkflowRequest {
  subprojectId: string;
  templateCodes?: string[];
  phaseType?: string;
}

export interface SubProjectWorkflowResponse {
  workflows: Workflow[];
  steps: WorkflowInstanceStep[];
  templates: WorkflowTemplate[];
}
