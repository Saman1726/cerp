import { z } from 'zod';

export type WorkOrderStatus = 'issued' | 'completed';

export interface WorkOrder {
  id: string;
  projectId: string;
  title: string;
  description: string | null;
  contractorPartnerId: string;
  contractorPartnerName: string;
  status: WorkOrderStatus;
  issuedAt: string;
  completedAt: string | null;
  updatedAt: string;
}

export const createWorkOrderRequestSchema = z.object({
  projectId: z.string().uuid(),
  title: z.string().min(1).max(200),
  description: z.string().max(2000).optional(),
  contractorPartnerId: z.string().min(1),
});
export type CreateWorkOrderRequest = z.infer<typeof createWorkOrderRequestSchema>;
