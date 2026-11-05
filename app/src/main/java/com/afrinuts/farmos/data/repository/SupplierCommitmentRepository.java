package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.QualityBatchDao;
import com.afrinuts.farmos.data.local.dao.SupplierCommitmentDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupplierCommitmentRepository {
    private static final String TAG = "CommitmentRepo";

    private final SupplierCommitmentDao commitmentDao;
    private final QualityBatchDao batchDao;
    private final SyncManager syncManager;
    private final FirestoreSyncManager firestoreSyncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final long farmId;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public SupplierCommitmentRepository(Context context, long farmId) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.commitmentDao = db.supplierCommitmentDao();
        this.batchDao = db.qualityBatchDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public CompletableFuture<Long> createCommitment(SupplierCommitmentEntity commitment,
                                                    String createdBy,
                                                    Callback<Long> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                commitment.setCreatedBy(createdBy);
                commitment.setUpdatedBy(createdBy);
                commitment.setSynced(false);
                commitment.setValidatedAt(null);
                commitment.setValidatedBy(null);

                long id = commitmentDao.insert(commitment);
                commitment.setId(id);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_commitments", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Created commitment " + id + " for supplier " + commitment.getSupplierId());
                if (callback != null) callback.onSuccess(id);
                return id;
            } catch (Exception e) {
                Log.e(TAG, "Error creating commitment", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // REVISION — the ONLY writer of `revisions`
    // ========================================

    public CompletableFuture<Void> appendRevision(long commitmentId, long revisedDate,
                                                  String reason, String revisedBy,
                                                  Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierCommitmentEntity c = commitmentDao.getById(commitmentId);
                if (c == null) throw new IllegalStateException("Commitment not found: " + commitmentId);

                // append-only; committedDate + status + revisions updated atomically in one update
                c.appendRevision(revisedDate, reason, revisedBy);
                c.setUpdatedBy(revisedBy);
                commitmentDao.update(c);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_commitments", commitmentId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Appended revision to commitment " + commitmentId);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error appending revision", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // LINK / UNLINK BATCHES — the ONLY writers of linkedBatchIds
    // ========================================

    public CompletableFuture<Void> linkBatches(long commitmentId, List<Long> batchIds,
                                               Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierCommitmentEntity c = commitmentDao.getById(commitmentId);
                if (c == null) throw new IllegalStateException("Commitment not found: " + commitmentId);

                List<Long> current = c.getLinkedBatchIdList();
                for (Long id : batchIds) {
                    if (id != null && !current.contains(id)) current.add(id);
                }
                c.setLinkedBatchIdList(current);
                commitmentDao.update(c);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_commitments", commitmentId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error linking batches", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Void> unlinkBatch(long commitmentId, long batchId,
                                               Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierCommitmentEntity c = commitmentDao.getById(commitmentId);
                if (c == null) throw new IllegalStateException("Commitment not found: " + commitmentId);

                List<Long> current = c.getLinkedBatchIdList();
                current.remove(Long.valueOf(batchId));
                c.setLinkedBatchIdList(current);
                commitmentDao.update(c);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_commitments", commitmentId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error unlinking batch", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // CLOSE — refuses PARTIAL/MISSED without delayReason
    // ========================================

    public CompletableFuture<Void> closeCommitment(long commitmentId,
                                                   SupplierCommitmentEntity.CommitmentStatus outcome,
                                                   SupplierCommitmentEntity.DelayReason delayReason,
                                                   boolean buyerRequestedChange,
                                                   Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierCommitmentEntity c = commitmentDao.getById(commitmentId);
                if (c == null) throw new IllegalStateException("Commitment not found: " + commitmentId);

                if ((outcome == SupplierCommitmentEntity.CommitmentStatus.PARTIAL
                        || outcome == SupplierCommitmentEntity.CommitmentStatus.MISSED)
                        && delayReason == null && !buyerRequestedChange) {
                    throw new IllegalArgumentException(
                            "Closing as " + outcome + " requires a delayReason or buyerRequestedChange");
                }

                c.setStatus(outcome);
                if (delayReason != null) c.setDelayReason(delayReason.name());
                c.setBuyerRequestedChange(buyerRequestedChange);
                commitmentDao.update(c);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_commitments", commitmentId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error closing commitment", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // DERIVED deliveredKg — cross-DAO, so lives here
    // ========================================

    public CompletableFuture<Double> getDeliveredKg(long commitmentId) {
        return CompletableFuture.supplyAsync(() -> {
            SupplierCommitmentEntity c = commitmentDao.getById(commitmentId);
            if (c == null) return 0.0;

            double total = 0;
            for (Long batchId : c.getLinkedBatchIdList()) {
                QualityBatchEntity b = batchDao.getBatchById(batchId);
                if (b != null) total += b.getWeightKg();
            }
            return total;
        }, executor);
    }

    // ========================================
    // READ PASS-THROUGHS
    // ========================================

    public LiveData<SupplierCommitmentEntity> getCommitmentById(long id) {
        return commitmentDao.getByIdLive(id);
    }

    public CompletableFuture<List<SupplierCommitmentEntity>> getBySupplierId(long supplierId) {
        return CompletableFuture.supplyAsync(() -> commitmentDao.getBySupplierId(supplierId), executor);
    }

    public CompletableFuture<List<SupplierCommitmentEntity>> getValidatedBySupplierIdAndSeason(
            long supplierId, String season) {
        return CompletableFuture.supplyAsync(
                () -> commitmentDao.getValidatedBySupplierIdAndSeason(supplierId, season), executor);
    }

    public CompletableFuture<Integer> countValidated(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> commitmentDao.countValidatedBySupplierId(supplierId), executor);
    }

    // ========================================
    // REMOTE SYNC APPLY
    // ========================================

    public void syncCommitmentFromFirestore(DocumentSnapshot doc) {
        executor.execute(() -> {
            try {
                SupplierCommitmentEntity remote = EntityMapperSupplierCommitmentStub.map(doc);
                if (remote == null) return;

                SupplierCommitmentEntity local = commitmentDao.getById(remote.getId());
                if (local == null) {
                    commitmentDao.insert(remote);
                } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                    commitmentDao.update(remote);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing commitment from Firestore", e);
            }
        });
    }

    public void shutdown() { executor.shutdown(); }
}