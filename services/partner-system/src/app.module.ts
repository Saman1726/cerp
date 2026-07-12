import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HealthModule } from './health/health.module';
import { PartnersModule } from './partners/partners.module';
import { AuthModule } from './auth/auth.module';
import { Partner } from './partners/entities/partner.entity';
import { PartnerUser } from './auth/entities/partner-user.entity';

// Standalone mock simulating a third-party partner system: partner login
// delegation + a partner company directory. Deliberately has no dependency
// on @cerp/core-api or any tenancy concept - it represents an external
// system cerp's main app integrates with, not part of cerp itself. Its own
// Postgres database (separate from cerp's) reflects that same independence.
@Module({
  imports: [
    TypeOrmModule.forRootAsync({
      useFactory: () => ({
        type: 'postgres' as const,
        url: process.env.DATABASE_URL,
        entities: [Partner, PartnerUser],
        synchronize: false,
      }),
    }),
    HealthModule,
    PartnersModule,
    AuthModule,
  ],
})
export class AppModule {}
