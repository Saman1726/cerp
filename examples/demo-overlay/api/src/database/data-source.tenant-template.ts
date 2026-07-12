import 'dotenv/config';
import { DataSourceOptions } from 'typeorm';
import { buildTenantDataSourceOptions as buildCoreTenantDataSourceOptions } from '@cerp/core-api';
import { DemoPropertyExtension } from '../demo/entities/demo-property-extension.entity';
import { DemoPropertyAudit } from '../demo/entities/demo-property-audit.entity';
import { tenantMigrations } from './migrations/tenant';

// This overlay's own complete migration set (copied core tables + its own
// addition, see migrations/tenant/index.ts) is passed in full - core
// contributes only the entity classes (the domain model), never DDL.
export function buildTenantDataSourceOptions(schemaName: string): DataSourceOptions {
  return buildCoreTenantDataSourceOptions({
    schemaName,
    extraEntities: [DemoPropertyExtension, DemoPropertyAudit],
    migrations: tenantMigrations,
  });
}
