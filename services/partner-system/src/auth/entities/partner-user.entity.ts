import { Column, CreateDateColumn, Entity, Index, PrimaryGeneratedColumn, UpdateDateColumn } from 'typeorm';

// partner_id is a plain FK column, not a TypeORM relation - AuthService only
// ever needs "look up the partner by code, then look up this user by
// username + partner id," two simple queries with no join required.
@Entity({ name: 'partner_users' })
@Index(['username', 'partnerId'], { unique: true })
export class PartnerUser {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 255 })
  username!: string;

  @Column({ name: 'password_hash', type: 'varchar', length: 255 })
  passwordHash!: string;

  @Column({ name: 'partner_id', type: 'uuid' })
  partnerId!: string;

  @Column({ type: 'varchar', length: 64 })
  role!: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt!: Date;
}
