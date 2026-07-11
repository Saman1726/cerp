import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn, UpdateDateColumn } from 'typeorm';

export type TenantStatus = 'provisioning' | 'active' | 'suspended' | 'archived';

// Explicit schema here (not the DataSource's ambient search_path) so lookups
// stay correct no matter which physical connection this repository happens
// to run on - see tenancy/tenancy.service.ts for why that matters.
@Entity({ schema: 'platform', name: 'tenants' })
export class Tenant {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 32, unique: true })
  slug!: string;

  @Column({ type: 'varchar', length: 200 })
  name!: string;

  @Column({ name: 'schema_name', type: 'varchar', length: 63, unique: true })
  schemaName!: string;

  @Column({ type: 'varchar', length: 20, default: 'provisioning' })
  status!: TenantStatus;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
