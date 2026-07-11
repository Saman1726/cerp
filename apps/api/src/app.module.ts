import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ClsModule } from 'nestjs-cls';
import { APP_GUARD } from '@nestjs/core';
import configuration from './config/configuration';
import { validate } from './config/validation.schema';
import { Tenant } from './platform/entities/tenant.entity';
import { PlatformAdmin } from './platform/entities/platform-admin.entity';
import { User } from './auth/entities/user.entity';
import { RefreshToken } from './auth/entities/refresh-token.entity';
import { Property } from './properties/entities/property.entity';
import { TenancyModule } from './tenancy/tenancy.module';
import { PlatformModule } from './platform/platform.module';
import { AuthModule } from './auth/auth.module';
import { PropertiesModule } from './properties/properties.module';
import { HealthModule } from './health/health.module';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, load: [configuration], validate }),
    ClsModule.forRoot({
      global: true,
      middleware: { mount: true, generateId: true },
    }),
    TypeOrmModule.forRootAsync({
      useFactory: () => ({
        type: 'postgres' as const,
        url: process.env.DATABASE_URL,
        synchronize: false,
        // Platform + tenant entities together: Tenant/PlatformAdmin carry an
        // explicit schema on their @Entity() decorator; User/RefreshToken/
        // Property intentionally don't (see those files) and are only ever
        // reached via TenancyService's dynamic search_path binding, never
        // through @InjectRepository on this default connection.
        entities: [Tenant, PlatformAdmin, User, RefreshToken, Property],
      }),
    }),
    TenancyModule,
    PlatformModule,
    AuthModule,
    PropertiesModule,
    HealthModule,
  ],
  providers: [{ provide: APP_GUARD, useClass: JwtAuthGuard }],
})
export class AppModule {}
