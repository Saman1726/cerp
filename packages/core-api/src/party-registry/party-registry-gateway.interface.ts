import type { PartnerLoginResult, PartyRegistryEntry } from './party-registry.types';

// Resolves/validates contractors, suppliers, and agents against the
// external Partner System. Deliberately narrow - callers get back a draft
// fact to use in their own aggregate, never a live handle into the external
// system's own data.
export interface PartyRegistryGateway {
  findAll(): Promise<PartyRegistryEntry[]>;
  findById(id: string): Promise<PartyRegistryEntry | null>;
  login(partnerCode: string, username: string, password: string): Promise<PartnerLoginResult>;
}
