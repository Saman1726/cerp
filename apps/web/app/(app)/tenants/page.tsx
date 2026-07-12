import { Users } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default function TenantsPage() {
  return (
    <div>
      <PageHeader title="Tenants" description="Manage tenants and lease agreements." />
      <ComingSoon
        icon={Users}
        title="Tenants module coming soon"
        description="Tenant profiles, lease terms, and rent schedules will live here."
      />
    </div>
  );
}
