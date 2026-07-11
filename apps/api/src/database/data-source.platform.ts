import 'dotenv/config';
import { DataSource } from 'typeorm';
import { Tenant } from '../platform/entities/tenant.entity';
import { PlatformAdmin } from '../platform/entities/platform-admin.entity';

// No search_path override here: the "platform" schema doesn't exist until
// this data source's own first migration creates it, so pinning search_path
// to "platform" up front leaves TypeORM unable to create its own migrations-
// tracking table (invalid_schema_name) on a fresh database. The actual
// platform tables are always fully schema-qualified in the migration SQL
// itself and in the Tenant/PlatformAdmin entity decorators regardless, so
// the tracking table landing in the default "public" schema is harmless.
//
// TypeORM's CLI also requires exactly one DataSource export from this file -
// a named export alongside the default (even pointing at the same instance)
// makes it refuse to load it.
export default new DataSource({
  type: 'postgres',
  url: process.env.DATABASE_URL,
  entities: [Tenant, PlatformAdmin],
  migrations: [__dirname + '/migrations/platform/*.{ts,js}'],
  synchronize: false,
});
