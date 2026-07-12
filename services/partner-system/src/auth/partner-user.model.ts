export interface PartnerUser {
  username: string;
  password: string;
  partnerCode: string;
  role: string;
}

// Mock credential store - a real partner system would have its own user DB.
export const PARTNER_USERS: PartnerUser[] = [
  { username: 'jane', password: 'partner123', partnerCode: 'ACME-SUPPLY', role: 'account-manager' },
  { username: 'bob', password: 'partner123', partnerCode: 'BUILDRIGHT', role: 'site-lead' },
];
