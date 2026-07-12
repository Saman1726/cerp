import { MigrationInterface, QueryRunner } from 'typeorm';

// Owned by this overlay, not imported from @cerp/core-api - core ships no
// migrations of its own (see docs/creating-a-new-overlay.md). This is a copy
// of the reference app's platform migration, since this overlay hasn't
// diverged from core's platform schema.
export class CreatePlatformSchema1700000000000 implements MigrationInterface {
  name = 'CreatePlatformSchema1700000000000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`CREATE SCHEMA IF NOT EXISTS "platform"`);
    await queryRunner.query(`CREATE EXTENSION IF NOT EXISTS pgcrypto`);

    await queryRunner.query(`
      CREATE TABLE "platform"."tenants" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "slug" varchar(32) NOT NULL UNIQUE,
        "name" varchar(200) NOT NULL,
        "schema_name" varchar(63) NOT NULL UNIQUE,
        "status" varchar(20) NOT NULL DEFAULT 'provisioning',
        "created_at" timestamptz NOT NULL DEFAULT now(),
        "updated_at" timestamptz NOT NULL DEFAULT now()
      )
    `);

    await queryRunner.query(`
      CREATE TABLE "platform"."admins" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "email" varchar(255) NOT NULL UNIQUE,
        "password_hash" varchar(255) NOT NULL,
        "created_at" timestamptz NOT NULL DEFAULT now()
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "platform"."admins"`);
    await queryRunner.query(`DROP TABLE IF EXISTS "platform"."tenants"`);
    await queryRunner.query(`DROP SCHEMA IF EXISTS "platform"`);
  }
}
