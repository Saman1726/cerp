-- V10: Plan tiers and their resource-quota limits.
--
-- One row per Plan. NULL in any max_* column means "no cap" (unlimited for Professional's
-- uncapped dimensions and all of Premium). Freemium's max_owners_per_project=1 /
-- max_managers_per_project=1 is an EXACT cardinality rule ("exactly 1 owner + 1 manager"),
-- not just a ceiling - enforcing the floor is project-service's job in a later phase, not
-- encoded structurally here.
--
-- No Projects/SubProjects columns: they scale 1:1 with Construction Sites/Buildings
-- (a project-service structural rule), so capping sites/buildings caps them transitively.
-- No price/billing columns: no payment integration exists yet; Phase 1 registration
-- always creates Freemium subscriptions regardless of which plan a caller might request.

CREATE TABLE identity.plan_limits (
    plan                     TEXT        PRIMARY KEY CHECK (plan IN ('FREEMIUM', 'PROFESSIONAL', 'PREMIUM')),
    max_construction_sites   INTEGER,
    max_buildings            INTEGER,
    max_owners_per_project   INTEGER,
    max_managers_per_project INTEGER,
    max_members_per_project  INTEGER,
    starting_bid_grant       BIGINT      NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO identity.plan_limits
    (plan, max_construction_sites, max_buildings, max_owners_per_project, max_managers_per_project, max_members_per_project, starting_bid_grant)
VALUES
    ('FREEMIUM',     1,    1,    1,    1,    NULL, 50),
    ('PROFESSIONAL', 10,   10,   NULL, NULL, 5,    200),
    ('PREMIUM',      NULL, NULL, NULL, NULL, NULL, 400)
ON CONFLICT (plan) DO NOTHING;
