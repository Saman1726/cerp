import { CreateUsersTable1700000000001 } from './1700000000001-CreateUsersTable';
import { CreateRefreshTokensTable1700000000002 } from './1700000000002-CreateRefreshTokensTable';
import { CreatePropertiesTable1700000000003 } from './1700000000003-CreatePropertiesTable';

export const tenantMigrations = [
  CreateUsersTable1700000000001,
  CreateRefreshTokensTable1700000000002,
  CreatePropertiesTable1700000000003,
];
