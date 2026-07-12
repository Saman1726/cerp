import { BadGatewayException, Injectable, UnauthorizedException } from '@nestjs/common';
import type { PartyRegistryGateway } from '../party-registry-gateway.interface';
import type { PartnerLoginResult, PartyRegistryEntry } from '../party-registry.types';

// Default adapter: calls the external Partner System over HTTP. A consumer
// overlay needing a different partner-directory backend swaps this via
// PartyRegistryModule.forRoot({ gatewayClass }), same seam as
// CorePropertiesModule.forRoot({ validatorClass }).
@Injectable()
export class HttpPartyRegistryGateway implements PartyRegistryGateway {
  private readonly baseUrl = process.env.PARTNER_SYSTEM_URL ?? 'http://localhost:3020';

  async findAll(): Promise<PartyRegistryEntry[]> {
    const res = await fetch(`${this.baseUrl}/partners`).catch(() => undefined);
    if (!res || !res.ok) {
      throw new BadGatewayException('Partner system is unreachable');
    }
    return (await res.json()) as PartyRegistryEntry[];
  }

  async findById(id: string): Promise<PartyRegistryEntry | null> {
    const res = await fetch(`${this.baseUrl}/partners/${id}`).catch(() => undefined);
    if (!res) {
      throw new BadGatewayException('Partner system is unreachable');
    }
    if (res.status === 404) {
      return null;
    }
    if (!res.ok) {
      throw new BadGatewayException('Partner system returned an unexpected error');
    }
    return (await res.json()) as PartyRegistryEntry;
  }

  async login(partnerCode: string, username: string, password: string): Promise<PartnerLoginResult> {
    const res = await fetch(`${this.baseUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ partnerCode, username, password }),
    }).catch(() => undefined);

    if (!res) {
      throw new BadGatewayException('Partner system is unreachable');
    }
    if (res.status === 401) {
      throw new UnauthorizedException('Invalid partner code, username or password');
    }
    if (!res.ok) {
      throw new BadGatewayException('Partner system returned an unexpected error');
    }
    return (await res.json()) as PartnerLoginResult;
  }
}
