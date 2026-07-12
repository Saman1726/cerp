import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn, UpdateDateColumn } from 'typeorm';

export type WorkOrderStatus = 'issued' | 'completed';

// Tenant-scoped, no schema set (see properties/entities/property.entity.ts)
// and deliberately no tenant_id column - physical schema separation is the
// isolation boundary. contractor_partner_id/name are a denormalized
// snapshot from Party Registry (a separate external system/database) taken
// at assignment time - there's no foreign key to enforce since it isn't our
// own table.
@Entity({ name: 'work_orders' })
export class WorkOrder {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'project_id', type: 'uuid' })
  projectId!: string;

  @Column({ type: 'varchar', length: 200 })
  title!: string;

  @Column({ type: 'text', nullable: true })
  description!: string | null;

  @Column({ name: 'contractor_partner_id', type: 'varchar', length: 64 })
  contractorPartnerId!: string;

  @Column({ name: 'contractor_partner_name', type: 'varchar', length: 255 })
  contractorPartnerName!: string;

  @Column({ type: 'varchar', length: 16, default: 'issued' })
  status!: WorkOrderStatus;

  @CreateDateColumn({ name: 'issued_at' })
  issuedAt!: Date;

  @Column({ name: 'completed_at', type: 'timestamptz', nullable: true })
  completedAt!: Date | null;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
