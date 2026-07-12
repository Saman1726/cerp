import { registerTenancyIsolationTests } from '@cerp/core-api/testing';
import { AppModule } from '../src/app.module';
import { buildTenantDataSourceOptions } from '../src/database/data-source.tenant-template';

registerTenancyIsolationTests({ appModule: AppModule, buildTenantDataSourceOptions });
