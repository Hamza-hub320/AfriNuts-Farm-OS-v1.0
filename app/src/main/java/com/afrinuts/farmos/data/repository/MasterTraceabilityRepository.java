package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;

import java.util.List;

public class MasterTraceabilityRepository extends BaseRepository {
    private static final String TAG = "MasterTraceabilityRepo";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirestoreSyncManager firestoreSyncManager;

    public MasterTraceabilityRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertMasterTraceability(MasterBatchTraceabilityEntity master, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.masterTraceabilityDao().insert(master);  // ✅ Now works
                master.setId(id);
                Log.d(TAG, "Master traceability saved locally with ID: " + id +
                        " Master: " + master.getMasterBatchNumber());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("master_traceability", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(id));
            } catch (Exception e) {
                Log.e(TAG, "Error inserting master traceability", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public long insertMasterTraceabilitySync(MasterBatchTraceabilityEntity master) {
        long id = database.masterTraceabilityDao().insert(master);  // ✅ Now works
        master.setId(id);
        Log.d(TAG, "Master traceability saved sync with ID: " + id);

        if (!firestoreSyncManager.isRemoteUpdate()) {
            syncManager.queueForSync("master_traceability", id, SyncEntity.SyncAction.CREATE);
            syncManager.triggerAutoSync();
        }

        return id;
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<MasterBatchTraceabilityEntity> getMasterById(long id) {
        return database.masterTraceabilityDao().getByIdLiveData(id);
    }

    public MasterBatchTraceabilityEntity getMasterByIdSync(long id) {
        return database.masterTraceabilityDao().getById(id);
    }

    public MasterBatchTraceabilityEntity getByMasterBatchNumber(String masterBatchNumber) {
        return database.masterTraceabilityDao().getByMasterBatchNumber(masterBatchNumber);
    }

    public LiveData<List<MasterBatchTraceabilityEntity>> getAllMasters() {
        return database.masterTraceabilityDao().getAllLiveData();
    }

    public List<MasterBatchTraceabilityEntity> getAllMastersSync() {
        return database.masterTraceabilityDao().getAll();
    }

    public List<MasterBatchTraceabilityEntity> getByBuyer(String buyer) {
        return database.masterTraceabilityDao().getByBuyer(buyer);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateMasterTraceability(MasterBatchTraceabilityEntity master, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.masterTraceabilityDao().update(master);
                Log.d(TAG, "Master traceability updated: " + master.getMasterBatchNumber());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("master_traceability", master.getId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));
            } catch (Exception e) {
                Log.e(TAG, "Error updating master traceability", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteMasterTraceability(MasterBatchTraceabilityEntity master, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.masterTraceabilityDao().delete(master);
                Log.d(TAG, "Master traceability deleted: " + master.getMasterBatchNumber());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("master_traceability", master.getId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));
            } catch (Exception e) {
                Log.e(TAG, "Error deleting master traceability", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // STATISTICS
    // ========================================

    public int getTotalCount() {
        return database.masterTraceabilityDao().getTotalCount();
    }

    public double getTotalWeight() {
        return database.masterTraceabilityDao().getTotalWeight();
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncMasterFromFirestore(com.google.firebase.firestore.DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                MasterBatchTraceabilityEntity master = EntityMapper.mapToMasterTraceability(document);
                if (master != null) {
                    MasterBatchTraceabilityEntity existing = database.masterTraceabilityDao()
                            .getById(master.getId());
                    if (existing == null) {
                        database.masterTraceabilityDao().insert(master);
                        Log.d(TAG, "Synced new master traceability: " + master.getMasterBatchNumber());
                    } else {
                        if (master.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.masterTraceabilityDao().update(master);
                            Log.d(TAG, "Synced updated master traceability: " + master.getMasterBatchNumber());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing master traceability from Firestore", e);
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}