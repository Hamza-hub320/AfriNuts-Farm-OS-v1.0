package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.converter.Converters;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Entity(tableName = "expenses",
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
                ),
                @ForeignKey(
                        entity = QualityBatchEntity.class,
                        parentColumns = "id",
                        childColumns = "batchId",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = SupplierEntity.class,
                        parentColumns = "id",
                        childColumns = "supplierId",
                        onDelete = ForeignKey.SET_NULL
                )
        })
public class ExpenseEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;

    private Long blockId;

    // ✅ New fields for procurement tracking
    private Long batchId;        // Link to quality batch
    private Long supplierId;     // Link to supplier

    private ExpenseCategory category;

    private double amount;

    private long date;

    private String description;

    private String receiptPath;  // For future photo attachment

    // ✅ Denormalized fields for quick display
    private String batchNumber;     // Quick reference
    private String supplierName;    // Quick reference

    // Firestore document ID used for cloud synchronization
    private String firestoreId;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;

    public enum ExpenseCategory {
        // Existing categories
        LAND_CLEARING("Land Clearing", "35 hectares plantation", R.drawable.ic_construction),
        PLOWING("Plowing", "35 hectares", R.drawable.ic_grain),
        SEEDLINGS("Seedlings", "3,500 trees + replacements", R.drawable.ic_grass),
        LABOR("Labor", "Supervision & workers", R.drawable.ic_people),
        SECURITY("Security", "1 year guard service", R.drawable.ic_security),
        FENCING("Fencing", "Perimeter fence - 35 hectares", R.drawable.ic_fence),
        PROCESSING_CENTER("Processing Center", "15 hectares - separate", R.drawable.ic_factory),
        FERTILIZER("Fertilizer", null, R.drawable.ic_fertilizer),
        IRRIGATION("Irrigation", null, R.drawable.ic_water_drop),
        EQUIPMENT("Equipment", null, R.drawable.ic_construction),
        MAINTENANCE("Maintenance", null, R.drawable.ic_maintenance),

        // ✅ New procurement categories
        PROCUREMENT("Procurement", "Cashew procurement from suppliers", R.drawable.ic_shopping_cart),
        TRANSPORT("Transport", "Logistics and transportation", R.drawable.ic_local_shipping),
        QUALITY_TESTING("Quality Testing", "Lab testing and quality control", R.drawable.ic_science),
        STORAGE("Storage", "Warehouse and storage costs", R.drawable.ic_warehouse),

        OTHER("Other", null, R.drawable.ic_more_horiz);

        private final String displayName;
        private final String defaultDescription;
        private final int iconResId;

        ExpenseCategory(String displayName, String defaultDescription, int iconResId) {
            this.displayName = displayName;
            this.defaultDescription = defaultDescription;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public String getDefaultDescription() { return defaultDescription; }
        public int getIconResId() { return iconResId; }

        // ✅ Helper to check if category is procurement-related
        public boolean isProcurementCategory() {
            return this == PROCUREMENT ||
                    this == TRANSPORT ||
                    this == QUALITY_TESTING ||
                    this == STORAGE;
        }
    }

    // ✅ PRIMARY CONSTRUCTOR - Room will use this one
    public ExpenseEntity(long farmId, Long blockId, Long batchId, Long supplierId,
                         ExpenseCategory category, double amount, long date, String description) {
        this.farmId = farmId;
        this.blockId = blockId;
        this.batchId = batchId;
        this.supplierId = supplierId;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ✅ IGNORE this constructor - Room should not use it
    @Ignore
    public ExpenseEntity(long farmId, Long blockId, ExpenseCategory category,
                         double amount, long date, String description) {
        this(farmId, blockId, null, null, category, amount, date, description);
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) {
        this.batchId = batchId;
        this.updatedAt = System.currentTimeMillis();
    }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
        this.updatedAt = System.currentTimeMillis();
    }

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

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public boolean isFarmWide() {
        return blockId == null;
    }

    // ✅ Check if expense is linked to procurement
    public boolean isProcurementRelated() {
        return batchId != null || supplierId != null || category.isProcurementCategory();
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new java.util.Date(date));
    }

    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%,.0f XAF", amount);
    }

    // ✅ Get procurement info for display
    public String getProcurementInfo() {
        if (batchNumber != null && supplierName != null) {
            return "📦 Batch: " + batchNumber + " | 🏢 Supplier: " + supplierName;
        } else if (batchNumber != null) {
            return "📦 Batch: " + batchNumber;
        } else if (supplierName != null) {
            return "🏢 Supplier: " + supplierName;
        }
        return "—";
    }

    // ✅ Get summary for list display
    public String getSummary() {
        String summary = category.getDisplayName();
        if (isProcurementRelated()) {
            if (batchNumber != null) {
                summary += " (Batch: " + batchNumber + ")";
            } else if (supplierName != null) {
                summary += " (Supplier: " + supplierName + ")";
            }
        }
        return summary;
    }
}