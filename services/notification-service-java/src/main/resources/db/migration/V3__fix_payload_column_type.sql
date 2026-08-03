-- Fix payload column type from JSONB to TEXT for proper String serialization
ALTER TABLE notification.notification_jobs
    ALTER COLUMN payload TYPE TEXT USING payload::text;

ALTER TABLE notification.notifications
    ALTER COLUMN action_result TYPE TEXT USING action_result::text;