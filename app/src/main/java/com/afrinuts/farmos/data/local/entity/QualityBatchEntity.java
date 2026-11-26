package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Locale;

@Entity(tableName = "quality_batches",
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
                        entity = SupplierEntity.class,
                        parentColumns = "id",
                        childColumns = "supplierId",
                        onDelete = ForeignKey.SET_NULL
                )
        })
public class QualityBatchEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;

    // Source tracking
    private Long blockId;
    private Long supplierId;

    private String verificationCode;

    private transient String supplierName;
    private transient String supplierCooperativeName;

    // Batch identification
    private String batchNumber;
    private long processingDate;

    // Quality metrics
    private double moistureContent;
    private int nutCountPerKg;
    private double defectiveRate;
    private double kernelRatio;
    private double weightKg;

    private QualityGrade qualityGrade;
    private String testResults;
    private double qualityScore;
    private String notes;

    // Source type indicator
    private SourceType sourceType;

    private boolean noChildLabor;
    private boolean farmOsMonitored;
    private boolean firebreakProtection;
    private boolean sustainableHarvesting;
    private long verifiedAt;

    // Audit timestamps
    private long createdAt;
    private long updatedAt;
    private boolean isSynced;

    // "RCN" or "KERNEL". Defaults to "KERNEL" for legacy rows (see migration).
    private String productType;

    // ✅ Step 1: RCN-only fields (null/0 for KERNEL batches)
    private double mouldyRate;           // %
    private double immatureRate;         // %
    private double insectDamagedRate;    // %
    private double shrivelledRate;       // %
    private double foreignMatter;        // %
    private String dryingMethod;         // "SUN" / "MECHANICAL" / "UNKNOWN"
    private double rcnQualityScore;      // %  (computed in Step 2; stays 0 for now)

    // ✅ Step 1: Kernel-only fields (0 for RCN batches)
    private double brokenKernels;        // %
    private double aflatoxinPpb;         // ppb
    private double heavyMetalsPpb;       // ppb
    private double pesticideResiduePpb;  // ppb
    private double kernelQualityScore;   // %  (computed in Step 2; stays 0 for now)
    // Typical Ivorian RCN: 249–278 g/kg. Convert to lbs/bag (÷ 5.67) for display only.
    private double korGPerKg;
    // If the sample is exactly 1000 g, these are redundant with korGPerKg.
    // If the sample is 950 g, korGPerKg = korKernelGrams / korSampleGrams * 1000.
    private double korSampleGrams;
    private double korKernelGrams;
    private long inspectionDate;        // ✅ RCN inspection timestamp (Phase One)
    private String inspectedBy;         // ✅ RCN inspector name

    public enum SourceType {
        OWN_FARM("Own Farm", "AfriNuts-owned farm, organic certified"),
        COOPERATIVE("Cooperative", "Cooperative farmer network"),
        INDEPENDENT("Independent", "Independent farmer"),
        REGIONAL_SUPPLIER("Regional Supplier", "Regional commercial aggregator"),
        PROCESSING_CENTER("Processing Center", "In-house processing facility");

        private final String displayName;
        private final String description;

        SourceType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public boolean isOrganicStream() {
            return this == OWN_FARM;
        }

        public boolean isConventionalStream() {
            return this == COOPERATIVE || this == INDEPENDENT || this == REGIONAL_SUPPLIER;
        }
    }

    public enum QualityGrade {
        PREMIUM("Premium", "Best quality, low defect rate"),
        STANDARD("Standard", "Good quality, acceptable defects"),
        REJECT("Reject", "Below acceptable thresholds — excluded from sale");

        private final String displayName;
        private final String description;

        QualityGrade(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // Test results
    public static class TestResults {
        private double aflatoxin;
        private double pesticideResidue;
        private double heavyMetals;

        public TestResults() {
            this.aflatoxin = 0;
            this.pesticideResidue = 0;
            this.heavyMetals = 0;
        }

        public TestResults(double aflatoxin, double pesticideResidue, double heavyMetals) {
            this.aflatoxin = aflatoxin;
            this.pesticideResidue = pesticideResidue;
            this.heavyMetals = heavyMetals;
        }

        public double getAflatoxin() { return aflatoxin; }
        public double getPesticideResidue() { return pesticideResidue; }
        public double getHeavyMetals() { return heavyMetals; }

        public void setAflatoxin(double aflatoxin) { this.aflatoxin = aflatoxin; }
        public void setPesticideResidue(double pesticideResidue) { this.pesticideResidue = pesticideResidue; }
        public void setHeavyMetals(double heavyMetals) { this.heavyMetals = heavyMetals; }

        public String toJson() {
            return new Gson().toJson(this);
        }

        public static TestResults fromJson(String json) {
            if (json == null || json.isEmpty()) return new TestResults();
            try {
                return new Gson().fromJson(json, TestResults.class);
            } catch (Exception e) {
                return new TestResults();
            }
        }

        public boolean hasResults() {
            return aflatoxin > 0 || pesticideResidue > 0 || heavyMetals > 0;
        }
    }

    // Constructor
    public QualityBatchEntity(long farmId, Long blockId, Long supplierId,
                              SourceType sourceType, String batchNumber,
                              long processingDate, double moistureContent,
                              int nutCountPerKg, double defectiveRate,
                              double kernelRatio, double weightKg,
                              QualityGrade qualityGrade, String notes) {
        this.farmId = farmId;
        this.blockId = blockId;
        this.supplierId = supplierId;
        this.sourceType = sourceType;
        this.batchNumber = batchNumber;
        this.processingDate = processingDate;
        this.moistureContent = moistureContent;
        this.nutCountPerKg = nutCountPerKg;
        this.defectiveRate = defectiveRate;
        this.kernelRatio = kernelRatio;
        this.weightKg = weightKg;
        this.qualityGrade = qualityGrade;
        this.notes = notes;
        this.isSynced = false;
        this.qualityScore = 0;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    private double calculateQualityScore() {
        double score = 100.0;

        if (moistureContent < 5 || moistureContent > 7) {
            score -= Math.abs(moistureContent - 6) * 2;
        }
        score -= defectiveRate * 0.5;
        if (kernelRatio < 85) {
            score -= (85 - kernelRatio) * 0.5;
        }
        if (nutCountPerKg > 200) score += 2;

        switch (qualityGrade) {
            case PREMIUM: score += 5; break;
            case STANDARD: score += 0; break;
            case REJECT: score -= 10; break;
        }

        return Math.max(0, Math.min(100, score));
    }

    // Getters and Setters
    public String getProductType() { return productType; }
    public void setProductType(String productType) {
        this.productType = productType;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(long inspectionDate) { this.inspectionDate = inspectionDate; }

    public String getInspectedBy() { return inspectedBy; }
    public void setInspectedBy(String inspectedBy) { this.inspectedBy = inspectedBy; }

    public double getMouldyRate() { return mouldyRate; }
    public void setMouldyRate(double mouldyRate) { this.mouldyRate = mouldyRate; }

    public double getImmatureRate() { return immatureRate; }
    public void setImmatureRate(double immatureRate) { this.immatureRate = immatureRate; }

    public double getInsectDamagedRate() { return insectDamagedRate; }
    public void setInsectDamagedRate(double insectDamagedRate) { this.insectDamagedRate = insectDamagedRate; }

    public double getShrivelledRate() { return shrivelledRate; }
    public void setShrivelledRate(double shrivelledRate) { this.shrivelledRate = shrivelledRate; }

    public double getForeignMatter() { return foreignMatter; }
    public void setForeignMatter(double foreignMatter) { this.foreignMatter = foreignMatter; }

    public String getDryingMethod() { return dryingMethod; }
    public void setDryingMethod(String dryingMethod) { this.dryingMethod = dryingMethod; }

    public double getRcnQualityScore() { return rcnQualityScore; }
    public void setRcnQualityScore(double rcnQualityScore) { this.rcnQualityScore = rcnQualityScore; }

    public double getBrokenKernels() { return brokenKernels; }
    public void setBrokenKernels(double brokenKernels) { this.brokenKernels = brokenKernels; }

    public double getAflatoxinPpb() { return aflatoxinPpb; }
    public void setAflatoxinPpb(double aflatoxinPpb) { this.aflatoxinPpb = aflatoxinPpb; }

    public double getHeavyMetalsPpb() { return heavyMetalsPpb; }
    public void setHeavyMetalsPpb(double heavyMetalsPpb) { this.heavyMetalsPpb = heavyMetalsPpb; }

    public double getPesticideResiduePpb() { return pesticideResiduePpb; }
    public void setPesticideResiduePpb(double pesticideResiduePpb) { this.pesticideResiduePpb = pesticideResiduePpb; }

    public double getKernelQualityScore() { return kernelQualityScore; }
    public void setKernelQualityScore(double kernelQualityScore) { this.kernelQualityScore = kernelQualityScore; }

    /** The most relevant date for this batch: inspection date for RCN, processing date for kernel. */
    public long getRelevantDate() {
        if (isRcn()) return inspectionDate > 0 ? inspectionDate : processingDate;
        return processingDate;
    }


    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public long getProcessingDate() { return processingDate; }
    public void setProcessingDate(long processingDate) { this.processingDate = processingDate; }

    public double getMoistureContent() { return moistureContent; }

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

    public double getKorGPerKg() { return korGPerKg; }
    public void setKorGPerKg(double korGPerKg) { this.korGPerKg = korGPerKg; }

    public double getKorSampleGrams() { return korSampleGrams; }
    public void setKorSampleGrams(double korSampleGrams) { this.korSampleGrams = korSampleGrams; }

    public double getKorKernelGrams() { return korKernelGrams; }
    public void setKorKernelGrams(double korKernelGrams) { this.korKernelGrams = korKernelGrams; }

    // ✅ Derived displays — never stored
    public double getKorLbsPerBag() {
        return korGPerKg / 5.67;  // 1 lb = 453.6 g, 80 kg bag = 80,000 g, so 80,000/453.6 = 176.37 kernels per lb; but 1 lb per bag convention: kg per 80kg = g/kg * 0.08; in lbs = g/kg * 0.08 * 2.205 = g/kg / 5.67
    }
    public double getKorAsPercent() {
        return korGPerKg / 10.0;  // g/kg ÷ 10 = percent (since 1 kg = 1000 g, % = g/kg ÷ 10)
    }

    // ✅ Backward-compat alias — returns g/kg, not the old lbs value
    public double getKorPercent() { return korGPerKg; }  // deprecated

    public double getKor() {
        return isRcn() ? korGPerKg : kernelRatio;
    }

    // Helper method
// ✅ UI-facing alias: kernelRatio is the KOR. No new stored field, no dual-write.
    public boolean isRcn() { return "RCN".equalsIgnoreCase(productType); }

    public boolean isKernel() { return "KERNEL".equalsIgnoreCase(productType); }

    public boolean isVerified() {
        return verifiedAt > 0;
    }

    /**
     * True when the batch has a real inspection: a KOR measurement and an
     * inspection timestamp. Placeholder grades (STANDARD with no measurements)
     * are NOT inspected.
     */
    public boolean isInspected() {
        return inspectionDate > 0 && korGPerKg > 0;
    }

    /**
     * SOP §4.5 + §4.6: only inspected, non-rejected batches may be sold or shipped.
     * A fresh delivery (STANDARD placeholder, no measurements) is not saleable.
     * A REJECT is not saleable.
     */
    public boolean isSaleable() {
        if (!isInspected()) return false;
        if (qualityGrade == QualityBatchEntity.QualityGrade.REJECT) return false;
        return true;
    }

    /** Human-readable reason why the batch cannot be sold, or null if it can. */
    public String getSaleBlockReason() {
        if (!isInspected()) {
            return "Batch has not been inspected. Record an RCN quality check first (SOP §4.5).";
        }
        if (qualityGrade == QualityBatchEntity.QualityGrade.REJECT) {
            return "Batch is REJECTED — excluded from sale (SOP §4.6).";
        }
        return null;
    }

    public String getFormattedVerifiedDate() {
        if (!isVerified()) return "Not verified";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new java.util.Date(verifiedAt));
    }

    public int getSustainabilityScore() {
        int score = 0;
        if (noChildLabor) score += 25;
        if (farmOsMonitored) score += 25;
        if (firebreakProtection) score += 25;
        if (sustainableHarvesting) score += 25;
        return score;
    }
    public void setMoistureContent(double moistureContent) { this.moistureContent = moistureContent; }

    public int getNutCountPerKg() { return nutCountPerKg; }
    public void setNutCountPerKg(int nutCountPerKg) { this.nutCountPerKg = nutCountPerKg; }

    public double getDefectiveRate() { return defectiveRate; }
    public void setDefectiveRate(double defectiveRate) { this.defectiveRate = defectiveRate; }

    public double getKernelRatio() { return kernelRatio; }
    public void setKernelRatio(double kernelRatio) { this.kernelRatio = kernelRatio; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public QualityGrade getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(QualityGrade qualityGrade) { this.qualityGrade = qualityGrade; }

    public String getTestResults() { return testResults; }
    public void setTestResults(String testResults) { this.testResults = testResults; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public double getQualityScore() { return qualityScore; }
    public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getSupplierCooperativeName() { return supplierCooperativeName; }
    public void setSupplierCooperativeName(String supplierCooperativeName) { this.supplierCooperativeName = supplierCooperativeName; }

    // Helper methods
    public String getFormattedBatchNumber() { return "#" + batchNumber; }

    public String getSourceDisplay() {
        return sourceType.getDisplayName();
    }

    public String getSourceDetail() {
        if (sourceType == SourceType.OWN_FARM && blockId != null) {
            return "Block " + blockId;
        }
        if (sourceType.isConventionalStream()) {
            return "External Supplier";
        }
        return sourceType.getDisplayName();
    }

    public boolean isOrganicStream() {
        return sourceType == SourceType.OWN_FARM;
    }

    public boolean isConventionalStream() {
        return sourceType.isConventionalStream();
    }

    public String getFormattedDate() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(processingDate));
    }
}
