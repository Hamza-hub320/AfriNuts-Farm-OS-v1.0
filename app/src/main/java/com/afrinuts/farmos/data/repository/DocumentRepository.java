package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.dao.DocumentDao;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.utils.CertificationIdUtils;
import com.afrinuts.farmos.utils.DocumentIdGenerator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DocumentRepository {
    private static final String TAG = "DocumentRepo";

    private final AppDatabase database;
    private final DocumentDao documentDao;
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
        void onSuccess(String documentId);
        void onError(String error);
    }

    public DocumentRepository(Context context, long farmId) {
        this.database = AppDatabase.getInstance(context);
        this.documentDao = database.documentDao();
        this.syncManager = SyncManager.getInstance(context);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.farmId = farmId;
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertDocument(DocumentEntity document, InsertCallback callback) {
        executor.execute(() -> {
            try {
                // ✅ Verify certification exists BEFORE generating ID
                CertificationEntity certification = database.certificationDao()
                        .getCertificationById(document.getCertificationId());

                if (certification == null) {
                    Log.e(TAG, "Certification not found: " + document.getCertificationId());
                    mainHandler.post(() -> callback.onError("Certification not found. Please sync and try again."));
                    return;
                }

                String programme = CertificationIdUtils.programmeCodeFromCertificationId(
                        document.getCertificationId());
                String prefix = DocumentIdGenerator.sequencePrefix(programme);
                int next = CertificationIdUtils.trailingSequence(
                        documentDao.getMaxDocumentIdForPrefix(prefix), prefix) + 1;
                document.setDocumentId(DocumentIdGenerator.buildDocumentId(programme, next));

                try {
                    documentDao.insertAbort(document);
                } catch (android.database.sqlite.SQLiteConstraintException e) {
                    mainHandler.post(() -> callback.onError(CertificationIdUtils.collisionMessage()));
                    return;
                }
                Log.d(TAG, "Document saved locally: " + document.getDocumentId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("documents", document.getDocumentId(), SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(document.getDocumentId()));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting document", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<DocumentEntity>> getAllDocuments() {
        return documentDao.getDocumentsByFarmIdLiveData(farmId);
    }

    public List<DocumentEntity> getDocumentsSync() {
        return documentDao.getDocumentsByFarmId(farmId);
    }

    public LiveData<List<DocumentEntity>> getDocumentsByCertification(String certificationId) {
        return documentDao.getDocumentsByCertificationIdLiveData(certificationId);
    }

    public LiveData<DocumentEntity> getDocumentById(String documentId) {
        return documentDao.getDocumentByIdLiveData(documentId);
    }

    public DocumentEntity getDocumentByIdSync(String documentId) {
        return documentDao.getDocumentById(documentId);
    }

    public List<DocumentEntity> getActiveDocuments() {
        long currentTime = System.currentTimeMillis();
        return documentDao.getActiveDocuments(farmId, currentTime);
    }

    public List<DocumentEntity> getExpiringDocuments() {
        long currentTime = System.currentTimeMillis();
        long expiryThreshold = currentTime + (90L * 24 * 60 * 60 * 1000);
        return documentDao.getExpiringDocuments(farmId, currentTime, expiryThreshold);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateDocument(DocumentEntity document, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                documentDao.update(document);
                Log.d(TAG, "Document updated: " + document.getDocumentId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("documents", document.getDocumentId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error updating document", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteDocument(DocumentEntity document, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                documentDao.delete(document);
                Log.d(TAG, "Document deleted: " + document.getDocumentId());

                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("documents", document.getDocumentId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting document", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}