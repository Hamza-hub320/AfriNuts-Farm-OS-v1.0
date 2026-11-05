package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.SupplierInspectionDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
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

public class SupplierInspectionRepository {
    private static final String TAG = "InspectionRepo";

    private final SupplierInspectionDao inspectionDao;
    private final SyncManager syncManager;
    private final FirestoreSyncManager firestoreSyncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final long farmId;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public SupplierInspectionRepository(Context context, long farmId) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.inspectionDao = db.supplierInspectionDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public CompletableFuture<Long> createInspection(SupplierInspectionEntity inspection,
                                                    String createdBy,
                                                    Callback<Long> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Enforce severity rule at the repository, not the dialog
                if ((inspection.getOutcome() == SupplierInspectionEntity.Outcome.FAIL
                        || inspection.getOutcome() == SupplierInspectionEntity.Outcome.CONDITIONAL)
                        && (inspection.getSeverity() == null || inspection.getSeverity().isEmpty())) {
                    throw new IllegalArgumentException(
                            "Severity is required when outcome is FAIL or CONDITIONAL");
                }

                inspection.setCreatedBy(createdBy);
                inspection.setUpdatedBy(createdBy);
                inspection.setValidatedAt(null);
                inspection.setValidatedBy(null);
                inspection.setSynced(false);

                long id = inspectionDao.insert(inspection);
                inspection.setId(id);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_inspections", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Created inspection " + id +
                        " for supplier " + inspection.getSupplierId() +
                        " outcome=" + inspection.getOutcome() +
                        " severity=" + inspection.getSeverity());

                if (callback != null) callback.onSuccess(id);
                return id;
            } catch (Exception e) {
                Log.e(TAG, "Error creating inspection", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // UPDATE FINDINGS / CORRECTIVE ACTION
    // ========================================

    public CompletableFuture<Void> updateFindings(long inspectionId,
                                                  String findings,
                                                  String correctiveAction,
                                                  String updatedBy,
                                                  Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierInspectionEntity i = inspectionDao.getById(inspectionId);
                if (i == null) {
                    throw new IllegalStateException("Inspection not found: " + inspectionId);
                }

                i.setFindings(findings);
                i.setCorrectiveAction(correctiveAction);
                i.setUpdatedBy(updatedBy);
                i.setUpdatedAt(System.currentTimeMillis());
                inspectionDao.update(i);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_inspections", inspectionId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Updated findings for inspection " + inspectionId);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error updating findings", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // CLOSE CORRECTIVE ACTION — closedDate + status CLOSED together
    // ========================================

    public CompletableFuture<Void> closeCorrectiveAction(long inspectionId,
                                                         long closedDate,
                                                         String updatedBy,
                                                         Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierInspectionEntity i = inspectionDao.getById(inspectionId);
                if (i == null) {
                    throw new IllegalStateException("Inspection not found: " + inspectionId);
                }

                // closedDate and status CLOSED must move together — this is the
                // only method that writes them, so they can never drift.
                i.setClosedDate(closedDate);
                i.setStatus(SupplierInspectionEntity.InspectionStatus.CLOSED);
                i.setUpdatedBy(updatedBy);
                i.setUpdatedAt(System.currentTimeMillis());
                inspectionDao.update(i);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_inspections", inspectionId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Closed corrective action for inspection " + inspectionId);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error closing corrective action", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // MARK DISPUTED
    // ========================================

    public CompletableFuture<Void> markDisputed(long inspectionId,
                                                String updatedBy,
                                                Callback<Void> callback) {
        return CompletableFuture.runAsync(() -> {
            try {
                SupplierInspectionEntity i = inspectionDao.getById(inspectionId);
                if (i == null) {
                    throw new IllegalStateException("Inspection not found: " + inspectionId);
                }

                i.setStatus(SupplierInspectionEntity.InspectionStatus.DISPUTED);
                i.setUpdatedBy(updatedBy);
                i.setUpdatedAt(System.currentTimeMillis());
                inspectionDao.update(i);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("supplier_inspections", inspectionId, SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "Marked inspection disputed: " + inspectionId);
                if (callback != null) callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Error marking inspection disputed", e);
                if (callback != null) callback.onError(e.getMessage());
                throw new RuntimeException(e);
            }
        }, executor);
    }

    // ========================================
    // READ PASS-THROUGHS
    // ========================================

    public LiveData<SupplierInspectionEntity> getInspectionById(long id) {
        return inspectionDao.getByIdLive(id);
    }

    public CompletableFuture<SupplierInspectionEntity> getInspectionByIdSync(long id) {
        return CompletableFuture.supplyAsync(() -> inspectionDao.getById(id), executor);
    }

    public LiveData<List<SupplierInspectionEntity>> getBySupplierIdLive(long supplierId) {
        return inspectionDao.getBySupplierIdLive(supplierId);
    }

    public CompletableFuture<List<SupplierInspectionEntity>> getBySupplierId(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> inspectionDao.getBySupplierId(supplierId), executor);
    }

    public CompletableFuture<List<SupplierInspectionEntity>> getUnvalidatedBySupplierId(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> inspectionDao.getUnvalidatedBySupplierId(supplierId), executor);
    }

    public CompletableFuture<List<SupplierInspectionEntity>> getValidatedBySupplierId(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> inspectionDao.getValidatedBySupplierId(supplierId), executor);
    }

    public CompletableFuture<Integer> countValidated(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> inspectionDao.countValidatedBySupplierId(supplierId), executor);
    }

    /**
     * Engine eligibility hook: validated FAIL + CRITICAL severity inspections.
     * The engine raises a warning / eligibility flag from this — never averaged
     * into the score (framework §7 critical risk controls).
     */
    public CompletableFuture<List<SupplierInspectionEntity>> getValidatedCriticalFails(long supplierId) {
        return CompletableFuture.supplyAsync(
                () -> inspectionDao.getValidatedCriticalFails(supplierId), executor);
    }

    // ========================================
    // REMOTE SYNC APPLY
    // ========================================

    public void syncInspectionFromFirestore(DocumentSnapshot doc) {
        executor.execute(() -> {
            try {
                SupplierInspectionEntity remote = EntityMapper.mapToSupplierInspection(doc);
                if (remote == null) return;

                SupplierInspectionEntity local = inspectionDao.getById(remote.getId());
                if (local == null) {
                    inspectionDao.insert(remote);
                    Log.d(TAG, "Synced new inspection from Firestore: " + remote.getId());
                } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                    inspectionDao.update(remote);
                    Log.d(TAG, "Synced updated inspection from Firestore: " + remote.getId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing inspection from Firestore", e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}