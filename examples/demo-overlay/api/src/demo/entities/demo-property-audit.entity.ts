import { Column, CreateDateColumn, Entity, PrimaryGeneratedColumn } from 'typeorm';

// Written by DemoApprovalListener in reaction to core's property.created
// event - proof the event-hook extension point works without core knowing
// this table (or this whole overlay) exists.
@Entity({ name: 'demo_property_audit' })
export class DemoPropertyAudit {
  @PrimaryGeneratedColumn('uuid')
  id!: string;

  @Column({ name: 'property_id', type: 'uuid' })
  propertyId!: string;

  @Column({ name: 'event_type', type: 'varchar', length: 100 })
  eventType!: string;

  @CreateDateColumn({ name: 'occurred_at' })
  occurredAt!: Date;
}
