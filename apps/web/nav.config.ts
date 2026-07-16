import { Building2, Handshake, HardHat, LayoutDashboard, Settings, Users, Package } from 'lucide-react';
import type { NavItem } from '@cerp/core-web/shell';

export const navItems: NavItem[] = [
  { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { label: 'Construction Site', href: '/construction-sites', icon: Building2 },
  { label: 'Construction Progress', href: '/construction', icon: HardHat },
  { label: 'Procurement', href: '/procurement', icon: Package },
  { label: 'Tenants', href: '/tenants', icon: Users },
  { label: 'Partners', href: '/partners', icon: Handshake },
  { label: 'Settings', href: '/settings', icon: Settings },
];
