package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.VerificationDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.utils.CertificationIdUtils;
import com.afrinuts.farmos.utils.DocumentIdGenerator;
import com.afrinuts.farmos.utils.VerificationIdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerificationRepository {
    private static final String TAG = "VerificationRepo";

    private final AppDatabase database;
    private final VerificationDao verificationDao;
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
        void onSuccess(String verificationId);
        void onError(String error);
    }

    public VerificationRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.verificationDao = database.verificationDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertVerification(VerificationEntity verification, InsertCallback callback) {
        executor.execute(() -> {
            try {
                String programme = CertificationIdUtils.programmeCodeFromCertificationId(
                        verification.getCertificationId());
                String prefix = VerificationIdGenerator.sequencePrefix(programme);
                int next = CertificationIdUtils.trailingSequence(
                        verificationDao.getMaxVerificationIdForPrefix(prefix), prefix) + 1;
                verification.setVerificationId(
                        VerificationIdGenerator.buildVerificationId(programme, next));

                try {
                    verificationDao.insertAbort(verification);
                } catch (android.database.sqlite.SQLiteConstraintException e) {
                    mainHandler.post(() -> callback.onError(CertificationIdUtils.collisionMessage()));
                    return;
                }

                Log.d(TAG, "Verification saved locally: " + verification.getVerificationId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("compliance_verifications",
                            verification.getVerificationId(), SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(verification.getVerificationId()));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting verification", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<VerificationEntity>> getAllVerifications() {
        return verificationDao.getVerificationsByFarmIdLiveData(farmId);
    }

    public List<VerificationEntity> getVerificationsSync() {
        return verificationDao.getVerificationsByFarmId(farmId);
    }

    public LiveData<List<VerificationEntity>> getVerificationsByCertification(String certificationId) {
        return verificationDao.getVerificationsByCertificationIdLiveData(certificationId);
    }

    public LiveData<VerificationEntity> getVerificationById(String verificationId) {
        return verificationDao.getVerificationByIdLiveData(verificationId);
    }

    public VerificationEntity getVerificationByIdSync(String verificationId) {
        return verificationDao.getVerificationById(verificationId);
    }

    public List<VerificationEntity> getPendingVerifications() {
        return verificationDao.getPendingVerifications(farmId);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateVerification(VerificationEntity verification, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                verificationDao.update(verification);
                Log.d(TAG, "Verification updated: " + verification.getVerificationId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("verifications", verification.getVerificationId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error updating verification", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteVerification(VerificationEntity verification, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                verificationDao.delete(verification);
                Log.d(TAG, "Verification deleted: " + verification.getVerificationId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("verifications", verification.getVerificationId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting verification", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}