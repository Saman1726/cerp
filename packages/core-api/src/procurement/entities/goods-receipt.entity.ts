import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn } from 'typeorm';

// The "certificate" that goods/materials from a purchase order actually
// arrived - a purchase order can have more than one (partial deliveries),
// which is why this is its own entity rather than a status flag. Plain FK
// column, not a TypeORM relation - same convention as WorkOrder's
// contractor reference and PartnerUser's partner_id.
@Entity({ name: 'goods_receipts' })
export class GoodsReceipt {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'purchase_order_id', type: 'uuid' })
  purchaseOrderId!: string;

  @Column({ type: 'text', nullable: true })
  notes!: string | null;

  @CreateDateColumn({ name: 'received_at' })
  receivedAt!: Date;
}
