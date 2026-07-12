import { PropertyForm } from '@cerp/core-web/properties';
import { apiClient } from '@cerp/core-web/lib/api-client';
import { DemoPropertyList, type ExtendedProperty } from './demo-property-list';
import { createDemoPropertyAction } from './actions';

// Core's PropertyForm is reused unmodified - the extraFields slot is exactly
// how a customer adds a field without forking the component.
export default async function PropertiesPage() {
  const properties = await apiClient.get<ExtendedProperty[]>('/demo/properties');

  return (
    <div>
      <h1>Properties</h1>
      <PropertyForm
        action={createDemoPropertyAction}
        extraFields={<input name="inspectorNotes" placeholder="Inspector notes (optional)" />}
      />
      <p style={{ color: '#666', fontSize: 14 }}>
        This tenant requires addresses to end in a 5-digit postal code.
      </p>
      <DemoPropertyList properties={properties} />
    </div>
  );
}
