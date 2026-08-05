import { redirect } from 'next/navigation';
import { getSession } from '@cerp/core-web/lib/session';
import { extractSessionUser } from '../../api/workflows/utils';
import { OpenTendersClient } from './OpenTendersClient';

export default async function OpenTendersPage() {
  const session = await getSession();
  if (!session) {
    redirect('/login');
  }

  const { userId, tenantId } = await extractSessionUser();

  return <OpenTendersClient tenantId={tenantId} userId={userId ?? ''} />;
}
