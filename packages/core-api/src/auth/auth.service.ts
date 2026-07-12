import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService, JwtSignOptions } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import * as argon2 from 'argon2';
import { randomBytes, randomUUID, createHash } from 'node:crypto';
import type { LoginResponse } from '@cerp/shared-types';
import { TenancyService } from '../tenancy/tenancy.service';
import { User } from './entities/user.entity';
import { RefreshToken } from './entities/refresh-token.entity';
import { LoginDto } from './dto/login.dto';
import { RefreshDto } from './dto/refresh.dto';

const REFRESH_TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000; // 30 days

interface TokenTenantInfo {
  tenantId: string;
  tenantSlug: string;
  schemaName: string;
}

@Injectable()
export class AuthService {
  constructor(
    private readonly tenancyService: TenancyService,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {}

  async login(dto: LoginDto): Promise<LoginResponse> {
    const tenant = await this.tenancyService.resolveTenantBySlug(dto.tenantSlug);
    this.tenancyService.setTenantContext({
      tenantId: tenant.id,
      tenantSlug: tenant.slug,
      schemaName: tenant.schemaName,
    });

    const manager = await this.tenancyService.getEntityManager();
    const user = await manager.findOne(User, { where: { email: dto.email } });
    if (!user || !(await argon2.verify(user.passwordHash, dto.password))) {
      throw new UnauthorizedException('Invalid email or password');
    }

    const { accessToken, refreshToken } = await this.issueTokens(
      { tenantId: tenant.id, tenantSlug: tenant.slug, schemaName: tenant.schemaName },
      user,
      randomUUID(),
    );

    return {
      accessToken,
      refreshToken,
      user: { id: user.id, email: user.email, role: user.role },
      tenant: { slug: tenant.slug, name: tenant.name },
    };
  }

  async refresh(dto: RefreshDto): Promise<LoginResponse> {
    const tenantContext = this.tenancyService.requireTenantContext();
    const manager = await this.tenancyService.getEntityManager();
    const tokenHash = hashToken(dto.refreshToken);
    const existing = await manager.findOne(RefreshToken, { where: { tokenHash } });

    if (!existing || existing.revokedAt || existing.expiresAt < new Date()) {
      if (existing?.revokedAt) {
        // Reuse of an already-rotated-out token: treat as compromise and
        // revoke the whole family, forcing full re-login.
        await manager.update(RefreshToken, { familyId: existing.familyId }, { revokedAt: new Date() });
      }
      throw new UnauthorizedException('Refresh token is invalid or expired');
    }

    await manager.update(RefreshToken, { id: existing.id }, { revokedAt: new Date() });
    const user = await manager.findOneOrFail(User, { where: { id: existing.userId } });

    const { accessToken, refreshToken } = await this.issueTokens(tenantContext, user, existing.familyId);

    return {
      accessToken,
      refreshToken,
      user: { id: user.id, email: user.email, role: user.role },
      tenant: { slug: tenantContext.tenantSlug, name: tenantContext.tenantSlug },
    };
  }

  private async issueTokens(
    tenant: TokenTenantInfo,
    user: User,
    familyId: string,
  ): Promise<{ accessToken: string; refreshToken: string }> {
    const accessToken = await this.jwtService.signAsync(
      {
        sub: user.id,
        email: user.email,
        role: user.role,
        tenantId: tenant.tenantId,
        tenantSlug: tenant.tenantSlug,
        schemaName: tenant.schemaName,
      },
      {
        secret: this.configService.getOrThrow<string>('jwt.accessSecret'),
        // jsonwebtoken's types want its branded StringValue type, not plain
        // string, for a value that's only known at runtime via config.
        expiresIn: (this.configService.get<string>('jwt.accessTtl') ?? '15m') as unknown as JwtSignOptions['expiresIn'],
      },
    );

    const refreshTokenValue = randomBytes(48).toString('hex');
    const manager = await this.tenancyService.getEntityManager();
    await manager.save(RefreshToken, {
      userId: user.id,
      tokenHash: hashToken(refreshTokenValue),
      familyId,
      expiresAt: new Date(Date.now() + REFRESH_TOKEN_TTL_MS),
      revokedAt: null,
    });

    return { accessToken, refreshToken: refreshTokenValue };
  }
}

function hashToken(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}
