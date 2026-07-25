export interface Vendor {
  id: string;
  tenantId: string;
  legalName: string;
  status: 'pending' | 'approved' | 'rejected';
  taxProfile?: Record<string, unknown>;
  categories: string[];
  city?: string;
  ratingAvg?: number;
  createdAt: string;
  updatedAt?: string;
}