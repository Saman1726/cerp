import { BadGatewayException, Injectable, UnauthorizedException } from '@nestjs/common';

export interface Partner {
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

// Talks to the external Partner System (services/partner-system) - a
// separate deployment, not part of cerp's own tenant database, so there's no
// TenancyService/schema-per-tenant involvement here at all.
@Injectable()
export class PartnersService {
  private readonly baseUrl = process.env.PARTNER_SYSTEM_URL ?? 'http://localhost:3020';

  async findAll(): Promise<Partner[]> {
    const res = await fetch(`${this.baseUrl}/partners`).catch(() => undefined);
    if (!res || !res.ok) {
      throw new BadGatewayException('Partner system is unreachable');
    }
    return (await res.json()) as Partner[];
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
