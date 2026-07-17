export interface Unit {
  id: string;
  label: string;
  usableArea: number;
  bedrooms: number;
  bathrooms: number;
  balconies: number;
  toilets: number;
}

export interface Floor {
  id: string;
  type: string;
  usableArea: number;
  totalArea: number;
  units: Unit[];
}

export interface Building {
  id: string;
  name: string;
  usableArea: number; // sum of floors
  totalArea: number; // sum of floors
  floors: Floor[];
}

export interface ConstructionSite {
  id: string;
  name: string;
  address: string;
  buildings: Building[];
  createdAt: string; // ISO timestamp
  updatedAt: string; // ISO timestamp
}

// Create/Update request shapes (frontend convenience)
export interface CreateConstructionSiteRequest {
  name: string;
  address: string;
}
export interface UpdateConstructionSiteRequest {
  name?: string;
  address?: string;
}

export interface CreateBuildingRequest {
  name: string;
}
export interface UpdateBuildingRequest {
  name?: string;
}

export interface CreateFloorRequest {
  type: string;
  usableArea: number;
  totalArea: number;
}
export interface UpdateFloorRequest {
  type?: string;
  usableArea?: number;
  totalArea?: number;
}

export interface CreateUnitRequest {
  label: string;
  usableArea: number;
  bedrooms: number;
  bathrooms: number;
  balconies: number;
  toilets: number;
}
export interface UpdateUnitRequest {
  label?: string;
  usableArea?: number;
  bedrooms?: number;
  bathrooms?: number;
  balconies?: number;
  toilets?: number;
}

// ── Project (Construction Project) ──

/**
 * Project status for construction projects.
 */
export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';

/**
 * Project type classification.
 */
export type ProjectType = 'RESIDENTIAL' | 'COMMERCIAL' | 'INDUSTRIAL' | 'INFRASTRUCTURE' | 'MIXED_USE' | 'OTHER';

/**
 * Represents a Construction Project that contains multiple SubProjects (Buildings).
 */
export interface Project {
  id: string;
  tenantId: string;
  orgId: string;
  externalId?: string;
  code: string;
  name: string;
  description?: string;
  type: ProjectType;
  status: ProjectStatus;
  // Construction Site linkage
  constructionSiteId?: string;
  // Location
  address?: string;
  city?: string;
  country?: string;
  // Dates
  startDate?: string;
  endDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  // Progress & budget
  progress?: number;
  budget?: number;
  currencyCode: string;
  // SubProjects (Buildings)
  subProjects: SubProject[];
  // Sync
  syncStatus?: 'NOT_SYNCED' | 'SYNCED' | 'SYNC_IN_PROGRESS' | 'SYNC_ERROR';
  // Audit
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Request DTO for creating a new Project.
 */
export interface ProjectCreateRequest {
  code: string;
  name: string;
  orgId?: string;
  description?: string;
  /** Backend field name is projectType (not type). Use ProjectType for enum values. */
  projectType?: ProjectType;
  status?: ProjectStatus;
  constructionSiteId?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  startDate?: string;
  endDate?: string;
  budget?: number;
  currencyCode?: string;
  externalId?: string;
}

/**
 * Request DTO for updating an existing Project.
 */
export interface ProjectUpdateRequest {
  code?: string;
  name?: string;
  description?: string;
  type?: ProjectType;
  status?: ProjectStatus;
  constructionSiteId?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  startDate?: string;
  endDate?: string;
  progress?: number;
  budget?: number;
  currencyCode?: string;
  externalId?: string;
}

/**
 * Response DTO for Project data (returned from API).
 */
export interface ProjectResponse {
  id: string;
  tenantId: string;
  orgId: string;
  externalId?: string;
  code: string;
  name: string;
  description?: string;
  type: ProjectType;
  status: ProjectStatus;
  constructionSiteId?: string;
  address?: string;
  city?: string;
  country?: string;
  startDate?: string;
  endDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  progress?: number;
  budget?: number;
  currencyCode: string;
  subProjects: SubProjectResponse[];
  syncStatus?: 'NOT_SYNCED' | 'SYNCED' | 'SYNC_IN_PROGRESS' | 'SYNC_ERROR';
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

// ── SubProject (Building as SubProject) ──

/**
 * Scope level for SubProject workflows.
 * - WHOLE: applies to the entire building/SubProject
 * - FLOOR: applies to specific floors within a SubProject
 * - PART: applies to specific building parts within a SubProject
 */
export type SubProjectScopeLevel = 'WHOLE' | 'FLOOR' | 'PART';

/**
 * Building type classification for SubProjects.
 */
export type SubProjectBuildingType =
  | 'RESIDENTIAL'
  | 'COMMERCIAL'
  | 'INDUSTRIAL'
  | 'MIXED_USE'
  | 'INFRASTRUCTURE'
  | 'CULTURAL'
  | 'EDUCATIONAL'
  | 'HEALTHCARE'
  | 'OTHER';

/**
 * Construction phase for SubProject tracking.
 * Matches the 23 construction phase workflow templates.
 */
export type SubProjectConstructionPhase =
  | 'PRE_CONSTRUCTION'
  | 'SITE_PREP'
  | 'FOUNDATION'
  | 'BASEMENT'
  | 'SUPERSTRUCTURE'
  | 'ROOF'
  | 'BUILDING_ENVELOPE'
  | 'INTERIOR_PARTITIONS'
  | 'MEP_ROUGHIN'
  | 'INTERIOR_FINISH'
  | 'EXTERIOR_WORKS'
  | 'TESTING_COMMISSIONING'
  | 'QUALITY_ASSURANCE'
  | 'SAFETY_MANAGEMENT'
  | 'PROCUREMENT'
  | 'EQUIPMENT_MANAGEMENT'
  | 'WORKFORCE_MANAGEMENT'
  | 'SUBCONTRACTOR_MANAGEMENT'
  | 'PROGRESS_MONITORING'
  | 'COST_CONTROL'
  | 'SNAGGING'
  | 'HANDOVER'
  | 'FACILITY_MANAGEMENT';

/**
 * SubProject represents a Building within a Project.
 * Each SubProject can have its own workflows scoped to the whole building,
 * specific floors, or specific building parts.
 */
export interface SubProject {
  id: string;
  projectId: string;
  tenantId: string;
  orgId: string;
  externalId?: string;
  code: string;
  name: string;
  description?: string;
  // Building specifications
  buildingType: SubProjectBuildingType;
  floors: SubProjectFloor[];
  buildingParts: SubProjectBuildingPart[];
  // Construction tracking
  constructionPhase: SubProjectConstructionPhase;
  status: SubProjectStatus;
  // Dates
  startDate?: string; // ISO date
  endDate?: string; // ISO date
  actualStartDate?: string; // ISO date
  actualEndDate?: string; // ISO date
  // Progress & budget
  progress?: number;
  budget?: number;
  currencyCode: string;
  // Sync
  syncStatus?: 'NOT_SYNCED' | 'SYNCED' | 'SYNC_IN_PROGRESS' | 'SYNC_ERROR';
  // Audit
  createdBy?: string;
  createdAt: string; // ISO timestamp
  updatedAt: string; // ISO timestamp
}

/**
 * Represents a floor within a SubProject (Building).
 */
export interface SubProjectFloor {
  id: string;
  code: string; // e.g., "F1", "F2", "GROUND", "BASEMENT_1"
  name: string;
  level: number; // negative for below ground, 0 for ground, positive for above
  area?: number;
  description?: string;
}

/**
 * Represents a building part within a SubProject.
 * Examples: facade, elevator_shaft, hvac_room, parking_garage, etc.
 */
export interface SubProjectBuildingPart {
  id: string;
  code: string;
  name: string;
  description?: string;
  /** Which scope this part belongs to (e.g., "WHOLE", "FLOOR_CODE") */
  scopeLevel: SubProjectScopeLevel;
}

/**
 * Status of a SubProject.
 */
export type SubProjectStatus =
  | 'PLANNED'
  | 'ACTIVE'
  | 'ON_HOLD'
  | 'COMPLETED'
  | 'CANCELLED';

/**
 * Request DTO for creating a new SubProject.
 */
export interface SubProjectCreateRequest {
  code: string;
  name: string;
  description?: string;
  buildingType: SubProjectBuildingType;
  constructionPhase?: SubProjectConstructionPhase;
  floors?: Array<{ code: string; name: string; level: number; area?: number }>;
  buildingParts?: Array<{ code: string; name: string; description?: string }>;
  startDate?: string;
  endDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  progress?: number;
  budget?: number;
  currencyCode?: string;
  externalId?: string;
}

/**
 * Request DTO for updating an existing SubProject.
 */
export interface SubProjectUpdateRequest {
  code?: string;
  name?: string;
  description?: string;
  buildingType?: SubProjectBuildingType;
  constructionPhase?: SubProjectConstructionPhase;
  status?: SubProjectStatus;
  floors?: Array<{ code: string; name: string; level: number; area?: number }>;
  buildingParts?: Array<{ code: string; name: string; description?: string }>;
  startDate?: string;
  endDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  progress?: number;
  budget?: number;
  currencyCode?: string;
  externalId?: string;
}

/**
 * Request DTO for creating a SubProject workflow with scope levels.
 */
export interface SubProjectWorkflowRequest {
  subProjectId: string;
  templateCode: string;
  templateVersion?: number;
  scopeLevel?: SubProjectScopeLevel;
  floorIds?: string[];
  partIds?: string[];
  startDate?: string;
  endDate?: string;
  dueDate?: string;
  parameters?: Record<string, unknown>;
}

/**
 * Response DTO for SubProject data (returned from API).
 */
export interface SubProjectResponse {
  id: string;
  projectId: string;
  tenantId: string;
  orgId: string;
  externalId?: string;
  code: string;
  name: string;
  description?: string;
  buildingType: SubProjectBuildingType;
  floors: SubProjectFloor[];
  buildingParts: SubProjectBuildingPart[];
  constructionPhase: SubProjectConstructionPhase;
  status: SubProjectStatus;
  startDate?: string;
  endDate?: string;
  actualStartDate?: string;
  actualEndDate?: string;
  progress?: number;
  budget?: number;
  currencyCode: string;
  syncStatus?: 'NOT_SYNCED' | 'SYNCED' | 'SYNC_IN_PROGRESS' | 'SYNC_ERROR';
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

// ── Workflow Template ──

/**
 * Workflow template category for grouping templates.
 */
export type WorkflowTemplateCategory =
  | 'CONSTRUCTION_PHASE'
  | 'MANAGEMENT'
  | 'POST_HANDOVER'
  | 'CUSTOM';

/**
 * Workflow template status.
 */
export type WorkflowTemplateStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED';

/**
 * Represents a reusable workflow template.
 */
export interface WorkflowTemplate {
  id: string;
  code: string;
  name: string;
  description?: string;
  category: WorkflowTemplateCategory;
  version: number;
  status: WorkflowTemplateStatus;
  scopeLevel: SubProjectScopeLevel;
  steps: WorkflowTemplateStep[];
  createdAt: string;
  updatedAt: string;
}

/**
 * Step within a workflow template.
 */
export interface WorkflowTemplateStep {
  id: string;
  code: string;
  name: string;
  description?: string;
  order: number;
  requiredDocuments?: string[];
  approvers?: string[];
}

/**
 * Request DTO for creating/updating a workflow template.
 */
export interface WorkflowTemplateCreateRequest {
  code: string;
  name: string;
  description?: string;
  category: WorkflowTemplateCategory;
  scopeLevel: SubProjectScopeLevel;
  steps: Array<{
    code: string;
    name: string;
    description?: string;
    order: number;
    requiredDocuments?: string[];
    approvers?: string[];
  }>;
}

/**
 * Request DTO for creating a workflow instance from a template.
 */
export interface WorkflowCreateRequest {
  templateId: string;
  projectId: string;
  subProjectId?: string;
  scopeLevel?: SubProjectScopeLevel;
  floorIds?: string[];
  partIds?: string[];
  startDate?: string;
  endDate?: string;
  dueDate?: string;
  description?: string;
}

/**
 * Workflow instance status.
 */
export type WorkflowStatus =
  | 'DRAFT'
  | 'INITIALIZED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'BLOCKED';

/**
 * Represents an active workflow instance.
 */
export interface Workflow {
  id: string;
  templateId: string;
  templateCode: string;
  projectId: string;
  subProjectId?: string;
  tenantId: string;
  scopeLevel: SubProjectScopeLevel;
  status: WorkflowStatus;
  description?: string;
  startDate?: string;
  endDate?: string;
  dueDate?: string;
  steps: WorkflowInstanceStep[];
  progress: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Step within a workflow instance.
 */
export interface WorkflowInstanceStep {
  id: string;
  templateStepId: string;
  code: string;
  name: string;
  description?: string;
  order: number;
  status: WorkflowStepStatus;
  startedAt?: string;
  completedAt?: string;
  approvedAt?: string;
  blockedAt?: string;
  blockReason?: string;
}

/**
 * Status of a workflow step instance.
 */
export type WorkflowStepStatus =
  | 'PENDING'
  | 'STARTED'
  | 'COMPLETED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'BLOCKED';

// ── Workflow instance step status transitions ──

export interface WorkflowStepUpdateRequest {
  userId: string;
  notes?: string;
  evidence?: string;
}