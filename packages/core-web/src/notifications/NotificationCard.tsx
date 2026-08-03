'use client';

import React from 'react';
import { useNotifications } from './NotificationProvider';
import {
  NotificationResponse,
  NotificationStatus,
  NotificationActionType,
} from '@cerp/shared-types';

interface NotificationCardProps {
  notification: NotificationResponse;
}

export const NotificationCard: React.FC<NotificationCardProps> = ({
  notification,
}) => {
  const { acknowledge, takeAction } = useNotifications();
  const [showActionForm, setShowActionForm] = React.useState(false);

  const handleAcknowledge = async () => {
    if (notification.status === NotificationStatus.PENDING) {
      await acknowledge(notification.id);
    }
  };

  const handleTakeAction = async (result: Record<string, unknown>) => {
    await takeAction(notification.id, result);
    setShowActionForm(false);
  };

  const statusColor =
    notification.status === NotificationStatus.PENDING
      ? 'bg-yellow-100 text-yellow-800'
      : notification.status === NotificationStatus.READ
      ? 'bg-gray-100 text-gray-800'
      : notification.status === NotificationStatus.ACTION_TAKEN
      ? 'bg-green-100 text-green-800'
      : 'bg-gray-100 text-gray-600';

  const borderColor =
    notification.status === NotificationStatus.PENDING
      ? 'border-l-blue-500'
      : notification.status === NotificationStatus.ACTION_TAKEN
      ? 'border-l-green-500'
      : 'border-l-gray-300';

  return (
    <div
      className={`rounded-r-lg border-l-4 ${borderColor} bg-white p-4 shadow-sm hover:shadow-md transition-shadow`}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h4 className="font-semibold text-gray-900 truncate">
              {notification.title}
            </h4>
            <span className={`rounded px-2 py-0.5 text-xs font-medium ${statusColor}`}>
              {notification.status}
            </span>
            <span className="rounded px-2 py-0.5 text-xs font-medium bg-blue-100 text-blue-800">
              {notification.actionType}
            </span>
          </div>
          <p className="mt-1 text-sm text-gray-600 truncate">
            {notification.description}
          </p>
          {notification.actionResult && (
            <div className="mt-2 rounded bg-gray-50 p-2">
              <p className="text-xs font-medium text-gray-500">Action Result:</p>
              <pre className="text-xs text-gray-700 mt-1 overflow-x-auto">
                {JSON.stringify(notification.actionResult, null, 2)}
              </pre>
            </div>
          )}
        </div>
        <button
          onClick={handleAcknowledge}
          className={`ml-4 flex-shrink-0 rounded px-3 py-1.5 text-sm font-medium ${
            notification.status === NotificationStatus.PENDING
              ? 'bg-blue-600 text-white hover:bg-blue-700'
              : 'bg-gray-200 text-gray-700'
          }`}
          disabled={notification.status !== NotificationStatus.PENDING}
        >
          {notification.status === NotificationStatus.PENDING ? 'Open' : 'Read'}
        </button>
      </div>

      <div className="mt-3 flex items-center gap-4 text-xs text-gray-500">
        <span>
          Due:{' '}
          {notification.dueAt
            ? new Date(notification.dueAt).toLocaleDateString()
            : 'N/A'}
        </span>
        <span>
          Created:{' '}
          {notification.createdAt
            ? new Date(notification.createdAt).toLocaleDateString()
            : 'N/A'}
        </span>
      </div>

      {notification.status === NotificationStatus.PENDING &&
        notification.actionType === NotificationActionType.INPUT && (
          <div className="mt-3">
            <button
              onClick={() => setShowActionForm(!showActionForm)}
              className="rounded bg-green-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-green-700"
            >
              Provide Input
            </button>
            {showActionForm && (
              <div className="mt-2 rounded border p-3 bg-gray-50">
                <form
                  onSubmit={(e: React.FormEvent<HTMLFormElement>) => {
                    e.preventDefault();
                    const formData = new FormData(e.currentTarget);
                    const result: Record<string, unknown> = {};
                    for (const key of formData.keys()) {
                      result[key] = formData.get(key);
                    }
                    handleTakeAction(result);
                  }}
                  className="flex gap-2"
                >
                  <input
                    name="value"
                    type="text"
                    placeholder="Enter your response..."
                    className="flex-1 rounded border px-3 py-1.5 text-sm"
                    autoFocus
                  />
                  <button
                    type="submit"
                    className="rounded bg-green-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-green-700"
                  >
                    Submit
                  </button>
                </form>
              </div>
            )}
          </div>
        )}
    </div>
  );
};