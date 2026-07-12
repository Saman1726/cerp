import { DynamicModule, Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ConfigModule } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ClsModule } from 'nestjs-cls';
import { EventEmitterModule } from '@nestjs/event-emitter';
import configuration from './config/configuration';
import { validate } from './config/validation.schema';
import { Tenant } from './platform/entities/tenant.entity';
import { PlatformAdmin } from './platform/entities/platform-admin.entity';
import { User } from './auth/entities/user.entity';
import { RefreshToken } from './auth/entities/refresh-token.entity';
import { Property } from './properties/entities/property.entity';
import { TenancyModule } from './tenancy/tenancy.module';
import { TenancyGuard } from './tenancy/tenancy.guard';
import { PlatformModule } from './platform/platform.module';
import { AuthModule } from './auth/auth.module';
import { CorePropertiesModule, CorePropertiesModuleOptions } from './properties/properties.module';
import { HealthModule } from './health/health.module';
import { JwtAuthGuard } from './common/guards/jwt-auth.guard';
import type { EntityClass } from './database/types';

export interface CoreModuleOptions {
  database?: {
    extraEntities?: EntityClass[];
  };
  properties?: CorePropertiesModuleOptions;
}

// The single composition root a consuming app imports. Everything a
// consumer can override/extend is threaded through `options` here, rather
// than requiring the consumer to reassemble this wiring by hand.
@Module({})
export class CoreModule {
  static forRoot(options: CoreModuleOptions = {}): DynamicModule {
    return {
      module: CoreModule,
      global: true,
      imports: [
        ConfigModule.forRoot({ isGlobal: true, load: [configuration], validate }),
        ClsModule.forRoot({
          global: true,
          middleware: { mount: true, generateId: true },
        }),
        EventEmitterModule.forRoot(),
        TypeOrmModule.forRootAsync({
          useFactory: () => ({
            type: 'postgres' as const,
            url: process.env.DATABASE_URL,
            synchronize: false,
            // Platform + tenant entities together: Tenant/PlatformAdmin carry
            // an explicit schema on their @Entity() decorator; User/
            // RefreshToken/Property (and any consumer extraEntities)
            // intentionally don't, and are only ever reached via
            // TenancyService's dynamic search_path binding, never through
            // @InjectRepository on this default connection.
            entities: [
              Tenant,
              PlatformAdmin,
              User,
              RefreshToken,
              Property,
              ...(options.database?.extraEntities ?? []),
            ],
          }),
        }),
        TenancyModule,
        PlatformModule,
        AuthModule,
        CorePropertiesModule.forRoot(options.properties ?? {}),
        HealthModule,
      ],
      providers: [
        // Order matters: multiple APP_GUARD providers run in registration
        // order. TenancyGuard resolves X-Tenant-Id first; JwtAuthGuard's
        // JwtStrategy.validate() then cross-checks/overrides from the token.
        { provide: APP_GUARD, useClass: TenancyGuard },
        { provide: APP_GUARD, useClass: JwtAuthGuard },
      ],
      // CorePropertiesModule exported too: a consumer's own sibling module
      // (e.g. one wrapping PropertiesService to add fields via a satellite
      // table) needs to inject it without re-importing/reconfiguring it.
      exports: [TenancyModule, AuthModule, CorePropertiesModule],
    };
  }
}
