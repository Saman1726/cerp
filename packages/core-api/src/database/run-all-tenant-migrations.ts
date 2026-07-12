import { DataSource, DataSourceOptions } from 'typeorm';
import { Tenant } from '../platform/entities/tenant.entity';
import { assertSafeSchemaName } from '../tenancy/safe-schema-name';
import { buildPlatformDataSourceOptions } from './build-platform-data-source-options';

// Takes the caller's own buildTenantDataSourceOptions (already merged with
// any extraEntities/extraMigrations) so this loop never needs to know
// whether the caller extended anything.
export async function runAllTenantMigrations(
  buildTenantDataSourceOptionsForSchema: (schemaName: string) => DataSourceOptions,
): Promise<void> {
  const platformDataSource = new DataSource(buildPlatformDataSourceOptions());
  await platformDataSource.initialize();

  const tenants = await platformDataSource.getRepository(Tenant).find({ where: { status: 'active' } });
  console.log(`Found ${tenants.length} active tenant(s)`);

  for (const tenant of tenants) {
    assertSafeSchemaName(tenant.schemaName);
    const dataSource = new DataSource(buildTenantDataSourceOptionsForSchema(tenant.schemaName));
    await dataSource.initialize();
    try {
      const migrations = await dataSource.runMigrations();
      console.log(`[${tenant.slug}] ran ${migrations.length} migration(s)`);
    } finally {
      await dataSource.destroy();
    }
  }

  await platformDataSource.destroy();
}
