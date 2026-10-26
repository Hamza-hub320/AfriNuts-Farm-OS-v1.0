package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.AuditDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.utils.AuditIdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuditRepository {
    private static final String TAG = "AuditRepo";

    private final AppDatabase database;
    private final AuditDao auditDao;
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
        void onSuccess(String auditId);
        void onError(String error);
    }

    public AuditRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.auditDao = database.auditDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertAudit(AuditEntity audit, InsertCallback callback) {
        executor.execute(() -> {
            try {
                // Generate ID if not set
                if (audit.getAuditId() == null || audit.getAuditId().isEmpty()) {
                    String id = AuditIdGenerator.generateAuditId(
                            audit.getAuditType(),
                            audit.getCertificationId()
                    );
                    audit.setAuditId(id);
                }

                auditDao.insert(audit);
                Log.d(TAG, "Audit saved locally: " + audit.getAuditId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("audits", audit.getAuditId(), SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(audit.getAuditId()));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting audit", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<AuditEntity>> getAllAudits() {
        return auditDao.getAuditsByFarmIdLiveData(farmId);
    }

    public List<AuditEntity> getAuditsSync() {
        return auditDao.getAuditsByFarmId(farmId);
    }

    public LiveData<List<AuditEntity>> getAuditsByCertification(String certificationId) {
        return auditDao.getAuditsByCertificationIdLiveData(certificationId);
    }

    public LiveData<AuditEntity> getAuditById(String auditId) {
        return auditDao.getAuditByIdLiveData(auditId);
    }

    public AuditEntity getAuditByIdSync(String auditId) {
        return auditDao.getAuditById(auditId);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateAudit(AuditEntity audit, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                auditDao.update(audit);
                Log.d(TAG, "Audit updated: " + audit.getAuditId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("audits", audit.getAuditId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error updating audit", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteAudit(AuditEntity audit, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                auditDao.delete(audit);
                Log.d(TAG, "Audit deleted: " + audit.getAuditId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("audits", audit.getAuditId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting audit", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}