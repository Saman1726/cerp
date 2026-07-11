import 'dotenv/config';
import { DataSource } from 'typeorm';
import * as argon2 from 'argon2';
import { Tenant } from '../../platform/entities/tenant.entity';
import { PlatformAdmin } from '../../platform/entities/platform-admin.entity';
import { User } from '../../auth/entities/user.entity';
import { assertSafeSchemaName } from '../../tenancy/safe-schema-name';
import { buildTenantDataSourceOptions } from '../data-source.tenant-template';

interface Args {
  slug: string;
  name: string;
  adminEmail: string;
  adminPassword: string;
}

function parseArgs(): Args {
  const args = process.argv.slice(2);
  const get = (flag: string): string | undefined =>
    args.find((arg) => arg.startsWith(`--${flag}=`))?.split('=').slice(1).join('=');

  const slug = get('slug');
  const name = get('name');
  const adminEmail = get('admin-email') ?? 'admin@example.com';
  const adminPassword = get('admin-password') ?? 'changeme123';

  if (!slug || !name) {
    throw new Error(
      'Usage: tenant:provision --slug=<slug> --name="<Display Name>" [--admin-email=...] [--admin-password=...]',
    );
  }
  if (!/^[a-z][a-z0-9-]{1,31}$/.test(slug)) {
    throw new Error('slug must be lowercase letters, numbers and hyphens, 2-32 chars, starting with a letter');
  }

  return { slug, name, adminEmail, adminPassword };
}

async function main(): Promise<void> {
  const { slug, name, adminEmail, adminPassword } = parseArgs();
  const schemaName = `tenant_${slug.replace(/-/g, '_')}`;
  assertSafeSchemaName(schemaName);

  const platformDataSource = new DataSource({
    type: 'postgres',
    url: process.env.DATABASE_URL,
    entities: [Tenant, PlatformAdmin],
  });
  await platformDataSource.initialize();

  const tenantRepo = platformDataSource.getRepository(Tenant);
  const existing = await tenantRepo.findOne({ where: { slug } });
  if (existing) {
    await platformDataSource.destroy();
    throw new Error(`Tenant with slug "${slug}" already exists`);
  }

  const tenant = await tenantRepo.save(tenantRepo.create({ slug, name, schemaName, status: 'provisioning' }));
  console.log(`Created tenant registry row for "${slug}" (schema: ${schemaName})`);

  await platformDataSource.query(`CREATE SCHEMA IF NOT EXISTS "${schemaName}"`);
  console.log(`Created schema "${schemaName}"`);

  const tenantDataSource = new DataSource(buildTenantDataSourceOptions(schemaName));
  await tenantDataSource.initialize();

  const migrations = await tenantDataSource.runMigrations();
  console.log(`Ran ${migrations.length} tenant migration(s) against "${schemaName}"`);

  const passwordHash = await argon2.hash(adminPassword);
  const userRepo = tenantDataSource.getRepository(User);
  await userRepo.save(userRepo.create({ email: adminEmail, passwordHash, role: 'owner' }));
  console.log(`Seeded first user "${adminEmail}" for tenant "${slug}"`);

  await tenantDataSource.destroy();

  await tenantRepo.update({ id: tenant.id }, { status: 'active' });
  console.log(`Tenant "${slug}" is now active.`);

  await platformDataSource.destroy();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
