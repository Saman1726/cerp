import type { SagaStatus } from '@cerp/shared-types';

/**
 * Re-export workflow types from shared-types for convenience.
 */
export type {
  Saga,
  SagaStatus,
  SagaStep,
  SagaQuery,
  SagaQueryResponse,
  SagaEvent,
  SagaAction,
  CreateSagaRequest,
  ExecuteSagaStepRequest,
  OrchestrationTemplate,
  OrchestrationTemplateStep,
  OrchestrationCondition,
  OrchestrationTemplateStatus,
  OrchestrationStepType,
  CreateOrchestrationTemplateRequest,
  UpdateOrchestrationTemplateRequest,
  MermaidDiagram,
  SequenceDiagram,
  SequenceDiagramNode,
  SequenceDiagramMessage,
  StateMachine,
  StateTransition,
  WorkflowKpi,
} from '@cerp/shared-types';

/**
 * UI-specific types for the saga monitoring interface.
 */

export type SagaFilterStatus = SagaStatus | 'ALL';

export interface SagaTableFilters {
  status: SagaFilterStatus;
  sagaType: string;
  tenantId: string;
  search: string;
}

export type SagaDetailTabId = 'overview' | 'steps' | 'events' | 'compensation';

export interface SagaDetailTab {
  id: SagaDetailTabId;
  label: string;
}

export const SAGA_DETAIL_TABS: SagaDetailTab[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'steps', label: 'Steps' },
  { id: 'events', label: 'Events Log' },
  { id: 'compensation', label: 'Compensation' },
];

export const SAGA_TYPE_LABELS: Record<string, string> = {
  PROCUREMENT_WORKFLOW: 'Procurement → Delivery → Financials',
  DELIVERY_WORKFLOW: 'Material Delivery & Discrepancy',
  BIDDING_WORKFLOW: 'Marketplace Bidding',
  PROGRESS_CLAIM_WORKFLOW: 'Subcontractor Progress → Payment',
  // Procurement Saga step labels
  CreateProcurementRequest: 'Create Procurement Request',
  SubmitForApproval: 'Submit for Approval',
  ApproveRequest: 'Approve Request',
  IssuePurchaseOrder: 'Issue Purchase Order',
  ReceiveDelivery: 'Receive Delivery',
  ConfirmDelivery: 'Confirm Delivery',
  CloseProcurement: 'Close Procurement',
  // Bidding Saga step labels
  'Tender.CreateAndOpen': 'Create & Open Tender',
  'Tender.InviteVendors': 'Invite Vendors',
  'Bidding.CollectBids': 'Collect Bids',
  'Bidding.Evaluate': 'Evaluate Bids',
  'Tender.Award': 'Award Tender',
};

// ── Procurement Saga Step Definitions ──

export const PROCUREMENT_SAGA_STEPS: { code: string; label: string; description: string }[] = [
  { code: 'CreateProcurementRequest', label: 'Create Request', description: 'Create a new procurement request' },
  { code: 'SubmitForApproval', label: 'Submit for Approval', description: 'Submit the request for approval' },
  { code: 'ApproveRequest', label: 'Approve Request', description: 'Approve or reject the request' },
  { code: 'IssuePurchaseOrder', label: 'Issue PO', description: 'Issue purchase order to vendor' },
  { code: 'ReceiveDelivery', label: 'Receive Delivery', description: 'Receive goods at site' },
  { code: 'ConfirmDelivery', label: 'Confirm Delivery', description: 'Confirm delivery accepted' },
  { code: 'CloseProcurement', label: 'Close', description: 'Close the procurement' },
];

// ── Bidding Saga Step Definitions ──

export const BIDDING_SAGA_STEPS: { code: string; label: string; description: string }[] = [
  { code: 'Tender.CreateAndOpen', label: 'Create Tender', description: 'Create and open a tender' },
  { code: 'Tender.InviteVendors', label: 'Invite Vendors', description: 'Invite vendors to bid' },
  { code: 'Bidding.CollectBids', label: 'Collect Bids', description: 'Collect bids from vendors' },
  { code: 'Bidding.Evaluate', label: 'Evaluate Bids', description: 'Evaluate received bids' },
  { code: 'Tender.Award', label: 'Award Tender', description: 'Award tender and issue PO' },
];

export const SAGA_STATUS_LABELS: Record<string, string> = {
  RUNNING: 'Running',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  COMPENSATING: 'Compensating',
  COMPENSATED: 'Compensated',
  REQUIRES_MANUAL_INTERVENTION: 'Needs Attention',
};

export const SAGA_STATUS_COLORS: Record<string, string> = {
  ALL: 'bg-gray-100 text-gray-800',
  RUNNING: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  COMPENSATING: 'bg-yellow-100 text-yellow-800',
  COMPENSATED: 'bg-purple-100 text-purple-800',
  REQUIRES_MANUAL_INTERVENTION: 'bg-orange-100 text-orange-800',
};

export const STEP_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-gray-100 text-gray-800',
  EXECUTING: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
  COMPENSATING: 'bg-yellow-100 text-yellow-800',
  COMPENSATED: 'bg-purple-100 text-purple-800',
};

export const STEP_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Pending',
  EXECUTING: 'Executing',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  COMPENSATING: 'Compensating',
  COMPENSATED: 'Compensated',
};