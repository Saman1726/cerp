import { Building2, Handshake, HardHat, LayoutDashboard, Settings, Users, Package, Store, GitBranch, FileText } from 'lucide-react';
import type { NavItem } from '@cerp/core-web/shell';

export const navItems: NavItem[] = [
  { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { label: 'Construction Site', href: '/construction-sites', icon: Building2 },
  { label: 'Project Management', href: '/construction', icon: HardHat },
  { label: 'Procurement', href: '/procurement', icon: Package },
  { label: 'Vendors', href: '/vendors', icon: Store },
  { label: 'My Purchase Orders', href: '/vendors/my-purchase-orders', icon: FileText },
  { label: 'Tenants', href: '/tenants', icon: Users },
  { label: 'Partners', href: '/partners', icon: Handshake },
  { label: 'Settings', href: '/settings', icon: Settings },
  { label: 'Workflows', href: '/workflows', icon: GitBranch },
];
