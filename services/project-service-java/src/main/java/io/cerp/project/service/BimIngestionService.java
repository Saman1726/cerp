package io.cerp.project.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.project.config.TenantContextFilter;
import io.cerp.project.domain.Building;
import io.cerp.project.domain.ConstructionMaterial;
import io.cerp.project.domain.ConstructionMaterial.PhysicalObjectType;
import io.cerp.project.domain.ConstructionSite;
import io.cerp.project.domain.Floor;
import io.cerp.project.domain.Unit;
import io.cerp.project.repository.BuildingRepository;
import io.cerp.project.repository.ConstructionMaterialRepository;
import io.cerp.project.repository.ConstructionSiteRepository;
import io.cerp.project.repository.FloorRepository;
import io.cerp.project.repository.UnitRepository;

/**
 * Ingests Industry Foundation Classes (.ifc) architectural files from a presigned URL
 * and populates Building, Floor, and Unit entities under a ConstructionSite.
 *
 * <p>IFC hierarchy to CERP domain mapping:
 * <pre>
 *   IfcBuilding            -&gt; Building
 *      |- IfcBuildingStorey  -&gt; Floor
 *           |- IfcSpace       -&gt; Unit
 * </pre>
 *
 * <p>Deduplication: on re-ingestion, buildings whose GlobalId or name already
 * exists on the same construction site are skipped. Materials are deduplicated
 * on (site, serviceCode/ifcMaterialName, objectType, objectId).
 */
@Service
public class BimIngestionService {

    private static final Logger log = LoggerFactory.getLogger(BimIngestionService.class);
    private static final Pattern ENTITY_DEF = Pattern.compile("#(\\d+)=IFC(\\w+)\\((.+)\\)\\s*;");
    private static final Pattern ENTITY_REF = Pattern.compile("#(\\d+)");

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final UnitRepository unitRepository;
    private final ConstructionSiteRepository constructionSiteRepository;
    private final ConstructionMaterialRepository constructionMaterialRepository;
    private final java.net.http.HttpClient httpClient;

    public BimIngestionService(BuildingRepository buildingRepository,
            FloorRepository floorRepository, UnitRepository unitRepository,
            ConstructionSiteRepository constructionSiteRepository,
            ConstructionMaterialRepository constructionMaterialRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.unitRepository = unitRepository;
        this.constructionSiteRepository = constructionSiteRepository;
        this.constructionMaterialRepository = constructionMaterialRepository;
        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    /**
     * Download an IFC file from the document-service (internal server-to-server endpoint),
     * parse it, and ingest into the site.
     */
    @Transactional
    public BimIngestionResult parseAndIngest(UUID constructionSiteId, String objectKey) throws Exception {
        log.info("Starting BIM ingestion for site={}, objectKey={}", constructionSiteId, objectKey);
        BimIngestionResult.Builder result = BimIngestionResult.builder().constructionSiteId(constructionSiteId);

        ConstructionSite site = constructionSiteRepository.findById(constructionSiteId)
                .orElseThrow(() -> new BimIngestionException("site lookup",
                        "No ConstructionSite found with id: " + constructionSiteId));

        byte[] ifcBuffer = downloadIfcViaDocumentService(objectKey);
        result.bytesProcessed(ifcBuffer.length);
        String ifcText = new String(ifcBuffer, "ISO-8859-1");

        IfcModel model = parseIfcFile(ifcText);
        log.info("Parsed IFC: {} buildings, {} storeys, {} spaces, {} materials",
                model.buildings.size(), model.storeys.size(), model.spaces.size(), model.materials.size());

        Map<String, Building> existingBuildings = new HashMap<>();

        for (IfcBuilding b : model.buildings.values()) {
            try { processBuilding(b, model, site, existingBuildings, result); }
            catch (BimIngestionException e) {
                log.warn("Skipping IfcBuilding '{}' (GlobalId={}): {}",
                        safe(b.name), b.globalId, e.getMessage());
                result.buildingSkipped();
            }
        }

        processMaterials(model, site, result);

        log.info("BIM ingestion completed: {}", result.build().summary());
        return result.build();
    }

    /** Download IFC file via document-service's internal server-to-server endpoint. */
    private byte[] downloadIfcViaDocumentService(String objectKey) throws IOException, InterruptedException {
        String documentServiceUrl = System.getenv("DOCUMENT_SERVICE_URL")
            + "/api/v1/media/download/internal";
        String tenantId = TenantContextFilter.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = "00000000-0000-0000-0000-000000000001";
        }

        String jsonBody = "{\"objectKey\":\"" + objectKey + "\"}";

        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(documentServiceUrl))
                .timeout(java.time.Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Id", tenantId)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            String body = new String(response.body(), "UTF-8");
            throw new BimIngestionException("download",
                    "HTTP " + response.statusCode() + " from document-service: " + body);
        }
        log.info("Downloaded IFC from document-service: {} bytes", response.body().length);
        return response.body();
    }

    @Deprecated
    private byte[] downloadFile(String url) throws IOException, InterruptedException {
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(60))
                .GET()
                .build();

        var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new BimIngestionException("download",
                    "HTTP " + response.statusCode() + " for " + truncateUrl(url, 120));
        }
        return response.body();
    }

    private static String truncateUrl(String url, int maxLen) {
        if (url == null) return null;
        return url.length() <= maxLen ? url : url.substring(0, maxLen) + "...";
    }

    private void processBuilding(IfcBuilding b, IfcModel model, ConstructionSite site,
            Map<String, Building> existingBuildings, BimIngestionResult.Builder result) {
        log.info("Processing IfcBuilding: name='{}', globalId='{}'", b.name, b.globalId);
        result.buildingParsed();

        Building dup = null;
        if (b.globalId != null) {
            dup = existingBuildings.get(b.globalId);
        }
        if (dup == null) {
            dup = buildingRepository.findByConstructionSiteIdAndName(
                    site.getId(), b.name).orElse(null);
        }
        if (dup != null) {
            log.info("Skipping IfcBuilding '{}' (GlobalId={}) — already exists (id={})",
                    b.name, b.globalId, dup.getId());
            result.buildingSkipped();
            return;
        }

        Building building = new Building(b.name);
        building.setConstructionSite(site);
        building = buildingRepository.save(building);
        log.info("Created Building: id={}, name='{}'", building.getId(), b.name);
        result.buildingCreated();

        // Register JPA ID for this IFC building entity
        model.jpaIds.put(b.entityId, building.getId());

        if (b.globalId != null) {
            existingBuildings.put(b.globalId, building);
        }

        processBuildingChildren(b, building, model, result);
    }

    private void processBuildingChildren(IfcBuilding parent, Building building,
            IfcModel model, BimIngestionResult.Builder result) {
        List<Integer> storeyIds = model.getStoreysInBuilding(parent.entityId);

        for (Integer sid : storeyIds) {
            IfcBuildingStorey s = model.storeys.get(sid);
            if (s == null) continue;
            String sname = safe(s.name);
            if (sname == null) sname = "Unnamed Floor";
            log.info("  Processing floor: '{}'", sname);
            result.floorParsed();

            Floor floor = new Floor(sname, BigDecimal.ZERO, BigDecimal.ZERO);
            floor.setBuilding(building);
            floor = floorRepository.save(floor);
            log.info("    Created Floor: id={}, type='{}'", floor.getId(), sname);
            model.jpaIds.put(sid, floor.getId());
            result.floorCreated();

            List<Integer> spaceIds = model.getSpacesOnFloor(sid);
            log.info("    Floor '{}' has {} IfcSpace entities", sname, spaceIds.size());
            for (Integer spid : spaceIds) {
                IfcSpace sp = model.spaces.get(spid);
                if (sp != null) processSpace(sp, floor, model, result);
            }
        }
    }

    private void processSpace(IfcSpace space, Floor floor, IfcModel model,
            BimIngestionResult.Builder result) {
        String name = safe(space.name);
        if (name == null) name = "Unnamed Space";
        log.info("      Processing IfcSpace: name='{}', globalId='{}'", name, space.globalId);
        result.spaceParsed();

        Unit unit = new Unit(name, null, null, null, null, null);
        unit.setFloor(floor);
        unit = unitRepository.save(unit);
        log.info("      Created Unit: id={}, label='{}'", unit.getId(), name);
        model.jpaIds.put(space.entityId, unit.getId());
        result.spaceCreated();
    }

    // ── Material processing ──────────────────────────────────────────

    private void processMaterials(IfcModel model, ConstructionSite site,
            BimIngestionResult.Builder result) {
        if (model.materials.isEmpty()) {
            log.info("No materials found in IFC model");
            return;
        }

        log.info("Processing {} IFC materials for site {}", model.materials.size(), site.getId());

        for (IfcMaterial mat : model.materials) {
            result.materialParsed();

            UUID physicalObjectId = null;
            PhysicalObjectType physicalObjectType = null;

            // Resolve IFC entity ID to JPA UUID
            if (mat.relatedBuildingId != null) {
                UUID bId = model.jpaIds.get(mat.relatedBuildingId);
                if (bId != null) {
                    physicalObjectId = bId;
                    physicalObjectType = PhysicalObjectType.BUILDING;
                }
            } else if (mat.relatedFloorId != null) {
                UUID fId = model.jpaIds.get(mat.relatedFloorId);
                if (fId != null) {
                    physicalObjectId = fId;
                    physicalObjectType = PhysicalObjectType.FLOOR;
                }
            } else if (mat.relatedUnitId != null) {
                UUID uId = model.jpaIds.get(mat.relatedUnitId);
                if (uId != null) {
                    physicalObjectId = uId;
                    physicalObjectType = PhysicalObjectType.UNIT;
                }
            }

            String dedupCode = mat.catalogCode != null && !mat.catalogCode.isBlank()
                    ? mat.catalogCode
                    : mat.ifcName != null && !mat.ifcName.isBlank()
                            ? mat.ifcName
                            : null;

            if (dedupCode == null) {
                log.debug("Skipping material with no name or code: entity #{}", mat.entityId);
                continue;
            }

            boolean found = constructionMaterialRepository
                    .findByConstructionSiteIdAndMaterialServiceCodeAndPhysicalObjectTypeAndPhysicalObjectId(
                            site.getId(), mat.catalogCode, physicalObjectType, physicalObjectId)
                    .isPresent();

            if (found) {
                log.debug("Skipping duplicate material: code={}, objectType={}, objectId={}",
                        dedupCode, physicalObjectType, physicalObjectId);
                continue;
            }

            ConstructionMaterial cm = new ConstructionMaterial(
                    site, physicalObjectType, physicalObjectId,
                    mat.catalogCode, mat.catalogName,
                    mat.ifcName, mat.quantity, mat.unit);
            constructionMaterialRepository.save(cm);
            result.materialCreated();
        }
    }

    // ── Lightweight IFC STEP parser ─────────────────────────────────

    private static class IfcModel {
        final Map<Integer, IfcBuilding> buildings = new HashMap<>();
        final Map<Integer, IfcBuildingStorey> storeys = new HashMap<>();
        final Map<Integer, IfcSpace> spaces = new HashMap<>();
        final List<IfcMaterial> materials = new ArrayList<>();
        final Map<Integer, List<Integer>> aggregationMap = new HashMap<>();
        final Map<Integer, List<Integer>> spatialStructMap = new HashMap<>();
        // Mapping from IFC entity IDs to JPA UUIDs (populated during building creation)
        final Map<Integer, UUID> jpaIds = new HashMap<>();

        List<Integer> getStoreysInBuilding(int buildingId) {
            return aggregationMap.getOrDefault(buildingId, List.of());
        }

        List<Integer> getSpacesOnFloor(int storeyId) {
            return spatialStructMap.getOrDefault(storeyId, List.of());
        }
    }

    private static class IfcBuilding {
        final int entityId;
        final String globalId, name, predefinedType;
        IfcBuilding(int id, String gid, String nm, String pt) {
            entityId = id; globalId = gid; name = nm; predefinedType = pt;
        }
    }

    private static class IfcBuildingStorey {
        final int entityId;
        final String globalId, name;
        IfcBuildingStorey(int id, String gid, String nm) {
            entityId = id; globalId = gid; name = nm;
        }
    }

    private static class IfcSpace {
        final int entityId;
        final String globalId, name;
        IfcSpace(int id, String gid, String nm) {
            entityId = id; globalId = gid; name = nm;
        }
    }

    /**
     * Lightweight IFC material representation extracted from IFCMATERIAL
     * and its assignments/property sets.
     */
    private static class IfcMaterial {
        final int entityId;
        final String ifcName, catalogCode, catalogName;
        final BigDecimal quantity;
        final String unit;
        final Integer relatedBuildingId, relatedFloorId, relatedUnitId;

        IfcMaterial(int id, String name, String code, String codeName,
                    BigDecimal qty, String u, Integer bId, Integer fId, Integer uId) {
            entityId = id; ifcName = name; catalogCode = code; catalogName = codeName;
            quantity = qty; unit = u; relatedBuildingId = bId;
            relatedFloorId = fId; relatedUnitId = uId;
        }
    }

    private IfcModel parseIfcFile(String ifcText) {
        IfcModel model = new IfcModel();
        String dataSection = extractDataSection(ifcText);
        Matcher m = ENTITY_DEF.matcher(dataSection);

        while (m.find()) {
            int id = Integer.parseInt(m.group(1));
            String type = m.group(2);
            String args = m.group(3);
            switch (type) {
                case "IFCBUILDING" -> model.buildings.put(id, mkBuilding(args));
                case "IFCBUILDINGSTOREY" -> model.storeys.put(id, mkStorey(args));
                case "IFCSPACE" -> model.spaces.put(id, mkSpace(args));
                case "IFCMATERIAL" -> model.materials.add(mkMaterial(args));
                case "IFCRELAGGREGATES" -> processAggregates(model, args);
                case "IFCRELCONTAINEDINSPATIALSTRUCTURE" -> processSpatialStructure(model, args);
                default -> {}
            }
        }
        log.info("Parsed IFC: {} bldgs, {} storeys, {} spaces, {} materials, {} aggs, {} spatial",
                model.buildings.size(), model.storeys.size(), model.spaces.size(),
                model.materials.size(), model.aggregationMap.size(), model.spatialStructMap.size());
        return model;
    }

    private IfcBuilding mkBuilding(String args) {
        List<String> a = parseAttrs(args);
        return new IfcBuilding(0, getStr(a,0), getStr(a,2), getStr(a,13));
    }

    private IfcBuildingStorey mkStorey(String args) {
        List<String> a = parseAttrs(args);
        return new IfcBuildingStorey(0, getStr(a,0), getStr(a,2));
    }

    private IfcSpace mkSpace(String args) {
        List<String> a = parseAttrs(args);
        return new IfcSpace(0, getStr(a,0), getStr(a,2));
    }

    /** IFCMATERIAL(0:Name, 1:Description, 2:Category) */
    private IfcMaterial mkMaterial(String args) {
        List<String> a = parseAttrs(args);
        String name = getStr(a, 0);
        String category = getStr(a, 2);
        // Category can be comma-separated list, e.g. "Concrete,Structural"
        return new IfcMaterial(0, name, null, category,
                null, null, null, null, null);
    }

    /** IFCBUILDY(0:GlobalId,1:OwnerHistory,2:Name,...,13:PredefinedType) */

    /** IFCRelAggregates(0:GlobalId,1:OwnerHistory,2:Name,3:Description,4:RelatedObjects,[storey IDs],5:RelatingObject,[building ID]) */
    private void processAggregates(IfcModel model, String args) {
        List<String> a = parseAttrs(args);
        // Index 4 = RelatedObjects (list of storey refs), index 5 = RelatingObject (building ref)
        String related = a.size() > 4 ? a.get(4) : "";
        String relating = a.size() > 5 ? a.get(5) : "";
        if (relating.startsWith("#")) {
            int buildingId = parseRef(relating);
            List<Integer> storeyIds = refsToList(related);
            model.aggregationMap.put(buildingId, storeyIds);
        }
    }

    /** IFCRelContainedInSpatialStructure(4:RelatedElements=[space IDs],5:RelatingStructure=[storey ID]) */
    private void processSpatialStructure(IfcModel model, String args) {
        List<String> a = parseAttrs(args);
        String related = a.size() > 4 ? a.get(4) : "";
        String relating = a.size() > 5 ? a.get(5) : "";
        if (relating.startsWith("#")) {
            int storeyId = parseRef(relating);
            List<Integer> spaceIds = refsToList(related);
            model.spatialStructMap.put(storeyId, spaceIds);
        }
    }

    private static int parseRef(String text) {
        Matcher m = ENTITY_REF.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static List<Integer> refsToList(String text) {
        List<Integer> result = new ArrayList<>();
        Matcher m = ENTITY_REF.matcher(text);
        while (m.find()) result.add(Integer.parseInt(m.group(1)));
        return result;
    }

    /** Split IFC args respecting nested parens and quoted strings. */
    private static List<String> parseAttrs(String args) {
        List<String> result = new ArrayList<>();
        int depth = 0, inQuote = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '"' && (i == 0 || args.charAt(i-1) != '\\')) { inQuote ^= 1; cur.append(c); }
            else if (inQuote == 1) { cur.append(c); }
            else if (c == '(') { depth++; cur.append(c); }
            else if (c == ')') { depth--; cur.append(c); }
            else if (c == ',' && depth == 0) {
                String s = cur.toString().trim();
                if (!s.isEmpty()) result.add(s);
                cur = new StringBuilder();
            } else { cur.append(c); }
        }
        String last = cur.toString().trim();
        if (!last.isEmpty()) result.add(last);
        return result;
    }

    private static String getStr(List<String> a, int i) {
        if (i < 0 || i >= a.size()) return null;
        String s = a.get(i);
        if (s.equals("$") || s.equals("*")) return null;
        if (s.startsWith("#")) return null;
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length()-1);
        return s.isEmpty() ? null : s;
    }

    private static String extractDataSection(String ifcText) {
        int ds = ifcText.indexOf("DATA ;;");
        int de = ifcText.indexOf("ENDSEC;");
        if (ds >= 0 && de > ds) return ifcText.substring(ds + 7, de).trim();
        return ifcText;
    }

    // ── Business helpers ────────────────────────────────────────────

    private static String safe(String v) {
        return v == null || v.isEmpty() ? null : v;
    }
}