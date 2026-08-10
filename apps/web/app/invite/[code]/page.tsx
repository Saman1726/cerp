import { UserPlus } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default async function InviteAcceptPage({ params }: { params: Promise<{ code: string }> }) {
  const { code } = await params;

  return (
    <div>
      <PageHeader
        title="Accept Organisation Invite"
        description="Join an organisation as an owner or employee using your invite link."
      />
      <ComingSoon
        icon={UserPlus}
        title="Invite acceptance coming soon"
        description={`This page will validate your invite and let you join the organisation. Invite code: ${code}`}
      />
    </div>
  );
}