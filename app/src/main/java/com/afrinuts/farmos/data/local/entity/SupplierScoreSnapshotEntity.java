package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Append-only. One row per engine calculation. A snapshot is NEVER updated
 * in place — a correction produces a NEW snapshot, and the dispute links to
 * it via recalculatedSnapshotId.
 */
@Entity(
        tableName = "supplier_score_history",
        foreignKeys = {
                @ForeignKey(
                        entity = FarmEntity.class,
                        parentColumns = "id",
                        childColumns = "farmId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = SupplierEntity.class,
                        parentColumns = "id",
                        childColumns = "supplierId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"supplierId", "calculatedAt"}),
                @Index(value = {"scoringVersion"})
        }
)
public class SupplierScoreSnapshotEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long supplierId;
    private long farmId;

    /**
     * Nullable: stays null until quality, delivery, and traceability all
     * have evidence. No silent renormalisation — a partial score is not a score.
     */
    private Double overall;

    // Six dimensions, all nullable (missing dimension = null, never 0)
    private Double quality;
    private Double delivery;
    private Double traceability;
    private Double compliance;
    private Double sustainability;
    private Double cooperation;

    private Confidence confidence;   // PRELIMINARY | DEVELOPING | ESTABLISHED
    private int verifiedTransactions;

    private String scoringVersion;   // e.g. "v1.0-2026-11-05"

    /** JSON TEXT object of the weights the engine used, e.g. {"quality":0.25,...}. */
    private String weightsUsed;

    private long calculatedAt;
    private String calculatedBy;     // "engine" for automatic runs, user id for manual

    /**
     * JSON TEXT array of evidence identifiers used in this calculation
     * (commitment IDs, inspection IDs, batch IDs). Reproducibility requirement.
     */
    private String evidenceRefs;

    // Audit
    private long createdAt;

    public enum Confidence { PRELIMINARY, DEVELOPING, ESTABLISHED }

    public SupplierScoreSnapshotEntity(long supplierId, long farmId,
                                       String scoringVersion,
                                       String calculatedBy) {
        this.supplierId = supplierId;
        this.farmId = farmId;
        this.scoringVersion = scoringVersion;
        this.calculatedBy = calculatedBy;
        this.calculatedAt = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.verifiedTransactions = 0;
        this.confidence = Confidence.PRELIMINARY;
        this.weightsUsed = "{}";
        this.evidenceRefs = "[]";
    }

    // ---- weightsUsed ergonomics (JSON TEXT) ----

    @Ignore
    public java.util.Map<String, Double> getWeightsMap() {
        if (weightsUsed == null || weightsUsed.isEmpty()) return new java.util.HashMap<>();
        try {
            Type t = new TypeToken<java.util.Map<String, Double>>(){}.getType();
            java.util.Map<String, Double> parsed = new Gson().fromJson(weightsUsed, t);
            return parsed != null ? parsed : new java.util.HashMap<>();
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    @Ignore
    public void setWeightsMap(java.util.Map<String, Double> weights) {
        this.weightsUsed = new Gson().toJson(
                weights != null ? weights : new java.util.HashMap<String, Double>());
    }

    // ---- evidenceRefs ergonomics (JSON TEXT) ----

    @Ignore
    public List<String> getEvidenceRefList() {
        if (evidenceRefs == null || evidenceRefs.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<String>>(){}.getType();
            List<String> parsed = new Gson().fromJson(evidenceRefs, t);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Ignore
    public void setEvidenceRefList(List<String> refs) {
        this.evidenceRefs = new Gson().toJson(
                refs != null ? refs : new ArrayList<String>());
    }

    // ---- getters / setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSupplierId() { return supplierId; }
    public void setSupplierId(long supplierId) { this.supplierId = supplierId; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public Double getOverall() { return overall; }
    public void setOverall(Double overall) { this.overall = overall; }

    public Double getQuality() { return quality; }
    public void setQuality(Double quality) { this.quality = quality; }

    public Double getDelivery() { return delivery; }
    public void setDelivery(Double delivery) { this.delivery = delivery; }

    public Double getTraceability() { return traceability; }
    public void setTraceability(Double traceability) { this.traceability = traceability; }

    public Double getCompliance() { return compliance; }
    public void setCompliance(Double compliance) { this.compliance = compliance; }

    public Double getSustainability() { return sustainability; }
    public void setSustainability(Double sustainability) { this.sustainability = sustainability; }

    public Double getCooperation() { return cooperation; }
    public void setCooperation(Double cooperation) { this.cooperation = cooperation; }

    public Confidence getConfidence() { return confidence; }
    public void setConfidence(Confidence confidence) { this.confidence = confidence; }

    public int getVerifiedTransactions() { return verifiedTransactions; }
    public void setVerifiedTransactions(int verifiedTransactions) { this.verifiedTransactions = verifiedTransactions; }

    public String getScoringVersion() { return scoringVersion; }
    public void setScoringVersion(String scoringVersion) { this.scoringVersion = scoringVersion; }

    public String getWeightsUsed() { return weightsUsed; }
    public void setWeightsUsed(String weightsUsed) { this.weightsUsed = weightsUsed; }

    public long getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(long calculatedAt) { this.calculatedAt = calculatedAt; }

    public String getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(String calculatedBy) { this.calculatedBy = calculatedBy; }

    public String getEvidenceRefs() { return evidenceRefs; }
    public void setEvidenceRefs(String evidenceRefs) { this.evidenceRefs = evidenceRefs; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}