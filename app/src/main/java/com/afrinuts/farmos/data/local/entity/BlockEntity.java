package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    // ✅ NEW: Harvest tracking
    private String harvestHistory;  // JSON string of harvest records
    private Long lastHarvestDate;   // Date of last harvest
    private double lastHarvestQuantity; // Quantity from last harvest (kg)
    private double totalHarvestQuantity; // Total harvested from this block (kg)
    private int harvestCount;       // Number of times harvested

    private String notes;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    // Enum for block status
    public enum BlockStatus {
        NOT_CLEARED("Not Cleared", "Land not yet prepared"),
        CLEARED("Cleared", "Land cleared, ready for plowing"),
        PLOWED("Plowed", "Soil prepared, ready for planting"),
        PLANTED("Planted", "Cashew trees planted"),
        HARVESTED("Harvested", "Trees have been harvested");

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

    // ✅ NEW: HarvestRecord inner class
    public static class HarvestRecord {
        private long harvestDate;
        private double quantityKg;
        private String grade;
        private String notes;

        public HarvestRecord(long harvestDate, double quantityKg, String grade, String notes) {
            this.harvestDate = harvestDate;
            this.quantityKg = quantityKg;
            this.grade = grade;
            this.notes = notes;
        }

        public long getHarvestDate() { return harvestDate; }
        public void setHarvestDate(long harvestDate) { this.harvestDate = harvestDate; }

        public double getQuantityKg() { return quantityKg; }
        public void setQuantityKg(double quantityKg) { this.quantityKg = quantityKg; }

        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(harvestDate));
        }

        public String getFormattedQuantity() {
            return String.format(Locale.getDefault(), "%,.0f kg", quantityKg);
        }
    }

    // In BlockEntity.java - add this method

    /**
     * Update a harvest record at a specific index
     */
    public boolean updateHarvestRecord(int index, HarvestRecord updatedRecord) {
        List<HarvestRecord> records = getHarvestRecords();
        if (index < 0 || index >= records.size()) {
            return false;
        }

        // Remove the old record
        HarvestRecord oldRecord = records.get(index);
        records.set(index, updatedRecord);

        // Update aggregated values
        this.harvestHistory = new Gson().toJson(records);

        // Recalculate totals from all records
        double total = 0;
        long lastDate = 0;
        double lastQty = 0;
        int count = records.size();

        for (HarvestRecord record : records) {
            total += record.getQuantityKg();
            if (record.getHarvestDate() > lastDate) {
                lastDate = record.getHarvestDate();
                lastQty = record.getQuantityKg();
            }
        }

        this.totalHarvestQuantity = total;
        this.lastHarvestDate = lastDate;
        this.lastHarvestQuantity = lastQty;
        this.harvestCount = count;
        this.updatedAt = System.currentTimeMillis();

        return true;
    }

    /**
     * Delete a harvest record at a specific index
     */
    public boolean deleteHarvestRecord(int index) {
        List<HarvestRecord> records = getHarvestRecords();
        if (index < 0 || index >= records.size()) {
            return false;
        }

        records.remove(index);
        this.harvestHistory = new Gson().toJson(records);

        // Recalculate aggregated values
        double total = 0;
        long lastDate = 0;
        double lastQty = 0;
        int count = records.size();

        for (HarvestRecord record : records) {
            total += record.getQuantityKg();
            if (record.getHarvestDate() > lastDate) {
                lastDate = record.getHarvestDate();
                lastQty = record.getQuantityKg();
            }
        }

        this.totalHarvestQuantity = total;
        this.lastHarvestDate = lastDate;
        this.lastHarvestQuantity = lastQty;
        this.harvestCount = count;
        this.updatedAt = System.currentTimeMillis();

        return true;
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

        // ✅ Harvest tracking defaults
        this.harvestHistory = new Gson().toJson(new ArrayList<HarvestRecord>());
        this.lastHarvestDate = null;
        this.lastHarvestQuantity = 0;
        this.totalHarvestQuantity = 0;
        this.harvestCount = 0;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public String getBlockName() { return blockName; }
    public void setBlockName(String blockName) { this.blockName = blockName; }

    public double getHectareSize() { return hectareSize; }
    public void setHectareSize(double hectareSize) { this.hectareSize = hectareSize; }

    public BlockStatus getStatus() { return status; }
    public void setStatus(BlockStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getClearedDate() { return clearedDate; }
    public void setClearedDate(Long clearedDate) {
        this.clearedDate = clearedDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getPlowedDate() { return plowedDate; }
    public void setPlowedDate(Long plowedDate) {
        this.plowedDate = plowedDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getPlantedDate() { return plantedDate; }
    public void setPlantedDate(Long plantedDate) {
        this.plantedDate = plantedDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getSurvivalRate() { return survivalRate; }
    public void setSurvivalRate(double survivalRate) {
        this.survivalRate = survivalRate;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getReplacementCount() { return replacementCount; }
    public void setReplacementCount(int replacementCount) {
        this.replacementCount = replacementCount;
        this.updatedAt = System.currentTimeMillis();
    }

    // ✅ Harvest Getters and Setters
    public String getHarvestHistory() { return harvestHistory; }
    public void setHarvestHistory(String harvestHistory) {
        this.harvestHistory = harvestHistory;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getLastHarvestDate() { return lastHarvestDate; }
    public void setLastHarvestDate(Long lastHarvestDate) {
        this.lastHarvestDate = lastHarvestDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getLastHarvestQuantity() { return lastHarvestQuantity; }
    public void setLastHarvestQuantity(double lastHarvestQuantity) {
        this.lastHarvestQuantity = lastHarvestQuantity;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getTotalHarvestQuantity() { return totalHarvestQuantity; }
    public void setTotalHarvestQuantity(double totalHarvestQuantity) {
        this.totalHarvestQuantity = totalHarvestQuantity;
        this.updatedAt = System.currentTimeMillis();
    }

    public int getHarvestCount() { return harvestCount; }
    public void setHarvestCount(int harvestCount) {
        this.harvestCount = harvestCount;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) {
        this.notes = notes;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // ✅ Harvest helper methods
    public List<HarvestRecord> getHarvestRecords() {
        if (harvestHistory == null || harvestHistory.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type listType = new TypeToken<List<HarvestRecord>>(){}.getType();
            return new Gson().fromJson(harvestHistory, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void addHarvestRecord(HarvestRecord record) {
        List<HarvestRecord> records = getHarvestRecords();
        records.add(record);
        this.harvestHistory = new Gson().toJson(records);
        this.lastHarvestDate = record.harvestDate;
        this.lastHarvestQuantity = record.quantityKg;
        this.totalHarvestQuantity += record.quantityKg;
        this.harvestCount = records.size();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getFormattedLastHarvestDate() {
        if (lastHarvestDate == null || lastHarvestDate == 0) return "Not harvested yet";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(lastHarvestDate));
    }

    public String getFormattedTotalHarvest() {
        return String.format(Locale.getDefault(), "%,.0f kg", totalHarvestQuantity);
    }

    public double getYieldPerHectare() {
        if (hectareSize <= 0 || totalHarvestQuantity == 0) return 0;
        return totalHarvestQuantity / hectareSize;
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
            case HARVESTED:
                return lastHarvestDate;
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
            case HARVESTED:
                return "Last Harvest";
            default:
                return "Date";
        }
    }

    // Helper method to calculate alive trees
    public int getAliveTrees() {
        if (status != BlockStatus.PLANTED && status != BlockStatus.HARVESTED) {
            return 0;
        }
        return (int) Math.round(100 * (survivalRate / 100.0));
    }

    // Helper method to calculate dead trees
    public int getDeadTrees() {
        if (status != BlockStatus.PLANTED && status != BlockStatus.HARVESTED) {
            return 0;
        }
        return 100 - getAliveTrees();
    }

    // Check if planting details are relevant
    public boolean isPlanted() {
        return status == BlockStatus.PLANTED || status == BlockStatus.HARVESTED;
    }

    public boolean isHarvestable() {
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
                ", harvestCount=" + harvestCount +
                ", totalHarvestQuantity=" + totalHarvestQuantity +
                '}';
    }
}