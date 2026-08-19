-- V004: Predefined construction categories for vendors
-- Creates a category management system with predefined construction-related categories.
-- Only admins can manage categories; vendors select from predefined options.

-- Enable pg_trgm extension for trigram-based text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Category groups for organizing categories
CREATE TABLE IF NOT EXISTS vendor.category_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    name TEXT NOT NULL,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_category_groups_name_active ON vendor.category_groups (name, is_active)
WHERE
    is_active = TRUE;

-- Predefined construction categories
CREATE TABLE IF NOT EXISTS vendor.category_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    group_id UUID REFERENCES vendor.category_groups (id) ON DELETE SET NULL,
    code TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    icon TEXT, -- lucide-react icon name
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ,
    UNIQUE (code, is_active)
);

CREATE INDEX IF NOT EXISTS idx_category_definitions_group_id ON vendor.category_definitions (group_id);

CREATE INDEX IF NOT EXISTS idx_category_definitions_code_active ON vendor.category_definitions (code, is_active)
WHERE
    is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_category_definitions_name ON vendor.category_definitions USING gin (name gin_trgm_ops)
WHERE
    is_active = TRUE;

-- Vendor-category association with validation
-- Use a constraint table approach: only categories that exist in category_definitions can be used
CREATE TABLE IF NOT EXISTS vendor.vendor_categories (
    vendor_id UUID NOT NULL REFERENCES vendor.vendors (id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES vendor.category_definitions (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (vendor_id, category_id)
);

-- Seed: Category groups
INSERT INTO vendor.category_groups (id, name, description, display_order) VALUES
    ('a0000000-0000-0000-0000-000000000001'::uuid, 'Trade & Services', 'Core construction trades and services', 1),
    ('a0000000-0000-0000-0000-000000000002'::uuid, 'Materials & Supplies', 'Construction materials and supplies', 2),
    ('a0000000-0000-0000-0000-000000000003'::uuid, 'Equipment & Technology', 'Equipment, tools, and technology solutions', 3),
    ('a0000000-0000-0000-0000-000000000004'::uuid, 'Specialized Services', 'Specialized construction-related services', 4)
ON CONFLICT DO NOTHING;

-- Seed: Category definitions - Trade & Services
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'foundation_structural', 'Foundation & Structural Work', 'Concrete foundations, footings, structural framing, and load-bearing elements', 'Anchor', 1),
    ('b0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'electrical', 'Electrical Systems', 'Wiring, panels, lighting, power distribution, and electrical installations', 'Zap', 2),
    ('b0000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'plumbing_hvac', 'Plumbing & HVAC', 'Pipe fitting, drainage, ventilation, heating, and climate control systems', 'ThermometerSun', 3),
    ('b0000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'carpentry_framing', 'Carpentry & Framing', 'Wood framing, formwork, interior carpentry, and structural woodwork', 'Hammer', 4),
    ('b0000000-0000-0000-0000-000000000005'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'masonry_concrete', 'Masonry & Concrete', 'Brickwork, block masonry, poured concrete, and stone work', 'BrickWall', 5),
    ('b0000000-0000-0000-0000-000000000006'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'roofing_waterproofing', 'Roofing & Waterproofing', 'Roof installation, membrane systems, waterproofing, and damp-proofing', 'Home', 6),
    ('b0000000-0000-0000-0000-000000000007'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'painting_finishing', 'Painting & Finishing', 'Interior/exterior painting, coatings, staining, and surface preparation', 'Palette', 7),
    ('b0000000-0000-0000-0000-000000000008'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'glazing_windows', 'Glazing & Windows', 'Glass installation, window replacement, curtain walls, and storefronts', 'Maximize', 8),
    ('b0000000-0000-0000-0000-000000000009'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'metal_works_steel', 'Metal Works & Steel', 'Steel fabrication, structural steel erection, and metal decking', 'Wrench', 9),
    ('b0000000-0000-0000-0000-000000000010'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'flooring_tiling', 'Flooring & Tiling', 'Hardwood, tile, carpet, vinyl, epoxy, and polished concrete floors', 'Grid3x3', 10),
    ('b0000000-0000-0000-0000-000000000011'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'fire_protection', 'Fire Protection Systems', 'Sprinkler systems, fireproofing, firestops, and fire detection', 'AlertTriangle', 11),
    ('b0000000-0000-0000-0000-000000000012'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'insulation_energy', 'Insulation & Energy Efficiency', 'Thermal insulation, acoustic panels, energy-efficient systems', 'Wind', 12)
ON CONFLICT (code, is_active) DO NOTHING;

-- Seed: Category definitions - Materials & Supplies
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0000-0000-0000-000000000013'::uuid, 'a0000000-0000-0000-0000-000000000002'::uuid, 'lumber_building_materials', 'Lumber & Building Materials', 'Lumber, plywood, drywall, dry fixtures, and general building supplies', 'Package', 1),
    ('b0000000-0000-0000-0000-000000000014'::uuid, 'a0000000-0000-0000-0000-000000000002'::uuid, 'doors_windows_supplies', 'Doors & Windows (Supplies)', 'Manufacturing and supply of doors, windows, and related hardware', 'DoorOpen', 2),
    ('b0000000-0000-0000-0000-000000000015'::uuid, 'a0000000-0000-0000-0000-000000000002'::uuid, 'safety_equipment_supplies', 'Safety Equipment & Supplies', 'PPE, safety barriers, signage, and site safety materials', 'ShieldCheck', 3)
ON CONFLICT (code, is_active) DO NOTHING;

-- Seed: Category definitions - Equipment & Technology
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0000-0000-0000-000000000016'::uuid, 'a0000000-0000-0000-0000-000000000003'::uuid, 'construction_equipment_rental', 'Construction Equipment & Rentals', 'Crane, excavator, scaffolding, and heavy equipment rental services', 'Tool', 1),
    ('b0000000-0000-0000-0000-000000000017'::uuid, 'a0000000-0000-0000-0000-000000000003'::uuid, 'construction_tools', 'Construction Tools & Hand Tools', 'Power tools, hand tools, measuring instruments, and tool accessories', 'Screwdriver', 2),
    ('b0000000-0000-0000-0000-000000000018'::uuid, 'a0000000-0000-0000-0000-000000000003'::uuid, 'smart_building_systems', 'Smart Building Systems', 'BMS, IoT sensors, automation, and smart building technology', 'Cpu', 3),
    ('b0000000-0000-0000-0000-000000000019'::uuid, 'a0000000-0000-0000-0000-000000000003'::uuid, 'building_management_software', 'Construction Software & BIM', 'Project management, BIM, estimation, and construction software solutions', 'Monitor', 4)
ON CONFLICT (code, is_active) DO NOTHING;

-- Seed: Category definitions - Specialized Services
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0000-0000-0000-000000000020'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid, 'site_prep_excavation', 'Site Preparation & Excavation', 'Land clearing, grading, excavation, earthmoving, and demolition', 'Shovel', 1),
    ('b0000000-0000-0000-0000-000000000021'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid, 'landscaping_exterior', 'Landscaping & Exterior', 'Landscaping, hardscaping, outdoor lighting, and irrigation', 'TreePine', 2),
    ('b0000000-0000-0000-0000-000000000022'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid, 'sustainability_green_building', 'Sustainable & Green Building', 'LEED consulting, green materials, renewable energy, and sustainable practices', 'Leaf', 3),
    ('b0000000-0000-0000-0000-000000000023'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid, 'structural_engineering', 'Structural Engineering Services', 'Structural analysis, design, inspection, and engineering consultation', 'Compass', 4),
    ('b0000000-0000-0000-0000-000000000024'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid, 'architectural_services', 'Architectural Services', 'Design, planning, blueprint creation, and architectural consultation', 'PenLine', 5),
    ('b0000000-0000-0000-0000-000000000025'::uuid, 'a0000000-0000-0000-0000-000000000004'::uuid, 'logistics_cranes', 'Logistics & Crane Services', 'Heavy lifting, transportation, rigging, and logistics coordination', 'Truck', 6)
ON CONFLICT (code, is_active) DO NOTHING;

-- View: Get all active categories with their groups
CREATE OR REPLACE VIEW vendor.active_categories_view AS
SELECT
    cd.id,
    cd.code,
    cd.name,
    cd.description,
    cd.icon,
    cd.display_order,
    cg.id AS group_id,
    cg.name AS group_name
FROM vendor.category_definitions cd
    LEFT JOIN vendor.category_groups cg ON cd.group_id = cg.id
WHERE
    cd.is_active = TRUE
ORDER BY COALESCE(
        (
            SELECT display_order
            FROM vendor.category_groups
            WHERE
                id = cd.group_id
        ), 999
    ), cd.display_order;

-- Function to get categories by group
CREATE OR REPLACE FUNCTION vendor.get_categories_by_group(p_group_id UUID)
RETURNS TABLE (
    id UUID,
    code TEXT,
    name TEXT,
    description TEXT,
    icon TEXT,
    display_order INT
) AS $$
BEGIN
    RETURN QUERY
    SELECT cd.id, cd.code, cd.name, cd.description, cd.icon, cd.display_order
    FROM vendor.category_definitions cd
    WHERE cd.group_id = p_group_id AND cd.is_active = TRUE
    ORDER BY cd.display_order;
END;
$$ LANGUAGE plpgsql;

-- Function to get all active categories
CREATE OR REPLACE FUNCTION vendor.get_all_categories()
RETURNS TABLE (
    id UUID,
    code TEXT,
    name TEXT,
    description TEXT,
    icon TEXT,
    display_order INT,
    group_id UUID,
    group_name TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT cd.id, cd.code, cd.name, cd.description, cd.icon, cd.display_order, cd.group_id, cg.name
    FROM vendor.category_definitions cd
    LEFT JOIN vendor.category_groups cg ON cd.group_id = cg.id
    WHERE cd.is_active = TRUE
    ORDER BY COALESCE((SELECT display_order FROM vendor.category_groups WHERE id = cd.group_id), 999), cd.display_order;
END;
$$ LANGUAGE plpgsql;