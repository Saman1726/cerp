import { DataSourceOptions } from 'typeorm';
import { Tenant } from '../platform/entities/tenant.entity';
import { PlatformAdmin } from '../platform/entities/platform-admin.entity';
import type { EntityClass, MigrationClass } from './types';

export interface BuildPlatformDataSourceOptionsInput {
  extraEntities?: EntityClass[];
  /**
   * Core ships no migrations of its own - DDL/migrations are entirely
   * consumer-owned (see docs/creating-a-new-overlay.md). Only the entity
   * classes (the domain model) live in core; pass whatever migrations your
   * app/overlay owns for the platform schema.
   */
  migrations?: MigrationClass[];
}

// No search_path override: the "platform" schema doesn't exist until this
// data source's own first migration creates it, so pinning search_path to
// "platform" up front leaves TypeORM unable to create its own migrations-
// tracking table (invalid_schema_name) on a fresh database. The actual
// platform tables are always fully schema-qualified in the migration SQL
// itself and in the Tenant/PlatformAdmin entity decorators regardless, so
// the tracking table landing in the default "public" schema is harmless.
export function buildPlatformDataSourceOptions(
  input: BuildPlatformDataSourceOptionsInput = {},
): DataSourceOptions {
  return {
    type: 'postgres',
    url: process.env.DATABASE_URL,
    entities: [Tenant, PlatformAdmin, ...(input.extraEntities ?? [])],
    migrations: input.migrations ?? [],
    synchronize: false,
  };
}
