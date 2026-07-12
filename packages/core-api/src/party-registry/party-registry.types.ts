// Shapes returned by the external Partner System (services/partner-system) -
// a separate deployment, not part of cerp's own tenant database.
export interface PartyRegistryEntry {
  id: string;
  code: string;
  name: string;
  type: string;
  contactEmail: string;
  status: string;
}

export interface PartnerLoginResult {
  partnerId: string;
  partnerCode: string;
  partnerName: string;
  username: string;
  role: string;
}
