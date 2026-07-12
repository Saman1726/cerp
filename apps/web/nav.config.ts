import { Building2, Handshake, HardHat, LayoutDashboard, Settings, Users } from 'lucide-react';
import type { NavItem } from '@cerp/core-web/shell';

export const navItems: NavItem[] = [
  { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { label: 'Properties', href: '/properties', icon: Building2 },
  { label: 'Construction', href: '/construction', icon: HardHat },
  { label: 'Tenants', href: '/tenants', icon: Users },
  { label: 'Partners', href: '/partners', icon: Handshake },
  { label: 'Settings', href: '/settings', icon: Settings },
];
