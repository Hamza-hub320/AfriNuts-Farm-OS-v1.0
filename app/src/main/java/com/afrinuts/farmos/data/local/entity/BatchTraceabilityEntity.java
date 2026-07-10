package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity(tableName = "batch_traceability",
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
                        childColumns = "qualityBatchId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = SupplierEntity.class,
                        parentColumns = "id",
                        childColumns = "supplierId",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = RevenueEntity.class,
                        parentColumns = "id",
                        childColumns = "harvestCycleId",
                        onDelete = ForeignKey.SET_NULL
                )
        })
public class BatchTraceabilityEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    // Core identifiers
    private String batchNumber;
    private long farmId;

    private Long harvestCycleId;          // FK → RevenueEntity
    private Long qualityBatchId;          // FK → QualityBatchEntity
    private Long supplierId;              // FK → SupplierEntity

    // Denormalized fields for quick display
    private String supplierName;
    private String qualityGrade;

    // Sustainability & Certification
    private String certificationIds;
    private String sustainabilityMetrics;

    // Date tracking
    private long processingDate;
    private Long exportDate;

    // ✅ ADD THIS FIELD
    private int harvestYear;               // Harvest year (e.g., 2026)

    // Shipment details (from Revenue)
    private String buyer;
    private String destination;
    private double weightKg;
    private String grade;
    private double pricePerKg;
    private double totalAmount;

    private String verificationCode;

    // Documents
    private String documents;

    // Traceability
    private String traceabilityUrl;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    // ========================================
    // Sustainability Metrics Inner Class
    // ========================================

    public static class SustainabilityMetrics {
        private boolean noChildLabor;
        private boolean farmOsMonitored;
        private boolean firebreakProtection;
        private boolean sustainableHarvesting;
        private long verifiedAt;
        private double qualityScore;

        public boolean isNoChildLabor() { return noChildLabor; }
        public void setNoChildLabor(boolean noChildLabor) { this.noChildLabor = noChildLabor; }

        public boolean isFarmOsMonitored() { return farmOsMonitored; }
        public void setFarmOsMonitored(boolean farmOsMonitored) { this.farmOsMonitored = farmOsMonitored; }

        public boolean isFirebreakProtection() { return firebreakProtection; }
        public void setFirebreakProtection(boolean firebreakProtection) { this.firebreakProtection = firebreakProtection; }

        public boolean isSustainableHarvesting() { return sustainableHarvesting; }
        public void setSustainableHarvesting(boolean sustainableHarvesting) { this.sustainableHarvesting = sustainableHarvesting; }

        public long getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(long verifiedAt) { this.verifiedAt = verifiedAt; }

        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

        public String toJson() {
            return new Gson().toJson(this);
        }

        public static SustainabilityMetrics fromJson(String json) {
            if (json == null || json.isEmpty()) return new SustainabilityMetrics();
            try {
                return new Gson().fromJson(json, SustainabilityMetrics.class);
            } catch (Exception e) {
                return new SustainabilityMetrics();
            }
        }

        public int getScore() {
            int score = 0;
            if (noChildLabor) score += 25;
            if (farmOsMonitored) score += 25;
            if (firebreakProtection) score += 25;
            if (sustainableHarvesting) score += 25;
            return score;
        }

        public String getFormattedVerifiedDate() {
            if (verifiedAt <= 0) return "Not verified";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new java.util.Date(verifiedAt));
        }
    }

    // ========================================
    // Documents Inner Class
    // ========================================

    public static class Documents {
        @SerializedName("qualityCertificate")
        private String qualityCertificate;      // Certificate of Origin

        @SerializedName("packingList")          // ✅ New dedicated field
        private String packingList;             // Packing List (Farm OS generated)

        @SerializedName("commercialInvoice")
        private String commercialInvoice;       // Commercial Invoice

        @SerializedName("phytoSanitary")
        private String phytoSanitary;           // Government issued

        @SerializedName("billOfLading")
        private String billOfLading;            // Shipping company issued

        @SerializedName("insuranceCertificate")
        private String insuranceCertificate;    // Insurance provider issued

        @SerializedName("additionalDocs")
        private List<String> additionalDocs;    // Any other documents

        // Getters and Setters
        public String getQualityCertificate() { return qualityCertificate; }
        public void setQualityCertificate(String qualityCertificate) {
            this.qualityCertificate = qualityCertificate;
        }

        public String getPackingList() { return packingList; }
        public void setPackingList(String packingList) {
            this.packingList = packingList;
        }

        public String getCommercialInvoice() { return commercialInvoice; }
        public void setCommercialInvoice(String commercialInvoice) {
            this.commercialInvoice = commercialInvoice;
        }

        public String getPhytoSanitary() { return phytoSanitary; }
        public void setPhytoSanitary(String phytoSanitary) {
            this.phytoSanitary = phytoSanitary;
        }

        public String getBillOfLading() { return billOfLading; }
        public void setBillOfLading(String billOfLading) {
            this.billOfLading = billOfLading;
        }

        public String getInsuranceCertificate() { return insuranceCertificate; }
        public void setInsuranceCertificate(String insuranceCertificate) {
            this.insuranceCertificate = insuranceCertificate;
        }

        public List<String> getAdditionalDocs() { return additionalDocs; }
        public void setAdditionalDocs(List<String> additionalDocs) {
            this.additionalDocs = additionalDocs;
        }

        public boolean hasDocuments() {
            return qualityCertificate != null ||
                    packingList != null ||
                    commercialInvoice != null ||
                    phytoSanitary != null ||
                    billOfLading != null ||
                    insuranceCertificate != null ||
                    (additionalDocs != null && !additionalDocs.isEmpty());
        }

        public String toJson() {
            return new Gson().toJson(this);
        }

        public static Documents fromJson(String json) {
            if (json == null || json.isEmpty()) return new Documents();
            try {
                return new Gson().fromJson(json, Documents.class);
            } catch (Exception e) {
                return new Documents();
            }
        }
    }

    // ========================================
    // Constructor
    // ========================================

    public BatchTraceabilityEntity(long farmId, String batchNumber, long processingDate,
                                   double weightKg, String grade, double pricePerKg) {
        this.farmId = farmId;
        this.batchNumber = batchNumber;
        this.processingDate = processingDate;
        this.weightKg = weightKg;
        this.grade = grade;
        this.pricePerKg = pricePerKg;
        this.totalAmount = weightKg * pricePerKg;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
        this.isSynced = false;

        // Generate traceability URL
        this.traceabilityUrl = generateTraceabilityUrl(batchNumber);
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public Long getHarvestCycleId() { return harvestCycleId; }
    public void setHarvestCycleId(Long harvestCycleId) { this.harvestCycleId = harvestCycleId; }

    public Long getQualityBatchId() { return qualityBatchId; }
    public void setQualityBatchId(Long qualityBatchId) { this.qualityBatchId = qualityBatchId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

    public String getCertificationIds() { return certificationIds; }
    public void setCertificationIds(String certificationIds) { this.certificationIds = certificationIds; }

    public String getSustainabilityMetrics() { return sustainabilityMetrics; }
    public void setSustainabilityMetrics(String sustainabilityMetrics) {
        this.sustainabilityMetrics = sustainabilityMetrics;
    }

    public SustainabilityMetrics getSustainabilityMetricsObject() {
        return SustainabilityMetrics.fromJson(sustainabilityMetrics);
    }

    public void setSustainabilityMetricsObject(SustainabilityMetrics metrics) {
        this.sustainabilityMetrics = metrics.toJson();
    }

    public long getProcessingDate() { return processingDate; }
    public void setProcessingDate(long processingDate) { this.processingDate = processingDate; }

    public Long getExportDate() { return exportDate; }
    public void setExportDate(Long exportDate) { this.exportDate = exportDate; }

    // ✅ GETTER AND SETTER FOR HARVEST YEAR
    public int getHarvestYear() { return harvestYear; }
    public void setHarvestYear(int harvestYear) { this.harvestYear = harvestYear; }

    public String getBuyer() { return buyer; }
    public void setBuyer(String buyer) { this.buyer = buyer; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
        this.totalAmount = weightKg * this.pricePerKg;
    }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(double pricePerKg) {
        this.pricePerKg = pricePerKg;
        this.totalAmount = this.weightKg * pricePerKg;
    }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getDocuments() { return documents; }
    public void setDocuments(String documents) { this.documents = documents; }

    public String getTraceabilityUrl() { return traceabilityUrl; }
    public void setTraceabilityUrl(String traceabilityUrl) { this.traceabilityUrl = traceabilityUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    // ========================================
    // Helper Methods
    // ========================================

    public String getFormattedProcessingDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new java.util.Date(processingDate));
    }

    public String getFormattedExportDate() {
        if (exportDate == null || exportDate <= 0) return "Not exported";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new java.util.Date(exportDate));
    }

    public String getFormattedTotal() {
        return String.format(Locale.getDefault(), "%,.0f XAF", totalAmount);
    }

    public String getFormattedWeight() {
        return String.format(Locale.getDefault(), "%,.0f kg", weightKg);
    }

    public String getFormattedPrice() {
        return String.format(Locale.getDefault(), "%,.0f XAF/kg", pricePerKg);
    }

    public Documents getDocumentsObject() {
        return Documents.fromJson(documents);
    }

    public void setDocumentsObject(Documents docs) {
        this.documents = docs.toJson();
    }

    public boolean hasDocuments() {
        return getDocumentsObject().hasDocuments();
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String generateTraceabilityUrl(String batchNumber) {
        return "https://www.afrinutsexport.com/trace/" + batchNumber;
    }

    public String getQrCodeUrl() {
        if (traceabilityUrl != null && !traceabilityUrl.isEmpty()) {
            return traceabilityUrl;
        }
        return generateTraceabilityUrl(batchNumber);
    }

    public boolean isExported() {
        return exportDate != null && exportDate > 0;
    }

    public boolean isVerified() {
        SustainabilityMetrics metrics = getSustainabilityMetricsObject();
        return metrics != null && metrics.getVerifiedAt() > 0;
    }

    // ✅ Extract sustainability metrics from QualityBatchEntity
    public static SustainabilityMetrics extractSustainabilityMetrics(QualityBatchEntity batch) {
        SustainabilityMetrics metrics = new SustainabilityMetrics();
        if (batch != null) {
            metrics.setNoChildLabor(batch.isNoChildLabor());
            metrics.setFarmOsMonitored(batch.isFarmOsMonitored());
            metrics.setFirebreakProtection(batch.isFirebreakProtection());
            metrics.setSustainableHarvesting(batch.isSustainableHarvesting());
            metrics.setVerifiedAt(batch.getVerifiedAt());
            metrics.setQualityScore(batch.getQualityScore());
        }
        return metrics;
    }
}