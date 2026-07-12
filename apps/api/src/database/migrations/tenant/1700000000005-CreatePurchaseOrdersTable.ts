import { MigrationInterface, QueryRunner } from 'typeorm';

// Unqualified table names throughout: run against a connection whose
// search_path is already set to the target tenant schema (see
// data-source.tenant-template.ts).
export class CreatePurchaseOrdersTable1700000000005 implements MigrationInterface {
  name = 'CreatePurchaseOrdersTable1700000000005';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "purchase_orders" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "project_id" uuid NOT NULL REFERENCES "properties"("id"),
        "vendor_partner_id" varchar(64) NOT NULL,
        "vendor_partner_name" varchar(255) NOT NULL,
        "description" varchar(500) NOT NULL,
        "status" varchar(16) NOT NULL DEFAULT 'ordered',
        "ordered_at" timestamptz NOT NULL DEFAULT now(),
        "updated_at" timestamptz NOT NULL DEFAULT now()
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "purchase_orders"`);
  }
}
