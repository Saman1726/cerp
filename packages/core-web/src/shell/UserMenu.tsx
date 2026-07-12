'use client';

import { ChevronsUpDown, LogOut } from 'lucide-react';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';

export interface UserMenuProps {
  tenantSlug: string;
  logoutAction: () => Promise<void>;
}

export function UserMenu({ tenantSlug, logoutAction }: UserMenuProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm outline-none hover:bg-accent hover:text-accent-foreground focus-visible:ring-2 focus-visible:ring-ring">
        <Avatar className="size-7">
          <AvatarFallback className="text-xs uppercase">{tenantSlug.slice(0, 2)}</AvatarFallback>
        </Avatar>
        <span className="font-medium">{tenantSlug}</span>
        <ChevronsUpDown className="size-3.5 text-muted-foreground" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        <DropdownMenuLabel>{tenantSlug}</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <form action={logoutAction} className="w-full">
            <button type="submit" className="flex w-full items-center gap-2">
              <LogOut className="size-4" />
              Log out
            </button>
          </form>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
