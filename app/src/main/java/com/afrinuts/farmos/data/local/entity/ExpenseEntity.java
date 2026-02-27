package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.data.local.converter.Converters;

@Entity(tableName = "expenses",
        foreignKeys = @ForeignKey(
                entity = FarmEntity.class,
                parentColumns = "id",
                childColumns = "farmId",
                onDelete = ForeignKey.CASCADE
        ))
public class ExpenseEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;  // Always link to farm

    private Long blockId;  // Nullable - null means farm-wide expense

    private ExpenseCategory category;

    private double amount;

    private long date;

    private String description;

    private String receiptPath;  // For future photo attachment

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    public enum ExpenseCategory {
        LAND_CLEARING("Land Clearing", "🚜", "35 hectares plantation"),
        PLOWING("Plowing", "🌾", "35 hectares"),
        SEEDLINGS("Seedlings", "🌱", "3,500 trees + replacements"),
        LABOR("Labor", "👥", "Supervision & workers"),
        SECURITY("Security", "🛡️", "1 year guard service"),
        FENCING("Fencing", "⛓️", "Perimeter fence - 35 hectares"),
        PROCESSING_CENTER("Processing Center", "🏭", "15 hectares - separate"),
        FERTILIZER("Fertilizer", "🧪", null),
        IRRIGATION("Irrigation", "💧", null),
        EQUIPMENT("Equipment", "🔧", null),
        MAINTENANCE("Maintenance", "🔨", null),
        OTHER("Other", "📦", null);

        private final String displayName;
        private final String icon;
        private final String defaultDescription;

        ExpenseCategory(String displayName, String icon, String defaultDescription) {
            this.displayName = displayName;
            this.icon = icon;
            this.defaultDescription = defaultDescription;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIcon() {
            return icon;
        }

        public String getDefaultDescription() {
            return defaultDescription;
        }
    }

    // Constructor
    public ExpenseEntity(long farmId, Long blockId, ExpenseCategory category,
                         double amount, long date, String description) {
        this.farmId = farmId;
        this.blockId = blockId;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;

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
    public void setBlockId(Long blockId) { this.blockId = blockId; }

    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) {
        this.category = category;
        this.updatedAt = System.currentTimeMillis();
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) {
        this.amount = amount;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getDate() { return date; }
    public void setDate(long date) {
        this.date = date;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) {
        this.receiptPath = receiptPath;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Helper to check if expense is farm-wide or block-specific
    public boolean isFarmWide() {
        return blockId == null;
    }
}