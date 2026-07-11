import type { Property } from '@cerp/shared-types';
import { apiClient } from '@/lib/api-client';
import { PropertyList } from './property-list';
import { PropertyForm } from './property-form';

export default async function PropertiesPage() {
  const properties = await apiClient.get<Property[]>('/properties');

  return (
    <div>
      <h1>Properties</h1>
      <PropertyForm />
      <PropertyList properties={properties} />
    </div>
  );
}
