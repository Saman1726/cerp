import { redirect } from 'next/navigation';
import { getSession } from '@cerp/core-web/lib/session';
import { NotificationsClient } from './NotificationsClient';

export default async function NotificationsPage() {
  const session = await getSession();
  if (!session) {
    redirect('/login');
  }

  // Extract user/tenant from session token claims
  const tenantId = 'default';
  const userId = 'default';

  return <NotificationsClient tenantId={tenantId} userId={userId} />;
}