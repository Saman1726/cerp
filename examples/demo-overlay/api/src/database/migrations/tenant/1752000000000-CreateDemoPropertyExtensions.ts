import { MigrationInterface, QueryRunner } from 'typeorm';

// Unqualified table names, same convention as core's own tenant migrations -
// runs against a connection whose search_path is already set to the target
// tenant schema (see data-source.tenant-template.ts).
export class CreateDemoPropertyExtensions1752000000000 implements MigrationInterface {
  name = 'CreateDemoPropertyExtensions1752000000000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`CREATE EXTENSION IF NOT EXISTS pgcrypto`);

    await queryRunner.query(`
      CREATE TABLE "demo_property_extensions" (
        "property_id" uuid PRIMARY KEY REFERENCES "properties"("id") ON DELETE CASCADE,
        "inspector_notes" text,
        "compliance_status" varchar(50) NOT NULL DEFAULT 'pending'
      )
    `);

    await queryRunner.query(`
      CREATE TABLE "demo_property_audit" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "property_id" uuid NOT NULL,
        "event_type" varchar(100) NOT NULL,
        "occurred_at" timestamptz NOT NULL DEFAULT now()
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "demo_property_audit"`);
    await queryRunner.query(`DROP TABLE IF EXISTS "demo_property_extensions"`);
  }
}
