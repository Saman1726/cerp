import type { Property, WorkOrder } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { PageHeader } from '../shell/PageHeader';
import type { Partner } from '../partners/types';
import { WorkOrderForm } from './WorkOrderForm';
import { WorkOrderList } from './WorkOrderList';
import type { IssueWorkOrderActionState } from './types';

export interface ConstructionEngineeringPageBodyProps {
  formAction: (state: IssueWorkOrderActionState, formData: FormData) => Promise<IssueWorkOrderActionState>;
}

export async function ConstructionEngineeringPageBody({ formAction }: ConstructionEngineeringPageBodyProps) {
  const [workOrders, properties, partners] = await Promise.all([
    apiClient.get<WorkOrder[]>('/work-orders'),
    apiClient.get<Property[]>('/properties'),
    apiClient.get<Partner[]>('/partners'),
  ]);

  return (
    <div>
      <PageHeader title="Construction" description="Issue and track work orders against your properties." />
      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">Issue work order</CardTitle>
        </CardHeader>
        <CardContent>
          <WorkOrderForm action={formAction} projects={properties} contractors={partners} />
        </CardContent>
      </Card>
      <Card>
        <CardContent className="p-0">
          <WorkOrderList workOrders={workOrders} />
        </CardContent>
      </Card>
    </div>
  );
}
