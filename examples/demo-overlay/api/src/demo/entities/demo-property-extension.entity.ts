import { Column, Entity, PrimaryColumn } from 'typeorm';

// Satellite table, not a modification of core's "properties" table: FKs back
// to properties.id, owns its own column set and migration, and follows the
// same tenancy convention as core entities (no schema on @Entity(), no
// tenant_id column - reached only via TenancyService.getEntityManager()).
@Entity({ name: 'demo_property_extensions' })
export class DemoPropertyExtension {
  @PrimaryColumn({ name: 'property_id', type: 'uuid' })
  propertyId!: string;

  @Column({ name: 'inspector_notes', type: 'text', nullable: true })
  inspectorNotes!: string | null;

  @Column({ name: 'compliance_status', type: 'varchar', length: 50, default: 'pending' })
  complianceStatus!: string;
}
