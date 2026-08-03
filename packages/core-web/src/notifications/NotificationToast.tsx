'use client';

import React, { useEffect } from 'react';
import { useNotifications } from './NotificationProvider';
import { NotificationResponse, NotificationStatus } from '@cerp/shared-types';

interface ToastNotificationProps {
  notification: NotificationResponse;
  onDismiss: () => void;
  onAcknowledge: () => void;
  onTakeAction: () => void;
}

const ToastNotification: React.FC<ToastNotificationProps> = ({
  notification,
  onDismiss,
  onAcknowledge,
  onTakeAction,
}) => {
  useEffect(() => {
    const timer = setTimeout(onDismiss, 10000);
    return () => clearTimeout(timer);
  }, [onDismiss]);

  return (
    <div className="flex w-80 items-start gap-3 rounded-lg bg-white p-4 shadow-lg border">
      <div
        className={`mt-1 h-3 w-3 rounded-full flex-shrink-0 ${
          notification.status === NotificationStatus.PENDING
            ? 'bg-blue-500 animate-pulse'
            : 'bg-gray-300'
        }`}
      />
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-gray-900">{notification.title}</p>
        <p className="text-sm text-gray-600 mt-0.5 line-clamp-2">
          {notification.description}
        </p>
        <div className="mt-2 flex gap-2">
          <button
            onClick={onAcknowledge}
            className="rounded bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:bg-blue-700"
          >
            Open
          </button>
          <button
            onClick={onTakeAction}
            className="rounded bg-green-600 px-3 py-1 text-xs font-medium text-white hover:bg-green-700"
          >
            Take Action
          </button>
          <button
            onClick={onDismiss}
            className="rounded bg-gray-200 px-3 py-1 text-xs font-medium text-gray-700 hover:bg-gray-300"
          >
            Dismiss
          </button>
        </div>
      </div>
    </div>
  );
};

export const NotificationToast: React.FC = () => {
  const { notifications } = useNotifications();
  const [visible, setVisible] = React.useState<NotificationResponse[]>([]);

  useEffect(() => {
    const newPending = notifications.filter(
      (n) => n.status === NotificationStatus.PENDING
    );
    setVisible(newPending.slice(-3));
  }, [notifications]);

  const dismiss = (id: string) => {
    setVisible((prev) => prev.filter((n) => n.id !== id));
  };

  if (visible.length === 0) {
    return null;
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {visible.map((notification) => (
        <ToastNotification
          key={notification.id}
          notification={notification}
          onDismiss={() => dismiss(notification.id)}
          onAcknowledge={() => dismiss(notification.id)}
          onTakeAction={() => dismiss(notification.id)}
        />
      ))}
    </div>
  );
};