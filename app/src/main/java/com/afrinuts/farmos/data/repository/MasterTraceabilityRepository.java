package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.google.gson.Gson;

import java.util.ArrayList;
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
                // F2 (migration 40→41): farmId is now a required column.
                // Set it from the repository's farmId if the caller didn't.
                if (master.getFarmId() <= 0) {
                    master.setFarmId(farmId);
                }

                long id = database.masterTraceabilityDao().insert(master);
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
        if (master.getFarmId() <= 0) {
            master.setFarmId(farmId);
        }
        long id = database.masterTraceabilityDao().insert(master);
        master.setId(id);
        Log.d(TAG, "Master traceability saved sync with ID: " + id);

        if (!firestoreSyncManager.isRemoteUpdate()) {
            syncManager.queueForSync("master_traceability", id, SyncEntity.SyncAction.CREATE);
            syncManager.triggerAutoSync();
        }

        return id;
    }

    // ========================================
    // F2 — membership wiring
    // ========================================
    //
    // masterBatchId on BatchTraceabilityEntity is the source of truth for
    // bulk membership. The two JSON columns on the master are derived views
    // regenerated from getChildrenOfMaster(masterBatchId) after any change.
    // Never write batchNumbersJson / batchTraceabilityIdsJson by hand.

    /**
     * Sets masterBatchId on each listed child traceability row, regenerates
     * the master's JSON views, and queues the child updates for sync.
     *
     * @param masterBatchNumber the master's business key (stored on each child)
     * @param childTraceabilityIds the traceability row ids to include
     */
    public void addChildrenToMaster(String masterBatchNumber,
                                    List<Long> childTraceabilityIds,
                                    RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                for (Long childId : childTraceabilityIds) {
                    BatchTraceabilityEntity child =
                            database.batchTraceabilityDao().getById(childId);
                    if (child == null) {
                        Log.w(TAG, "Child traceability not found: " + childId);
                        continue;
                    }
                    String existingMaster = child.getMasterBatchId();
                    if (existingMaster != null) {
                        if (!existingMaster.equals(masterBatchNumber)) {
                            Log.w(TAG, "Refusing to move child " + childId + " from master "
                                    + existingMaster + " to " + masterBatchNumber);
                        }
                        continue;
                    }
                    child.setMasterBatchId(masterBatchNumber);
                    database.batchTraceabilityDao().update(child);

                    if (!firestoreSyncManager.isRemoteUpdate()) {
                        syncManager.queueForSync("batch_traceability", childId,
                                SyncEntity.SyncAction.UPDATE);
                    }
                }

                regenerateMasterJson(masterBatchNumber);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));
            } catch (Exception e) {
                Log.e(TAG, "Error adding children to master", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Clears masterBatchId on each listed child, regenerates the master's
     * JSON views, and queues updates.
     */
    public void removeChildrenFromMaster(String masterBatchNumber,
                                         List<Long> childTraceabilityIds,
                                         RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                for (Long childId : childTraceabilityIds) {
                    BatchTraceabilityEntity child =
                            database.batchTraceabilityDao().getById(childId);
                    if (child == null) continue;

                    // Guard: only clear if this child actually belongs to
                    // this master.
                    if (masterBatchNumber.equals(child.getMasterBatchId())) {
                        child.setMasterBatchId(null);
                        database.batchTraceabilityDao().update(child);

                        if (!firestoreSyncManager.isRemoteUpdate()) {
                            syncManager.queueForSync("batch_traceability", childId,
                                    SyncEntity.SyncAction.UPDATE);
                        }
                    }
                }

                regenerateMasterJson(masterBatchNumber);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));
            } catch (Exception e) {
                Log.e(TAG, "Error removing children from master", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Synchronous variant of addChildrenToMaster, for callers already on a
     * worker thread (e.g. CreateMasterTraceabilityDialog's create thread).
     * Sets masterBatchId on each child, updates the child rows, then
     * regenerates the master's JSON views. No sync queueing — the caller is
     * responsible for triggering sync after this returns, or relies on the
     * subsequent updateMasterTraceability path.
     */
    public List<Long> addChildrenToMasterSync(String masterBatchNumber, List<Long> childTraceabilityIds) {
        List<Long> refusedIds = new ArrayList<>();
        try {
            for (Long childId : childTraceabilityIds) {
                BatchTraceabilityEntity child = database.batchTraceabilityDao().getById(childId);
                if (child == null) {
                    Log.w(TAG, "Child traceability not found: " + childId);
                    continue;
                }
                String existingMaster = child.getMasterBatchId();
                if (existingMaster != null) {
                    if (!existingMaster.equals(masterBatchNumber)) {
                        Log.w(TAG, "Refusing to move child " + childId + " from master "
                                + existingMaster + " to " + masterBatchNumber);
                        refusedIds.add(childId);
                    }
                    continue;
                }
                child.setMasterBatchId(masterBatchNumber);
                database.batchTraceabilityDao().update(child);

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("batch_traceability", childId,
                            SyncEntity.SyncAction.UPDATE);
                }
            }
            regenerateMasterJson(masterBatchNumber);
            if (!firestoreSyncManager.isRemoteUpdate()) {
                syncManager.triggerAutoSync();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in addChildrenToMasterSync", e);
        }
        return refusedIds;
    }

    /**
     * Recomputes batchNumbersJson / batchTraceabilityIdsJson from the
     * current getChildrenOfMaster(masterBatchNumber) set, and persists
     * the master. This is the ONLY writer of the two JSON columns.
     */
    private void regenerateMasterJson(String masterBatchNumber) {
        MasterBatchTraceabilityEntity master =
                database.masterTraceabilityDao().getByMasterBatchNumber(masterBatchNumber);
        if (master == null) {
            Log.w(TAG, "Master not found for JSON regeneration: " + masterBatchNumber);
            return;
        }

        List<BatchTraceabilityEntity> children =
                database.batchTraceabilityDao().getChildrenOfMaster(masterBatchNumber);

        List<String> batchNumbers = new ArrayList<>();
        List<Long> traceabilityIds = new ArrayList<>();
        for (BatchTraceabilityEntity c : children) {
            batchNumbers.add(c.getBatchNumber());
            traceabilityIds.add(c.getId());
        }

        master.setBatchNumbersJson(new Gson().toJson(batchNumbers));
        master.setBatchTraceabilityIdsJson(new Gson().toJson(traceabilityIds));

        database.masterTraceabilityDao().update(master);

        if (!firestoreSyncManager.isRemoteUpdate()) {
            syncManager.queueForSync("master_traceability", master.getId(),
                    SyncEntity.SyncAction.UPDATE);
        }

        Log.d(TAG, "Regenerated master JSON: " + masterBatchNumber
                + " → " + children.size() + " children");
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

    public List<BatchTraceabilityEntity> getChildrenOfMaster(String masterBatchNumber) {
        return database.batchTraceabilityDao().getChildrenOfMaster(masterBatchNumber);
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