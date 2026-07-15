import type { AuthUser } from './auth';

export interface Organisation {
  id: string;
  name: string;
  owners: AuthUser[];
  employees: AuthUser[];
  createdAt?: string;
  updatedAt?: string;
}

export type InviteStatus = 'pending' | 'accepted' | 'expired';

export interface OrganisationInvite {
  id: string;
  orgId: string;
  code: string;
  status: InviteStatus;
  createdByUserId: string;
  createdAt: string;
  acceptedByUserId?: string;
  acceptedAt?: string;
}