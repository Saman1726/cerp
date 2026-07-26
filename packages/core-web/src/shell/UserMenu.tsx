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
  logoutAction?: () => Promise<void>;
}

export function UserMenu({ logoutAction }: UserMenuProps) {
  const handleLogout = async () => {
    if (logoutAction) {
      await logoutAction();
      return;
    }
    // Call the /logout POST route handler to clear cookies, then navigate
    // to /login via window.location so the browser does a full page load
    // (required because next/navigation redirect won't work from inside a
    // client-component async event handler that was triggered by a form
    // button click in the RSC tree).
    await fetch('/logout', { method: 'POST' });
    window.location.href = '/login';
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex items-center gap-2 rounded-md px-2 text-sm outline-none hover:bg-accent hover:text-accent-foreground focus-visible:ring-2 focus-visible:ring-ring">
        <Avatar className="size-7">
          <AvatarFallback className="text-xs uppercase">U</AvatarFallback>
        </Avatar>
        <span className="font-medium">Account</span>
        <ChevronsUpDown className="size-3.5 text-muted-foreground" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        <DropdownMenuLabel>Account</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={handleLogout}>
          <LogOut className="size-4" />
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
