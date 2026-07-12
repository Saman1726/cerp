import { Building2 } from 'lucide-react';
import type { NavItem } from '@cerp/core-web/shell';

// This overlay only has one real page - a longer nav list would just be dead
// links. Proves nav is consumer-supplied config, not core-hardcoded.
export const navItems: NavItem[] = [{ label: 'Properties', href: '/properties', icon: Building2 }];
