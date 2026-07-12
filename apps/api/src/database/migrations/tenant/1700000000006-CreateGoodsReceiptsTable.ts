import { MigrationInterface, QueryRunner } from 'typeorm';

export class CreateGoodsReceiptsTable1700000000006 implements MigrationInterface {
  name = 'CreateGoodsReceiptsTable1700000000006';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`
      CREATE TABLE "goods_receipts" (
        "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
        "purchase_order_id" uuid NOT NULL REFERENCES "purchase_orders"("id"),
        "notes" text,
        "received_at" timestamptz NOT NULL DEFAULT now()
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "goods_receipts"`);
  }
}
