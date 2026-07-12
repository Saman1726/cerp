import { DataSourceOptions } from 'typeorm';
import { User } from '../auth/entities/user.entity';
import { RefreshToken } from '../auth/entities/refresh-token.entity';
import { Property } from '../properties/entities/property.entity';
import type { EntityClass, MigrationClass } from './types';

export interface BuildTenantDataSourceOptionsInput {
  schemaName: string;
  extraEntities?: EntityClass[];
  /**
   * Core ships no migrations of its own - DDL/migrations are entirely
   * consumer-owned (see docs/creating-a-new-overlay.md). Pass the full set
   * your app/overlay owns for the tenant schema (core's own users/
   * refresh_tokens/properties tables plus anything you've added).
   */
  migrations?: MigrationClass[];
}

// extra.options here is load-bearing (unlike the platform data source):
// tenant migrations are plain, unqualified SQL that relies on search_path to
// land in the right schema, and each of these DataSources is used briefly
// and never shared, so there's no connection-pool leakage risk in setting it
// this way.
export function buildTenantDataSourceOptions(input: BuildTenantDataSourceOptionsInput): DataSourceOptions {
  return {
    type: 'postgres',
    url: process.env.DATABASE_URL,
    extra: { options: `-c search_path=${input.schemaName}` },
    entities: [User, RefreshToken, Property, ...(input.extraEntities ?? [])],
    migrations: input.migrations ?? [],
    synchronize: false,
  };
}
