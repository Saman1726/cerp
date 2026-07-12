import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { DataSource } from 'typeorm';
import * as argon2 from 'argon2';
import { Tenant, User } from '@cerp/core-api';
import { registerTenancyIsolationTests } from '@cerp/core-api/testing';
import { AppModule } from '../src/app.module';
import { buildTenantDataSourceOptions } from '../src/database/data-source.tenant-template';

// Proves the core architecture didn't regress even with this overlay's
// customizations active - the demo validator requires a postal-code suffix,
// so the shared sample payload must satisfy it.
registerTenancyIsolationTests({
  appModule: AppModule,
  samplePropertyPayload: { name: 'Demo Isolation Test', address: '1 Demo Street 12345' },
  // Required: this is this overlay's own full migration set (nothing comes
  // from core by default). property.created fires for these tenants too
  // (the event emitter is app-wide), so without this DemoApprovalListener's
  // insert would fail - demo_property_audit wouldn't exist in their schemas.
  buildTenantDataSourceOptions,
});

// Proves the three extension points this overlay actually uses, each
// exercised for real over HTTP - not just "it compiles".
describe('Demo overlay extension points (e2e)', () => {
  let app: INestApplication;
  let dataSource: DataSource;

  const suffix = Date.now();
  const tenant = { slug: `e2e-demoext-${suffix}`, name: 'E2E Demo Ext Tenant', schemaName: `tenant_e2e_demoext_${suffix}` };
  const password = 'password123';
  let token: string;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
    await app.init();

    dataSource = moduleRef.get(DataSource);

    await dataSource.query(`CREATE SCHEMA IF NOT EXISTS "${tenant.schemaName}"`);
    await dataSource.getRepository(Tenant).save({ ...tenant, status: 'active' });

    const tenantDataSource = new DataSource(buildTenantDataSourceOptions(tenant.schemaName));
    await tenantDataSource.initialize();
    await tenantDataSource.runMigrations();
    const userRepo = tenantDataSource.getRepository(User);
    await userRepo.save(
      userRepo.create({ email: 'owner@example.com', passwordHash: await argon2.hash(password), role: 'owner' }),
    );
    await tenantDataSource.destroy();

    const res = await request(app.getHttpServer())
      .post('/auth/login')
      .send({ tenantSlug: tenant.slug, email: 'owner@example.com', password })
      .expect(200);
    token = res.body.accessToken;
  });

  afterAll(async () => {
    await dataSource.query(`DROP SCHEMA IF EXISTS "${tenant.schemaName}" CASCADE`);
    await dataSource.getRepository(Tenant).delete({ slug: tenant.slug });
    await app.close();
  });

  it('rejects property creation on the base /properties endpoint when the address has no postal code (DI-override validator)', async () => {
    await request(app.getHttpServer())
      .post('/properties')
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'Bad Address', address: 'No Postal Code Here' })
      .expect(400);
  });

  it('accepts and round-trips inspector notes via the extended /demo/properties endpoint (satellite table)', async () => {
    const createRes = await request(app.getHttpServer())
      .post('/demo/properties')
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'Inspected Building', address: '42 Compliance Ave 90210', inspectorNotes: 'Looks good' })
      .expect(201);

    expect(createRes.body.inspectorNotes).toBe('Looks good');
    expect(createRes.body.complianceStatus).toBe('pending');

    const listRes = await request(app.getHttpServer())
      .get('/demo/properties')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    const found = (listRes.body as Array<{ id: string; inspectorNotes: string | null }>).find(
      (p) => p.id === createRes.body.id,
    );
    expect(found).toBeDefined();
    expect(found?.inspectorNotes).toBe('Looks good');
  });

  it('records an audit row when a property is created (event-hook extension point)', async () => {
    const createRes = await request(app.getHttpServer())
      .post('/demo/properties')
      .set('Authorization', `Bearer ${token}`)
      .send({ name: 'Audited Building', address: '7 Event Lane 10001' })
      .expect(201);

    // Verify via a dedicated tenant-scoped connection, same as everywhere
    // else in this codebase - never SET search_path on the shared pooled
    // dataSource directly, since that connection could be handed to an
    // unrelated request afterwards.
    const verifyDataSource = new DataSource(buildTenantDataSourceOptions(tenant.schemaName));
    await verifyDataSource.initialize();
    try {
      const auditRows = await verifyDataSource.query(
        `SELECT * FROM "demo_property_audit" WHERE property_id = $1`,
        [createRes.body.id],
      );
      expect(auditRows.length).toBeGreaterThanOrEqual(1);
      expect(auditRows[0].event_type).toBe('property.created');
    } finally {
      await verifyDataSource.destroy();
    }
  });
});
