import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreateRefreshTokensTable1700000000002 implements MigrationInterface {
  name = 'CreateRefreshTokensTable1700000000002';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "refresh_tokens" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "user_id" uuid NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "token_hash" varchar(255) NOT NULL,
        "family_id" uuid NOT NULL,
        "revoked_at" timestamptz,
        "expires_at" timestamptz NOT NULL,
        "created_at" timestamptz NOT NULL DEFAULT now()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_refresh_tokens_user_id" ON "refresh_tokens" ("user_id")`);
    await queryRunner.query(`CREATE INDEX "idx_refresh_tokens_family_id" ON "refresh_tokens" ("family_id")`);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "refresh_tokens"`);
  }
}
