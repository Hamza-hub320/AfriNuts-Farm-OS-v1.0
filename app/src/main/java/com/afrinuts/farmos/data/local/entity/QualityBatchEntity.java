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
                        entity = RevenueEntity.class,
                        parentColumns = "id",
                        childColumns = "harvestCycleId",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = SupplierEntity.class,
                        parentColumns = "id",
                        childColumns = "supplierId",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = SupplierEntity.class,
                        parentColumns = "id",
                        childColumns = "supplierId",
                        onDelete = ForeignKey.CASCADE
                )
        })
public class QualityBatchEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long farmId;

    // Source tracking
    private Long blockId;           // NULL = not from AfriNuts farm block
    private Long harvestCycleId;     // Reference to RevenueEntity (AfriNuts harvest)
    private Long supplierId;         // Reference to SupplierEntity (external source)

    private transient String supplierName;

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
        PROCESSING("Processing", "For processing, higher defects");

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
    public QualityBatchEntity(long farmId, Long blockId, Long harvestCycleId, Long supplierId,
                              SourceType sourceType, String batchNumber, long processingDate,
                              double moistureContent, int nutCountPerKg, double defectiveRate,
                              double kernelRatio, double weightKg, QualityGrade qualityGrade,
                              String notes) {
        this.farmId = farmId;
        this.blockId = blockId;
        this.harvestCycleId = harvestCycleId;
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
        this.qualityScore = calculateQualityScore();

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
            case PROCESSING: score -= 10; break;
        }

        return Math.max(0, Math.min(100, score));
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public Long getBlockId() { return blockId; }
    public void setBlockId(Long blockId) { this.blockId = blockId; }

    public Long getHarvestCycleId() { return harvestCycleId; }
    public void setHarvestCycleId(Long harvestCycleId) { this.harvestCycleId = harvestCycleId; }

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

    // Helper method
    public boolean isVerified() {
        return verifiedAt > 0;
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
    public void setMoistureContent(double moistureContent) {
        this.moistureContent = moistureContent;
        this.qualityScore = calculateQualityScore();
    }

    public int getNutCountPerKg() { return nutCountPerKg; }
    public void setNutCountPerKg(int nutCountPerKg) {
        this.nutCountPerKg = nutCountPerKg;
        this.qualityScore = calculateQualityScore();
    }

    public double getDefectiveRate() { return defectiveRate; }
    public void setDefectiveRate(double defectiveRate) {
        this.defectiveRate = defectiveRate;
        this.qualityScore = calculateQualityScore();
    }

    public double getKernelRatio() { return kernelRatio; }
    public void setKernelRatio(double kernelRatio) {
        this.kernelRatio = kernelRatio;
        this.qualityScore = calculateQualityScore();
    }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public QualityGrade getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(QualityGrade qualityGrade) {
        this.qualityGrade = qualityGrade;
        this.qualityScore = calculateQualityScore();
    }

    public String getTestResults() { return testResults; }
    public void setTestResults(String testResults) { this.testResults = testResults; }

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