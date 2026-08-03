'use client';

import React from 'react';
import { useNotifications } from './NotificationProvider';

interface NotificationBadgeProps {
  className?: string;
}

/**
 * NotificationBadge - A small badge component showing unread notification count.
 * Typically placed next to a notification bell icon.
 */
export const NotificationBadge: React.FC<NotificationBadgeProps> = ({
  className = '',
}) => {
  const { unreadCount } = useNotifications();

  if (unreadCount === 0) {
    return null;
  }

  return (
    <span
      className={`inline-flex items-center justify-center rounded-full bg-red-500 px-1.5 py-0.5 text-xs font-medium text-white min-w-[1.25rem] ${className}`}
    >
      {unreadCount > 99 ? '99+' : unreadCount}
    </span>
  );
};