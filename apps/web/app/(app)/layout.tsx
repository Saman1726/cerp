import type { ReactNode } from 'react';
import { redirect } from 'next/navigation';
import { getSession, clearSession } from '@cerp/core-web/lib/session';
import { AppShell } from '@cerp/core-web/shell';
import { branding } from '@/branding.config';
import { navItems } from '@/nav.config';

export default async function AppLayout({ children }: { children: ReactNode }) {
  const session = await getSession();
  if (!session) {
    redirect('/login');
  }

  async function logout() {
    'use server';
    await clearSession();
    redirect('/login');
  }

  return (
    <AppShell branding={branding} navItems={navItems} logoutAction={logout}>
      {children}
    </AppShell>
  );
}
