import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { DataSource } from 'typeorm';
import * as argon2 from 'argon2';
import { AppModule } from '../src/app.module';
import { Tenant } from '../src/platform/entities/tenant.entity';
import { User } from '../src/auth/entities/user.entity';
import { buildTenantDataSourceOptions } from '../src/database/data-source.tenant-template';

// The one test this scaffold cannot ship without: proves schema-per-tenant
// isolation actually holds at the HTTP layer, not just in code review.
describe('Tenant isolation (e2e)', () => {
  let app: INestApplication;
  let dataSource: DataSource;

  const suffix = Date.now();
  const tenantA = { slug: `e2e-a-${suffix}`, name: 'E2E Tenant A', schemaName: `tenant_e2e_a_${suffix}` };
  const tenantB = { slug: `e2e-b-${suffix}`, name: 'E2E Tenant B', schemaName: `tenant_e2e_b_${suffix}` };
  const password = 'password123';

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] }).compile();
    app = moduleRef.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
    await app.init();

    dataSource = moduleRef.get(DataSource);

    for (const tenant of [tenantA, tenantB]) {
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
    }
  });

  afterAll(async () => {
    for (const tenant of [tenantA, tenantB]) {
      await dataSource.query(`DROP SCHEMA IF EXISTS "${tenant.schemaName}" CASCADE`);
      await dataSource.getRepository(Tenant).delete({ slug: tenant.slug });
    }
    await app.close();
  });

  async function loginAndGetToken(tenantSlug: string): Promise<string> {
    const res = await request(app.getHttpServer())
      .post('/auth/login')
      .send({ tenantSlug, email: 'owner@example.com', password })
      .expect(200);
    return res.body.accessToken;
  }

  it('never returns tenant A data when authenticated as tenant B', async () => {
    const tokenA = await loginAndGetToken(tenantA.slug);
    const tokenB = await loginAndGetToken(tenantB.slug);

    const createRes = await request(app.getHttpServer())
      .post('/properties')
      .set('Authorization', `Bearer ${tokenA}`)
      .send({ name: 'Only in tenant A', address: '1 A Street' })
      .expect(201);

    const tenantAList = await request(app.getHttpServer())
      .get('/properties')
      .set('Authorization', `Bearer ${tokenA}`)
      .expect(200);
    expect(tenantAList.body).toEqual(
      expect.arrayContaining([expect.objectContaining({ id: createRes.body.id })]),
    );

    const tenantBList = await request(app.getHttpServer())
      .get('/properties')
      .set('Authorization', `Bearer ${tokenB}`)
      .expect(200);
    expect(tenantBList.body).toEqual([]);
  });

  it('rejects a token for tenant A when X-Tenant-Id header claims tenant B', async () => {
    const tokenA = await loginAndGetToken(tenantA.slug);

    await request(app.getHttpServer())
      .get('/properties')
      .set('Authorization', `Bearer ${tokenA}`)
      .set('X-Tenant-Id', tenantB.slug)
      .expect(403);
  });
});
