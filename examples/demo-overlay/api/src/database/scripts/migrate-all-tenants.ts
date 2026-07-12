import 'dotenv/config';
import { runAllTenantMigrations } from '@cerp/core-api';
import { buildTenantDataSourceOptions } from '../data-source.tenant-template';

runAllTenantMigrations(buildTenantDataSourceOptions).catch((error) => {
  console.error(error);
  process.exit(1);
});
