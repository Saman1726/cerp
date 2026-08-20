-- V008: Create material_services and price_table tables
-- Adds Material/Service catalog entities with type distinction and price history tracking

CREATE TABLE IF NOT EXISTS catalog.material_services (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    type TEXT NOT NULL DEFAULT 'MATERIAL' CHECK (type IN ('MATERIAL', 'SERVICE')),
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    unit TEXT NOT NULL CHECK (unit IN ('Kg', 'qm', 'unit', 'ton', 'Liter', 'Meter', 'Piece', 'Box', 'Pack', 'Roll', 'Sheet', 'Pair', 'Set', 'Bundle', 'Pallet', 'Other')),
    document_url TEXT,
    category_group_ids UUID[] DEFAULT '{}',
    category_ids UUID[] DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Indexes for material_services
CREATE INDEX IF NOT EXISTS idx_material_services_tenant_id ON catalog.material_services (tenant_id);
CREATE INDEX IF NOT EXISTS idx_material_services_code ON catalog.material_services (code);
CREATE INDEX IF NOT EXISTS idx_material_services_type ON catalog.material_services (type);
CREATE INDEX IF NOT EXISTS idx_material_services_category_group_ids ON catalog.material_services USING GIN (category_group_ids);
CREATE INDEX IF NOT EXISTS idx_material_services_category_ids ON catalog.material_services USING GIN (category_ids);

-- Price table for material/service pricing history
CREATE TABLE IF NOT EXISTS catalog.price_table (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    material_service_id UUID NOT NULL REFERENCES catalog.material_services (id) ON DELETE CASCADE,
    currency CHAR(3) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_to TIMESTAMPTZ,
    last_price_id UUID REFERENCES catalog.price_table (id),
    next_price_id UUID REFERENCES catalog.price_table (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Indexes for price_table
CREATE INDEX IF NOT EXISTS idx_price_table_material_service_id ON catalog.price_table (material_service_id);
CREATE INDEX IF NOT EXISTS idx_price_table_valid_from ON catalog.price_table (material_service_id, valid_from DESC);
CREATE INDEX IF NOT EXISTS idx_price_table_last_price_id ON catalog.price_table (last_price_id);
CREATE INDEX IF NOT EXISTS idx_price_table_next_price_id ON catalog.price_table (next_price_id);
CREATE INDEX IF NOT EXISTS idx_price_table_tenant_id ON catalog.price_table (tenant_id);