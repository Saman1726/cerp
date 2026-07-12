import type { CSSProperties, ReactNode } from 'react';
import type { BrandingConfig } from './branding';
import type { NavItem } from './nav-item';
import { NavLink } from './NavLink';
import { UserMenu } from './UserMenu';

export interface AppShellProps {
  branding: BrandingConfig;
  navItems: NavItem[];
  tenantSlug: string;
  logoutAction: () => Promise<void>;
  children: ReactNode;
}

// Branding carries data in (product name, logo, colors); CSS custom
// properties are the mechanism that lets that data cascade to arbitrary
// nested components without prop-drilling. --primary/--primary-foreground/
// --ring are redeclared here (not just --brand-*) so the override is
// resolved directly on this element's own style, rather than relying on a
// var() fallback chain that would only ever see the :root default.
export function AppShell({ branding, navItems, tenantSlug, logoutAction, children }: AppShellProps) {
  const themeStyle = {
    '--brand-background': branding.colors.background ?? '#fff',
    '--primary': branding.colors.primary,
    '--primary-foreground': branding.colors.primaryForeground ?? '#fff',
    '--ring': branding.colors.primary,
  } as CSSProperties;

  return (
    <div style={themeStyle} className="flex h-screen bg-background text-foreground">
      <aside className="flex w-64 shrink-0 flex-col border-r border-border">
        <div
          className="flex h-14 shrink-0 items-center gap-2 border-b border-border px-4"
          style={{ background: 'var(--brand-background)' }}
        >
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
          <strong className="text-base">{branding.productName}</strong>
        </div>
        <nav className="flex flex-1 flex-col gap-1 overflow-y-auto p-2">
          {navItems.map((item) => (
            <NavLink key={item.href} href={item.href} label={item.label} icon={<item.icon className="size-4 shrink-0" />} />
          ))}
        </nav>
      </aside>
      <div className="flex flex-1 flex-col overflow-hidden">
        <header className="flex h-14 shrink-0 items-center justify-end border-b border-border px-6">
          <UserMenu tenantSlug={tenantSlug} logoutAction={logoutAction} />
        </header>
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}
