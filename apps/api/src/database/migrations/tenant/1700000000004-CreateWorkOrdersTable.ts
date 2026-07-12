import { MigrationInterface, QueryRunner } from 'typeorm';

// Unqualified table names throughout: run against a connection whose
// search_path is already set to the target tenant schema (see
// data-source.tenant-template.ts).
export class CreateWorkOrdersTable1700000000004 implements MigrationInterface {
  name = 'CreateWorkOrdersTable1700000000004';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "work_orders" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "project_id" uuid NOT NULL REFERENCES "properties"("id"),
        "title" varchar(200) NOT NULL,
        "description" text,
        "contractor_partner_id" varchar(64) NOT NULL,
        "contractor_partner_name" varchar(255) NOT NULL,
        "status" varchar(16) NOT NULL DEFAULT 'issued',
        "issued_at" timestamptz NOT NULL DEFAULT now(),
        "completed_at" timestamptz,
        "updated_at" timestamptz NOT NULL DEFAULT now()
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "work_orders"`);
  }
}
