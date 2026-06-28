package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupplierRepository {
    private static final String TAG = "SupplierRepository";

    private final AppDatabase database;
    private final SyncManager syncManager;
    private final FirestoreSyncManager firestoreSyncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final long farmId;

    public interface InsertCallback {
        void onSuccess(long id);
        void onError(String error);
    }

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public SupplierRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertSupplier(SupplierEntity supplier, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.supplierDao().insert(supplier);
                supplier.setId(id);
                Log.d(TAG, "Supplier saved locally: " + supplier.getName());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("suppliers", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(id));
            } catch (Exception e) {
                Log.e(TAG, "Error inserting supplier", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<SupplierEntity>> getAllSuppliers() {
        return database.supplierDao().getSuppliersByFarmIdLiveData(farmId);
    }

    public LiveData<SupplierEntity> getSupplierById(long supplierId) {
        return database.supplierDao().getSupplierByIdLiveData(supplierId);
    }

    public List<SupplierEntity> getSuppliersSync() {
        return database.supplierDao().getSuppliersByFarmId(farmId);
    }

    public SupplierEntity getSupplierByIdSync(long supplierId) {
        return database.supplierDao().getSupplierById(supplierId);
    }

    public List<SupplierEntity> getActiveSuppliers() {
        return database.supplierDao().getActiveSuppliers(farmId);
    }

    public List<SupplierEntity> getCertifiedSuppliers() {
        return database.supplierDao().getCertifiedSuppliers(farmId);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateSupplier(SupplierEntity supplier, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.supplierDao().update(supplier);
                Log.d(TAG, "Supplier updated: " + supplier.getName());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("suppliers", supplier.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));
            } catch (Exception e) {
                Log.e(TAG, "Error updating supplier", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    // In SupplierRepository.java, update the deleteSupplier method

    // In SupplierRepository.java
    public void deleteSupplier(SupplierEntity supplier, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // 1. Get all batches for this supplier
                List<QualityBatchEntity> batches = database.qualityBatchDao()
                        .getBatchesBySupplierIdSync(supplier.getId());

                // 2. Delete each batch from local database and queue for sync
                for (QualityBatchEntity batch : batches) {
                    database.qualityBatchDao().delete(batch);
                    if (!firestoreSyncManager.isRemoteUpdate()) {
                        syncManager.queueForSync("quality_batch", batch.getId(), SyncEntity.SyncAction.DELETE);
                    }
                    Log.d(TAG, "Deleted associated batch: " + batch.getBatchNumber());
                }

                // 3. Delete the supplier from local database
                database.supplierDao().delete(supplier);
                Log.d(TAG, "Supplier deleted locally: " + supplier.getName());

                // 4. Queue supplier deletion for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("suppliers", supplier.getId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting supplier and associated data", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // STATISTICS
    // ========================================

    public CompletableFuture<Integer> getTotalSupplierCount() {
        return CompletableFuture.supplyAsync(() ->
                database.supplierDao().getTotalSupplierCount(farmId), executor);
    }

    public CompletableFuture<Integer> getActiveSupplierCount() {
        return CompletableFuture.supplyAsync(() ->
                database.supplierDao().getActiveSupplierCount(farmId), executor);
    }

    public CompletableFuture<Integer> getCertifiedSupplierCount() {
        return CompletableFuture.supplyAsync(() ->
                database.supplierDao().getCertifiedSupplierCount(farmId), executor);
    }

    public CompletableFuture<Double> getAverageRating() {
        return CompletableFuture.supplyAsync(() ->
                database.supplierDao().getAverageRating(farmId), executor);
    }

    public CompletableFuture<Double> getTotalAnnualVolume() {
        return CompletableFuture.supplyAsync(() ->
                database.supplierDao().getTotalAnnualVolume(farmId), executor);
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncSupplierFromFirestore(DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                SupplierEntity supplier = EntityMapper.mapToSupplier(document);
                if (supplier != null) {
                    SupplierEntity existing = database.supplierDao().getSupplierById(supplier.getId());
                    if (existing == null) {
                        database.supplierDao().insert(supplier);
                        Log.d(TAG, "Synced new supplier: " + supplier.getName());
                    } else {
                        if (supplier.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.supplierDao().update(supplier);
                            Log.d(TAG, "Synced updated supplier: " + supplier.getName());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing supplier", e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}