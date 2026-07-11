import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn } from 'typeorm';

// Platform-operator staff, not tenant end-users. Modeled now for architectural
// separation; no module/API exposes this yet (see plan: out of scope for v1).
@Entity({ schema: 'platform', name: 'admins' })
export class PlatformAdmin {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ type: 'varchar', length: 255, unique: true })
  email!: string;

  @Column({ name: 'password_hash', type: 'varchar', length: 255 })
  passwordHash!: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt!: Date;
}
