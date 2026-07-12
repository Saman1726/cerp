import { Body, Controller, HttpCode, HttpStatus, Inject, Post } from '@nestjs/common';
import { Public } from '../common/decorators/public.decorator';
import { PARTY_REGISTRY_GATEWAY } from './party-registry.tokens';
import type { PartyRegistryGateway } from './party-registry-gateway.interface';
import { PartnerLoginDto } from './dto/partner-login.dto';

// Partners aren't cerp tenant users - this delegates credential
// verification entirely to the external Partner System, unrelated to cerp's
// own tenant/JWT auth.
@Controller('auth')
export class PartnerAuthController {
  constructor(@Inject(PARTY_REGISTRY_GATEWAY) private readonly gateway: PartyRegistryGateway) {}

  @Public()
  @HttpCode(HttpStatus.OK)
  @Post('partner-login')
  partnerLogin(@Body() dto: PartnerLoginDto) {
    return this.gateway.login(dto.partnerCode, dto.username, dto.password);
  }
}
