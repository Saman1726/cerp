import { Building2, Handshake, HardHat, Users } from 'lucide-react';
import type { Property, WorkOrder } from '@cerp/shared-types';
import type { Partner } from '@cerp/core-web/partners';
import { apiClient } from '@cerp/core-web/lib/api-client';
import { StatCard } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default async function DashboardPage() {
  const [properties, partners, workOrders] = await Promise.all([
    apiClient.get<Property[]>('/properties'),
    apiClient.get<Partner[]>('/partners'),
    apiClient.get<WorkOrder[]>('/work-orders'),
  ]);

  return (
    <div>
      <PageHeader title="Dashboard" description="An overview of your portfolio." />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Properties" value={properties.length} icon={Building2} />
        <StatCard label="Partners" value={partners.length} icon={Handshake} />
        <StatCard label="Work orders" value={workOrders.length} icon={HardHat} />
        <StatCard label="Tenants" value="—" icon={Users} />
      </div>
    </div>
  );
}
