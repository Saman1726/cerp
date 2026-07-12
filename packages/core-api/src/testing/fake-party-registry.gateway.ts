import { Injectable } from '@nestjs/common';
import type { PartyRegistryGateway } from '../party-registry/party-registry-gateway.interface';
import type { PartnerLoginResult, PartyRegistryEntry } from '../party-registry/party-registry.types';

// CI (and most local test runs) don't have the real partner-system service
// running - only Postgres. Any test exercising a module that resolves a
// Party (Construction Engineering's contractor, Procurement's vendor)
// should override PARTY_REGISTRY_GATEWAY with this in-memory double instead
// of depending on that separate deployable being up.
export const FAKE_PARTY_REGISTRY_ENTRIES: PartyRegistryEntry[] = [
  {
    id: 'fake-partner-active',
    code: 'FAKE-ACTIVE',
    name: 'Fake Active Vendor Co.',
    type: 'supplier',
    contactEmail: 'active@fake-vendor.test',
    status: 'active',
  },
  {
    id: 'fake-partner-inactive',
    code: 'FAKE-INACTIVE',
    name: 'Fake Inactive Vendor Co.',
    type: 'supplier',
    contactEmail: 'inactive@fake-vendor.test',
    status: 'inactive',
  },
];

@Injectable()
export class FakePartyRegistryGateway implements PartyRegistryGateway {
  async findAll(): Promise<PartyRegistryEntry[]> {
    return FAKE_PARTY_REGISTRY_ENTRIES;
  }

  async findById(id: string): Promise<PartyRegistryEntry | null> {
    return FAKE_PARTY_REGISTRY_ENTRIES.find((entry) => entry.id === id) ?? null;
  }

  login(): Promise<PartnerLoginResult> {
    throw new Error('FakePartyRegistryGateway does not implement login()');
  }
}
