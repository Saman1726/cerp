import { DynamicModule, Module, Type } from '@nestjs/common';
import { PartnersController } from './partners.controller';
import { PartnerAuthController } from './partner-auth.controller';
import { HttpPartyRegistryGateway } from './adapters/http-party-registry.gateway';
import { PARTY_REGISTRY_GATEWAY } from './party-registry.tokens';
import type { PartyRegistryGateway } from './party-registry-gateway.interface';

export interface PartyRegistryModuleOptions {
  gatewayClass?: Type<PartyRegistryGateway>;
}

@Module({})
export class PartyRegistryModule {
  static forRoot(options: PartyRegistryModuleOptions = {}): DynamicModule {
    return {
      module: PartyRegistryModule,
      controllers: [PartnersController, PartnerAuthController],
      providers: [{ provide: PARTY_REGISTRY_GATEWAY, useClass: options.gatewayClass ?? HttpPartyRegistryGateway }],
      exports: [PARTY_REGISTRY_GATEWAY],
    };
  }
}
