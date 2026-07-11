import 'dotenv/config';
import { DataSourceOptions } from 'typeorm';
import { User } from '../auth/entities/user.entity';
import { RefreshToken } from '../auth/entities/refresh-token.entity';
import { Property } from '../properties/entities/property.entity';

// Builds throwaway, single-purpose DataSource options for provisioning or
// migrating exactly one tenant schema. extra.options here is load-bearing
// (unlike the platform data source): tenant migrations are plain, unqualified
// SQL that relies on search_path to land in the right schema, and each of
// these DataSources is used briefly and never shared, so there's no
// connection-pool leakage risk in setting it this way.
export function buildTenantDataSourceOptions(schemaName: string): DataSourceOptions {
  return {
    type: 'postgres',
    url: process.env.DATABASE_URL,
    extra: { options: `-c search_path=${schemaName}` },
    entities: [User, RefreshToken, Property],
    migrations: [__dirname + '/migrations/tenant/*.{ts,js}'],
    synchronize: false,
  };
}
