package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.afrinuts.farmos.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Revenue records the sale of a batch — nothing about harvest.
 *
 * Harvest attribution is owned by BlockEntity (harvestHistory, written by
 * RecordHarvestDialog). Per-block P&L is a portal computation. Historical
 * harvest* fields and blockId have been removed by migration 40→41, Part A1:
 * they were junk writes populated by AddRevenueDialog and
 * AddRevenueFromTraceabilityDialog, not genuine harvest attribution. There is
 * no live non-harvest reader (the one candidate — ProfitAnalytics — was
 * orphaned code and is deleted in the same commit, closing the escape hatch).
 *
 * `createdAt` now doubles as the user-picked sale date (previously stored in
 * `harvestDate`). It is no longer "row insert time" — a semantic change that
 * is a portal-contract item (A6).
 */
@Entity(tableName = "revenues",
        foreignKeys = {
                @ForeignKey(
                        entity = FarmEntity.class,
                        parentColumns = "id",
                        childColumns = "farmId",
                        onDelete = ForeignKey.CASCADE
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
public class RevenueEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;

    /** Batch sold — the substance of a revenue record. */
    private Long batchId;

    /** Supplier that supplied the batch — denormalized FK. */
    private Long supplierId;

    private double quantityKg;
    private double pricePerKg;
    private double totalAmount;

    private String buyer;
    private QualityGrade quality;
    private String notes;

    /** Denormalized for display — no round-trip needed on list screens. */
    private String batchNumber;
    private String supplierName;
    /** Invoice number shared by every document generated for this sale. */
    private String invoiceNumber;

    /**
     * For newly-created records this is the sale date the user picked in
     * AddRevenueDialog (A1). For records migrated from v40 it is whatever
     * createdAt previously held. Both are acceptable — createdAt is now the
     * canonical revenue date.
     */
    private long createdAt;

    private long updatedAt;

    public enum QualityGrade {
        PREMIUM("Premium", "Premium Grade", R.drawable.ic_star),
        STANDARD("Standard", "Standard Grade", R.drawable.ic_check_circle),
        PROCESSING("Processing", "For processing", R.drawable.ic_factory);

        private final String displayName;
        private final String description;
        private final int iconResId;

        QualityGrade(String displayName, String description, int iconResId) {
            this.displayName = displayName;
            this.description = description;
            this.iconResId = iconResId;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getIconResId() { return iconResId; }
    }

    /**
     * Canonical constructor. `createdAt` doubles as the user-picked sale date
     * after A1 (previously stored in `harvestDate`). Pass 0 to default to the
     * current time.
     *
     * Parameter is named `createdAt` — not `saleDate` — because Room matches
     * @Entity constructor parameters to fields by name for the POJO check.
     * The semantic (sale date) is preserved; only the parameter name is
     * aligned to the field it populates.
     */
    public RevenueEntity(long farmId, Long batchId, Long supplierId,
                         long createdAt,
                         double quantityKg, double pricePerKg,
                         String buyer, QualityGrade quality, String notes) {
        this.farmId = farmId;
        this.batchId = batchId;
        this.supplierId = supplierId;
        this.quantityKg = quantityKg;
        this.pricePerKg = pricePerKg;
        this.totalAmount = quantityKg * pricePerKg;
        this.buyer = buyer;
        this.quality = quality;
        this.notes = notes;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and setters

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

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
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

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

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods

    public boolean isProcurementRelated() {
        return batchId != null || supplierId != null;
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

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new java.util.Date(createdAt));
    }

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
}
