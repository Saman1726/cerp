import type { ReactNode } from 'react';
import type { BrandingConfig } from './branding';
import type { NavItem } from './nav-item';
import { NavLink } from './NavLink';
import { ResponsiveShell } from './ResponsiveShell';

export interface AppShellProps {
  branding: BrandingConfig;
  navItems: NavItem[];
  logoutAction?: () => Promise<void>;
  children: ReactNode;
}

// Stays a Server Component deliberately: NavItem.icon is a raw component
// reference (e.g. `LayoutDashboard` from lucide-react), and those can't
// cross the server/client boundary as a prop - only already-rendered
// elements can. Rendering the nav links here, in the same server pass that
// already has the icon references in scope, and handing the *result* down
// to the client-side ResponsiveShell sidesteps that entirely.
export function AppShell({ branding, navItems, logoutAction, children }: AppShellProps) {
  const nav = navItems.map((item) => (
    <NavLink key={item.href} href={item.href} label={item.label} icon={<item.icon className="size-4 shrink-0" />} />
  ));

  return (
    <ResponsiveShell branding={branding} logoutAction={logoutAction} nav={nav}>
      {children}
    </ResponsiveShell>
  );
}
