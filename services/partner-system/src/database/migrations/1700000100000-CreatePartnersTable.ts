import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreatePartnersTable1700000100000 implements MigrationInterface {
  name = 'CreatePartnersTable1700000100000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`CREATE EXTENSION IF NOT EXISTS pgcrypto`);
    await queryRunner.query(`
      CREATE TABLE "partners" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "code" varchar(64) NOT NULL UNIQUE,
        "name" varchar(255) NOT NULL,
        "type" varchar(32) NOT NULL,
        "contact_email" varchar(255) NOT NULL,
        "status" varchar(16) NOT NULL DEFAULT 'active',
        "created_at" timestamptz NOT NULL DEFAULT now(),
        "updated_at" timestamptz NOT NULL DEFAULT now()
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "partners"`);
  }
}
