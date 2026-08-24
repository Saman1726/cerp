package io.cerp.project.service;

import java.util.UUID;

/**
 * Immutable result returned by {@link BimIngestionService#parseAndIngest}.
 *
 * <p>Provides granular counts so callers can report back to the user exactly what
 * was created, updated, or skipped during an IFC ingestion run.
 */
public final class BimIngestionResult {

    private final UUID constructionSiteId;
    private final int buildingsParsed;
    private final int buildingsCreated;
    private final int buildingsSkipped;
    private final int floorsParsed;
    private final int floorsCreated;
    private final int spacesParsed;
    private final int spacesCreated;
    private final int spacesSkipped;
    private final int materialsParsed;
    private final int materialsCreated;
    private final long bytesProcessed;

    private BimIngestionResult(Builder builder) {
        this.constructionSiteId = builder.constructionSiteId;
        this.buildingsParsed = builder.buildingsParsed;
        this.buildingsCreated = builder.buildingsCreated;
        this.buildingsSkipped = builder.buildingsSkipped;
        this.floorsParsed = builder.floorsParsed;
        this.floorsCreated = builder.floorsCreated;
        this.spacesParsed = builder.spacesParsed;
        this.spacesCreated = builder.spacesCreated;
        this.spacesSkipped = builder.spacesSkipped;
        this.materialsParsed = builder.materialsParsed;
        this.materialsCreated = builder.materialsCreated;
        this.bytesProcessed = builder.bytesProcessed;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public int getBuildingsParsed() {
        return buildingsParsed;
    }

    public int getBuildingsCreated() {
        return buildingsCreated;
    }

    public int getBuildingsSkipped() {
        return buildingsSkipped;
    }

    public int getFloorsParsed() {
        return floorsParsed;
    }

    public int getFloorsCreated() {
        return floorsCreated;
    }

    public int getSpacesParsed() {
        return spacesParsed;
    }

    public int getSpacesCreated() {
        return spacesCreated;
    }

    public int getSpacesSkipped() {
        return spacesSkipped;
    }

    public int getMaterialsParsed() {
        return materialsParsed;
    }

    public int getMaterialsCreated() {
        return materialsCreated;
    }

    public long getBytesProcessed() {
        return bytesProcessed;
    }

    /** Returns a summary string suitable for logging / API response. */
    public String summary() {
        return String.format(
            "site=%s | buildings: %d parsed, %d created, %d skipped | "
            + "floors: %d parsed, %d created | "
            + "spaces: %d parsed, %d created, %d skipped | "
            + "materials: %d parsed, %d created | "
            + "bytes=%d",
            constructionSiteId,
            buildingsParsed, buildingsCreated, buildingsSkipped,
            floorsParsed, floorsCreated,
            spacesParsed, spacesCreated, spacesSkipped,
            materialsParsed, materialsCreated,
            bytesProcessed);
    }

    /** Returns true if any entities were actually created. */
    public boolean hadCreations() {
        return buildingsCreated + floorsCreated + spacesCreated + materialsCreated > 0;
    }

    // ── Builder ──────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID constructionSiteId;
        private int buildingsParsed;
        private int buildingsCreated;
        private int buildingsSkipped;
        private int floorsParsed;
        private int floorsCreated;
        private int spacesParsed;
        private int spacesCreated;
        private int spacesSkipped;
        private int materialsParsed;
        private int materialsCreated;
        private long bytesProcessed;

        private Builder() {
        }

        public Builder constructionSiteId(UUID constructionSiteId) {
            this.constructionSiteId = constructionSiteId;
            return this;
        }

        public Builder buildingParsed() {
            this.buildingsParsed++;
            return this;
        }

        public Builder buildingCreated() {
            this.buildingsCreated++;
            return this;
        }

        public Builder buildingSkipped() {
            this.buildingsSkipped++;
            return this;
        }

        public Builder floorParsed() {
            this.floorsParsed++;
            return this;
        }

        public Builder floorCreated() {
            this.floorsCreated++;
            return this;
        }

        public Builder spaceParsed() {
            this.spacesParsed++;
            return this;
        }

        public Builder spaceCreated() {
            this.spacesCreated++;
            return this;
        }

        public Builder spaceSkipped() {
            this.spacesSkipped++;
            return this;
        }

        public Builder materialParsed() {
            this.materialsParsed++;
            return this;
        }

        public Builder materialCreated() {
            this.materialsCreated++;
            return this;
        }

        public Builder bytesProcessed(long bytes) {
            this.bytesProcessed += bytes;
            return this;
        }

        public BimIngestionResult build() {
            return new BimIngestionResult(this);
        }
    }
}