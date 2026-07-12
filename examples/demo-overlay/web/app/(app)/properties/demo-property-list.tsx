export interface ExtendedProperty {
  id: string;
  name: string;
  address: string;
  inspectorNotes: string | null;
  complianceStatus: string;
}

// This overlay's own presentational component - core's PropertyList only
// knows about name/address, so this extends the shape rather than trying to
// force the extra columns through core's version.
export function DemoPropertyList({ properties }: { properties: ExtendedProperty[] }) {
  if (properties.length === 0) {
    return <p>No properties yet.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          <th align="left">Name</th>
          <th align="left">Address</th>
          <th align="left">Inspector Notes</th>
          <th align="left">Compliance</th>
        </tr>
      </thead>
      <tbody>
        {properties.map((property) => (
          <tr key={property.id}>
            <td>{property.name}</td>
            <td>{property.address}</td>
            <td>{property.inspectorNotes ?? '—'}</td>
            <td>{property.complianceStatus}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
