package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.afrinuts.farmos.R;

@Entity(tableName = "revenues",
        foreignKeys = {
                @ForeignKey(
                        entity = FarmEntity.class,
                        parentColumns = "id",
                        childColumns = "farmId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = BlockEntity.class,
                        parentColumns = "id",
                        childColumns = "blockId",
                        onDelete = ForeignKey.SET_NULL
                )
        })
public class RevenueEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;
    private Long blockId;  // null for farm-wide revenue (e.g., processing center sales)

    private long harvestDate;
    private double quantityKg;
    private double pricePerKg;
    private double totalAmount;  // quantityKg * pricePerKg

    private String buyer;
    private QualityGrade quality;  // PREMIUM, STANDARD, etc.
    private String notes;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    private String harvestCycleId;
    private int harvestYear = 0;      // Default value
    private int harvestSeason = 1;    // Default value (first harvest)


    public enum QualityGrade {
        PREMIUM("Premium", "Premium Grade", R.drawable.ic_star),
        STANDARD("Standard", "Standard Grade", R.drawable.ic_check_circle),
        PROCESSING("Processing", "For processing", R.drawable.ic_factory);

        private final String displayName;
        private final String description;
        private final int iconResId; // Store vector icon resource ID

        QualityGrade(String displayName, String description, int iconResId) {
            this.displayName = displayName;
            this.description = description;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getIconResId() { return iconResId; } // New getter for vector icon
    }

    // Constructor
    public RevenueEntity(long farmId, Long blockId, long harvestDate,
                         double quantityKg, double pricePerKg,
                         String buyer, QualityGrade quality, String notes) {
        this.farmId = farmId;
        this.blockId = blockId;
        this.harvestDate = harvestDate;
        this.quantityKg = quantityKg;
        this.pricePerKg = pricePerKg;
        this.totalAmount = quantityKg * pricePerKg;
        this.buyer = buyer;
        this.quality = quality;
        this.notes = notes;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) {
        this.blockId = blockId;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getHarvestDate() { return harvestDate; }
    public void setHarvestDate(long harvestDate) {
        this.harvestDate = harvestDate;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getQuantityKg() { return quantityKg; }
    public void setQuantityKg(double quantityKg) {
        this.quantityKg = quantityKg;
        this.totalAmount = quantityKg * this.pricePerKg;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(double pricePerKg) {
        this.pricePerKg = pricePerKg;
        this.totalAmount = this.quantityKg * pricePerKg;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getTotalAmount() { return totalAmount; }

    // Required by Room for hydration from database
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBuyer() { return buyer; }
    public void setBuyer(String buyer) {
        this.buyer = buyer;
        this.updatedAt = System.currentTimeMillis();
    }

    public QualityGrade getQuality() { return quality; }
    public void setQuality(QualityGrade quality) {
        this.quality = quality;
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

    public String getHarvestCycleId() { return harvestCycleId; }
    public void setHarvestCycleId(String harvestCycleId) { this.harvestCycleId = harvestCycleId; }

    public int getHarvestYear() { return harvestYear; }
    public void setHarvestYear(int harvestYear) { this.harvestYear = harvestYear; }

    public int getHarvestSeason() { return harvestSeason; }
    public void setHarvestSeason(int harvestSeason) { this.harvestSeason = harvestSeason; }

    // Helper methods
    public boolean isFarmWide() {
        return blockId == null;
    }

    public String getFormattedTotal() {
        return String.format("%,.0f XAF", totalAmount);
    }

    public String getFormattedQuantity() {
        return String.format("%.1f kg", quantityKg);
    }

    public String getFormattedPrice() {
        return String.format("%,.0f XAF/kg", pricePerKg);
    }

    public static String generateHarvestCycleId(long blockId, int year) {
        return "BLOCK_" + blockId + "_YEAR_" + year;
    }
}