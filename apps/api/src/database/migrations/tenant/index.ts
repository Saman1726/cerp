import { CreateUsersTable1700000000001 } from './1700000000001-CreateUsersTable';
import { CreateRefreshTokensTable1700000000002 } from './1700000000002-CreateRefreshTokensTable';
import { CreatePropertiesTable1700000000003 } from './1700000000003-CreatePropertiesTable';
import { CreateWorkOrdersTable1700000000004 } from './1700000000004-CreateWorkOrdersTable';
import { CreatePurchaseOrdersTable1700000000005 } from './1700000000005-CreatePurchaseOrdersTable';
import { CreateGoodsReceiptsTable1700000000006 } from './1700000000006-CreateGoodsReceiptsTable';

export const tenantMigrations = [
  CreateUsersTable1700000000001,
  CreateRefreshTokensTable1700000000002,
  CreatePropertiesTable1700000000003,
  CreateWorkOrdersTable1700000000004,
  CreatePurchaseOrdersTable1700000000005,
  CreateGoodsReceiptsTable1700000000006,
];
