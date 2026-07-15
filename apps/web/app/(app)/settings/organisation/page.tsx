import { Users } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default function OrganisationSettingsPage() {
  return (
    <div>
      <PageHeader
        title="Organisation"
        description="Manage owners, employees, and invite links for your organisation."
      />
      <ComingSoon
        icon={Users}
        title="Organisation management"
        description="Assign an organisation to your account, manage owners and employees, and invite others via a shareable link."
      />
    </div>
  );
}