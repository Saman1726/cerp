import 'dotenv/config';
import { DataSourceOptions } from 'typeorm';
import { buildTenantDataSourceOptions as buildCoreTenantDataSourceOptions } from '@cerp/core-api';
import { tenantMigrations } from './migrations/tenant';

// Reference app: core's migrations only, nothing extra. See
// examples/demo-overlay/api/src/database/data-source.tenant-template.ts for
// a version of this same file that adds customer-specific migrations too.
export function buildTenantDataSourceOptions(schemaName: string): DataSourceOptions {
  return buildCoreTenantDataSourceOptions({ schemaName, migrations: tenantMigrations });
}
