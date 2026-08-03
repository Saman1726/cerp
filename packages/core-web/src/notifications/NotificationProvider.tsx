'use client';

import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import {
  NotificationResponse,
  NotificationListResponse,
  NotificationJobCreateRequest,
  NotificationJobResponse,
  NotificationStatus,
  NotificationJobStatus,
} from '@cerp/shared-types';

// Use relative URLs for notification endpoints so they go through Next.js API routes.
// This avoids CORS issues when running in the browser (API_URL points to Docker hostnames).
const notificationApi = {
  createJob: async (
    tenantId: string,
    request: NotificationJobCreateRequest
  ): Promise<NotificationJobResponse> => {
    const response = await fetch(`/api/notifications/jobs`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-Id': tenantId,
      },
      body: JSON.stringify(request),
    });
    return response.json();
  },

  getJob: async (
    tenantId: string,
    jobId: string
  ): Promise<NotificationJobResponse> => {
    const response = await fetch(
      `/api/notifications/jobs/${jobId}`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },

  listJobs: async (
    tenantId: string,
    status?: NotificationJobStatus,
    page = 0,
    size = 20
  ): Promise<NotificationJobResponse[]> => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
      ...(status && { status: status }),
    });
    const response = await fetch(
      `/api/notifications/jobs?${params}`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },

  publishJob: async (
    tenantId: string,
    jobId: string
  ): Promise<NotificationJobResponse> => {
    const response = await fetch(
      `/api/notifications/jobs/${jobId}/publish`,
      {
        method: 'POST',
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },

  cancelJob: async (
    tenantId: string,
    jobId: string
  ): Promise<NotificationJobResponse> => {
    const response = await fetch(
      `/api/notifications/jobs/${jobId}/cancel`,
      {
        method: 'POST',
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },

  getJobNotifications: async (
    tenantId: string,
    jobId: string
  ): Promise<NotificationResponse[]> => {
    const response = await fetch(
      `/api/notifications/jobs/${jobId}/notifications`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },

  getMyNotifications: async (
    tenantId: string,
    status?: NotificationStatus,
    page = 0,
    size = 20
  ): Promise<NotificationListResponse> => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
      ...(status && { status: status }),
    });
    const response = await fetch(
      `/api/notifications/me?${params}`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },

  getUnreadCount: async (tenantId: string): Promise<number> => {
    const response = await fetch(
      `/api/notifications/me/unread-count`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    const data = await response.json();
    return data.count;
  },

  acknowledgeNotification: async (
    tenantId: string,
    notificationId: string
  ): Promise<NotificationResponse> => {
    const response = await fetch(
      `/api/notifications/me/${notificationId}/ack`,
      {
        method: 'POST',
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },

  takeAction: async (
    tenantId: string,
    notificationId: string,
    result: Record<string, unknown>
  ): Promise<NotificationResponse> => {
    const response = await fetch(
      `/api/notifications/me/${notificationId}/action`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Tenant-Id': tenantId,
        },
        body: JSON.stringify({ result }),
      }
    );
    return response.json();
  },

  getNotification: async (
    tenantId: string,
    notificationId: string
  ): Promise<NotificationResponse> => {
    const response = await fetch(
      `/api/notifications/me/${notificationId}`,
      {
        headers: { 'X-Tenant-Id': tenantId },
      }
    );
    return response.json();
  },
};

export interface NotificationItem extends NotificationResponse {
  unread: boolean;
}

interface NotificationContextValue {
  notifications: NotificationItem[];
  unreadCount: number;
  loading: boolean;
  refresh: () => Promise<void>;
  acknowledge: (notificationId: string) => Promise<void>;
  takeAction: (
    notificationId: string,
    result: Record<string, unknown>
  ) => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | null>(null);

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error(
      'useNotifications must be used within a NotificationProvider'
    );
  }
  return context;
};

interface NotificationProviderProps {
  children: ReactNode;
  tenantId: string;
  userId: string;
  refreshInterval?: number;
}

export const NotificationProvider: React.FC<NotificationProviderProps> = ({
  children,
  tenantId,
  userId,
  refreshInterval = 30000,
}) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const result = await notificationApi.getMyNotifications(tenantId);
      const items: NotificationItem[] = result.notifications.map((n) => ({
        ...n,
        unread: n.status === NotificationStatus.PENDING,
      }));
      setNotifications(items);
      setUnreadCount(items.filter((n) => n.unread).length);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      setLoading(false);
    }
  }, [tenantId, userId]);

  const refresh = useCallback(async () => {
    await fetchNotifications();
  }, [fetchNotifications]);

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, refreshInterval);
    return () => clearInterval(interval);
  }, [fetchNotifications, refreshInterval]);

  const acknowledge = useCallback(
    async (notificationId: string) => {
      await notificationApi.acknowledgeNotification(tenantId, notificationId);
      await fetchNotifications();
    },
    [tenantId, fetchNotifications]
  );

  const takeAction = useCallback(
    async (notificationId: string, result: Record<string, unknown>) => {
      await notificationApi.takeAction(tenantId, notificationId, result);
      await fetchNotifications();
    },
    [tenantId, fetchNotifications]
  );

  const value: NotificationContextValue = {
    notifications,
    unreadCount,
    loading,
    refresh,
    acknowledge,
    takeAction,
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
};