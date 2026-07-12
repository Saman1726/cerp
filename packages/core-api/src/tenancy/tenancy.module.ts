import { Global, Module } from '@nestjs/common';
import { APP_INTERCEPTOR } from '@nestjs/core';
import { PlatformModule } from '../platform/platform.module';
import { TenancyService } from './tenancy.service';
import { TenancyCleanupInterceptor } from './tenancy-cleanup.interceptor';

@Global()
@Module({
  imports: [PlatformModule],
  providers: [TenancyService, { provide: APP_INTERCEPTOR, useClass: TenancyCleanupInterceptor }],
  exports: [TenancyService],
})
export class TenancyModule {}
