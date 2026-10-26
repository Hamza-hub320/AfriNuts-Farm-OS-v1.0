package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.dao.CertificationDao;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.utils.CertificationIdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CertificationRepository {
    private static final String TAG = "CertificationRepo";

    private final AppDatabase database;
    private final CertificationDao certificationDao;
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
        void onSuccess(String certificationId);
        void onError(String error);
    }

    public CertificationRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.certificationDao = database.certificationDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertCertification(CertificationEntity certification, InsertCallback callback) {
        executor.execute(() -> {
            try {
                if (certification.getCertificationId() == null || certification.getCertificationId().isEmpty()) {
                    String id = CertificationIdGenerator.generateCertificationId(
                            certification.getCertificationName(),
                            certification.getCertificationBody()
                    );
                    certification.setCertificationId(id);
                }

                certificationDao.insert(certification);
                Log.d(TAG, "Certification saved locally: " + certification.getCertificationId());

                // ✅ Queue for sync using string ID
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("certifications", certification.getCertificationId(), SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(certification.getCertificationId()));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting certification", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<CertificationEntity>> getAllCertifications() {
        return certificationDao.getCertificationsByFarmIdLiveData(farmId);
    }

    public List<CertificationEntity> getCertificationsSync() {
        return certificationDao.getCertificationsByFarmId(farmId);
    }

    public LiveData<CertificationEntity> getCertificationById(String certificationId) {
        return certificationDao.getCertificationByIdLiveData(certificationId);
    }

    public CertificationEntity getCertificationByIdSync(String certificationId) {
        return certificationDao.getCertificationById(certificationId);
    }

    public List<CertificationEntity> getActiveCertifications() {
        return certificationDao.getActiveCertifications(farmId);
    }

    public List<CertificationEntity> getExpiringCertifications() {
        long currentTime = System.currentTimeMillis();
        long expiryThreshold = currentTime + (90L * 24 * 60 * 60 * 1000); // 90 days
        return certificationDao.getExpiringCertifications(farmId, currentTime, expiryThreshold);
    }

    public List<CertificationEntity> getExpiredCertifications() {
        long currentTime = System.currentTimeMillis();
        return certificationDao.getExpiredCertifications(farmId, currentTime);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateCertification(CertificationEntity certification, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // ✅ Update local database
                certificationDao.update(certification);
                Log.d(TAG, "Certification updated locally: " + certification.getCertificationId());

                // ✅ Queue for sync
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    // Use the string ID for sync
                    syncManager.queueForSync("certifications", certification.getCertificationId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "Queued certification update for sync: " + certification.getCertificationId());
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error updating certification", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

// ========================================
    // DELETE
    // ========================================

    public void deleteCertification(CertificationEntity certification, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String certificationId = certification.getCertificationId();

                // ✅ 1. Get all related records
                List<AuditEntity> audits = database.auditDao().getAuditsByCertificationId(certificationId);
                List<InspectionEntity> inspections = database.inspectionDao().getInspectionsByCertificationId(certificationId);
                List<VerificationEntity> verifications = database.verificationDao().getVerificationsByCertificationId(certificationId);
                List<DocumentEntity> documents = database.documentDao().getDocumentsByCertificationId(certificationId);

                Log.d(TAG, "Deleting certification " + certificationId + " with " +
                        audits.size() + " audits, " +
                        inspections.size() + " inspections, " +
                        verifications.size() + " verifications, " +
                        documents.size() + " documents");

                // ✅ 2. Delete all related records from local DB and queue for sync
                for (AuditEntity audit : audits) {
                    database.auditDao().delete(audit);
                    if (!firestoreSyncManager.isRemoteUpdate()) {
                        syncManager.queueForSync("audits", audit.getAuditId(), SyncEntity.SyncAction.DELETE);
                    }
                    Log.d(TAG, "🗑️ Deleted audit: " + audit.getAuditId());
                }

                for (InspectionEntity inspection : inspections) {
                    database.inspectionDao().delete(inspection);
                    if (!firestoreSyncManager.isRemoteUpdate()) {
                        syncManager.queueForSync("inspections", inspection.getInspectionId(), SyncEntity.SyncAction.DELETE);
                    }
                    Log.d(TAG, "🗑️ Deleted inspection: " + inspection.getInspectionId());
                }

                for (VerificationEntity verification : verifications) {
                    database.verificationDao().delete(verification);
                    if (!firestoreSyncManager.isRemoteUpdate()) {
                        syncManager.queueForSync("verifications", verification.getVerificationId(), SyncEntity.SyncAction.DELETE);
                    }
                    Log.d(TAG, "🗑️ Deleted verification: " + verification.getVerificationId());
                }

                for (DocumentEntity document : documents) {
                    database.documentDao().delete(document);
                    if (!firestoreSyncManager.isRemoteUpdate()) {
                        syncManager.queueForSync("documents", document.getDocumentId(), SyncEntity.SyncAction.DELETE);
                    }
                    Log.d(TAG, "🗑️ Deleted document: " + document.getDocumentId());
                }

                // ✅ 3. Delete the certification itself
                database.certificationDao().delete(certification);
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("certifications", certificationId, SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                Log.d(TAG, "✅ Certification and all related records deleted locally");
                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting certification with cascade", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // STATISTICS
    // ========================================

    public int getCertificationCountByStatus(CertificationEntity.CertificationStatus status) {
        return certificationDao.countByStatus(farmId, status.name());
    }

    public void shutdown() {
        executor.shutdown();
    }
}