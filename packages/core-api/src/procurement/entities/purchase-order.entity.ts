import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn, UpdateDateColumn } from 'typeorm';

export type PurchaseOrderStatus = 'ordered' | 'received';

// Tenant-scoped, no schema set (see properties/entities/property.entity.ts)
// and deliberately no tenant_id column - physical schema separation is the
// isolation boundary. vendor_partner_id/name are a denormalized snapshot
// from Party Registry (a separate external system/database) taken at order
// time - there's no foreign key to enforce since it isn't our own table.
@Entity({ name: 'purchase_orders' })
export class PurchaseOrder {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'project_id', type: 'uuid' })
  projectId!: string;

  @Column({ name: 'vendor_partner_id', type: 'varchar', length: 64 })
  vendorPartnerId!: string;

  @Column({ name: 'vendor_partner_name', type: 'varchar', length: 255 })
  vendorPartnerName!: string;

  @Column({ type: 'varchar', length: 500 })
  description!: string;

  @Column({ type: 'varchar', length: 16, default: 'ordered' })
  status!: PurchaseOrderStatus;

  @CreateDateColumn({ name: 'ordered_at' })
  orderedAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
