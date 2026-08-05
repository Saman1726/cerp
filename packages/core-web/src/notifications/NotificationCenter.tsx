'use client';

import React, { useState } from 'react';
import { useNotifications } from './NotificationProvider';
import { NotificationStatus } from '@cerp/shared-types';

/**
 * NotificationCenter - A panel/modal that displays user notifications.
 * Supports filtering by status and marking as read.
 */
export const NotificationCenter: React.FC = () => {
  const { notifications, loading, acknowledge, refresh } = useNotifications();
  const [filter, setFilter] = useState<NotificationStatus | undefined>();
  const [expanded, setExpanded] = useState(false);

  const filteredNotifications = filter
    ? notifications.filter((n) => n.status === filter)
    : notifications;

  const handleMarkRead = async (id: string) => {
    await acknowledge(id);
  };

  const handleMarkAllRead = async () => {
    const pendingNotifications = notifications.filter(
      (n) => n.status === NotificationStatus.PENDING
    );
    for (const n of pendingNotifications) {
      await acknowledge(n.id);
    }
    await refresh();
  };

  return (
    <div className="relative">
      <button
        onClick={() => setExpanded(!expanded)}
        className="relative p-2 text-gray-600 hover:text-gray-900"
        aria-label="Notifications"
      >
        <svg
          className="h-6 w-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>
        {notifications.filter((n) => n.status === NotificationStatus.PENDING)
          .length > 0 && (
          <span className="absolute right-0.5 top-0.5 h-2.5 w-2.5 rounded-full bg-red-500" />
        )}
      </button>

      {expanded && (
        <div className="absolute right-0 top-12 w-96 max-h-[600px] overflow-y rounded-lg border bg-white shadow-lg">
          <div className="border-b p-4">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">Notifications</h3>
              <button
                onClick={handleMarkAllRead}
                className="text-sm text-blue-600 hover:text-blue-800"
              >
                Mark all as read
              </button>
            </div>
            <div className="mt-3 flex gap-2">
              {([undefined, NotificationStatus.PENDING, NotificationStatus.READ] as (
                | NotificationStatus
                | undefined
              )[]).map((status) => (
                <button
                  key={status ?? 'all'}
                  onClick={() => setFilter(status)}
                  className={`rounded px-3 py-1 text-sm ${
                    filter === status
                      ? 'bg-blue-100 text-blue-700'
                      : 'text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  {status === undefined
                    ? 'All'
                    : status === NotificationStatus.PENDING
                    ? 'Pending'
                    : 'Read'}
                </button>
              ))}
            </div>
          </div>
          <div className="overflow-y-auto" style={{ maxHeight: '450px' }}>
            {loading && <div className="p-4 text-center">Loading...</div>}
            {!loading && filteredNotifications.length === 0 && (
              <div className="p-4 text-center text-gray-500">
                No notifications
              </div>
            )}
            {filteredNotifications.map((notification) => (
              <div
                key={notification.id}
                className={`border-b p-4 cursor-pointer transition-colors hover:bg-gray-50 ${
                  notification.status === NotificationStatus.PENDING
                    ? 'bg-blue-50'
                    : ''
                }`}
                onClick={() =>
                  notification.status === NotificationStatus.PENDING &&
                  handleMarkRead(notification.id)
                }
              >
                <div className="flex items-start gap-3">
                  <div
                    className={`mt-1 h-2 w-2 rounded-full flex-shrink-0 ${
                      notification.status === NotificationStatus.PENDING
                        ? 'bg-blue-500'
                        : 'bg-gray-300'
                    }`}
                  />
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-gray-900 truncate">
                      {notification.title}
                    </p>
                    <p className="text-sm text-gray-600 truncate">
                      {notification.description}
                    </p>
                    <div className="mt-1 flex items-center gap-2">
                      <span
                        className={`inline-flex rounded px-2 py-0.5 text-xs ${
                          notification.status === NotificationStatus.PENDING
                            ? 'bg-yellow-100 text-yellow-700'
                            : notification.status ===
                              NotificationStatus.ACTION_TAKEN
                            ? 'bg-green-100 text-green-700'
                            : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {notification.status}
                      </span>
                      <span className="text-xs text-gray-400">
                        {notification.createdAt
                          ? new Date(notification.createdAt).toLocaleString()
                          : ''}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};