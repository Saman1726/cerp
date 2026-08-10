import { Building2, Camera, HardHat, LayoutDashboard, Settings, Users, Package, Store, GitBranch, FileText, Wallet, Bell, Megaphone } from 'lucide-react';
import type { NavItem } from '@cerp/core-web/shell';

export const navItems: NavItem[] = [
  { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { label: 'Construction Site', href: '/construction-sites', icon: Building2 },
  { label: 'Project Management', href: '/construction', icon: HardHat },
  { label: 'Site Capture', href: '/site-capture', icon: Camera },
  { label: 'Procurement', href: '/procurement', icon: Package },
  { label: 'Bid Wallet', href: '/procurement/wallet', icon: Wallet },
  { label: 'Vendors', href: '/vendors', icon: Store },
  { label: 'My Purchase Orders', href: '/vendors/my-purchase-orders', icon: FileText },
  { label: 'Tenants', href: '/tenants', icon: Users },
  { label: 'Settings', href: '/settings', icon: Settings },
  { label: 'Workflows', href: '/workflows', icon: GitBranch },
  { label: 'Notifications', href: '/notifications', icon: Bell },
  { label: 'Open Tenders', href: '/open-tenders', icon: Megaphone },
];
