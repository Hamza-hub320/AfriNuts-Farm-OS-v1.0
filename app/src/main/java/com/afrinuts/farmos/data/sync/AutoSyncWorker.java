package com.afrinuts.farmos.data.sync;

import static com.afrinuts.farmos.data.remote.FirebaseManager.COLLECTION_FARMS;
import static com.afrinuts.farmos.data.remote.FirebaseManager.SUBCOLLECTION_TASKS;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.utils.NetworkUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AutoSyncWorker extends Worker {
    private static final String TAG = "AutoSyncWorker";

    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private String farmId;

    public AutoSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        database = AppDatabase.getInstance(context);
        firebaseManager = FirebaseManager.getInstance();
        farmId = String.valueOf(getInputData().getLong("farmId", -1));
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "========== AUTO SYNC WORKER STARTED ==========");
        Log.d(TAG, "Farm ID: " + farmId);

        // A process death can leave an item stuck in SYNCING. Make it eligible again.
        database.syncDao().reclaimStaleSyncing(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

        // Check if we have internet
        if (!NetworkUtils.isConnected(getApplicationContext())) {
            Log.e(TAG, "No internet connection, rescheduling...");
            return Result.retry();
        }

        if (farmId == null) {
            Log.e(TAG, "No farmId provided");
            return Result.failure();
        }

        try {
            // Get all pending sync items
            List<SyncEntity> pendingItems = database.syncDao().getPendingSyncItems();
            Log.d(TAG, "Found " + pendingItems.size() + " pending items");

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "No pending items to sync");
                return Result.success();
            }

            int successCount = 0;
            int failCount = 0;

            // Process each item
            for (SyncEntity syncItem : pendingItems) {
                Log.d(TAG, "Processing sync item: " + syncItem.getEntityType() +
                        ":" + syncItem.getEntityId() + " action: " + syncItem.getAction());

                boolean success = processSyncItem(syncItem);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            Log.d(TAG, "Sync completed - Success: " + successCount + ", Failed: " + failCount);

            // Clean up old completed items (older than 7 days)
            long weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
            int deleted = database.syncDao().deleteOldCompleted(weekAgo);
            Log.d(TAG, "Cleaned up " + deleted + " old completed items");

            Log.d(TAG, "========== AUTO SYNC WORKER FINISHED ==========");

            if (failCount > 0) {
                String reason = pendingItems.stream()
                        .filter(item -> item.getStatus() == SyncEntity.SyncStatus.FAILED)
                        .map(SyncEntity::getErrorMessage)
                        .filter(message -> message != null && !message.trim().isEmpty())
                        .findFirst().orElse("One or more records could not be synced");
                return Result.success(new Data.Builder()
                        .putInt("synced_count", successCount)
                        .putInt("failed_count", failCount)
                        .putString("failure_reason", reason)
                        .build());
            }
            if (successCount > 0) {
                getApplicationContext().getSharedPreferences("sync_center", Context.MODE_PRIVATE)
                        .edit().putLong("last_sync_success_" + farmId, System.currentTimeMillis()).apply();
            }
            return Result.success(new Data.Builder().putInt("synced_count", successCount).putInt("failed_count", 0).build());

        } catch (Exception e) {
            Log.e(TAG, "Sync failed with exception", e);
            return Result.retry();
        }
    }

    private boolean processSyncItem(SyncEntity syncItem) {
        syncItem.setStatus(SyncEntity.SyncStatus.SYNCING);
        syncItem.setLastAttemptAt(System.currentTimeMillis());
        database.syncDao().update(syncItem);

        try {
            Log.d(TAG, "EntityType received: " + syncItem.getEntityType());
                switch (syncItem.getEntityType()) {
                case "block":
                case "blocks":
                    syncBlock(syncItem);
                    break;
                case "expense":
                case "expenses":
                    syncExpense(syncItem);
                    break;
                case "revenue":
                case "revenues":
                    syncRevenue(syncItem);
                    break;
                case "task":
                case "tasks":
                    syncTask(syncItem);
                    break;
                case "photo":
                case "photos":
                    syncPhoto(syncItem);
                    break;
                case "users":
                    syncUser(syncItem);
                    break;
                case "assignment":
                case "assignments":
                    syncAssignment(syncItem);
                    break;
                case "suppliers":
                    syncSupplier(syncItem);
                    break;
                case "quality_batch":
                    syncQualityBatch(syncItem);
                    break;
                case "batch_traceability":
                    syncBatchTraceability(syncItem);
                    break;
                case "master_traceability":
                    syncMasterTraceability(syncItem);
                    break;
                case "certification":
                case "certifications":
                    syncCertification(syncItem);
                    break;
                    case "audit":
                    case "audits":
                        syncAudit(syncItem);
                        break;
                    case "inspection":
                    case "inspections":
                        syncInspection(syncItem);
                        break;
                    case "verifications":
                    case "compliance_verifications":
                        syncVerification(syncItem);
                        break;
                    case "document":
                    case "documents":
                        syncDocument(syncItem);
                        break;
                    case "supplier_commitments":
                        syncSupplierCommitment(syncItem);
                        break;
                    case "supplier_inspections":
                        syncSupplierInspection(syncItem);
                        break;
                    case "supplier_disputes":
                        syncSupplierDispute(syncItem);
                        break;
                default:
                    Log.e(TAG, "Unknown entity type: " + syncItem.getEntityType());
                    throw new RuntimeException("Unknown entity type: " + syncItem.getEntityType());
            }

            // Mark as completed
            syncItem.setStatus(SyncEntity.SyncStatus.COMPLETED);
            syncItem.setErrorMessage(null);
            database.syncDao().update(syncItem);
            Log.d(TAG, "Successfully synced: " + syncItem.getEntityType() + ":" + syncItem.getEntityId());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to sync item: " + syncItem.getId(), e);

            syncItem.setStatus(SyncEntity.SyncStatus.FAILED);
            syncItem.setAttemptCount(syncItem.getAttemptCount() + 1);
            String reason = e.getMessage();
            syncItem.setErrorMessage(reason == null || reason.trim().isEmpty()
                    ? e.getClass().getSimpleName() : reason.trim());
            database.syncDao().update(syncItem);
            return false;
        }
    }

    private void syncDocument(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncDocument called: ID=" + syncItem.getEntityStringId() + " action=" + syncItem.getAction());

        String documentId = syncItem.getEntityStringId();

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                DocumentEntity document = database.documentDao().getDocumentById(documentId);
                if (document == null) {
                    Log.e(TAG, "Document not found in local DB: " + documentId);
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveDocument(document, farmId);
                Log.d(TAG, "Document saved to Firestore: " + document.getDocumentId());
                break;

            case DELETE:
                Log.d(TAG, "Deleting document from Firestore: ID=" + documentId);
                try {
                    firebaseManager.deleteDocument(farmId, documentId);
                    Log.d(TAG, "Document deleted from Firestore: ID=" + documentId);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete document from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    private void syncVerification(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncVerification called: ID=" + syncItem.getEntityStringId() + " action=" + syncItem.getAction());

        String verificationId = syncItem.getEntityStringId();

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                VerificationEntity verification = database.verificationDao()
                        .getVerificationById(verificationId);
                if (verification == null) {
                    Log.e(TAG, "Verification not found in local DB: " + verificationId);
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveVerification(verification, farmId);
                Log.d(TAG, "Verification saved to Firestore: " + verification.getVerificationId());
                break;

            case DELETE:
                Log.d(TAG, "Deleting verification from Firestore: ID=" + verificationId);
                try {
                    firebaseManager.deleteVerification(farmId, verificationId);
                    Log.d(TAG, "Verification deleted from Firestore: ID=" + verificationId);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete verification from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    private void syncInspection(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncInspection called: ID=" + syncItem.getEntityStringId() + " action=" + syncItem.getAction());

        String inspectionId = syncItem.getEntityStringId();

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                InspectionEntity inspection = database.inspectionDao().getInspectionById(inspectionId);
                if (inspection == null) {
                    Log.e(TAG, "Inspection not found in local DB: " + inspectionId);
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveInspection(inspection, farmId);
                Log.d(TAG, "Inspection saved to Firestore: " + inspection.getInspectionId());
                break;

            case DELETE:
                Log.d(TAG, "Deleting inspection from Firestore: ID=" + inspectionId);
                try {
                    firebaseManager.deleteInspection(farmId, inspectionId);
                    Log.d(TAG, "Inspection deleted from Firestore: ID=" + inspectionId);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete inspection from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    // Add audit sync method
    private void syncAudit(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncAudit called: ID=" + syncItem.getEntityStringId() + " action=" + syncItem.getAction());

        String auditId = syncItem.getEntityStringId();

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                AuditEntity audit = database.auditDao().getAuditById(auditId);
                if (audit == null) {
                    Log.e(TAG, "Audit not found in local DB: " + auditId);
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveAudit(audit, farmId);
                Log.d(TAG, "Audit saved to Firestore: " + audit.getAuditId());
                break;

            case DELETE:
                Log.d(TAG, "Deleting audit from Firestore: ID=" + auditId);
                try {
                    firebaseManager.deleteAudit(farmId, auditId);
                    Log.d(TAG, "Audit deleted from Firestore: ID=" + auditId);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete audit from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    private void syncCertification(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncCertification called: ID=" + syncItem.getEntityStringId() + " action=" + syncItem.getAction());

        // ✅ Use the string ID directly
        String certificationId = syncItem.getEntityStringId();

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                CertificationEntity certification = database.certificationDao()
                        .getCertificationById(certificationId);
                if (certification == null) {
                    Log.e(TAG, "Certification not found in local DB: " + certificationId);
                    // Try to get by numeric ID as fallback
                    certification = database.certificationDao()
                            .getCertificationById(String.valueOf(syncItem.getEntityId()));
                    if (certification == null) {
                        Log.e(TAG, "Certification not found by numeric ID either: " + syncItem.getEntityId());
                        throw new IllegalStateException("Local record not found");
                    }
                }
                firebaseManager.saveCertification(certification, farmId);
                Log.d(TAG, "Certification saved to Firestore: " + certification.getCertificationId());
                break;

            case DELETE:
                Log.d(TAG, "Deleting certification from Firestore: ID=" + certificationId);
                try {
                    firebaseManager.deleteCertification(farmId, certificationId);
                    Log.d(TAG, "Certification deleted from Firestore: ID=" + certificationId);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete certification from Firestore", e);
                    throw e;
                }
                break;
        }
    }
    private void syncBatchTraceability(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncBatchTraceability called: ID=" + syncItem.getEntityId() +
                " action=" + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                BatchTraceabilityEntity traceability = database.batchTraceabilityDao()
                        .getById(syncItem.getEntityId());
                if (traceability == null) {
                    Log.e(TAG, "Traceability not found in local DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveBatchTraceability(traceability, farmId);
                Log.d(TAG, "Traceability saved to Firestore: " + traceability.getBatchNumber());
                break;

            case DELETE:
                Log.d(TAG, "Deleting traceability from Firestore: ID=" + syncItem.getEntityId());
                try {
                    firebaseManager.deleteBatchTraceability(farmId, syncItem.getEntityId());
                    Log.d(TAG, "Traceability deleted from Firestore: " + syncItem.getEntityId());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete traceability from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    private void syncMasterTraceability(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncMasterTraceability called: ID=" + syncItem.getEntityId() +
                " action=" + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                MasterBatchTraceabilityEntity master = database.masterTraceabilityDao()
                        .getById(syncItem.getEntityId());
                if (master == null) {
                    Log.e(TAG, "Master traceability not found in local DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveMasterTraceability(master, farmId);
                Log.d(TAG, "Master traceability saved to Firestore: " + master.getMasterBatchNumber());
                break;

            case DELETE:
                Log.d(TAG, "Deleting master traceability from Firestore: ID=" + syncItem.getEntityId());
                try {
                    firebaseManager.deleteMasterTraceability(farmId, syncItem.getEntityId());
                    Log.d(TAG, "Master traceability deleted from Firestore: " + syncItem.getEntityId());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete master traceability from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    private void syncAssignment(SyncEntity syncItem) throws Exception {
        TaskAssignmentHistoryEntity assignment = database.taskAssignmentDao().getAssignmentById(syncItem.getEntityId());
        if (assignment == null) {
            Log.e(TAG, "Assignment not found in local DB: " + syncItem.getEntityId());
            throw new IllegalStateException("Local record not found");
        }

        Log.d(TAG, "Syncing assignment ID: " + syncItem.getEntityId() +
                " action: " + syncItem.getAction() +
                " taskId: " + assignment.getTaskId());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveTaskAssignment(assignment, farmId);
                Log.d(TAG, "Assignment saved to Firestore: " + assignment.getId());
                break;
            case DELETE:
                // Delete from Firestore
                firebaseManager.deleteAssignment(farmId, assignment.getId());
                Log.d(TAG, "Assignment deleted from Firestore: " + assignment.getId());
                break;
        }
    }

    private void syncUser(SyncEntity syncItem) throws Exception {
        UserEntity user = database.userDao().getUserById(syncItem.getEntityId());
        if (user == null) {
            Log.e(TAG, "User not found in local DB: " + syncItem.getEntityId());
            throw new IllegalStateException("Local record not found");
        }

        Log.d(TAG, "Syncing user ID: " + syncItem.getEntityId());

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:

                firebaseManager.saveUser(user, farmId);

                success[0] = true;
                latch.countDown();

                break;

            case DELETE:
                Log.d(TAG, "User delete not implemented yet");
                success[0] = true;
                latch.countDown();
                break;
        }

        // WAIT for Firebase
        if (!latch.await(60, TimeUnit.SECONDS)) {
            throw new RuntimeException("User sync timeout");
        }

        if (!success[0]) {
            throw new RuntimeException("User sync failed: " +
                    (errorMessage[0] != null ? errorMessage[0] : "Unknown error"));
        }
    }

    private void syncBlock(SyncEntity syncItem) throws Exception {
        BlockEntity block = database.blockDao().getBlockById(syncItem.getEntityId());
        if (block == null) {
            Log.e(TAG, "Block not found in local DB: " + syncItem.getEntityId());
            throw new IllegalStateException("Local record not found");
        }

        Log.d(TAG, "Syncing block: " + block.getBlockName() + " action: " + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                try {
                    Tasks.await(firebaseManager.saveBlockMap(
                            EntityMapper.blockToMap(block), String.valueOf(farmId), block.getId()));
                    Log.d(TAG, "Block saved to Firestore: " + block.getBlockName());
                } catch (Exception e) {
                    Log.e(TAG, "Firestore saveBlock failed", e);
                    throw e;
                }
                break;
            case DELETE:
                try {
                    firebaseManager.deleteBlock(farmId, block.getId());
                    Log.d(TAG, "Block deleted from Firestore: " + block.getBlockName());
                } catch (Exception e) {
                    Log.e(TAG, "Firestore deleteBlock failed", e);
                    throw e;
                }
                break;
        }
    }

    private void syncExpense(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncExpense called: ID=" + syncItem.getEntityId() + " action=" + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                ExpenseEntity expense = database.expenseDao().getExpenseById(syncItem.getEntityId());
                if (expense == null) {
                    Log.e(TAG, "Expense not found in local DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }
                Log.d(TAG, "Syncing expense: " + expense.getId() + " amount: " + expense.getAmount());
                firebaseManager.saveExpense(expense, farmId);
                Log.d(TAG, "Expense saved to Firestore: " + expense.getId());
                break;

            case DELETE:
                Log.d(TAG, "Deleting expense from Firestore: ID=" + syncItem.getEntityId());
                firebaseManager.deleteExpense(farmId, syncItem.getEntityId());
                Log.d(TAG, "Expense deleted from Firestore: " + syncItem.getEntityId());
                break;
        }
    }

    private void syncRevenue(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncRevenue called: ID=" + syncItem.getEntityId() + " action=" + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                RevenueEntity revenue = database.revenueDao().getRevenueById(syncItem.getEntityId());
                if (revenue == null) {
                    Log.e(TAG, "Revenue not found in local DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }
                Log.d(TAG, "Syncing revenue: " + revenue.getId() + " amount: " + revenue.getTotalAmount());
                firebaseManager.saveRevenue(revenue, farmId);
                Log.d(TAG, "Revenue saved to Firestore: " + revenue.getId());
                break;

            case DELETE:
                Log.d(TAG, "Deleting revenue from Firestore: ID=" + syncItem.getEntityId());
                firebaseManager.deleteRevenue(farmId, syncItem.getEntityId());
                Log.d(TAG, "Revenue deleted from Firestore: " + syncItem.getEntityId());
                break;
        }
    }

    private void syncTask(SyncEntity syncItem) throws Exception {
        TaskEntity task = database.taskDao().getTaskById(syncItem.getEntityId());

        // For DELETE, the task might already be gone from local DB
        if (task == null && syncItem.getAction() == SyncEntity.SyncAction.DELETE) {
            Log.d(TAG, "Task already deleted locally, attempting Firestore deletion for ID: " + syncItem.getEntityId());

            // Get assignments before deletion
            List<TaskAssignmentHistoryEntity> assignments =
                    database.taskAssignmentDao().getAssignmentsForTask(syncItem.getEntityId());

            // ✅ Pass 3 arguments: farmId, taskId, assignments
            firebaseManager.deleteTask(farmId, syncItem.getEntityId(), assignments);
            return;
        }

        if (task == null) {
            Log.e(TAG, "Task not found in local DB: " + syncItem.getEntityId());
            throw new IllegalStateException("Local record not found");
        }

        Log.d(TAG, "Syncing task ID: " + syncItem.getEntityId() + " action: " + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                firebaseManager.saveTask(task, farmId);
                Log.d(TAG, "Task saved to Firestore: " + task.getTitle());
                break;
            case DELETE:
                // ✅ Also get assignments when task still exists locally
                List<TaskAssignmentHistoryEntity> assignments =
                        database.taskAssignmentDao().getAssignmentsForTask(task.getId());

                firebaseManager.deleteTask(farmId, task.getId(), assignments);
                Log.d(TAG, "Task deleted from Firestore: " + task.getTitle());
                break;
        }
    }

    // Add this new method
    // In AutoSyncWorker.java, update the syncSupplier method:

    private void syncSupplier(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncSupplier called: ID=" + syncItem.getEntityId() + " action=" + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                SupplierEntity supplier = database.supplierDao().getSupplierById(syncItem.getEntityId());
                if (supplier == null) {
                    Log.e(TAG, "Supplier not found in local DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveSupplier(supplier, farmId);
                Log.d(TAG, "Supplier saved to Firestore: " + supplier.getName());
                break;

            case DELETE:
                // For DELETE, the supplier might already be gone from local DB
                Log.d(TAG, "Deleting supplier from Firestore: ID=" + syncItem.getEntityId());
                try {
                    // First, delete all associated quality batches from Firestore
                    List<QualityBatchEntity> batches = database.qualityBatchDao()
                            .getBatchesBySupplierIdSync(syncItem.getEntityId());

                    for (QualityBatchEntity batch : batches) {
                        firebaseManager.deleteQualityBatch(farmId, batch.getId());
                        Log.d(TAG, "Deleted associated batch from Firestore: " + batch.getBatchNumber());
                    }

                    // Then delete the supplier
                    firebaseManager.deleteSupplier(farmId, syncItem.getEntityId());
                    Log.d(TAG, "Supplier deleted from Firestore: ID=" + syncItem.getEntityId());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete supplier from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    // ========================================
// SUPPLIER TRUST SYNC — 6b
// ========================================

    private void syncSupplierCommitment(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncSupplierCommitment id=" + syncItem.getEntityId()
                + " action=" + syncItem.getAction());
        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                SupplierCommitmentEntity c =
                        database.supplierCommitmentDao().getById(syncItem.getEntityId());
                if (c == null) {
                    Log.e(TAG, "Commitment not in DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }
                // Commitments carry no evidence files — no uploader pass.
                firebaseManager.saveSupplierCommitment(c, farmId);
                break;
            case DELETE:
                firebaseManager.deleteSupplierCommitment(farmId, syncItem.getEntityId());
                break;
        }
    }

    private void syncSupplierInspection(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncSupplierInspection id=" + syncItem.getEntityId()
                + " action=" + syncItem.getAction());
        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                SupplierInspectionEntity i =
                        database.supplierInspectionDao().getById(syncItem.getEntityId());
                if (i == null) {
                    Log.e(TAG, "Inspection not in DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }

                // ── ordering rule: upload evidence → rewrite URLs → save record ──
                EvidencePhotoUploader uploader =
                        new EvidencePhotoUploader(firebaseManager, farmId);
                i = uploader.prepareInspection(i);

                if (!uploader.allEvidenceIsRemote(i.getEvidenceUrlList())) {
                    Log.w(TAG, "Inspection " + i.getId()
                            + " has unresolved file:// evidence; re-queuing instead of pushing");
                    throw new IllegalStateException("Evidence upload incomplete");
                }

                // Persist the rewritten URLs in Room too, so a crash after upload
                // doesn't re-upload the same files next pass.
                database.supplierInspectionDao().update(i);

                firebaseManager.saveSupplierInspection(i, farmId);
                break;

            case DELETE:
                firebaseManager.deleteSupplierInspection(farmId, syncItem.getEntityId());
                break;
        }
    }

    private void syncSupplierDispute(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncSupplierDispute id=" + syncItem.getEntityId()
                + " action=" + syncItem.getAction());
        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                SupplierDisputeEntity d =
                        database.supplierDisputeDao().getById(syncItem.getEntityId());
                if (d == null) {
                    Log.e(TAG, "Dispute not in DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }

                EvidencePhotoUploader uploader =
                        new EvidencePhotoUploader(firebaseManager, farmId);
                d = uploader.prepareDispute(d);

                if (!uploader.allEvidenceIsRemote(d.getEvidenceUrlList())) {
                    Log.w(TAG, "Dispute " + d.getId()
                            + " has unresolved file:// evidence; re-queuing");
                    throw new IllegalStateException("Evidence upload incomplete");
                }

                database.supplierDisputeDao().update(d);

                firebaseManager.saveSupplierDispute(d, farmId);
                break;

            case DELETE:
                firebaseManager.deleteSupplierDispute(farmId, syncItem.getEntityId());
                break;
        }
    }

    // Update syncQualityBatch method:
    private void syncQualityBatch(SyncEntity syncItem) throws Exception {
        Log.d(TAG, "syncQualityBatch called: ID=" + syncItem.getEntityId() + " action=" + syncItem.getAction());

        switch (syncItem.getAction()) {
            case CREATE:
            case UPDATE:
                QualityBatchEntity batch = database.qualityBatchDao().getBatchById(syncItem.getEntityId());
                if (batch == null) {
                    Log.e(TAG, "Quality batch not found in local DB: " + syncItem.getEntityId());
                    throw new IllegalStateException("Local record not found");
                }
                firebaseManager.saveQualityBatch(batch, farmId);
                Log.d(TAG, "Quality batch saved to Firestore: " + batch.getBatchNumber());
                break;

            case DELETE:
                // For DELETE, the batch might already be gone from local DB
                Log.d(TAG, "Deleting quality batch from Firestore: ID=" + syncItem.getEntityId());
                try {
                    firebaseManager.deleteQualityBatch(farmId, syncItem.getEntityId());
                    Log.d(TAG, "Quality batch deleted from Firestore: ID=" + syncItem.getEntityId());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete quality batch from Firestore", e);
                    throw e;
                }
                break;
        }
    }

    private void syncPhoto(SyncEntity syncItem) throws Exception {
        PhotoEntity photo = database.photoDao().getPhotoById(syncItem.getEntityId());
        if (photo == null) {
            Log.e(TAG, "Photo not found in local DB: " + syncItem.getEntityId());
            throw new IllegalStateException("Local photo record not found");
        }

        File photoFile = new File(photo.getFilePath());
        Log.d(TAG, "📸 Photo file path: " + photo.getFilePath());
        Log.d(TAG, "📸 File exists: " + photoFile.exists());

        if (!photoFile.exists()) {
            Log.e(TAG, "Photo file not found: " + photo.getFilePath());
            throw new IllegalStateException("Photo file not found");
        }

        Log.d(TAG, "📸 File size: " + photoFile.length() + " bytes");

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMessage = {null};

        // FIXED: Pass all 7 required arguments
        firebaseManager.uploadPhoto(
                photoFile,                                    // File photoFile
                String.valueOf(photo.getId()),                // String photoId
                farmId,                                       // String farmId
                photo.getBlockId(),                           // Long blockId
                photo.getTaskId(),                            // Long taskId
                photo.getExpenseId(),                         // Long expenseId
                new FirebaseManager.PhotoUploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        Log.d(TAG, "✅ Photo uploaded successfully to Storage. URL: " + downloadUrl);

                        // Save metadata to Firestore
                        firebaseManager.savePhotoMetadata(photo, farmId, downloadUrl,
                                new FirebaseManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "✅ Photo metadata saved to Firestore");
                                        success[0] = true;
                                        latch.countDown();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "❌ Failed to save photo metadata: " + error);
                                        errorMessage[0] = error;
                                        latch.countDown();
                                    }
                                });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Photo upload failed: " + error);
                        errorMessage[0] = error;
                        latch.countDown();
                    }

                    @Override
                    public void onProgress(double progress) {
                        Log.d(TAG, "📤 Photo upload progress: " + (int)progress + "%");
                    }
                });

        try {
            if (!latch.await(2, TimeUnit.MINUTES)) {
                Log.e(TAG, "❌ Photo upload timed out after 2 minutes");
                throw new RuntimeException("Photo upload timed out");
            }
            if (!success[0]) {
                throw new RuntimeException("Photo upload failed: " +
                        (errorMessage[0] != null ? errorMessage[0] : "Unknown error"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e(TAG, "❌ Photo upload interrupted", e);
            throw new RuntimeException("Photo upload interrupted", e);
        }
    }
}
