import type { ReactNode } from 'react';
import { redirect } from 'next/navigation';
import { getSession, clearSession } from '@/lib/session';

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
    <div>
      <header
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '12px 24px',
          borderBottom: '1px solid #ddd',
        }}
      >
        <strong>{session.tenantSlug}</strong>
        <form action={logout}>
          <button type="submit">Log out</button>
        </form>
      </header>
      <main style={{ padding: 24 }}>{children}</main>
    </div>
  );
}
