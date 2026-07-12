import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreatePartnerUsersTable1700000100001 implements MigrationInterface {
  name = 'CreatePartnerUsersTable1700000100001';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "partner_users" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "username" varchar(255) NOT NULL,
        "password_hash" varchar(255) NOT NULL,
        "partner_id" uuid NOT NULL REFERENCES "partners"("id") ON DELETE CASCADE,
        "role" varchar(64) NOT NULL,
        "created_at" timestamptz NOT NULL DEFAULT now(),
        "updated_at" timestamptz NOT NULL DEFAULT now(),
        UNIQUE ("username", "partner_id")
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "partner_users"`);
  }
}
