import { Settings } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default function SettingsPage() {
  return (
    <div>
      <PageHeader title="Settings" description="Organization, team, and account settings." />
      <ComingSoon
        icon={Settings}
        title="Settings module coming soon"
        description="Team management, billing, and organization preferences will live here."
      />
    </div>
  );
}
