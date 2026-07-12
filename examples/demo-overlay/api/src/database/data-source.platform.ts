import 'dotenv/config';
import { DataSource } from 'typeorm';
import { buildPlatformDataSourceOptions } from '@cerp/core-api';
import { platformMigrations } from './migrations/platform';

// Migrations owned here, not in @cerp/core-api - see
// docs/creating-a-new-overlay.md. This overlay hasn't diverged from core's
// platform schema, so this is an unmodified copy of the reference app's.
export default new DataSource(buildPlatformDataSourceOptions({ migrations: platformMigrations }));
