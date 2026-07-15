import { Settings } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';
import Link from 'next/link';

export default function SettingsPage() {
  return (
    <div>
      <PageHeader title="Settings" description="Organisation, team, and account settings." />
      <div className="mt-6">
        <h2 className="mb-2 text-sm font-medium text-muted-foreground">Sections</h2>
        <ul className="list-disc pl-6">
          <li>
            <Link href="/settings/organisation" className="text-primary underline underline-offset-4">
              Organisation
            </Link>
          </li>
        </ul>
      </div>
      <ComingSoon
        icon={Settings}
        title="Settings module coming soon"
        description="Team management, billing, and organisation preferences will live here."
      />
    </div>
  );
}
