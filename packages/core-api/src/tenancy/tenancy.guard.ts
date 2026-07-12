import { CanActivate, ExecutionContext, Injectable } from '@nestjs/common';
import { Request } from 'express';
import { TenancyService } from './tenancy.service';

const TENANT_HEADER = 'x-tenant-id';

// Pre-auth / convenience resolution only. Once a request carries a valid JWT,
// JwtStrategy.validate() re-derives (and authoritatively overrides) the
// tenant context from the token's own claims - see strategies/jwt.strategy.ts.
//
// A guard, not middleware: guards run strictly after the entire middleware
// phase (including ClsModule's own context-initializing middleware), so this
// never races against ClsModule for Express middleware registration order -
// unlike two middlewares from different (possibly differently-nested)
// modules, whose relative order NestJS does not guarantee.
@Injectable()
export class TenancyGuard implements CanActivate {
  constructor(private readonly tenancyService: TenancyService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<Request>();
    const header = request.headers[TENANT_HEADER];
    if (typeof header !== 'string' || header.length === 0) {
      return true;
    }

    const tenant = await this.tenancyService.resolveTenantBySlug(header);
    this.tenancyService.setTenantContext({
      tenantId: tenant.id,
      tenantSlug: tenant.slug,
      schemaName: tenant.schemaName,
    });
    return true;
  }
}
