package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

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

    public enum QualityGrade {
        PREMIUM("Premium", "⭐", "Best quality, large nuts"),
        STANDARD("Standard", "✓", "Good quality, medium nuts"),
        PROCESSING("Processing", "⚙️", "For processing, smaller nuts");

        private final String displayName;
        private final String icon;
        private final String description;

        QualityGrade(String displayName, String icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public String getDescription() { return description; }
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
    // No setter for totalAmount - it's calculated

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
}