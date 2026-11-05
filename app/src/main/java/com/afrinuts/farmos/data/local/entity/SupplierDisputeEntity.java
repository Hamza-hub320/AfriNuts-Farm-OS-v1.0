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

@Entity(
        tableName = "supplier_disputes",
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
                @Index(value = {"supplierId", "submittedDate"}),
                @Index(value = {"targetEntity", "targetId"}),
                @Index(value = {"reviewStatus"})
        }
)
public class SupplierDisputeEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long supplierId;
    private long farmId;

    private TargetEntity targetEntity;   // COMMITMENT | INSPECTION | BATCH | SCORE
    private long targetId;               // long in Room (Decision 1); Firestore may carry string for SCORE

    private String reason;

    /** JSON TEXT array of URL strings, matching BlockEntity.harvestHistory pattern. */
    private String evidenceUrls;

    private String submittedBy;
    private long submittedDate;

    private ReviewStatus reviewStatus;   // OPEN | UNDER_REVIEW | RESOLVED | REJECTED

    /** Only settable via recordResolution(); nullable until resolution. */
    private String decision;
    private String reviewedBy;
    private Long resolvedDate;

    /** True when the resolution produced a data correction (not just a decision). */
    private boolean correctionApplied;

    /** What the record said before / after the correction — audit trail, not just a flag. */
    private String originalValue;
    private String correctedValue;

    /** When targetEntity = SCORE and a correction produced a new snapshot, the new snapshot ID. */
    private Long recalculatedSnapshotId;

    // Sync gate — disputes are audit records; they sync but never carry validatedAt
    private boolean isSynced;

    // Audit
    private String createdBy;
    private String updatedBy;
    private long createdAt;
    private long updatedAt;

    public enum TargetEntity { COMMITMENT, INSPECTION, BATCH, SCORE }

    public enum ReviewStatus { OPEN, UNDER_REVIEW, RESOLVED, REJECTED }

    public SupplierDisputeEntity(long supplierId, long farmId,
                                 TargetEntity targetEntity, long targetId,
                                 String reason, String submittedBy) {
        this.supplierId = supplierId;
        this.farmId = farmId;
        this.targetEntity = targetEntity;
        this.targetId = targetId;
        this.reason = reason;
        this.submittedBy = submittedBy;
        this.submittedDate = System.currentTimeMillis();
        this.reviewStatus = ReviewStatus.OPEN;
        this.evidenceUrls = "[]";
        this.correctionApplied = false;
        this.isSynced = false;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- evidenceUrls ergonomics (JSON TEXT) ----

    @Ignore
    public List<String> getEvidenceUrlList() {
        if (evidenceUrls == null || evidenceUrls.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<String>>(){}.getType();
            List<String> parsed = new Gson().fromJson(evidenceUrls, t);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Ignore
    public void setEvidenceUrlList(List<String> urls) {
        this.evidenceUrls = new Gson().toJson(
                urls != null ? urls : new ArrayList<String>());
        this.updatedAt = System.currentTimeMillis();
    }

    // ---- getters / setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSupplierId() { return supplierId; }
    public void setSupplierId(long supplierId) { this.supplierId = supplierId; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public TargetEntity getTargetEntity() { return targetEntity; }
    public void setTargetEntity(TargetEntity targetEntity) { this.targetEntity = targetEntity; }

    public long getTargetId() { return targetId; }
    public void setTargetId(long targetId) { this.targetId = targetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public long getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(long submittedDate) { this.submittedDate = submittedDate; }

    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public Long getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(Long resolvedDate) { this.resolvedDate = resolvedDate; }

    public boolean isCorrectionApplied() { return correctionApplied; }
    public void setCorrectionApplied(boolean correctionApplied) { this.correctionApplied = correctionApplied; }

    public String getOriginalValue() { return originalValue; }
    public void setOriginalValue(String originalValue) { this.originalValue = originalValue; }

    public String getCorrectedValue() { return correctedValue; }
    public void setCorrectedValue(String correctedValue) { this.correctedValue = correctedValue; }

    public Long getRecalculatedSnapshotId() { return recalculatedSnapshotId; }
    public void setRecalculatedSnapshotId(Long recalculatedSnapshotId) {
        this.recalculatedSnapshotId = recalculatedSnapshotId;
    }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}