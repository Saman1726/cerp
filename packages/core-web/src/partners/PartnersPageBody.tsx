import { apiClient } from '../lib/api-client';
import { Badge } from '../components/ui/badge';
import { Card, CardContent } from '../components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { PageHeader } from '../shell/PageHeader';
import type { Partner } from './types';

export async function PartnersPageBody() {
  const partners = await apiClient.get<Partner[]>('/partners');

  return (
    <div>
      <PageHeader
        title="Partners"
        description="Suppliers, contractors, and agents connected through the partner directory."
      />
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Code</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Contact</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {partners.map((partner) => (
                <TableRow key={partner.id}>
                  <TableCell className="font-medium">{partner.name}</TableCell>
                  <TableCell className="text-muted-foreground">{partner.code}</TableCell>
                  <TableCell className="text-muted-foreground capitalize">
                    {partner.type.replace(/-/g, ' ')}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{partner.contactEmail}</TableCell>
                  <TableCell>
                    <Badge variant={partner.status === 'active' ? 'success' : 'secondary'}>
                      {partner.status}
                    </Badge>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}
