'use client';

import Link from 'next/link';
import type { ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import { cn } from '../lib/utils';

export interface NavLinkProps {
  href: string;
  label: string;
  /** Rendered on the server (AppShell) and passed down as an element, not a
   * component reference - RSC can't serialize a bare component/function
   * reference across the server->client boundary, only actual elements. */
  icon: ReactNode;
}

export function NavLink({ href, label, icon }: NavLinkProps) {
  const pathname = usePathname();
  const isActive = pathname === href || pathname.startsWith(`${href}/`);

  return (
    <Link
      href={href}
      className={cn(
        'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
        isActive
          ? 'bg-primary text-primary-foreground'
          : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
      )}
    >
      {icon}
      {label}
    </Link>
  );
}
