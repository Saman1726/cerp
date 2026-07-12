import type { TenantContext } from '../../tenancy/interfaces/tenant-context.interface';
import type { Property } from '../entities/property.entity';

export const PROPERTY_CREATED_EVENT = 'property.created';

export interface PropertyCreatedEvent {
  property: Property;
  tenantContext: TenantContext;
}
