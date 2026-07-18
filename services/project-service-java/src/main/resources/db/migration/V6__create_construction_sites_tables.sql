-- Create construction sites table
CREATE TABLE construction_sites (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    org_id UUID,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(128),
    updated_by VARCHAR(128)
);

-- Create indexes for construction_sites
CREATE INDEX idx_construction_sites_tenant_id ON construction_sites (tenant_id);

CREATE INDEX idx_construction_sites_org_id ON construction_sites (org_id);

-- Create buildings table
CREATE TABLE buildings (
    id UUID PRIMARY KEY,
    construction_site_id UUID NOT NULL REFERENCES construction_sites (id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    usable_area DECIMAL(12, 2),
    total_area DECIMAL(12, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create floors table
CREATE TABLE floors (
    id UUID PRIMARY KEY,
    building_id UUID NOT NULL REFERENCES buildings (id) ON DELETE CASCADE,
    type VARCHAR(128) NOT NULL,
    usable_area DECIMAL(12, 2),
    total_area DECIMAL(12, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create units table
CREATE TABLE units (
    id UUID PRIMARY KEY,
    floor_id UUID NOT NULL REFERENCES floors (id) ON DELETE CASCADE,
    label VARCHAR(128) NOT NULL,
    usable_area DECIMAL(12, 2),
    bedrooms INTEGER,
    bathrooms INTEGER,
    balconies INTEGER,
    toilets INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);