/// <reference types="jest" />
import { INestApplication, ValidationPipe } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { DataSource } from 'typeorm';
import * as argon2 from 'argon2';
import { Tenant, User, PARTY_REGISTRY_GATEWAY, Property } from '@cerp/core-api';
import { FakePartyRegistryGateway } from '@cerp/core-api/testing';
import { AppModule } from '../src/app.module';
import { buildTenantDataSourceOptions } from '../src/database/data-source.tenant-template';

// End-to-end coverage for Procurement HTTP endpoints backed by tenant-scoped
// tables, with the Party Registry dependency swapped out for an in-memory fake.
// Mirrors testing patterns used elsewhere (see tenancy-isolation and demo overlay).
describe('Procurement (e2e)', () => {
  let app: INestApplication;
  let dataSource: DataSource;

  const suffix = Date.now();
  const tenant = { slug: `e2e-proc-${suffix}`, name: 'E2E Procurement Tenant', schemaName: `tenant_e2e_proc_${suffix}` };
  const password = 'password123';
  let token: string;
  let projectId: string;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(PARTY_REGISTRY_GATEWAY)
      .useClass(FakePartyRegistryGateway)
      .compile();

    app = moduleRef.createNestApplication();
    app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
    await app.init();

    dataSource = moduleRef.get(DataSource);

    // Provision tenant schema and baseline user
    await dataSource.query(`CREATE SCHEMA IF NOT EXISTS "${tenant.schemaName}"`);
    await dataSource.getRepository(Tenant).save({ ...tenant, status: 'active' });

    const tenantDataSource = new DataSource(buildTenantDataSourceOptions(tenant.schemaName));
    await tenantDataSource.initialize();
    try {
      await tenantDataSource.runMigrations();
      const userRepo = tenantDataSource.getRepository(User);
      await userRepo.save(
        userRepo.create({ email: 'owner@example.com', passwordHash: await argon2.hash(password), role: 'owner' }),
      );
      const propertyRepo = tenantDataSource.getRepository(Property);
      const property = await propertyRepo.save(propertyRepo.create({ name: 'E2E Project', address: '123 Test St' }));
      projectId = property.id;
    } finally {
      await tenantDataSource.destroy();
    }

    // Login to obtain JWT for authenticated requests
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

  it('starts with no purchase orders', async () => {
    const res = await request(app.getHttpServer())
      .get('/purchase-orders')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);

    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body).toHaveLength(0);
  });

  it('rejects raising a purchase order with unknown vendor', async () => {
    await request(app.getHttpServer())
      .post('/purchase-orders')
      .set('Authorization', `Bearer ${token}`)
      .send({
        projectId: projectId,
        vendorPartnerId: 'unknown-partner',
        description: 'Test PO with unknown vendor',
      })
      .expect(400);
  });

  it('rejects raising a purchase order with an inactive vendor', async () => {
    await request(app.getHttpServer())
      .post('/purchase-orders')
      .set('Authorization', `Bearer ${token}`)
      .send({
        projectId: projectId,
        vendorPartnerId: 'fake-partner-inactive',
        description: 'Test PO with inactive vendor',
      })
      .expect(400);
  });

  let purchaseOrderId: string;

  it('raises a purchase order with an active vendor', async () => {
    const createRes = await request(app.getHttpServer())
      .post('/purchase-orders')
      .set('Authorization', `Bearer ${token}`)
      .send({
        projectId: projectId,
        vendorPartnerId: 'fake-partner-active',
        description: 'Concrete delivery',
      })
      .expect(201);

    purchaseOrderId = createRes.body.id;
    expect(createRes.body.vendorPartnerName).toBeDefined();
    expect(createRes.body.status).toBe('ordered');
  });

  it('lists and retrieves the created purchase order with no receipts initially', async () => {
    const listRes = await request(app.getHttpServer())
      .get('/purchase-orders')
      .set('Authorization', `Bearer ${token}`)
      .expect(200);
    expect(listRes.body).toEqual(
      expect.arrayContaining([expect.objectContaining({ id: purchaseOrderId, status: 'ordered' })]),
    );

    const getRes = await request(app.getHttpServer())
      .get(`/purchase-orders/${purchaseOrderId}`)
      .set('Authorization', `Bearer ${token}`)
      .expect(200);
    expect(getRes.body.id).toBe(purchaseOrderId);
    expect(getRes.body.receipts).toEqual([]);
  });

  it('records a goods receipt and updates status to received', async () => {
    const receiptRes = await request(app.getHttpServer())
      .post(`/purchase-orders/${purchaseOrderId}/receipts`)
      .set('Authorization', `Bearer ${token}`)
      .send({ notes: 'Delivered on site' })
      .expect(201);

    expect(receiptRes.body.id).toBe(purchaseOrderId);
    expect(Array.isArray(receiptRes.body.receipts)).toBe(true);
    expect(receiptRes.body.receipts.length).toBeGreaterThanOrEqual(1);
    expect(receiptRes.body.status).toBe('received');

    const verifyRes = await request(app.getHttpServer())
      .get(`/purchase-orders/${purchaseOrderId}`)
      .set('Authorization', `Bearer ${token}`)
      .expect(200);
    expect(verifyRes.body.status).toBe('received');
    expect(verifyRes.body.receipts.length).toBeGreaterThanOrEqual(1);
  });
});