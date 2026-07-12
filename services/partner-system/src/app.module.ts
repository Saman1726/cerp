import { Module } from '@nestjs/common';
import { HealthModule } from './health/health.module';
import { PartnersModule } from './partners/partners.module';
import { AuthModule } from './auth/auth.module';

// Standalone mock simulating a third-party partner system: partner login
// delegation + a partner company directory. Deliberately has no dependency
// on @cerp/core-api or any tenancy concept - it represents an external
// system cerp's main app integrates with, not part of cerp itself.
@Module({
  imports: [HealthModule, PartnersModule, AuthModule],
})
export class AppModule {}
