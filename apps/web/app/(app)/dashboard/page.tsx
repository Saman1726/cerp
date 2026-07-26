import { Building2, Handshake, HardHat, Users } from 'lucide-react';
import type { ConstructionSite } from '@cerp/shared-types';
import { apiClient } from '@cerp/core-web/lib/api-client';
import { StatCard } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default async function DashboardPage() {
  const [constructionSites] = await Promise.all([
    apiClient.get<ConstructionSite[]>('/construction-sites'),
  ]);

  return (
    <div>
      <PageHeader title="Dashboard" description="An overview of your portfolio." />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Construction sites" value={constructionSites.length} icon={Building2} />
        <StatCard label="Partners" value="—" icon={Handshake} />
        <StatCard label="Work orders" value="—" icon={HardHat} />
        <StatCard label="Tenants" value="—" icon={Users} />
      </div>
    </div>
  );
}
