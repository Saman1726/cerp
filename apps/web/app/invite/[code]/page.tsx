import { UserPlus } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default function InviteAcceptPage({ params }: { params: { code: string } }) {
  const { code } = params;

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