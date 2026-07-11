import { Global, MiddlewareConsumer, Module, NestModule } from '@nestjs/common';
import { APP_INTERCEPTOR } from '@nestjs/core';
import { PlatformModule } from '../platform/platform.module';
import { TenancyService } from './tenancy.service';
import { TenancyMiddleware } from './tenancy.middleware';
import { TenancyCleanupInterceptor } from './tenancy-cleanup.interceptor';

@Global()
@Module({
  imports: [PlatformModule],
  providers: [TenancyService, { provide: APP_INTERCEPTOR, useClass: TenancyCleanupInterceptor }],
  exports: [TenancyService],
})
export class TenancyModule implements NestModule {
  configure(consumer: MiddlewareConsumer): void {
    consumer.apply(TenancyMiddleware).forRoutes('*');
  }
}
