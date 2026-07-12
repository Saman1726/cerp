import type { WorkOrder } from '@cerp/shared-types';
import { Badge } from '../components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';

export function WorkOrderList({ workOrders }: { workOrders: WorkOrder[] }) {
  if (workOrders.length === 0) {
    return <p className="py-6 text-center text-sm text-muted-foreground">No work orders yet.</p>;
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Title</TableHead>
          <TableHead>Contractor</TableHead>
          <TableHead>Status</TableHead>
          <TableHead>Issued</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {workOrders.map((workOrder) => (
          <TableRow key={workOrder.id}>
            <TableCell className="font-medium">{workOrder.title}</TableCell>
            <TableCell className="text-muted-foreground">{workOrder.contractorPartnerName}</TableCell>
            <TableCell>
              <Badge variant={workOrder.status === 'completed' ? 'success' : 'secondary'}>{workOrder.status}</Badge>
            </TableCell>
            <TableCell className="text-muted-foreground">{new Date(workOrder.issuedAt).toLocaleDateString()}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
