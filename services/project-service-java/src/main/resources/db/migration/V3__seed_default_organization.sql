-- V3: Seed default Standard Organization for development/testing
-- This provides a default organization ID that can be used when no specific
-- organization is selected during project creation.

-- Use INSERT ... WHERE NOT EXISTS to avoid conflicts without requiring a unique index
INSERT INTO
    organizations (
        id,
        tenant_id,
        external_id,
        name,
        legal_form,
        address_line_1,
        city,
        country_code,
        status,
        currency_code,
        sync_status,
        created_at,
        updated_at
    )
SELECT '00000000-0000-0000-0000-000000000001', 'default', 'STD-ORG-001', 'Standard Organization', 'GmbH', '123 Main Street', 'Berlin', 'DE', 'ACTIVE', 'EUR', 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE
    NOT EXISTS (
        SELECT 1
        FROM organizations
        WHERE
            external_id = 'STD-ORG-001'
    );

COMMENT ON
TABLE organizations IS 'Business organization profiles with seeded default for development';