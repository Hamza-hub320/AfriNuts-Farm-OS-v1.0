package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.InspectionDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.utils.CertificationIdUtils;
import com.afrinuts.farmos.utils.InspectionIdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InspectionRepository {
    private static final String TAG = "InspectionRepo";

    private final AppDatabase database;
    private final InspectionDao inspectionDao;
    private final SyncManager syncManager;
    private final FirestoreSyncManager firestoreSyncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final long farmId;

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface InsertCallback {
        void onSuccess(String inspectionId);
        void onError(String error);
    }

    public InspectionRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.inspectionDao = database.inspectionDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    public void insertInspection(InspectionEntity inspection, InsertCallback callback) {
        executor.execute(() -> {
            try {
                String programme = CertificationIdUtils.programmeCodeFromCertificationId(
                        inspection.getCertificationId());
                String prefix = InspectionIdGenerator.sequencePrefix(programme);
                int next = CertificationIdUtils.trailingSequence(
                        inspectionDao.getMaxInspectionIdForPrefix(prefix), prefix) + 1;
                inspection.setInspectionId(
                        InspectionIdGenerator.buildInspectionId(programme, next));

                try {
                    inspectionDao.insertAbort(inspection);
                } catch (android.database.sqlite.SQLiteConstraintException e) {
                    mainHandler.post(() -> callback.onError(CertificationIdUtils.collisionMessage()));
                    return;
                }
                Log.d(TAG, "Inspection saved locally: " + inspection.getInspectionId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("inspections", inspection.getInspectionId(), SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(inspection.getInspectionId()));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting inspection", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public LiveData<List<InspectionEntity>> getAllInspections() {
        return inspectionDao.getInspectionsByFarmIdLiveData(farmId);
    }

    public List<InspectionEntity> getInspectionsSync() {
        return inspectionDao.getInspectionsByFarmId(farmId);
    }

    public LiveData<List<InspectionEntity>> getInspectionsByCertification(String certificationId) {
        return inspectionDao.getInspectionsByCertificationIdLiveData(certificationId);
    }

    public LiveData<InspectionEntity> getInspectionById(String inspectionId) {
        return inspectionDao.getInspectionByIdLiveData(inspectionId);
    }

    public InspectionEntity getInspectionByIdSync(String inspectionId) {
        return inspectionDao.getInspectionById(inspectionId);
    }

    public void updateInspection(InspectionEntity inspection, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                inspectionDao.update(inspection);
                Log.d(TAG, "Inspection updated: " + inspection.getInspectionId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("inspections", inspection.getInspectionId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error updating inspection", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void deleteInspection(InspectionEntity inspection, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                inspectionDao.delete(inspection);
                Log.d(TAG, "Inspection deleted: " + inspection.getInspectionId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("inspections", inspection.getInspectionId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting inspection", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}