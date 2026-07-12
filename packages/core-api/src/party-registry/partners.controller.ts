import { Controller, Get, Inject } from '@nestjs/common';
import { PARTY_REGISTRY_GATEWAY } from './party-registry.tokens';
import type { PartyRegistryGateway } from './party-registry-gateway.interface';

@Controller('partners')
export class PartnersController {
  constructor(@Inject(PARTY_REGISTRY_GATEWAY) private readonly gateway: PartyRegistryGateway) {}

  @Get()
  findAll() {
    return this.gateway.findAll();
  }
}
