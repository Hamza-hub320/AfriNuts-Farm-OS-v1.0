package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.data.local.converter.Converters;

/**
 * Block entity representing a 1-hectare block in the farm.
 * Each block contains approximately 100 cashew trees.
 */
@Entity(tableName = "blocks",
        foreignKeys = @ForeignKey(
                entity = FarmEntity.class,
                parentColumns = "id",
                childColumns = "farmId",
                onDelete = ForeignKey.CASCADE
        ))
public class BlockEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;  // Foreign key to FarmEntity

    private String blockName;  // e.g., "A1", "B2", "C3"

    private double hectareSize;  // Usually 1.0, but could be fractional

    private BlockStatus status;

    // Track dates for each operational state
    private Long clearedDate;    // null if not yet cleared
    private Long plowedDate;     // null if not yet plowed
    private Long plantedDate;    // null if not yet planted

    // Planting details (only relevant when status >= PLANTED)
    private double survivalRate;  // Percentage (0-100)
    private int replacementCount;  // Number of trees replaced

    private String notes;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    // Enum for block status
    public enum BlockStatus {
        NOT_CLEARED("Not Cleared", "Land not yet prepared"),
        CLEARED("Cleared", "Land cleared, ready for plowing"),
        PLOWED("Plowed", "Soil prepared, ready for planting"),
        PLANTED("Planted", "Cashew trees planted");

        private final String displayName;
        private final String description;

        BlockStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    // Constructor for new blocks (NOT_CLEARED by default)
    public BlockEntity(long farmId, String blockName, double hectareSize, String notes) {
        this.farmId = farmId;
        this.blockName = blockName;
        this.hectareSize = hectareSize;
        this.status = BlockStatus.NOT_CLEARED;
        this.notes = notes;

        // Planting details start as null/zero
        this.clearedDate = null;
        this.plowedDate = null;
        this.plantedDate = null;
        this.survivalRate = 0.0;
        this.replacementCount = 0;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFarmId() {
        return farmId;
    }

    public void setFarmId(long farmId) {
        this.farmId = farmId;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public double getHectareSize() {
        return hectareSize;
    }

    public void setHectareSize(double hectareSize) {
        this.hectareSize = hectareSize;
    }

    public BlockStatus getStatus() {
        return status;
    }

    public void setStatus(BlockStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getClearedDate() {
        return clearedDate;
    }

    public void setClearedDate(Long clearedDate) {
        this.clearedDate = clearedDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getPlowedDate() {
        return plowedDate;
    }

    public void setPlowedDate(Long plowedDate) {
        this.plowedDate = plowedDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getPlantedDate() {
        return plantedDate;
    }

    public void setPlantedDate(Long plantedDate) {
        this.plantedDate = plantedDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getSurvivalRate() {
        return survivalRate;
    }

    public void setSurvivalRate(double survivalRate) {
        this.survivalRate = survivalRate;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getReplacementCount() {
        return replacementCount;
    }

    public void setReplacementCount(int replacementCount) {
        this.replacementCount = replacementCount;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper method to get the relevant date based on status
    public Long getRelevantDate() {
        switch (status) {
            case CLEARED:
                return clearedDate;
            case PLOWED:
                return plowedDate;
            case PLANTED:
                return plantedDate;
            default:
                return null;
        }
    }

    // Helper method to get date label based on status
    public String getDateLabel() {
        switch (status) {
            case CLEARED:
                return "Cleared";
            case PLOWED:
                return "Plowed";
            case PLANTED:
                return "Planted";
            default:
                return "Date";
        }
    }

    // Helper method to calculate alive trees
    public int getAliveTrees() {
        if (status != BlockStatus.PLANTED) {
            return 0;
        }
        return (int) Math.round(100 * (survivalRate / 100.0));
    }

    // Helper method to calculate dead trees
    public int getDeadTrees() {
        if (status != BlockStatus.PLANTED) {
            return 0;
        }
        return 100 - getAliveTrees();
    }

    // Check if planting details are relevant
    public boolean isPlanted() {
        return status == BlockStatus.PLANTED;
    }

    @Override
    public String toString() {
        return "BlockEntity{" +
                "id=" + id +
                ", farmId=" + farmId +
                ", blockName='" + blockName + '\'' +
                ", status=" + status +
                ", clearedDate=" + clearedDate +
                ", plowedDate=" + plowedDate +
                ", plantedDate=" + plantedDate +
                ", survivalRate=" + survivalRate +
                ", replacementCount=" + replacementCount +
                '}';
    }
}