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
        tableName = "supplier_commitments",
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
                @Index(value = {"supplierId", "season"}),
                @Index(value = {"validatedAt"}),
                @Index(value = {"isSynced"})
        }
)
public class SupplierCommitmentEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long supplierId;
    private long farmId;
    private String contractId;          // free string business key, e.g. "CNT-2026-014"
    private String season;              // e.g. "2026A"

    private ProductType productType;    // RCN | KERNEL (KERNEL unused in Phase One)

    private double committedKg;
    private long committedDate;         // currently effective agreed date (mirrors last revision)

    /**
     * JSON TEXT array of long batch IDs, matching the BlockEntity.harvestHistory
     * pattern. deliveredKg is NOT stored — derive via getDeliveredKg(dao).
     */
    private String linkedBatchIds;

    private CommitmentStatus status;
    private String delayReason;         // enum name, nullable
    private boolean buyerRequestedChange;

    /**
     * Append-only revision log (JSON TEXT). Never overwritten; only appendRevision()
     * is allowed to write it. getEffectiveCommittedDate() reads the committedDate column.
     */
    private String revisions;

    // Validation / sync gate
    private boolean isSynced;
    private Long validatedAt;           // null = not yet validated (engine ignores)
    private String validatedBy;

    // Audit
    private String createdBy;
    private String updatedBy;
    private long createdAt;
    private long updatedAt;

    public enum ProductType { RCN, KERNEL }

    public enum CommitmentStatus {
        PENDING, FULFILLED, PARTIAL, MISSED, REVISED, CANCELLED
    }

    public enum DelayReason {
        SUPPLIER, WEATHER, BUYER, LOGISTICS, OTHER
    }

    /** Append-only revision record. Internal shape; not a Room entity. */
    public static class Revision {
        public long revisedDate;
        public String revisedReason;
        public String revisedBy;
        public long revisedAt;

        public Revision() { }

        public Revision(long revisedDate, String revisedReason, String revisedBy, long revisedAt) {
            this.revisedDate = revisedDate;
            this.revisedReason = revisedReason;
            this.revisedBy = revisedBy;
            this.revisedAt = revisedAt;
        }
    }

    public SupplierCommitmentEntity(long supplierId, long farmId, String contractId,
                                    String season, ProductType productType,
                                    double committedKg, long committedDate) {
        this.supplierId = supplierId;
        this.farmId = farmId;
        this.contractId = contractId;
        this.season = season;
        this.productType = productType;
        this.committedKg = committedKg;
        this.committedDate = committedDate;
        this.status = CommitmentStatus.PENDING;
        this.buyerRequestedChange = false;
        this.linkedBatchIds = "[]";
        this.revisions = "[]";
        this.isSynced = false;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ---- linkedBatchIds ergonomics (JSON TEXT) ----

    @Ignore
    public List<Long> getLinkedBatchIdList() {
        if (linkedBatchIds == null || linkedBatchIds.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<Long>>(){}.getType();
            List<Long> parsed = new Gson().fromJson(linkedBatchIds, t);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Ignore
    public void setLinkedBatchIdList(List<Long> ids) {
        this.linkedBatchIds = new Gson().toJson(ids != null ? ids : new ArrayList<Long>());
        this.updatedAt = System.currentTimeMillis();
    }

    // ---- revisions (append-only) ----

    @Ignore
    public List<Revision> getRevisionList() {
        if (revisions == null || revisions.isEmpty()) return new ArrayList<>();
        try {
            Type t = new TypeToken<List<Revision>>(){}.getType();
            List<Revision> parsed = new Gson().fromJson(revisions, t);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * The ONLY writer of the revisions field. A revision never overwrites history:
     * it is appended, committedDate is moved forward, and status becomes REVISED
     * if it was not already terminal.
     */
    public void appendRevision(long newCommittedDate, String reason, String revisedBy) {
        List<Revision> list = getRevisionList();
        list.add(new Revision(newCommittedDate, reason, revisedBy, System.currentTimeMillis()));
        this.revisions = new Gson().toJson(list);
        this.committedDate = newCommittedDate;
        if (status != CommitmentStatus.CANCELLED
                && status != CommitmentStatus.FULFILLED
                && status != CommitmentStatus.MISSED) {
            this.status = CommitmentStatus.REVISED;
        }
        this.updatedAt = System.currentTimeMillis();
    }

    // ---- getters / setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getSupplierId() { return supplierId; }
    public void setSupplierId(long supplierId) { this.supplierId = supplierId; }

    public long getFarmId() { return farmId; }
    public void setFarmId(long farmId) { this.farmId = farmId; }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public double getCommittedKg() { return committedKg; }
    public void setCommittedKg(double committedKg) { this.committedKg = committedKg; }

    public long getCommittedDate() { return committedDate; }
    public void setCommittedDate(long committedDate) { this.committedDate = committedDate; }

    public String getLinkedBatchIds() { return linkedBatchIds; }
    public void setLinkedBatchIds(String linkedBatchIds) { this.linkedBatchIds = linkedBatchIds; }

    public CommitmentStatus getStatus() { return status; }
    public void setStatus(CommitmentStatus status) { this.status = status; }

    public String getDelayReason() { return delayReason; }
    public void setDelayReason(String delayReason) { this.delayReason = delayReason; }

    public boolean isBuyerRequestedChange() { return buyerRequestedChange; }
    public void setBuyerRequestedChange(boolean buyerRequestedChange) {
        this.buyerRequestedChange = buyerRequestedChange;
    }

    public String getRevisions() { return revisions; }
    public void setRevisions(String revisions) { this.revisions = revisions; }

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