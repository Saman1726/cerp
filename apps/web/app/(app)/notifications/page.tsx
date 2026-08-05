import { redirect } from 'next/navigation';
import { getSession } from '@cerp/core-web/lib/session';
import { extractSessionUser } from '../../api/workflows/utils';
import { NotificationsClient } from './NotificationsClient';

export default async function NotificationsPage() {
  const session = await getSession();
  if (!session) {
    redirect('/login');
  }

  const { userId, tenantId } = await extractSessionUser();

  return <NotificationsClient tenantId={tenantId} userId={userId ?? ''} />;
}