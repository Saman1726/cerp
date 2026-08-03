/**
 * Notification priority levels
 */
export enum NotificationPriority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT'
}

/**
 * Notification job action types
 */
export enum NotificationActionType {
  REVIEW = 'REVIEW',
  APPROVE = 'APPROVE',
  REJECT = 'REJECT',
  ACKNOWLEDGE = 'ACKNOWLEDGE',
  EXECUTE = 'EXECUTE',
  INPUT = 'INPUT'
}

/**
 * Notification job status
 */
export enum NotificationJobStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  PARTIALLY_COMPLETED = 'PARTIALLY_COMPLETED',
  COMPLETED = 'COMPLETED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED'
}

/**
 * Individual notification status
 */
export enum NotificationStatus {
  PENDING = 'PENDING',
  READ = 'READ',
  ACTION_TAKEN = 'ACTION_TAKEN',
  EXPIRED = 'EXPIRED',
  ARCHIVED = 'ARCHIVED'
}

/**
 * Request to create a notification job
 */
export interface NotificationJobCreateRequest {
  code: string;
  title: string;
  description: string;
  actionType: NotificationActionType;
  payload?: Record<string, unknown>;
  sourceService?: string;
  sourceEntityId?: string; // UUID as string
  sourceEntityType?: string;
  priority?: NotificationPriority;
  recipientUserIds: string[]; // UUIDs as strings
  recipientRoles?: string[];
  dueAt?: string; // ISO 8601 datetime
  reminderIntervalMinutes?: number;
}

/**
 * Response for a notification job
 */
export interface NotificationJobResponse {
  id: string;
  tenantId: string;
  code: string;
  title: string;
  description: string;
  actionType: NotificationActionType;
  payload?: Record<string, unknown>;
  sourceService?: string;
  sourceEntityId?: string;
  sourceEntityType?: string;
  priority: NotificationPriority;
  status: NotificationJobStatus;
  recipientUserIds: string[];
  recipientRoles: string[];
  dueAt?: string;
  reminderIntervalMinutes?: number;
  createdBy?: string;
  createdAt?: string;
  publishedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
}

/**
 * Individual notification response
 */
export interface NotificationResponse {
  id: string;
  code?: string;
  title?: string;
  description?: string;
  actionType?: string;
  status?: string;
  priority?: string;
  jobId: string;
  recipientId: string;
  recipientRole?: string;
  recipientEmail?: string;
  payload?: Record<string, unknown>;
  sourceEntityId?: string;
  sourceEntityType?: string;
  actionResult?: Record<string, unknown>;
  createdAt?: string;
  updatedAt?: string;
  openedAt?: string;
  acknowledgedAt?: string;
  completedAt?: string;
  dueAt?: string;
}

/**
 * Paginated notification list response
 */
export interface NotificationListResponse {
  notifications: NotificationResponse[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

/**
 * Request to acknowledge a notification
 */
export interface NotificationAcknowledgeRequest {
  tenantId?: string;
}

/**
 * Request to take action on a notification
 */
export interface NotificationActionRequest {
  tenantId?: string;
  result: Record<string, unknown>;
}

/**
 * Notification-related events for Kafka
 */
export interface NotificationJobCreatedEvent {
  jobId: string;
  tenantId: string;
  code: string;
  title: string;
  description: string;
  actionType: string;
  priority: string;
  createdBy: string;
  createdAt: string;
}

export interface NotificationJobPublishedEvent {
  jobId: string;
  tenantId: string;
  code: string;
  title: string;
  notificationIds: string[];
  recipientIds: string[];
  publishedAt: string;
}

export interface NotificationOpenedEvent {
  eventId?: string;
  notificationId: string;
  userId: string;
  tenantId: string;
  sourceEntityId?: string;
  sourceEntityType?: string;
  occurredAt: string;
}

export interface NotificationActionTakenEvent {
  eventId?: string;
  notificationId: string;
  userId: string;
  tenantId: string;
  actionType: string;
  result?: string;
  comment?: string;
  sourceEntityId?: string;
  sourceEntityType?: string;
  actionData?: Record<string, unknown>;
  occurredAt: string;
}
