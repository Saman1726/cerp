import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectDataSource, InjectRepository } from '@nestjs/typeorm';
import { ClsService } from 'nestjs-cls';
import { DataSource, EntityManager, QueryRunner, Repository } from 'typeorm';
import { Tenant } from '../platform/entities/tenant.entity';
import { TENANT_CONTEXT_KEY, TENANT_QUERY_RUNNER_KEY } from './tenancy.constants';
import { TenantContext } from './interfaces/tenant-context.interface';
import { assertSafeSchemaName } from './safe-schema-name';

@Injectable()
export class TenancyService {
  private readonly logger = new Logger(TenancyService.name);

  constructor(
    @InjectDataSource() private readonly dataSource: DataSource,
    @InjectRepository(Tenant) private readonly tenantRepository: Repository<Tenant>,
    private readonly cls: ClsService,
  ) {}

  async resolveTenantBySlug(slug: string): Promise<Tenant> {
    const tenant = await this.tenantRepository.findOne({ where: { slug } });
    if (!tenant || tenant.status !== 'active') {
      throw new NotFoundException(`Unknown or inactive tenant: ${slug}`);
    }
    return tenant;
  }

  setTenantContext(context: TenantContext): void {
    this.cls.set(TENANT_CONTEXT_KEY, context);
  }

  getTenantContext(): TenantContext | undefined {
    return this.cls.get(TENANT_CONTEXT_KEY);
  }

  requireTenantContext(): TenantContext {
    const context = this.getTenantContext();
    if (!context) {
      throw new Error('No tenant context resolved for this request');
    }
    return context;
  }

  // Lazily binds one physical connection to the resolved tenant's schema for
  // the lifetime of the current request, and caches it in CLS so repeated
  // calls within the same request reuse it. Every acquisition unconditionally
  // re-issues SET search_path - never trust an ambient/leftover schema on a
  // connection handed back by the pool.
  async getEntityManager(): Promise<EntityManager> {
    const cached = this.cls.get<QueryRunner | undefined>(TENANT_QUERY_RUNNER_KEY);
    if (cached) {
      return cached.manager;
    }

    const { schemaName } = this.requireTenantContext();
    assertSafeSchemaName(schemaName);

    const queryRunner = this.dataSource.createQueryRunner();
    await queryRunner.connect();
    await queryRunner.query(`SET search_path TO "${schemaName}"`);
    this.cls.set(TENANT_QUERY_RUNNER_KEY, queryRunner);

    return queryRunner.manager;
  }

  async releaseConnection(): Promise<void> {
    const queryRunner = this.cls.get<QueryRunner | undefined>(TENANT_QUERY_RUNNER_KEY);
    if (!queryRunner) {
      return;
    }
    try {
      await queryRunner.release();
    } catch (error) {
      this.logger.error('Failed to release tenant query runner', error as Error);
    }
  }
}
