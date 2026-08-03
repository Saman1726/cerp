'use client';

import { useState } from 'react';
import { Bell, Check, Clock } from 'lucide-react';
import { NotificationProvider, useNotifications } from '@cerp/core-web/notifications';
import { NotificationStatus } from '@cerp/shared-types';

function NotificationsContent() {
  const { notifications, loading, acknowledge, takeAction, refresh } = useNotifications();
  const [filter, setFilter] = useState<NotificationStatus | undefined>();
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [actionResult, setActionResult] = useState<Record<string, unknown> | null>(null);

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

  const handleTakeAction = async (notificationId: string) => {
    try {
      await takeAction(notificationId, actionResult ?? {});
      setActionResult(null);
      setExpandedId(null);
      await refresh();
    } catch (error) {
      console.error('Failed to take action:', error);
    }
  };

  const getStatusColor = (status: string | undefined) => {
    switch (status) {
      case NotificationStatus.PENDING:
        return 'bg-yellow-100 text-yellow-700';
      case NotificationStatus.READ:
        return 'bg-gray-100 text-gray-700';
      case NotificationStatus.ACTION_TAKEN:
        return 'bg-green-100 text-green-700';
      case NotificationStatus.EXPIRED:
        return 'bg-red-100 text-red-700';
      case NotificationStatus.ARCHIVED:
        return 'bg-blue-100 text-blue-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  const getPriorityColor = (priority: string | undefined) => {
    switch (priority) {
      case 'URGENT':
        return 'text-red-600';
      case 'HIGH':
        return 'text-orange-600';
      case 'MEDIUM':
        return 'text-yellow-600';
      case 'LOW':
        return 'text-gray-500';
      default:
        return 'text-gray-500';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Notifications</h1>
          <p className="text-sm text-gray-500">
            {notifications.filter((n) => n.status === NotificationStatus.PENDING).length} unread
            out of {notifications.length} total
          </p>
        </div>
        <button
          onClick={handleMarkAllRead}
          className="flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          <Check className="h-4 w-4" />
          Mark all as read
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex gap-2">
          {[
            { status: undefined, label: 'All' },
            { status: NotificationStatus.PENDING, label: 'Pending' },
            { status: NotificationStatus.READ, label: 'Read' },
            { status: NotificationStatus.ACTION_TAKEN, label: 'Action Taken' },
            { status: NotificationStatus.EXPIRED, label: 'Expired' },
          ].map(({ status, label }) => (
            <button
              key={status ?? 'all'}
              onClick={() => setFilter(status)}
              className={`rounded-t-md px-3 py-2 text-sm font-medium ${
                filter === status
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
            >
              {label}
            </button>
          ))}
        </nav>
      </div>

      {/* Notifications List */}
      <div className="space-y-3">
        {loading && <div className="p-4 text-center text-gray-500">Loading...</div>}

        {!loading && filteredNotifications.length === 0 && (
          <div className="flex flex-col items-center justify-center rounded-lg border border-dashed p-12 text-center">
            <Bell className="mb-4 h-12 w-12 text-gray-300" />
            <p className="text-gray-500">No notifications yet</p>
          </div>
        )}

        {filteredNotifications.map((notification) => (
          <div
            key={notification.id}
            className={`overflow-hidden rounded-lg border transition-colors hover:shadow-sm ${
              notification.status === NotificationStatus.PENDING
                ? 'border-blue-200 bg-blue-50/50'
                : 'border-gray-200 bg-white'
            }`}
          >
            <div className="p-4">
              <div className="flex items-start gap-4">
                {/* Status indicator */}
                <div
                  className={`mt-1 h-2.5 w-2.5 flex-shrink-0 rounded-full ${
                    notification.status === NotificationStatus.PENDING
                      ? 'bg-blue-500'
                      : 'bg-gray-300'
                  }`}
                />

                <div className="flex-1 min-w-0">
                  {/* Title and metadata */}
                  <div className="flex items-center gap-2">
                    <h3 className="font-medium text-gray-900">{notification.title}</h3>
                    {notification.priority && (
                      <span className={`text-xs font-medium ${getPriorityColor(notification.priority)}`}>
                        {notification.priority}
                      </span>
                    )}
                  </div>

                  {/* Description */}
                  {notification.description && (
                    <p className="mt-1 text-sm text-gray-600">
                      {notification.description}
                    </p>
                  )}

                  {/* Meta info */}
                  <div className="mt-2 flex items-center gap-3">
                    <span className={`inline-flex rounded px-2 py-0.5 text-xs ${getStatusColor(notification.status)}`}>
                      {notification.status}
                    </span>
                    <span className="flex items-center gap-1 text-xs text-gray-400">
                      <Clock className="h-3 w-3" />
                      {notification.createdAt
                        ? new Date(notification.createdAt).toLocaleString()
                        : ''}
                    </span>
                    {notification.dueAt && (
                      <span className="text-xs text-gray-400">
                        Due: {new Date(notification.dueAt).toLocaleString()}
                      </span>
                    )}
                  </div>

                  {/* Action expand/collapse */}
                  {notification.status === NotificationStatus.PENDING && (
                    <>
                      <div className="mt-3 flex items-center gap-2">
                        <button
                          onClick={() => handleMarkRead(notification.id)}
                          className="rounded-md border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
                        >
                          Mark as read
                        </button>
                        <button
                          onClick={() =>
                            setExpandedId(expandedId === notification.id ? null : notification.id)
                          }
                          className="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-blue-700"
                        >
                          {expandedId === notification.id ? 'Cancel' : 'Take Action'}
                        </button>
                      </div>

                      {/* Action form */}
                      {expandedId === notification.id && (
                        <div className="mt-3 rounded-md border border-gray-200 bg-white p-4">
                          <p className="mb-3 text-sm font-medium text-gray-700">
                            Enter action result:
                          </p>
                          <textarea
                            value={JSON.stringify(actionResult ?? {}, null, 2)}
                            onChange={(e) => {
                              try {
                                setActionResult(JSON.parse(e.target.value || '{}'));
                              } catch {
                                // Keep previous valid value
                              }
                            }}
                            className="mb-3 w-full rounded-md border border-gray-300 p-2 text-sm font-mono focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                            rows={4}
                          />
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => handleTakeAction(notification.id)}
                              className="rounded-md bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700"
                            >
                              Submit
                            </button>
                            <button
                              onClick={() => {
                                setExpandedId(null);
                                setActionResult(null);
                              }}
                              className="rounded-md border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function NotificationsClient({ tenantId, userId }: { tenantId: string; userId: string }) {
  return (
    <NotificationProvider tenantId={tenantId} userId={userId}>
      <NotificationsContent />
    </NotificationProvider>
  );
}