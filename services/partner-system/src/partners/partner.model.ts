export interface Partner {
  id: string;
  code: string;
  name: string;
  type: 'supplier' | 'contractor' | 'real-estate-agent';
  contactEmail: string;
  status: 'active' | 'inactive';
}

// In-memory mock directory - a real partner system would back this with its
// own database. Fine for a local-dev/test stub: state resets on restart.
export const PARTNERS: Partner[] = [
  {
    id: '1',
    code: 'ACME-SUPPLY',
    name: 'Acme Supply Co.',
    type: 'supplier',
    contactEmail: 'contact@acmesupply.test',
    status: 'active',
  },
  {
    id: '2',
    code: 'BUILDRIGHT',
    name: 'BuildRight Contractors',
    type: 'contractor',
    contactEmail: 'info@buildright.test',
    status: 'active',
  },
  {
    id: '3',
    code: 'CITYREALTY',
    name: 'City Realty Group',
    type: 'real-estate-agent',
    contactEmail: 'hello@cityrealty.test',
    status: 'active',
  },
];
