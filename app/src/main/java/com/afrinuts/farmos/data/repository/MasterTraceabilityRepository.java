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
import java.util.Calendar;
import java.util.LinkedHashSet;
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
                    child.setUpdatedAt(System.currentTimeMillis());
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
                        child.setUpdatedAt(System.currentTimeMillis());
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
                child.setUpdatedAt(System.currentTimeMillis());
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
        master.setUpdatedAt(System.currentTimeMillis());

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

    /**
     * Repairs missing child links from the master's stored child IDs/numbers. If those derived
     * lists are empty as well, only recover children when export-day, buyer, destination, count,
     * and total weight identify one unique set of currently unlinked batches.
     */
    public List<BatchTraceabilityEntity> repairMissingChildrenSync(MasterBatchTraceabilityEntity master) {
        if (master == null) return new ArrayList<>();
        String masterNumber = master.getMasterBatchNumber();
        List<BatchTraceabilityEntity> children = database.batchTraceabilityDao().getChildrenOfMaster(masterNumber);
        if (children == null) children = new ArrayList<>();
        if (children.size() >= master.getTotalBatches()) return children;

        List<Long> storedIds = master.getBatchTraceabilityIds();
        List<String> storedNumbers = master.getBatchNumbers();
        int storedIdCount = storedIds == null ? 0 : storedIds.size();
        int storedNumberCount = storedNumbers == null ? 0 : storedNumbers.size();
        Log.i(TAG, "Master children before repair: linked=" + children.size()
                + ", JSON ids=" + storedIdCount + ", JSON numbers=" + storedNumberCount
                + ", totalBatches=" + master.getTotalBatches());

        LinkedHashSet<Long> recoveryIds = new LinkedHashSet<>();
        for (BatchTraceabilityEntity child : children) recoveryIds.add(child.getId());
        if (storedIds != null) {
            for (Long id : storedIds) {
                if (id != null && database.batchTraceabilityDao().getById(id) != null) recoveryIds.add(id);
            }
        }
        if (storedNumbers != null) {
            for (String number : storedNumbers) {
                if (number == null || number.trim().isEmpty()) continue;
                BatchTraceabilityEntity child = database.batchTraceabilityDao().getByBatchNumber(number);
                if (child != null) recoveryIds.add(child.getId());
            }
        }

        int recoveryCandidateCount = Math.max(0, recoveryIds.size() - children.size());
        if (recoveryIds.size() < master.getTotalBatches()) {
            double linkedWeight = 0;
            for (BatchTraceabilityEntity child : children) linkedWeight += child.getWeightKg();
            int missingCount = master.getTotalBatches() - children.size();
            double missingWeight = master.getTotalWeightKg() - linkedWeight;
            List<Long> exactUniqueSet = findUniqueUnlinkedChildren(master, missingCount, missingWeight);
            if (exactUniqueSet != null) {
                recoveryIds.addAll(exactUniqueSet);
                recoveryCandidateCount = Math.max(0, recoveryIds.size() - children.size());
            }
        }

        if (recoveryIds.size() > children.size()) {
            addChildrenToMasterSync(masterNumber, new ArrayList<>(recoveryIds));
            children = database.batchTraceabilityDao().getChildrenOfMaster(masterNumber);
            if (children == null) children = new ArrayList<>();
        }
        Log.i(TAG, "Master children after repair: linked=" + children.size()
                + ", JSON ids=" + storedIdCount + ", JSON numbers=" + storedNumberCount
                + ", totalBatches=" + master.getTotalBatches()
                + ", recovery candidates=" + recoveryCandidateCount);
        return children;
    }

    private List<Long> findUniqueUnlinkedChildren(MasterBatchTraceabilityEntity master,
                                                   int expectedCount, double expectedWeight) {
        if (expectedCount <= 0 || expectedCount > 12 || expectedWeight <= 0
                || master.getExportDate() <= 0 || isBlank(master.getBuyer())
                || isBlank(master.getDestination())) return null;

        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(master.getExportDate());
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        long start = day.getTimeInMillis();
        day.add(Calendar.DAY_OF_MONTH, 1);
        long end = day.getTimeInMillis() - 1;
        List<BatchTraceabilityEntity> candidates = database.batchTraceabilityDao()
                .getUnlinkedForMasterRecovery(master.getBuyer(), master.getDestination(), start, end);
        if (candidates == null || candidates.size() < expectedCount || candidates.size() > 24) return null;

        MatchingSet match = new MatchingSet();
        matchChildren(candidates, 0, expectedCount, expectedWeight, 0,
                new ArrayList<>(), match);
        return match.matches == 1 ? match.ids : null;
    }

    private void matchChildren(List<BatchTraceabilityEntity> candidates, int start, int remainingCount,
                               double targetWeight, double currentWeight, List<Long> selected,
                               MatchingSet result) {
        if (result.matches > 1) return;
        if (remainingCount == 0) {
            if (Math.abs(currentWeight - targetWeight) <= 0.5) {
                result.matches++;
                if (result.matches == 1) result.ids = new ArrayList<>(selected);
            }
            return;
        }
        for (int i = start; i <= candidates.size() - remainingCount && result.matches <= 1; i++) {
            BatchTraceabilityEntity candidate = candidates.get(i);
            selected.add(candidate.getId());
            matchChildren(candidates, i + 1, remainingCount - 1, targetWeight,
                    currentWeight + candidate.getWeightKg(), selected, result);
            selected.remove(selected.size() - 1);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class MatchingSet {
        int matches;
        List<Long> ids;
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
