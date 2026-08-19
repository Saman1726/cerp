-- V005: Update vendor taxonomy to 8 category groups (~100 subcategories)
-- Source: construction_vendor_taxonomy.pdf
-- This migration deletes old seed data and re-seeds with the new 8-group taxonomy
-- Uses ON CONFLICT DO UPDATE for safe, idempotent execution

-- =====================================================================
-- STEP 1: Delete old seed data (V004 data with old UUIDs)
-- =====================================================================
-- Delete categories with old UUIDs (b0000000-...)
DELETE FROM vendor.category_definitions WHERE id::text LIKE 'b0000000-%';
-- Delete groups with old UUIDs (a0000000-0000-...)
DELETE FROM vendor.category_groups WHERE id::text LIKE 'a0000000-0000-%';

-- =====================================================================
-- STEP 2: Insert 8 Category Groups (deterministic UUIDs, idempotent)
-- =====================================================================
INSERT INTO vendor.category_groups (id, name, description, display_order) VALUES
    ('a0000000-0005-0000-0000-000000000001'::uuid, 'Construction Trades',
     'Site preparation, structural works, finishes, and external trades', 1),
    ('a0000000-0005-0000-0000-000000000002'::uuid, 'MEP / Building Services',
     'Mechanical, electrical, plumbing, and building automation systems', 2),
    ('a0000000-0005-0000-0000-000000000003'::uuid, 'Civil & Infrastructure',
     'Roads, bridges, tunnels, utilities, drainage, and rail infrastructure', 3),
    ('a0000000-0005-0000-0000-000000000004'::uuid, 'Materials & Products',
     'Raw materials, construction products, coatings, and fasteners', 4),
    ('a0000000-0005-0000-0000-000000000005'::uuid, 'Machinery & Equipment',
     'Construction machinery, tools, equipment rental, and site plant', 5),
    ('a0000000-0005-0000-0000-000000000006'::uuid, 'Professional & Engineering Services',
     'Architecture, engineering, surveying, project management, and certification', 6),
    ('a0000000-0005-0000-0000-000000000007'::uuid, 'Specialized Systems',
     'Elevators, security, fire alarm, BMS, solar, EV charging, and renewable energy', 7),
    ('a0000000-0005-0000-0000-000000000008'::uuid, 'Site & Temporary Services',
     'Site containers, temporary services, security, waste management, and logistics', 8)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name,
                              description = EXCLUDED.description,
                              display_order = EXCLUDED.display_order;

-- =====================================================================
-- STEP 3: Insert Subcategories — Group 1: Construction Trades (19)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0001-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'site_prep_earthworks', 'Site Preparation & Earthworks', 'Land clearing, grading, excavation, earthmoving, and soil stabilization', 'Shovel', 1),
    ('b0000000-0005-0001-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'demolition', 'Demolition', 'Building demolition, structural deconstruction, rubble removal, and site clearance', 'Pickaxe', 2),
    ('b0000000-0005-0001-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'foundations', 'Foundations', 'Pile foundations, strip footings, raft foundations, and ground beams', 'Anchor', 3),
    ('b0000000-0005-0001-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'concrete_reinforced', 'Concrete & Reinforced Concrete', 'Concrete pouring, reinforcement, precast elements, formwork, and finishing', 'Cube', 4),
    ('b0000000-0005-0001-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'masonry', 'Masonry', 'Bricklaying, blockwork, stone masonry, pointing, and repointing', 'Layers', 5),
    ('b0000000-0005-0001-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'structural_steel', 'Structural Steel', 'Steel framing, columns, beams, trusses, and structural metalwork fabrication', 'Cpu', 6),
    ('b0000000-0005-0001-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'timber_structures', 'Timber Structures', 'Timber framing, roof trusses, CLT panels, and timber frame construction', 'TreeDeciduous', 7),
    ('b0000000-0005-0001-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'roofing', 'Roofing', 'Roof construction, tiles, slates, metal roofing, felt, and roof insulation', 'Home', 8),
    ('b0000000-0005-0001-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'waterproofing', 'Waterproofing', 'Basement tanking, roof membranes, liquid membranes, and damp proof courses', 'Droplet', 9),
    ('b0000000-0005-0001-0001-000000000010'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'facade', 'Facade', 'Curtain walls, rainscreen cladding, EIFS, ventilated facades, and facade engineering', 'Building2', 10),
    ('b0000000-0005-0001-0001-000000000011'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'windows_glazing', 'Windows & Glazing', 'uPVC windows, aluminium systems, structural glazing, skylights, and storefronts', 'Window', 11),
    ('b0000000-0005-0001-0001-000000000012'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'doors_gates', 'Doors & Gates', 'Entrance doors, fire doors, sectional doors, gates, and automated door systems', 'DoorOpen', 12),
    ('b0000000-0005-0001-0001-000000000013'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'drywall_partitions', 'Drywall & Partitions', 'Plasterboard, stud walls, acoustic partitions, ceiling grids, and suspended ceilings', 'Panel', 13),
    ('b0000000-0005-0001-0001-000000000014'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'ceilings', 'Ceilings', 'Suspended ceilings, acoustic tiles, coving, cornices, and decorative ceilings', 'Square', 14),
    ('b0000000-0005-0001-0001-000000000015'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'screed', 'Screed', 'Floor screeding, liquid screed, underfloor heating preparation, and leveling', 'Minus', 15),
    ('b0000000-0005-0001-0001-000000000016'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'flooring', 'Flooring', 'Wood flooring, vinyl, carpet, polished concrete, epoxy, and resin floors', 'Grid', 16),
    ('b0000000-0005-0001-0001-000000000017'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'tiling', 'Tiling', 'Wall tiling, floor tiling, terrazzo, mosaic, and ceramic/porcelain installation', 'SquareSplit', 17),
    ('b0000000-0005-0001-0001-000000000018'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'painting_decorating', 'Painting & Decorating', 'Internal/external painting, wallpapering, staining, decorative finishes, and spray painting', 'PaintBrush', 18),
    ('b0000000-0005-0001-0001-000000000019'::uuid, 'a0000000-0005-0000-0000-000000000001'::uuid, 'landscaping', 'Landscaping', 'Hard and soft landscaping, paving, fencing, decks, and garden construction', 'TreePine', 19)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;
-- =====================================================================
-- STEP 4: Insert Subcategories — Group 2: MEP / Building Services (13)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0002-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_electrical', 'Electrical', 'Power distribution, wiring, switchgear, LV panels, and electrical installations', 'Zap', 1),
    ('b0000000-0005-0002-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_lighting', 'Lighting', 'Interior/exterior lighting design, LED systems, emergency lighting, and controls', 'Lightbulb', 2),
    ('b0000000-0005-0002-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_plumbing', 'Plumbing', 'Water supply, drainage, soil stacks, pipework, and plumbing fixtures', 'Droplet', 3),
    ('b0000000-0005-0002-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_sanitary', 'Sanitary', 'Sanitary ware, waste systems, ventilation stacks, and bathroom installations', 'Sink', 4),
    ('b0000000-0005-0002-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_heating', 'Heating', 'Boilers, radiators, underfloor heating, heat pumps, and heating distribution', 'Flame', 5),
    ('b0000000-0005-0002-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_cooling', 'Cooling / Air Conditioning', 'HVAC systems, split units, VRF/VRV, chillers, and air handling units', 'Wind', 6),
    ('b0000000-0005-0002-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_ventilation', 'Ventilation', 'Mechanical ventilation, extract fans, ductwork, and air quality systems', 'Fan', 7),
    ('b0000000-0005-0002-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_refrigeration', 'Refrigeration', 'Cold rooms, commercial refrigeration, cooling systems, and refrigerant handling', 'Snowflake', 8),
    ('b0000000-0005-0002-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_fire_protection', 'Fire Protection', 'Fire sprinklers, fire suppression, hose reels, and fire prevention systems', 'FireExtinguisher', 9),
    ('b0000000-0005-0002-0001-000000000010'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_building_automation', 'Building Automation', 'BMS controls, energy management, building monitoring, and automation integration', 'Settings', 10),
    ('b0000000-0005-0002-0001-000000000011'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_smart_building', 'Smart Building', 'IoT sensors, smart meters, occupancy detection, and building intelligence', 'Cpu', 11),
    ('b0000000-0005-0002-0001-000000000012'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_solar_pv', 'Solar / PV', 'Photovoltaic panels, solar thermal, inverters, and solar energy systems', 'Sun', 12),
    ('b0000000-0005-0002-0001-000000000013'::uuid, 'a0000000-0005-0000-0000-000000000002'::uuid, 'mep_energy_systems', 'Energy Systems', 'Energy storage, battery systems, microgrids, and energy efficiency solutions', 'Battery', 13)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;
-- =====================================================================
-- STEP 5: Insert Subcategories — Group 3: Civil & Infrastructure (9)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0003-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_roads', 'Roads', 'Road construction, resurfacing, road markings, traffic infrastructure, and highways', 'Road', 1),
    ('b0000000-0005-0003-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_bridges', 'Bridges', 'Bridge construction, decks, piers, abutments, and bridge rehabilitation', 'CornerDownLeft', 2),
    ('b0000000-0005-0003-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_tunnels', 'Tunnels', 'Tunnel boring, cut-and-cover, mining, and underground construction', 'Minimize', 3),
    ('b0000000-0005-0003-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_utilities', 'Utilities', 'Underground utilities, service connections, duct banks, and utility corridors', 'CircuitBoard', 4),
    ('b0000000-0005-0003-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_water_sewer', 'Water & Sewer', 'Water supply networks, sewerage systems, pumping stations, and treatment works', 'Droplet', 5),
    ('b0000000-0005-0003-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_drainage', 'Drainage', 'Surface water drainage, SUDS, culverts, gullies, and drainage systems', 'Droplets', 6),
    ('b0000000-0005-0003-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_rail', 'Rail', 'Railway construction, track laying, signaling, stations, and rail infrastructure', 'TrainFront', 7),
    ('b0000000-0005-0003-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_traffic', 'Traffic Systems', 'Traffic signals, signage, speed cameras, ITS, and traffic management', 'TrafficCone', 8),
    ('b0000000-0005-0003-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000003'::uuid, 'civil_infrastructure_construction', 'Infrastructure Construction', 'General civil infrastructure, earthworks, retaining walls, and embankments', 'Mountain', 9)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;
-- =====================================================================
-- STEP 6: Insert Subcategories — Group 4: Materials & Products (15)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0004-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'cement_concrete', 'Cement & Concrete Products', 'Portland cement, ready-mix concrete, concrete admixtures, and precast concrete elements', 'Cube', 1),
    ('b0000000-0005-0004-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'steel_rebar', 'Steel & Rebar', 'Structural steel, rebar, mesh, wire, channels, angles, beams, and structural sections', 'Cpu', 2),
    ('b0000000-0005-0004-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'timber_wood', 'Timber & Wood Products', 'Structural timber, plywood, MDF, chipboard, engineered wood, and wood treatment', 'TreeDeciduous', 3),
    ('b0000000-0005-0004-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'bricks_blocks', 'Bricks & Blocks', 'Clay bricks, concrete blocks, aerated concrete blocks, pavers, and masonry units', 'Layers', 4),
    ('b0000000-0005-0004-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'aggregates', 'Aggregates & Fill', 'Sand, gravel, crushed stone, recycled aggregates, and backfill materials', 'Mountain', 5),
    ('b0000000-0005-0004-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'insulation', 'Insulation', 'Thermal insulation, acoustic insulation, mineral wool, PIR/PUR boards, and spray foam', 'Thermometer', 6),
    ('b0000000-0005-0004-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'waterproofing_materials', 'Waterproofing & Damp Proofing', 'Membranes, liquid membranes, tanking slurry, damp proof course, and sealants', 'Droplet', 7),
    ('b0000000-0005-0004-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'paints_coatings', 'Paints & Coatings', 'Interior/exterior paints, primers, sealants, epoxy coatings, and protective finishes', 'PaintBrush', 8),
    ('b0000000-0005-0004-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'roofing_materials', 'Roofing Materials', 'Roof tiles, slates, felt, metal roofing, insulation boards, flashings, and gutters', 'Home', 9),
    ('b0000000-0005-0004-0001-000000000010'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'glazing_glass', 'Glazing & Glass', 'Float glass, toughened glass, laminated glass, double/triple glazing units', 'Window', 10),
    ('b0000000-0005-0004-0001-000000000011'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'fasteners_fixings', 'Fasteners & Fixings', 'Screws, bolts, anchors, rivets, wall plugs, and construction adhesives', 'Wrench', 11),
    ('b0000000-0005-0004-0001-000000000012'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'plumbing_fittings', 'Plumbing & Pipe Fittings', 'Pipes, fittings, valves, taps, waste traps, and plumbing accessories', 'Droplet', 12),
    ('b0000000-0005-0004-0001-000000000013'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'electrical_materials', 'Electrical Materials', 'Cables, conduits, junction boxes, switchgear, circuit breakers, and accessories', 'Zap', 13),
    ('b0000000-0005-0004-0001-000000000014'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'adhesives_sealants', 'Adhesives & Sealants', 'Construction adhesives, silicones, polyurethanes, and sealing compounds', 'Droplet', 14),
    ('b0000000-0005-0004-0001-000000000015'::uuid, 'a0000000-0005-0000-0000-000000000004'::uuid, 'gypsum_plaster', 'Gypsum & Plaster Products', 'Gypsum boards, plaster, joint compounds, and plasterboard accessories', 'Square', 15)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;
-- =====================================================================
-- STEP 7: Insert Subcategories — Group 5: Machinery & Equipment (11)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0005-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'excavators', 'Excavators', 'Hydraulic excavators, mini excavators, long-reach excavators, and crawler excavators', 'Pickaxe', 1),
    ('b0000000-0005-0005-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'bulldozers_loaders', 'Bulldozers & Loaders', 'Bulldozers, wheel loaders, backhoe loaders, and skid steer loaders', 'Square', 2),
    ('b0000000-0005-0005-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'dumpers', 'Dumpers & Tipper Trucks', 'Articulated dumpers, rigid dumpers, tipper trucks, and off-highway trucks', 'Truck', 3),
    ('b0000000-0005-0005-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'cranes', 'Cranes & Lifting', 'Tower cranes, mobile cranes, truck-mounted cranes, and overhead cranes', 'ArrowUpNarrowWide', 4),
    ('b0000000-0005-0005-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'concrete_equipment', 'Concrete Equipment', 'Concrete pumps, mixers, batching plants, vibrators, and finishing equipment', 'Cube', 5),
    ('b0000000-0005-0005-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'compaction', 'Compaction Equipment', 'Rollers, plate compactors, rammers, piling rigs, and soil compactors', 'Disc', 6),
    ('b0000000-0005-0005-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'generators_compressors', 'Generators & Compressors', 'Diesel generators, air compressors, welders, and power tools', 'Zap', 7),
    ('b0000000-0005-0005-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'scaffolding', 'Scaffolding & Access', 'Tube-and-fit scaffolding, modular systems, mobile towers, scissor lifts, and MEWPs', 'PanelTop', 8),
    ('b0000000-0005-0005-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'tools_hand', 'Hand & Power Tools', 'Power tools, hand tools, measuring instruments, and small equipment', 'Hammer', 9),
    ('b0000000-0005-0005-0001-000000000010'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'site_plant', 'Site Plant & Temp Equipment', 'Lighting towers, temporary fencing, site security systems, and temporary utilities', 'TriangleAlert', 10),
    ('b0000000-0005-0005-0001-000000000011'::uuid, 'a0000000-0005-0000-0000-000000000005'::uuid, 'meiwps_lifts', 'MEWPs & Access Platforms', 'Mobile elevating work platforms, boom lifts, cherry pickers, and scissor lifts', 'ArrowUpNarrowWide', 11)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;
-- =====================================================================
-- STEP 8: Insert Subcategories — Group 6: Professional & Engineering Services (13)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0006-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'architectural_services', 'Architectural Services', 'Design, planning, blueprint creation, and architectural consultation', 'PenLine', 1),
    ('b0000000-0005-0006-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'structural_engineering', 'Structural Engineering', 'Structural analysis, design, inspection, and engineering consultation', 'Compass', 2),
    ('b0000000-0005-0006-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'geotechnical_services', 'Geotechnical Engineering', 'Soil investigation, ground stabilization, slope analysis, and foundation design', 'Mountain', 3),
    ('b0000000-0005-0006-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'construction_management', 'Construction Management', 'Project management, site supervision, and construction administration', 'Briefcase', 4),
    ('b0000000-0005-0006-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'quantity_surveying', 'Quantity Surveying', 'Cost estimation, measurement, procurement, and contract administration', 'Calculator', 5),
    ('b0000000-0005-0006-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'surveying_mapping', 'Surveying & Mapping', 'Land surveying, topographic surveys, setting out, and GIS mapping', 'Compass', 6),
    ('b0000000-0005-0006-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'environmental_services', 'Environmental Services', 'Environmental impact assessment, EIA, sustainability consulting, and monitoring', 'Leaf', 7),
    ('b0000000-0005-0006-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'building_inspection', 'Building Inspection & Testing', 'Structural inspections, non-destructive testing, and building surveys', 'Search', 8),
    ('b0000000-0005-0006-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'safety_services', 'Health & Safety Services', 'Safety consulting, risk assessments, CDM coordination, and HSE compliance', 'ShieldAlert', 9),
    ('b0000000-0005-0006-0001-000000000010'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'legal_services', 'Legal & Planning Services', 'Planning applications, construction law, disputes, and contract advice', 'Scale', 10),
    ('b0000000-0005-0006-0001-000000000011'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'acoustic_services', 'Acoustic Consulting', 'Noise assessment, sound insulation, and acoustic design', 'AudioWaveform', 11),
    ('b0000000-0005-0006-0001-000000000012'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'lighting_design', 'Lighting Design', 'Architectural lighting, daylight studies, and illuminated escape routes', 'Lightbulb', 12),
    ('b0000000-0005-0006-0001-000000000013'::uuid, 'a0000000-0005-0000-0000-000000000006'::uuid, 'energy_assessment', 'Energy Assessment & Certification', 'EPCs, SBEM, SAP assessments, and energy performance consulting', 'Battery', 13)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;
-- =====================================================================
-- STEP 9: Insert Subcategories — Group 7: Specialized Systems (10)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0007-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'elevators_lifts', 'Elevators & Lifts', 'Passenger elevators, freight lifts, hoists, and lift maintenance', 'ArrowUpDown', 1),
    ('b0000000-0005-0007-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'escalators', 'Escalators & Moving Walks', 'Escalators, moving walkways, and automated passenger conveyors', 'ArrowRightLeft', 2),
    ('b0000000-0005-0007-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'security_systems', 'Security Systems', 'CCTV, access control, intruder alarms, and security integration', 'Shield', 3),
    ('b0000000-0005-0007-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'fire_alarm', 'Fire Alarm Systems', 'Fire detection, alarm panels, call points, and fire alarm maintenance', 'Bell', 4),
    ('b0000000-0005-0007-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'data_communications', 'Data & Communications', 'Structured cabling, network infrastructure, fiber optics, and IT systems', 'Cable', 5),
    ('b0000000-0005-0007-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'av_systems', 'Audio Visual Systems', 'AV installation, conferencing, public address, and display systems', 'Speaker', 6),
    ('b0000000-0005-0007-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'ev_charging', 'EV Charging Infrastructure', 'EV charge points, cables, installation, and smart charging systems', 'Zap', 7),
    ('b0000000-0005-0007-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'renewable_energy', 'Renewable Energy Systems', 'Heat pumps, solar thermal, biomass, and renewable energy installation', 'Sun', 8),
    ('b0000000-0005-0007-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'domotics', 'Domotics & Home Automation', 'Smart home systems, automation controls, and IoT devices', 'Settings', 9),
    ('b0000000-0005-0007-0001-000000000010'::uuid, 'a0000000-0005-0000-0000-000000000007'::uuid, 'parking_systems', 'Parking & Barrier Systems', 'Parking barriers, ticketing, payment systems, and car park management', 'Square', 10)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;

-- =====================================================================
-- STEP 10: Insert Subcategories — Group 8: Site & Temporary Services (10)
-- =====================================================================
INSERT INTO vendor.category_definitions (id, group_id, code, name, description, icon, display_order) VALUES
    ('b0000000-0005-0008-0001-000000000001'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'site_containers', 'Site Containers & Office', 'Site cabins, site offices, welfare facilities, and site containers', 'Square', 1),
    ('b0000000-0005-0008-0001-000000000002'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'temp_toilets', 'Temporary Toilets & Sanitation', 'Lifts, shower units, chemical toilets, and sanitation facilities', 'Sink', 2),
    ('b0000000-0005-0008-0001-000000000003'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'temp_power', 'Temporary Power Supply', 'Temporary electricity connections, distribution boards, and power cables', 'Zap', 3),
    ('b0000000-0005-0008-0001-000000000004'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'temp_water', 'Temporary Water Supply', 'Temporary water connections, storage tanks, and water treatment', 'Droplet', 4),
    ('b0000000-0005-0008-0001-000000000005'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'site_fencing', 'Site Fencing & Hoardings', 'Temporary fencing, site hoardings, barriers, and perimeter security', 'Panel', 5),
    ('b0000000-0005-0008-0001-000000000006'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'waste_mgmt', 'Waste Management', 'Skips, waste removal, recycling, C&D waste management, and arboriculture', 'Recycle', 6),
    ('b0000000-0005-0008-0001-000000000007'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'site_security', 'Site Security Services', 'Security guards, mobile patrols, key holding, and alarm response', 'ShieldAlert', 7),
    ('b0000000-0005-0008-0001-000000000008'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'traffic_management', 'Traffic Management', 'Temporary traffic lights, lane closures, signage, and highway works', 'TrafficCone', 8),
    ('b0000000-0005-0008-0001-000000000009'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'logistics_transport', 'Logistics & Transport', 'Haulage, deliveries, heavy transport, and logistics coordination', 'Truck', 9),
    ('b0000000-0005-0008-0001-000000000010'::uuid, 'a0000000-0005-0000-0000-000000000008'::uuid, 'site_services', 'Site Services & Facilities', 'First aid, site cleaning, welfare facilities, and site management services', 'Briefcase', 10)
ON CONFLICT (code, is_active) DO UPDATE SET name = EXCLUDED.name,
    description = EXCLUDED.description, icon = EXCLUDED.icon,
    display_order = EXCLUDED.display_order, group_id = EXCLUDED.group_id;
-- =====================================================================
-- STEP 11: Recreate View and Functions (idempotent - overwrites existing)
-- =====================================================================
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

-- =====================================================================
-- STEP 12: Summary of taxonomy
-- =====================================================================
-- Total: 8 groups, 100 subcategories
--   Group 1 (Construction Trades):     19 subcategories
--   Group 2 (MEP / Building Services): 13 subcategories
--   Group 3 (Civil & Infrastructure):   9 subcategories
--   Group 4 (Materials & Products):    15 subcategories
--   Group 5 (Machinery & Equipment):   11 subcategories
--   Group 6 (Prof. & Engineering):     13 subcategories
--   Group 7 (Specialized Systems):     10 subcategories
--   Group 8 (Site & Temporary Services): 10 subcategories
-----
