import type { Property } from '@cerp/shared-types';

export function PropertyList({ properties }: { properties: Property[] }) {
  if (properties.length === 0) {
    return <p>No properties yet.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          <th align="left">Name</th>
          <th align="left">Address</th>
        </tr>
      </thead>
      <tbody>
        {properties.map((property) => (
          <tr key={property.id}>
            <td>{property.name}</td>
            <td>{property.address}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
