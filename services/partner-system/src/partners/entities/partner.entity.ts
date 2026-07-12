import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn, UpdateDateColumn } from 'typeorm';

export type PartnerType = 'supplier' | 'contractor' | 'real-estate-agent';
export type PartnerStatus = 'active' | 'inactive';

@Entity({ name: 'partners' })
export class Partner {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 64, unique: true })
  code!: string;

  @Column({ type: 'varchar', length: 255 })
  name!: string;

  @Column({ type: 'varchar', length: 32 })
  type!: PartnerType;

  @Column({ name: 'contact_email', type: 'varchar', length: 255 })
  contactEmail!: string;

  @Column({ type: 'varchar', length: 16, default: 'active' })
  status!: PartnerStatus;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
