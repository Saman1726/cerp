import 'dotenv/config';
import { provisionTenant } from '@cerp/core-api';
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

  return { slug, name, adminEmail, adminPassword };
}

const { slug, name, adminEmail, adminPassword } = parseArgs();

provisionTenant({ slug, name, adminEmail, adminPassword, buildTenantDataSourceOptions }).catch((error) => {
  console.error(error);
  process.exit(1);
});
