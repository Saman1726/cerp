import { ForbiddenException, Injectable } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ConfigService } from '@nestjs/config';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { TenancyService } from '../../tenancy/tenancy.service';

export interface JwtPayload {
  sub: string;
  email: string;
  role: string;
  tenantId: string;
  tenantSlug: string;
  schemaName: string;
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    configService: ConfigService,
    private readonly tenancyService: TenancyService,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: configService.getOrThrow<string>('jwt.accessSecret'),
    });
  }

  // Runs after TenancyMiddleware, so this always has the final say: a header
  // resolved tenant must match the token, and the token's own claims become
  // the authoritative tenant context for the rest of the request either way.
  validate(payload: JwtPayload): JwtPayload {
    const existing = this.tenancyService.getTenantContext();
    if (existing && existing.tenantId !== payload.tenantId) {
      // 403, not 401: the token itself is valid, it's just scoped to a
      // different tenant than the one the request otherwise resolved to.
      throw new ForbiddenException('Tenant mismatch between request and access token');
    }

    this.tenancyService.setTenantContext({
      tenantId: payload.tenantId,
      tenantSlug: payload.tenantSlug,
      schemaName: payload.schemaName,
    });

    return payload;
  }
}
