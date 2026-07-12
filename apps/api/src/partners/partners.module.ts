import { Module } from '@nestjs/common';
import { PartnersController } from './partners.controller';
import { PartnerAuthController } from './partner-auth.controller';
import { PartnersService } from './partners.service';

// Integrates with the external Partner System (services/partner-system) for
// partner login delegation + partner directory lookup.
@Module({
  controllers: [PartnersController, PartnerAuthController],
  providers: [PartnersService],
})
export class PartnersModule {}
