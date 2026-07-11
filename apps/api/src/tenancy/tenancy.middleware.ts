import { Injectable, NestMiddleware } from '@nestjs/common';
import { NextFunction, Request, Response } from 'express';
import { TenancyService } from './tenancy.service';

const TENANT_HEADER = 'x-tenant-id';

// Pre-auth / convenience resolution only. Once a request carries a valid JWT,
// JwtStrategy.validate() re-derives (and authoritatively overrides) the
// tenant context from the token's own claims - see strategies/jwt.strategy.ts.
@Injectable()
export class TenancyMiddleware implements NestMiddleware {
  constructor(private readonly tenancyService: TenancyService) {}

  async use(req: Request, res: Response, next: NextFunction): Promise<void> {
    const header = req.headers[TENANT_HEADER];
    if (typeof header !== 'string' || header.length === 0) {
      next();
      return;
    }

    try {
      const tenant = await this.tenancyService.resolveTenantBySlug(header);
      this.tenancyService.setTenantContext({
        tenantId: tenant.id,
        tenantSlug: tenant.slug,
        schemaName: tenant.schemaName,
      });
      next();
    } catch (error) {
      next(error as Error);
    }
  }
}
