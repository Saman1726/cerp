'use client';

import { Menu, X } from 'lucide-react';
import type { CSSProperties, ReactNode } from 'react';
import { useState } from 'react';
import { usePathname } from 'next/navigation';
import { cn } from '../lib/utils';
import type { BrandingConfig } from './branding';
import { UserMenu } from './UserMenu';

export interface ResponsiveShellProps {
  branding: BrandingConfig;
  logoutAction?: () => Promise<void>;
  /** Pre-rendered nav links - built server-side in AppShell so the raw icon
   * component references in NavItem never have to cross the client boundary. */
  nav: ReactNode;
  children: ReactNode;
}

// Branding carries data in (product name, logo, colors); CSS custom
// properties are the mechanism that lets that data cascade to arbitrary
// nested components without prop-drilling. --primary/--primary-foreground/
// --ring are redeclared here (not just --brand-*) so the override is
// resolved directly on this element's own style, rather than relying on a
// var() fallback chain that would only ever see the :root default.
export function ResponsiveShell({ branding, logoutAction, nav, children }: ResponsiveShellProps) {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const pathname = usePathname();

  // Below `lg`, the sidebar is an overlay drawer rather than a permanent
  // column - close it whenever navigation actually happens, same as any
  // mobile nav pattern. Adjusted during render (React's documented pattern
  // for "reset state when a prop changes") rather than in an effect, so the
  // close is applied before paint instead of one extra render behind.
  const [closedForPathname, setClosedForPathname] = useState(pathname);
  if (pathname !== closedForPathname) {
    setClosedForPathname(pathname);
    setIsSidebarOpen(false);
  }

  const themeStyle = {
    '--brand-background': branding.colors.background ?? '#fff',
    '--primary': branding.colors.primary,
    '--primary-foreground': branding.colors.primaryForeground ?? '#fff',
    '--ring': branding.colors.primary,
  } as CSSProperties;

  return (
    <div style={themeStyle} className="flex h-screen bg-background text-foreground">
      {isSidebarOpen ? (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
          aria-hidden="true"
        />
      ) : null}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-64 shrink-0 flex-col border-r border-border bg-background transition-transform duration-200 lg:static lg:translate-x-0',
          isSidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div
          className="flex h-14 shrink-0 items-center justify-between gap-2 border-b border-border px-4"
          style={{ background: 'var(--brand-background)' }}
        >
          <div className="flex items-center gap-2 overflow-hidden">
            {branding.logo ? (
              // Plain <img>, not next/image: this component accepts an
              // arbitrary logo URL per consumer, and requiring every overlay to
              // configure remote image patterns just to show a small logo is
              // more ceremony than it's worth here.
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={branding.logo.src}
                width={branding.logo.width}
                height={branding.logo.height}
                alt={branding.logo.alt ?? branding.productName}
              />
            ) : null}
            <strong className="truncate text-base">{branding.productName}</strong>
          </div>
          <button
            type="button"
            onClick={() => setIsSidebarOpen(false)}
            className="shrink-0 rounded-md p-1 hover:bg-black/10 lg:hidden"
            aria-label="Close menu"
          >
            <X className="size-5" />
          </button>
        </div>
        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto p-2">{nav}</nav>
      </aside>

      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex h-14 shrink-0 items-center gap-2 border-b border-border px-4 sm:px-6">
          <button
            type="button"
            onClick={() => setIsSidebarOpen(true)}
            className="-ml-1 rounded-md p-2 hover:bg-accent lg:hidden"
            aria-label="Open menu"
          >
            <Menu className="size-5" />
          </button>
          <div className="ml-auto">
            <UserMenu logoutAction={logoutAction} />
          </div>
        </header>
        <main className="flex-1 overflow-y-auto p-3 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
