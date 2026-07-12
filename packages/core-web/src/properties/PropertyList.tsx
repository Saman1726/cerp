import type { Property } from '@cerp/shared-types';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';

export function PropertyList({ properties }: { properties: Property[] }) {
  if (properties.length === 0) {
    return <p className="py-6 text-center text-sm text-muted-foreground">No properties yet.</p>;
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Name</TableHead>
          <TableHead>Address</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {properties.map((property) => (
          <TableRow key={property.id}>
            <TableCell className="font-medium">{property.name}</TableCell>
            <TableCell className="text-muted-foreground">{property.address}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
