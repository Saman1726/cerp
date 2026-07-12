import { CreateUsersTable1700000000001 } from './1700000000001-CreateUsersTable';
import { CreateRefreshTokensTable1700000000002 } from './1700000000002-CreateRefreshTokensTable';
import { CreatePropertiesTable1700000000003 } from './1700000000003-CreatePropertiesTable';
import { CreateDemoPropertyExtensions1752000000000 } from './1752000000000-CreateDemoPropertyExtensions';

// This overlay's complete, owned migration set: the tables it needs from
// core (copied, not imported - core ships no DDL) plus its own addition.
export const tenantMigrations = [
  CreateUsersTable1700000000001,
  CreateRefreshTokensTable1700000000002,
  CreatePropertiesTable1700000000003,
  CreateDemoPropertyExtensions1752000000000,
];
