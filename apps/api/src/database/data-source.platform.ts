import 'dotenv/config';
import { DataSource } from 'typeorm';
import { buildPlatformDataSourceOptions } from '@cerp/core-api';
import { platformMigrations } from './migrations/platform';

// TypeORM's CLI requires exactly one DataSource export from this file - a
// named export alongside the default (even pointing at the same instance)
// makes it refuse to load it.
//
// Migrations are owned here, not in @cerp/core-api - core ships entities
// (the domain model) but no DDL. See docs/creating-a-new-overlay.md.
export default new DataSource(buildPlatformDataSourceOptions({ migrations: platformMigrations }));
