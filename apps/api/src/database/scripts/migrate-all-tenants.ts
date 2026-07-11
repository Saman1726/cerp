import 'dotenv/config';
import { DataSource } from 'typeorm';
import { Tenant } from '../../platform/entities/tenant.entity';
import { PlatformAdmin } from '../../platform/entities/platform-admin.entity';
import { assertSafeSchemaName } from '../../tenancy/safe-schema-name';
import { buildTenantDataSourceOptions } from '../data-source.tenant-template';

async function main(): Promise<void> {
  const platformDataSource = new DataSource({
    type: 'postgres',
    url: process.env.DATABASE_URL,
    entities: [Tenant, PlatformAdmin],
  });
  await platformDataSource.initialize();

  const tenants = await platformDataSource.getRepository(Tenant).find({ where: { status: 'active' } });
  console.log(`Found ${tenants.length} active tenant(s)`);

  for (const tenant of tenants) {
    assertSafeSchemaName(tenant.schemaName);
    const dataSource = new DataSource(buildTenantDataSourceOptions(tenant.schemaName));
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

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
