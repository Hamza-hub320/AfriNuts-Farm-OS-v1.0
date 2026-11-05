package com.afrinuts.farmos.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "supplier_inspections",
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
                @Index(value = {"supplierId", "inspectionDate"}),
                @Index(value = {"validatedAt"}),
                @Index(value = {"isSynced"}),
                // Index used by the engine's critical-violation lookup
                @Index(value = {"outcome", "severity"})
        }
)
public class SupplierInspectionEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long supplierId;
    private long farmId;

    private InspectionType inspectionType;
    private long inspectionDate;

    private Outcome outcome;

    /** Nullable. Set only when outcome is FAIL or CONDITIONAL. */
    private String severity;        // enum name: MINOR | MAJOR | CRITICAL

    private String findings;

    /** Nullable. May be present on PASS (preventive) or on FAIL/CONDITIONAL. */
    private String correctiveAction;

    /** Nullable. Set when the corrective action is later closed. */
    private Long closedDate;

    private InspectionStatus status;   // OPEN | IN_PROGRESS | CLOSED | DISPUTED

    private String inspector;

    /**
     * JSON TEXT array of URL strings, same pattern as BlockEntity.harvestHistory.
     * Populated via the existing photo flow (step 6a).
     */
    private String evidenceUrls;

    /**
     * JSON TEXT array of long batch IDs. Optional — an inspection may concern
     * the supplier generally, or specific batches.
     */
    private String linkedBatchIds;

    // Validation / sync gate
    private boolean isSynced;
    private Long validatedAt;       // null = not yet validated (engine ignores)
    private String validatedBy;

    // Audit
    private String createdBy;
    private String updatedBy;
    private long createdAt;
    private long updatedAt;

    public enum InspectionType {
        FOOD_SAFETY, LABOR, ENVIRONMENTAL, DOCUMENTATION, ORGANIC, OTHER
    }

    public enum Outcome { PASS, FAIL, CONDITIONAL }

    public enum Severity { MINOR, MAJOR, CRITICAL }

    public enum InspectionStatus { OPEN, IN_PROGRESS, CLOSED, DISPUTED }

    public SupplierInspectionEntity(long supplierId, long farmId,
                                    InspectionType inspectionType, long inspectionDate,
                                    Outcome outcome, String findings, String inspector) {
        this.supplierId = supplierId;
        this.farmId = farmId;
        this.inspectionType = inspectionType;
        this.inspectionDate = inspectionDate;
        this.outcome = outcome;
        this.findings = findings;
        this.inspector = inspector;
        this.status = InspectionStatus.OPEN;
        this.evidenceUrls = "[]";
        this.linkedBatchIds = "[]";
        this.isSynced = false;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- evidenceUrls ergonomics ----
    @androidx.room.Ignore
    public java.util.List<String> getEvidenceUrlList() {
        return jsonList(evidenceUrls);
    }

    @androidx.room.Ignore
    public void setEvidenceUrlList(java.util.List<String> urls) {
        this.evidenceUrls = new com.google.gson.Gson().toJson(
                urls != null ? urls : new java.util.ArrayList<String>());
        this.updatedAt = System.currentTimeMillis();
    }

    // ---- linkedBatchIds ergonomics ----
    @androidx.room.Ignore
    public java.util.List<Long> getLinkedBatchIdList() {
        if (linkedBatchIds == null || linkedBatchIds.isEmpty()) return new java.util.ArrayList<>();
        try {
            java.lang.reflect.Type t =
                    new com.google.gson.reflect.TypeToken<java.util.List<Long>>(){}.getType();
            java.util.List<Long> parsed = new com.google.gson.Gson().fromJson(linkedBatchIds, t);
            return parsed != null ? parsed : new java.util.ArrayList<>();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    @androidx.room.Ignore
    public void setLinkedBatchIdList(java.util.List<Long> ids) {
        this.linkedBatchIds = new com.google.gson.Gson().toJson(
                ids != null ? ids : new java.util.ArrayList<Long>());
        this.updatedAt = System.currentTimeMillis();
    }

    private static java.util.List<String> jsonList(String json) {
        if (json == null || json.isEmpty()) return new java.util.ArrayList<>();
        try {
            java.lang.reflect.Type t =
                    new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType();
            java.util.List<String> parsed = new com.google.gson.Gson().fromJson(json, t);
            return parsed != null ? parsed : new java.util.ArrayList<>();
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    // ---- getters / setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSupplierId() { return supplierId; }
    public void setSupplierId(long supplierId) { this.supplierId = supplierId; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public InspectionType getInspectionType() { return inspectionType; }
    public void setInspectionType(InspectionType inspectionType) { this.inspectionType = inspectionType; }

    public long getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(long inspectionDate) { this.inspectionDate = inspectionDate; }

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public String getCorrectiveAction() { return correctiveAction; }
    public void setCorrectiveAction(String correctiveAction) { this.correctiveAction = correctiveAction; }

    public Long getClosedDate() { return closedDate; }
    public void setClosedDate(Long closedDate) { this.closedDate = closedDate; }

    public InspectionStatus getStatus() { return status; }
    public void setStatus(InspectionStatus status) { this.status = status; }

    public String getInspector() { return inspector; }
    public void setInspector(String inspector) { this.inspector = inspector; }

    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }

    public String getLinkedBatchIds() { return linkedBatchIds; }
    public void setLinkedBatchIds(String linkedBatchIds) { this.linkedBatchIds = linkedBatchIds; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public Long getValidatedAt() { return validatedAt; }
    public void setValidatedAt(Long validatedAt) { this.validatedAt = validatedAt; }

    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}