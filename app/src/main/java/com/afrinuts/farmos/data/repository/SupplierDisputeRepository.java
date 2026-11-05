package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.QualityBatchDao;
import com.afrinuts.farmos.data.local.dao.SupplierCommitmentDao;
import com.afrinuts.farmos.data.local.dao.SupplierDisputeDao;
import com.afrinuts.farmos.data.local.dao.SupplierInspectionDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupplierDisputeRepository {
    private static final String TAG = "DisputeRepo";

    private final SupplierDisputeDao disputeDao;
    private final SupplierCommitmentDao commitmentDao;
    private final SupplierInspectionDao inspectionDao;
    private final QualityBatchDao batchDao;
    private final SyncManager syncManager;
    private final FirestoreSyncManager firestoreSyncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final long farmId;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public SupplierDisputeRepository(Context context, long farmId) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.disputeDao = db.supplierDisputeDao();
        this.commitmentDao = db.supplierCommitmentDao();
        this.inspectionDao = db.supplierInspectionDao();
        this.batchDao = db.qualityBatchDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // RAISE — verifies target exists before insert
    // ========================================

    public CompletableFuture<Long> raiseDispute(SupplierDisputeEntity dispute,
                                                Callback<Long> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Target existence check — the whole point of raising from a record
                if (!targetExists(dispute.getTargetEntity(), dispute.getTargetId())) {
                    throw new IllegalStateException(
                            "Cannot raise dispute: target " + dispute.getTargetEntity()
                                    + " #" + dispute.getTargetId() + " not found locally");
                }

                dispute.setReviewStatus(SupplierDisputeEntity.ReviewStatus.OPEN);
                dispute.setSynced(false);
                dispute.setCorrectionApplied(false);

                long id = disputeDao.insert(dispute);
                dispute.setId(id);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_disputes", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Raised dispute " + id + " on "
                        + dispute.getTargetEntity() + "#" + dispute.getTargetId());
                if (callback != null) callback.onSuccess(id);
                return id;
            } catch (Exception e) {
                Log.e(TAG, "Error raising dispute", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private boolean targetExists(SupplierDisputeEntity.TargetEntity entity, long id) {
        switch (entity) {
            case COMMITMENT:
                return commitmentDao.getById(id) != null;
            case INSPECTION:
                return inspectionDao.getById(id) != null;
            case BATCH:
                return batchDao.getBatchById(id) != null;
            case SCORE:
                // Score snapshots are portal-side. The app cannot raise a score dispute;
                // resolving one is allowed (recordResolution), raising is not.
                return false;
            default:
                return false;
        }
    }

    // ========================================
    // RESOLUTION — the ONLY writer of review fields
    // ========================================

    /**
     * Records the outcome of a dispute review. This is the only method that may
     * touch: reviewStatus, decision, reviewedBy, resolvedDate, correctionApplied,
     * originalValue, correctedValue, recalculatedSnapshotId.
     *
     * The reason and evidenceUrls are immutable after submission (framework rule):
     * no method here can change them.
     */
    public CompletableFuture<Void> recordResolution(long disputeId,
                                                    SupplierDisputeEntity.ReviewStatus newStatus,
                                                    String decision,
                                                    String reviewedBy,
                                                    boolean correctionApplied,
                                                    String originalValue,
                                                    String correctedValue,
                                                    Long recalculatedSnapshotId,
                                                    Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (newStatus == SupplierDisputeEntity.ReviewStatus.OPEN
                        || newStatus == SupplierDisputeEntity.ReviewStatus.UNDER_REVIEW) {
                    throw new IllegalArgumentException(
                            "recordResolution is for RESOLVED/REJECTED only; use markUnderReview() otherwise");
                }
                if (correctionApplied
                        && (originalValue == null || correctedValue == null)) {
                    throw new IllegalArgumentException(
                            "correctionApplied requires both originalValue and correctedValue");
                }

                SupplierDisputeEntity d = disputeDao.getById(disputeId);
                if (d == null) throw new IllegalStateException("Dispute not found: " + disputeId);

                d.setReviewStatus(newStatus);
                d.setDecision(decision);
                d.setReviewedBy(reviewedBy);
                d.setResolvedDate(System.currentTimeMillis());
                d.setCorrectionApplied(correctionApplied);
                if (correctionApplied) {
                    d.setOriginalValue(originalValue);
                    d.setCorrectedValue(correctedValue);
                    if (recalculatedSnapshotId != null) {
                        d.setRecalculatedSnapshotId(recalculatedSnapshotId);
                    }
                }
                d.setUpdatedBy(reviewedBy);
                disputeDao.update(d);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_disputes", disputeId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Resolved dispute " + disputeId + " as " + newStatus
                        + (correctionApplied ? " (corrected)" : ""));
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error recording resolution", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    /** Small transition — OPEN → UNDER_REVIEW only. */
    public CompletableFuture<Void> markUnderReview(long disputeId, String reviewedBy,
                                                   Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierDisputeEntity d = disputeDao.getById(disputeId);
                if (d == null) throw new IllegalStateException("Dispute not found: " + disputeId);
                if (d.getReviewStatus() != SupplierDisputeEntity.ReviewStatus.OPEN) {
                    throw new IllegalStateException(
                            "Only OPEN disputes can be moved to UNDER_REVIEW");
                }
                d.setReviewStatus(SupplierDisputeEntity.ReviewStatus.UNDER_REVIEW);
                d.setReviewedBy(reviewedBy);
                d.setUpdatedBy(reviewedBy);
                disputeDao.update(d);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_disputes", disputeId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error marking dispute under review", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // READS
    // ========================================

    public LiveData<SupplierDisputeEntity> getDisputeById(long id) {
        return disputeDao.getByIdLive(id);
    }

    public CompletableFuture<List<SupplierDisputeEntity>> getBySupplierId(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> disputeDao.getBySupplierId(supplierId), executor);
    }

    public CompletableFuture<List<SupplierDisputeEntity>> getByTarget(
            SupplierDisputeEntity.TargetEntity entity, long targetId) {
        return CompletableFuture.supplyAsync(
                () -> disputeDao.getByTarget(entity.name(), targetId), executor);
    }

    public CompletableFuture<List<SupplierDisputeEntity>> getOpenBySupplierId(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> disputeDao.getOpenBySupplierId(supplierId), executor);
    }

    // ========================================
    // REMOTE SYNC APPLY
    // ========================================

    public void syncDisputeFromFirestore(DocumentSnapshot doc) {
        executor.execute(() -> {
            try {
                SupplierDisputeEntity remote = EntityMapper.mapToDispute(doc);
                if (remote == null) return;

                SupplierDisputeEntity local = disputeDao.getById(remote.getId());
                if (local == null) {
                    disputeDao.insert(remote);
                } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                    disputeDao.update(remote);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing dispute from Firestore", e);
            }
        });
    }

    public void shutdown() { executor.shutdown(); }
}