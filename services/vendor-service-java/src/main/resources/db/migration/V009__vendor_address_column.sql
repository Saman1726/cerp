-- V009: Ensure address column exists in vendor.vendors table
-- This migration is a safety net for environments where V008 may not have
-- been applied (e.g., database volume reset, Flyway state inconsistency).

ALTER TABLE vendor.vendors
  ADD COLUMN IF NOT EXISTS address TEXT;