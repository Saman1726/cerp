import { z } from 'zod';
import type { VendorCategoryLink } from './vendor';

// ── Goods Receipt & Purchase Order Types ──

export interface GoodsReceipt {
  id: string;
  purchaseOrderId: string;
  notes: string | null;
  receivedAt: string;
}

export type PurchaseOrderStatus = 'ordered' | 'received';

export interface PurchaseOrder {
  id: string;
  projectId: string;
  vendorPartnerId: string;
  vendorPartnerName: string;
  description: string;
  status: PurchaseOrderStatus;
  orderedAt: string;
  updatedAt: string;
}

export type PurchaseOrderWithReceipts = PurchaseOrder & {
  receipts: GoodsReceipt[];
};

export const raisePurchaseOrderRequestSchema = z.object({
  projectId: z.string().uuid(),
  vendorPartnerId: z.string().min(1),
  description: z.string().min(1).max(500),
});
export type RaisePurchaseOrderRequest = z.infer<typeof raisePurchaseOrderRequestSchema>;

export const recordGoodsReceiptRequestSchema = z.object({
  notes: z.string().max(2000).optional(),
});
export type RecordGoodsReceiptRequest = z.infer<typeof recordGoodsReceiptRequestSchema>;

// ── Vendor Category Link Schema (for validation, interface exported from vendor.ts) ──

export const VendorCategoryLinkSchema = z.object({
  categoryId: z.string(),
  categoryName: z.string(),
  categoryCode: z.string(),
});

// Note: VendorCategoryLink interface is exported from vendor.ts, import it there if needed

// ── Project Types (for selection in forms) ──

export interface ProjectSummary {
  id: string;
  code: string;
  name: string;
  description: string | null;
  status: string;
  city: string | null;
  countryCode: string | null;
}

export const projectSummarySchema = z.object({
  id: z.string(),
  code: z.string(),
  name: z.string(),
  description: z.string().nullable(),
  status: z.string(),
  city: z.string().nullable(),
  countryCode: z.string().nullable(),
});

// ── Vendor Partner Types (for selection in forms) ──

export interface VendorPartnerSummary {
  id: string;
  tenantId: string;
  legalName: string;
  status: 'pending' | 'approved' | 'rejected';
  city: string | null;
  categories: VendorCategoryLink[];
}

export const vendorPartnerSummarySchema = z.object({
  id: z.string(),
  tenantId: z.string(),
  legalName: z.string(),
  status: z.enum(['pending', 'approved', 'rejected']),
  city: z.string().nullable(),
  categories: z.array(VendorCategoryLinkSchema),
});

// ── Procurement Saga Types ──

/**
 * Request to create a ProcurementSaga instance.
 * Coordinates: Procurement Request → Approval → PO → Delivery → Close
 */
export const createProcurementSagaRequestSchema = z.object({
  sagaType: z.literal('PROCUREMENT_WORKFLOW'),
  tenantId: z.string(),
  sagaId: z.string().optional(), // Deterministic saga ID for idempotent creation
  aggregateId: z.string().optional(),
  data: z.object({
    sagaName: z.string().optional(), // Human-readable name for the saga
    projectId: z.string(),
    materialId: z.string().optional(),
    quantity: z.number().positive().optional(),
    vendorPartnerId: z.string().optional(),
    description: z.string().min(1).max(500).optional(),
    approvalData: z.object({
      approved: z.boolean().optional(),
      rejectionReason: z.string().optional(),
    }).optional(),
    poData: z.record(z.string(), z.any()).optional(),
  }),
});
export type CreateProcurementSagaRequest = z.infer<typeof createProcurementSagaRequestSchema>;

/**
 * Command to execute the next step in a ProcurementSaga.
 */
export const executeProcurementSagaStepRequestSchema = z.object({
  command: z.enum([
    'CreateProcurementRequest',
    'SubmitForApproval',
    'ApproveRequest',
    'IssuePurchaseOrder',
    'ReceiveDelivery',
    'ConfirmDelivery',
    'CloseProcurement',
  ]),
  data: z.record(z.string(), z.any()).optional(),
  idempotencyKey: z.string().optional(),
});
export type ExecuteProcurementSagaStepRequest = z.infer<typeof executeProcurementSagaStepRequestSchema>;

// ── Bidding Saga Types ──

/**
 * Bidding mode: INVITED or OPEN_TENDER
 * - INVITED: Specific vendors are invited to bid
 * - OPEN_TENDER: All vendors can bid (no specific vendorIds required)
 */
export type BiddingMode = 'INVITED' | 'OPEN_TENDER';

/**
 * Request to create a BiddingSaga instance from a Procurement.
 * Coordinates: Tender → Vendor Invitation → Bidding → Evaluation → Award
 *
 * Bidding is always related to a ProcurementSaga (not directly to projects).
 */
export const createBiddingSagaFromProcurementRequestSchema = z.object({
  sagaType: z.literal('BIDDING_WORKFLOW'),
  tenantId: z.string(),
  aggregateId: z.string().optional(),
  data: z.object({
    procurementSagaId: z.string(),
    biddingMode: z.enum(['INVITED', 'OPEN_TENDER']),
    vendorIds: z.array(z.string()).optional(), // Only required for INVITED mode
    projectId: z.string(), // Inherited from procurement
    description: z.string().optional(),
    inviteConfig: z.object({
      expectedBids: z.number().positive().optional(),
      bidDeadline: z.string().optional(),
    }).optional(),
    bids: z.array(z.object({
      vendorId: z.string(),
      price: z.number(),
      timeline: z.string().optional(),
      notes: z.string().optional(),
      paymentMethod: z.enum(['BANK_TRANSFER', 'LETTER_OF_CREDIT', 'CASH', 'INSTALLMENTS']).optional(),
    })).optional(),
    evaluation: z.record(z.string(), z.any()).optional(),
  }),
});
export type CreateBiddingSagaFromProcurementRequest = z.infer<typeof createBiddingSagaFromProcurementRequestSchema>;

/**
 * Legacy bidding saga request for backward compatibility.
 * Coordinates: Tender → Vendor Invitation → Bidding → Evaluation → Award
 */
export const createBiddingSagaRequestSchema = z.object({
  sagaType: z.literal('BIDDING_WORKFLOW'),
  tenantId: z.string(),
  aggregateId: z.string().optional(),
  data: z.object({
    projectId: z.string(),
    inviteConfig: z.object({
      expectedBids: z.number().positive().optional(),
      bidDeadline: z.string().optional(),
    }).optional(),
    vendorIds: z.array(z.string()),
    bids: z.array(z.object({
      vendorId: z.string(),
      price: z.number(),
      timeline: z.string().optional(),
      notes: z.string().optional(),
      paymentMethod: z.enum(['BANK_TRANSFER', 'LETTER_OF_CREDIT', 'CASH', 'INSTALLMENTS']).optional(),
    })).optional(),
    evaluation: z.record(z.string(), z.any()).optional(),
  }),
});
export type CreateBiddingSagaRequest = z.infer<typeof createBiddingSagaRequestSchema>;

/**
 * Command to execute the next step in a BiddingSaga.
 */
export const executeBiddingSagaStepRequestSchema = z.object({
  command: z.enum([
    'Tender.CreateAndOpen',
    'Tender.InviteVendors',
    'OpenTender.Publish',
    'Bidding.CollectBids',
    'Bidding.Evaluate',
    'Tender.Award',
  ]),
  data: z.record(z.string(), z.any()).optional(),
  idempotencyKey: z.string().optional(),
});
export type ExecuteBiddingSagaStepRequest = z.infer<typeof executeBiddingSagaStepRequestSchema>;