package com.afrinuts.farmos.data.sync;

import static com.afrinuts.farmos.data.remote.FirebaseManager.COLLECTION_FARMS;


import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirestoreSyncManager {
    private static final String TAG = "FirestoreSync";
    private static FirestoreSyncManager instance;

    private FirebaseFirestore db;
    private AppDatabase database;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String farmId;

    // Prevent sync loops when applying remote updates
    private boolean isApplyingRemoteUpdate = false;

    // Listeners
    private ListenerRegistration blocksListener;
    private ListenerRegistration expensesListener;
    private ListenerRegistration revenuesListener;
    private ListenerRegistration tasksListener;
    private ListenerRegistration photosListener;
    private ListenerRegistration usersListener;
    private ListenerRegistration assignmentsListener;
    private ListenerRegistration qualityBatchesListener;
    private ListenerRegistration suppliersListener;
    private ListenerRegistration batchTraceabilityListener;
    private ListenerRegistration masterTraceabilityListener;
    private ListenerRegistration certificationsListener;
    private ListenerRegistration auditsListener;
    private ListenerRegistration inspectionsListener;
    private ListenerRegistration verificationsListener;
    private ListenerRegistration documentsListener;
    private ListenerRegistration supplierCommitmentsListener;
    private ListenerRegistration supplierInspectionsListener;
    private ListenerRegistration supplierDisputesListener;
    private ListenerRegistration scoreHistoryListener;
    private List<QualityBatchEntity> pendingBatches = new ArrayList<>();
    // Stage 1, step 6: children skipped because the parent certification wasn't
    // local yet. Drained when the certification lands. In-memory only — a restart
    // re-delivers each collection's snapshot, so persistence would only leak.
    private final Set<String> pendingAuditIds = new HashSet<>();
    private final Set<String> pendingInspectionIds = new HashSet<>();
    private final Set<String> pendingVerificationIds = new HashSet<>();
    private final Set<String> pendingDocumentIds = new HashSet<>();



    private FirestoreSyncManager(Context context) {
        db = FirebaseFirestore.getInstance();
        database = AppDatabase.getInstance(context);
    }

    public static synchronized FirestoreSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreSyncManager(context);
        }
        return instance;
    }

    /**
     * Start listening for all changes for a farm
     */
    public void startListening(String farmId, boolean isOwner) {
        this.farmId = farmId;
        Log.d(TAG, "Starting Firestore listeners for farm: " + farmId);

        listenToBlocks();
        listenToExpenses();
        listenToRevenues();
        listenToTasks();
        listenToPhotos();
        if (isOwner) {
            listenToUsers();
        }
        listenToAssignments();
        listenToQualityBatches();
        listenToSuppliers();
        listenToMasterTraceability();
        listenToBatchTraceability();
        listenToCertifications();
        listenToAudits();
        listenToInspections();
        listenToVerifications();
        listenToDocuments();
        listenToSupplierCommitments();
        listenToSupplierInspections();
        listenToSupplierDisputes();
        listenToScoreHistory();
    }

    /**
     * Stop all listeners (call in onDestroy)
     */
    public void stopListening() {
        if (blocksListener != null) blocksListener.remove();
        if (expensesListener != null) expensesListener.remove();
        if (revenuesListener != null) revenuesListener.remove();
        if (tasksListener != null) tasksListener.remove();
        if (photosListener != null) photosListener.remove();
        if (usersListener != null) usersListener.remove();
        if (assignmentsListener != null) assignmentsListener.remove();
        if (qualityBatchesListener != null) qualityBatchesListener.remove();
        if (suppliersListener != null) suppliersListener.remove();
        if (batchTraceabilityListener != null) batchTraceabilityListener.remove();
        if (masterTraceabilityListener != null) masterTraceabilityListener.remove();
        if (certificationsListener != null) certificationsListener.remove();
        if (auditsListener != null) auditsListener.remove();
        if (inspectionsListener != null) inspectionsListener.remove();
        if (verificationsListener != null) verificationsListener.remove();
        if (documentsListener != null) documentsListener.remove();
        if (supplierCommitmentsListener != null) supplierCommitmentsListener.remove();
        if (supplierInspectionsListener != null) supplierInspectionsListener.remove();
        if (supplierDisputesListener != null)     supplierDisputesListener.remove();
        if (scoreHistoryListener != null)         scoreHistoryListener.remove();


        Log.d(TAG, "All Firestore listeners stopped");
    }

    public boolean isRemoteUpdate() {
        return isApplyingRemoteUpdate;
    }

    // ========================================
    // BLOCKS LISTENER with Conflict Resolution
    // ========================================

    private void listenToBlocks() {
        blocksListener = db.collection("farms")
                .document(farmId)
                .collection("blocks")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Blocks listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            BlockEntity remoteBlock = EntityMapper.fromFirestoreBlock(dc.getDocument());
                            if (remoteBlock == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // Get local version
                                    BlockEntity localBlock = database.blockDao().getBlockById(remoteBlock.getId());

                                    isApplyingRemoteUpdate = true;

                                    if (localBlock == null) {
                                        Log.d(TAG, "New block from Firestore: " + remoteBlock.getBlockName());
                                        database.blockDao().insert(remoteBlock);
                                    } else {
                                        if (remoteBlock.getUpdatedAt() > localBlock.getUpdatedAt()) {
                                            Log.d(TAG, "Updating block (remote newer) ID=" + remoteBlock.getId()
                                                    + " local=" + localBlock.getUpdatedAt()
                                                    + " remote=" + remoteBlock.getUpdatedAt());
                                            database.blockDao().update(remoteBlock);
                                        } else {
                                            Log.d(TAG, "Skipping block (local newer) ID=" + remoteBlock.getId()
                                                    + " local=" + localBlock.getUpdatedAt()
                                                    + " remote=" + remoteBlock.getUpdatedAt());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    Log.d(TAG, "Block removed from Firestore: " + remoteBlock.getBlockName());
                                    database.blockDao().delete(remoteBlock);
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "Blocks listener registered");
    }

    // ========================================
    // EXPENSES LISTENER with Conflict Resolution
    // ========================================

    private void listenToExpenses() {
        expensesListener = db.collection("farms")
                .document(farmId)
                .collection("expenses")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Expenses listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            ExpenseEntity remoteExpense = EntityMapper.fromFirestoreExpense(dc.getDocument());
                            if (remoteExpense == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // ✅ Check if batch/supplier exists before inserting
                                    if (remoteExpense.getBatchId() != null && remoteExpense.getBatchId() > 0) {
                                        QualityBatchEntity batch = database.qualityBatchDao()
                                                .getBatchById(remoteExpense.getBatchId());
                                        if (batch == null) {
                                            Log.w(TAG, "⚠️ Skipping expense - batch " + remoteExpense.getBatchId() +
                                                    " not found in local DB. Will sync later.");
                                            // Queue for later processing or skip for now
                                            continue;
                                        }
                                    }

                                    if (remoteExpense.getSupplierId() != null && remoteExpense.getSupplierId() > 0) {
                                        SupplierEntity supplier = database.supplierDao()
                                                .getSupplierById(remoteExpense.getSupplierId());
                                        if (supplier == null) {
                                            Log.w(TAG, "⚠️ Skipping expense - supplier " + remoteExpense.getSupplierId() +
                                                    " not found in local DB. Will sync later.");
                                            continue;
                                        }
                                    }

                                    ExpenseEntity localExpense = database.expenseDao().getExpenseById(remoteExpense.getId());

                                    isApplyingRemoteUpdate = true;

                                    if (localExpense == null) {
                                        database.expenseDao().insert(remoteExpense);
                                        Log.d(TAG, "✅ New expense: " + remoteExpense.getId() +
                                                " (Batch: " + remoteExpense.getBatchNumber() +
                                                ", Supplier: " + remoteExpense.getSupplierName() + ")");
                                    } else {
                                        if (remoteExpense.getUpdatedAt() > localExpense.getUpdatedAt()) {
                                            database.expenseDao().update(remoteExpense);
                                            Log.d(TAG, "📝 Updated expense (remote newer): " + remoteExpense.getId());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped expense (local newer): " + remoteExpense.getId());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    database.expenseDao().delete(remoteExpense);
                                    Log.d(TAG, "❌ Expense deleted: " + remoteExpense.getId());
                                    break;
                            }
                        }
                    });
                });
    }

    // ========================================
    // REVENUES LISTENER with Conflict Resolution
    // ========================================

    private void listenToRevenues() {
        revenuesListener = db.collection("farms")
                .document(farmId)
                .collection("revenues")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Revenues listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            RevenueEntity remoteRevenue = EntityMapper.fromFirestoreRevenue(dc.getDocument());
                            if (remoteRevenue == null) continue;

                            // ✅ First ensure the farm exists
                            FarmEntity farm = database.farmDao().getFarmById(remoteRevenue.getFarmId());
                            if (farm == null) {
                                Log.w(TAG, "⚠️ Skipping revenue " + remoteRevenue.getId() +
                                        " - farm " + remoteRevenue.getFarmId() + " not found");
                                // Create default farm if it's farm 1
                                if (remoteRevenue.getFarmId() == 1) {
                                    createDefaultFarm();
                                }
                                continue;
                            }

                            // ✅ Check if batch/supplier exists before inserting
                            if (remoteRevenue.getBatchId() != null && remoteRevenue.getBatchId() > 0) {
                                QualityBatchEntity batch = database.qualityBatchDao()
                                        .getBatchById(remoteRevenue.getBatchId());
                                if (batch == null) {
                                    Log.w(TAG, "⚠️ Skipping revenue - batch " + remoteRevenue.getBatchId() +
                                            " not found in local DB. Will sync later.");
                                    continue;
                                }
                            }

                            if (remoteRevenue.getSupplierId() != null && remoteRevenue.getSupplierId() > 0) {
                                SupplierEntity supplier = database.supplierDao()
                                        .getSupplierById(remoteRevenue.getSupplierId());
                                if (supplier == null) {
                                    Log.w(TAG, "⚠️ Skipping revenue - supplier " + remoteRevenue.getSupplierId() +
                                            " not found in local DB. Will sync later.");
                                    continue;
                                }
                            }

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    RevenueEntity localRevenue = database.revenueDao().getRevenueById(remoteRevenue.getId());

                                    isApplyingRemoteUpdate = true;

                                    if (localRevenue == null) {
                                        try {
                                            database.revenueDao().insert(remoteRevenue);
                                            Log.d(TAG, "✅ New revenue: " + remoteRevenue.getId() +
                                                    " (Batch: " + remoteRevenue.getBatchNumber() +
                                                    ", Supplier: " + remoteRevenue.getSupplierName() + ")");
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error inserting revenue", e);
                                        }
                                    } else {
                                        if (remoteRevenue.getUpdatedAt() > localRevenue.getUpdatedAt()) {
                                            database.revenueDao().update(remoteRevenue);
                                            Log.d(TAG, "📝 Updated revenue (remote newer): " + remoteRevenue.getId());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped revenue (local newer): " + remoteRevenue.getId());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    database.revenueDao().delete(remoteRevenue);
                                    Log.d(TAG, "❌ Revenue deleted: " + remoteRevenue.getId());
                                    break;
                            }
                        }
                    });
                });
    }

    private void createDefaultFarm() {
        executor.execute(() -> {
            FarmEntity farm = new FarmEntity(
                    "AfriNuts Odienné",
                    "Odienné, Côte d'Ivoire",
                    35.0,
                    35.0,
                    100,
                    2024
            );
            farm.setId(1);
            database.farmDao().insert(farm);
            Log.d(TAG, "Created default farm with ID: 1");
        });
    }

    // ========================================
    // TASKS LISTENER with Conflict Resolution
    // ========================================

    private void listenToTasks() {
        tasksListener = db.collection("farms")
                .document(farmId)
                .collection("tasks")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Tasks listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            try {
                                // Add null check for document
                                if (dc.getDocument() == null) {
                                    Log.e(TAG, "Document is null in tasks listener");
                                    continue;
                                }

                                TaskEntity remoteTask = EntityMapper.fromFirestoreTask(dc.getDocument());
                                if (remoteTask == null) {
                                    Log.e(TAG, "Failed to map task from Firestore document: " + dc.getDocument().getId());
                                    continue;
                                }

                                // Validate required fields
                                if (remoteTask.getId() <= 0) {
                                    Log.e(TAG, "Task has invalid ID: " + remoteTask.getId());
                                    continue;
                                }

                                if (remoteTask.getFarmId() <= 0) {
                                    Log.e(TAG, "Task has invalid farmId: " + remoteTask.getFarmId());
                                    continue;
                                }

                                switch (dc.getType()) {
                                    case ADDED:
                                    case MODIFIED:
                                        TaskEntity localTask = null;
                                        try {
                                            localTask = database.taskDao().getTaskById(remoteTask.getId());
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error getting local task: " + remoteTask.getId(), e);
                                        }

                                        isApplyingRemoteUpdate = true;

                                        try {
                                            if (localTask == null) {
                                                database.taskDao().insertTask(remoteTask);
                                                Log.d(TAG, "➕ New task synced: " + remoteTask.getTitle());
                                            } else {
                                                if (remoteTask.getUpdatedAt() > localTask.getUpdatedAt()) {
                                                    database.taskDao().updateTask(remoteTask);
                                                    Log.d(TAG, "Updated task (remote newer): " + remoteTask.getTitle());
                                                } else {
                                                    Log.d(TAG, "Skipped task (local newer): " + remoteTask.getTitle());
                                                }
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error applying task update: " + remoteTask.getId(), e);
                                        } finally {
                                            isApplyingRemoteUpdate = false;
                                        }
                                        break;

                                    case REMOVED:
                                        try {
                                            TaskEntity taskToDelete = database.taskDao().getTaskById(remoteTask.getId());
                                            if (taskToDelete != null) {
                                                database.taskDao().deleteTask(taskToDelete);
                                                Log.d(TAG, "Task deleted: " + remoteTask.getTitle());
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error deleting task: " + remoteTask.getId(), e);
                                        }
                                        break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Unhandled exception in tasks listener", e);
                            }
                        }
                    });
                });
    }

    // ========================================
    // PHOTOS LISTENER with Conflict Resolution
    // ========================================

    private void listenToPhotos() {
        photosListener = db.collection("farms")
                .document(farmId)
                .collection("photos")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Photos listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    Log.d(TAG, "📸 Photos listener received " + snapshots.size() + " changes");

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            PhotoEntity remotePhoto = EntityMapper.fromFirestorePhoto(dc.getDocument());
                            if (remotePhoto == null) continue;

                            // ✅ Ensure farm exists
                            FarmEntity farm = database.farmDao().getFarmById(remotePhoto.getFarmId());
                            if (farm == null) {
                                Log.w(TAG, "⚠️ Skipping photo " + remotePhoto.getId() +
                                        " - farm " + remotePhoto.getFarmId() + " not found");
                                if (remotePhoto.getFarmId() == 1) {
                                    createDefaultFarm();
                                }
                                continue;
                            }

                            // ✅ Ensure block exists if applicable
                            if (remotePhoto.getBlockId() != null) {
                                BlockEntity block = database.blockDao().getBlockById(remotePhoto.getBlockId());
                                if (block == null) {
                                    Log.w(TAG, "⚠️ Skipping photo " + remotePhoto.getId() +
                                            " - block " + remotePhoto.getBlockId() + " not found");
                                    continue;
                                }
                            }

                            // Defensive log at the start of the loop to detect stuck processing
                            Log.d(TAG, "Processing photo change: " + dc.getType() + " ID=" + remotePhoto.getId());

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    PhotoEntity localPhoto = database.photoDao().getPhotoById(remotePhoto.getId());

                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (localPhoto == null) {
                                            database.photoDao().insert(remotePhoto);
                                            Log.d(TAG, "➕ New photo synced: " + remotePhoto.getId());
                                        } else {
                                            if (remotePhoto.getUpdatedAt() > localPhoto.getUpdatedAt()) {
                                                database.photoDao().update(remotePhoto);
                                                Log.d(TAG, "📝 Updated photo: " + remotePhoto.getId());
                                            } else {
                                                Log.d(TAG, "⏭️ Skipped older photo: " + remotePhoto.getId());
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "❌ Error processing photo sync: " + remotePhoto.getId(), e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;

                                case REMOVED:
                                    database.photoDao().delete(remotePhoto);
                                    Log.d(TAG, "❌ Photo deleted: " + remotePhoto.getId());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Photos listener registered for farms/" + farmId + "/photos");
    }

    // ========================================
    // USERS LISTENER with Conflict Resolution
    // ========================================

    private void listenToUsers() {
        usersListener = db.collection("users")
                .whereEqualTo("farmId", farmId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Users listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            UserEntity remoteUser = EntityMapper.fromFirestoreUser(dc.getDocument());
                            if (remoteUser == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // ✅ Only sync if user already exists locally
                                    UserEntity existing = database.userDao().getUserByUid(remoteUser.getUid());

                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (existing != null) {
                                            // User exists locally - update
                                            existing.setName(remoteUser.getName());
                                            existing.setRole(remoteUser.getRole());
                                            existing.setPhone(remoteUser.getPhone());
                                            existing.setActive(remoteUser.isActive());
                                            database.userDao().update(existing);
                                            Log.d(TAG, "📝 Updated user from Firestore: " + remoteUser.getName());
                                        } else {
                                            Log.w(TAG, "⚠️ User not in local DB, skipping sync: " + remoteUser.getUid());
                                            // ❌ NO INSERT - user must be created by owner only
                                        }
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;

                                case REMOVED:
                                    UserEntity userToDelete = database.userDao().getUserByUid(remoteUser.getUid());
                                    if (userToDelete != null) {
                                        database.userDao().delete(userToDelete);
                                        Log.d(TAG, "❌ User deleted from Firestore: " + remoteUser.getName());
                                    }
                                    break;
                            }
                        }
                    });
                });
    }

    // ========================================
    // SUPPLIERS LISTENER
    // ========================================

    private void listenToSuppliers() {
        suppliersListener = db.collection("farms")
                .document(farmId)
                .collection("suppliers")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Suppliers listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            SupplierEntity remoteSupplier =
                                    EntityMapper.fromFirestoreSupplier(dc.getDocument());
                            if (remoteSupplier == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    SupplierEntity localSupplier =
                                            database.supplierDao().getSupplierById(remoteSupplier.getId());

                                    isApplyingRemoteUpdate = true;

                                    if (localSupplier == null) {
                                        database.supplierDao().insert(remoteSupplier);
                                        Log.d(TAG, "➕ New supplier: " + remoteSupplier.getName());
                                    } else {
                                        if (remoteSupplier.getUpdatedAt() > localSupplier.getUpdatedAt()) {
                                            database.supplierDao().update(remoteSupplier);
                                            Log.d(TAG, "📥 Updated supplier: " + remoteSupplier.getName());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped supplier (local newer): " +
                                                    remoteSupplier.getName());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    // ✅ CASCADE DELETE: Delete all associated batches first
                                    try {
                                        // 1. Get all batches for this supplier
                                        List<QualityBatchEntity> batches = database.qualityBatchDao()
                                                .getBatchesBySupplierIdSync(remoteSupplier.getId());

                                        // 2. Delete all batches
                                        for (QualityBatchEntity batch : batches) {
                                            database.qualityBatchDao().delete(batch);
                                            Log.d(TAG, "🗑️ Deleted batch: " + batch.getBatchNumber() +
                                                    " for supplier: " + remoteSupplier.getName());
                                        }

                                        // 3. Delete the supplier
                                        database.supplierDao().delete(remoteSupplier);
                                        Log.d(TAG, "🗑️ Deleted supplier: " + remoteSupplier.getName() +
                                                " with " + batches.size() + " batches");

                                    } catch (Exception e) {
                                        Log.e(TAG, "Error deleting supplier and batches", e);
                                    }
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Suppliers listener registered");
    }

    // Add this helper method
    private void deleteSupplierAndBatches(SupplierEntity supplier) {
        try {
            // 1. Get all batches for this supplier
            List<QualityBatchEntity> batches = database.qualityBatchDao()
                    .getBatchesBySupplierIdSync(supplier.getId());

            // 2. Delete all batches
            for (QualityBatchEntity batch : batches) {
                database.qualityBatchDao().delete(batch);
                Log.d(TAG, "🗑️ Deleted batch: " + batch.getBatchNumber() + " for supplier: " + supplier.getName());
            }

            // 3. Delete the supplier
            database.supplierDao().delete(supplier);
            Log.d(TAG, "🗑️ Deleted supplier: " + supplier.getName() + " with " + batches.size() + " batches");

        } catch (Exception e) {
            Log.e(TAG, "Error deleting supplier and batches", e);
        }
    }

    // ========================================
// SUPPLIER COMMITMENTS LISTENER
// ========================================

    // ========================================
// SUPPLIER COMMITMENTS LISTENER
// ========================================

    private void listenToSupplierCommitments() {
        supplierCommitmentsListener = db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection("supplier_commitments")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Commitments listener failed", error);
                        return;
                    }
                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            SupplierCommitmentEntity remote =
                                    EntityMapper.mapToCommitment(dc.getDocument());
                            if (remote == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    SupplierCommitmentEntity local =
                                            database.supplierCommitmentDao().getById(remote.getId());
                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (local == null) {
                                            // 3.6: parent-existence guard
                                            if (database.supplierDao()
                                                    .getSupplierById(remote.getSupplierId()) == null) {
                                                Log.w(TAG, "Skipping commitment " + remote.getId()
                                                        + " — supplier " + remote.getSupplierId()
                                                        + " not present locally");
                                                break;
                                            }
                                            database.supplierCommitmentDao().insert(remote);
                                            Log.d(TAG, "➕ Commitment: " + remote.getId());
                                        } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                                            database.supplierCommitmentDao().update(remote);
                                            Log.d(TAG, "📥 Updated commitment: " + remote.getId());
                                        }
                                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                                        Log.w(TAG, "Commitment " + remote.getId()
                                                + " rejected by FK constraint — skipped", e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "❌ Commitment removed remotely: " + remote.getId()
                                            + " (local record kept — audit trail)");
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Supplier commitments listener registered");
    }

// ========================================
// SUPPLIER INSPECTIONS LISTENER
// ========================================

    private void listenToSupplierInspections() {
        supplierInspectionsListener = db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection("supplier_inspections")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Supplier inspections listener failed", error);
                        return;
                    }
                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            SupplierInspectionEntity remote =
                                    EntityMapper.mapToSupplierInspection(dc.getDocument());
                            if (remote == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    SupplierInspectionEntity local =
                                            database.supplierInspectionDao().getById(remote.getId());
                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (local == null) {
                                            // 3.6: parent-existence guard
                                            if (database.supplierDao()
                                                    .getSupplierById(remote.getSupplierId()) == null) {
                                                Log.w(TAG, "Skipping inspection " + remote.getId()
                                                        + " — supplier " + remote.getSupplierId()
                                                        + " not present locally");
                                                break;
                                            }
                                            database.supplierInspectionDao().insert(remote);
                                            Log.d(TAG, "➕ Inspection: " + remote.getId());
                                        } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                                            database.supplierInspectionDao().update(remote);
                                            Log.d(TAG, "📥 Updated inspection: " + remote.getId());
                                        }
                                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                                        Log.w(TAG, "Inspection " + remote.getId()
                                                + " rejected by FK constraint — skipped", e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "❌ Inspection removed remotely: " + remote.getId()
                                            + " (local record kept — audit trail)");
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Supplier inspections listener registered");
    }

// ========================================
// SUPPLIER DISPUTES LISTENER
// ========================================

    private void listenToSupplierDisputes() {
        supplierDisputesListener = db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection("supplier_disputes")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Supplier disputes listener failed", error);
                        return;
                    }
                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            SupplierDisputeEntity remote =
                                    EntityMapper.mapToDispute(dc.getDocument());
                            if (remote == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    SupplierDisputeEntity local =
                                            database.supplierDisputeDao().getById(remote.getId());
                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (local == null) {
                                            // 3.6: parent-existence guard
                                            if (database.supplierDao()
                                                    .getSupplierById(remote.getSupplierId()) == null) {
                                                Log.w(TAG, "Skipping dispute " + remote.getId()
                                                        + " — supplier " + remote.getSupplierId()
                                                        + " not present locally");
                                                break;
                                            }
                                            database.supplierDisputeDao().insert(remote);
                                            Log.d(TAG, "➕ Dispute: " + remote.getId());
                                        } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                                            database.supplierDisputeDao().update(remote);
                                            Log.d(TAG, "📥 Updated dispute: " + remote.getId());
                                        }
                                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                                        Log.w(TAG, "Dispute " + remote.getId()
                                                + " rejected by FK constraint — skipped", e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "❌ Dispute removed remotely: " + remote.getId()
                                            + " (local record kept — audit trail)");
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Supplier disputes listener registered");
    }

// ========================================
// SCORE HISTORY LISTENER (Firestore → Room only)
// ========================================

    private void listenToScoreHistory() {
        scoreHistoryListener = db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection("supplier_score_history")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Score history listener failed", error);
                        return;
                    }
                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            SupplierScoreSnapshotEntity remote =
                                    EntityMapper.mapToSnapshot(dc.getDocument());
                            if (remote == null) continue;

                            if (dc.getType() == DocumentChange.Type.ADDED
                                    || dc.getType() == DocumentChange.Type.MODIFIED) {
                                SupplierScoreSnapshotEntity local =
                                        database.supplierScoreSnapshotDao().getById(remote.getId());
                                if (local == null) {
                                    // 3.6: parent-existence guard
                                    if (database.supplierDao()
                                            .getSupplierById(remote.getSupplierId()) == null) {
                                        Log.w(TAG, "Skipping snapshot " + remote.getId()
                                                + " — supplier " + remote.getSupplierId()
                                                + " not present locally");
                                        continue;
                                    }
                                    try {
                                        database.supplierScoreSnapshotDao().insert(remote);
                                        Log.d(TAG, "➕ Snapshot: " + remote.getId()
                                                + " for supplier " + remote.getSupplierId());
                                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                                        Log.w(TAG, "Snapshot " + remote.getId()
                                                + " rejected by FK constraint — skipped", e);
                                    }
                                }
                                // else: immutable; do nothing
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Score history listener registered");
    }

    // ========================================
    // QUALITY BATCHES LISTENER with Supplier Check
    // ========================================

    private void listenToQualityBatches() {
        qualityBatchesListener = db.collection("farms")
                .document(farmId)
                .collection("quality_batches")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Quality batches listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            QualityBatchEntity remoteBatch =
                                    EntityMapper.fromFirestoreQualityBatch(dc.getDocument());
                            if (remoteBatch == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // Existing supplier queue behaviour (kept)
                                    boolean supplierExists = true;
                                    if (remoteBatch.getSupplierId() != null && remoteBatch.getSupplierId() > 0) {
                                        SupplierEntity supplier = database.supplierDao()
                                                .getSupplierById(remoteBatch.getSupplierId());
                                        if (supplier == null) {
                                            supplierExists = false;
                                            pendingBatches.add(remoteBatch);
                                            Log.d(TAG, "⏳ Queued batch " + remoteBatch.getBatchNumber() +
                                                    " waiting for supplier " + remoteBatch.getSupplierId());
                                        }
                                    }

                                    if (!supplierExists) {
                                        continue;
                                    }

                                    processPendingBatches(remoteBatch.getSupplierId());

                                    // 3.6: block guard — no queue for blocks; skip and log
                                    if (remoteBatch.getBlockId() != null && remoteBatch.getBlockId() > 0) {
                                        if (database.blockDao().getBlockById(remoteBatch.getBlockId()) == null) {
                                            Log.w(TAG, "Skipping quality batch " + remoteBatch.getBatchNumber()
                                                    + " — block " + remoteBatch.getBlockId()
                                                    + " not present locally");
                                            continue;
                                        }
                                    }

                                    QualityBatchEntity localBatch =
                                            database.qualityBatchDao().getBatchById(remoteBatch.getId());

                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (localBatch == null) {
                                            database.qualityBatchDao().insert(remoteBatch);
                                            Log.d(TAG, "➕ New quality batch: " + remoteBatch.getBatchNumber());
                                        } else if (remoteBatch.getUpdatedAt() > localBatch.getUpdatedAt()) {
                                            database.qualityBatchDao().update(remoteBatch);
                                            Log.d(TAG, "📥 Updated quality batch: " + remoteBatch.getBatchNumber());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped quality batch (local newer): " +
                                                    remoteBatch.getBatchNumber());
                                        }
                                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                                        Log.w(TAG, "Quality batch " + remoteBatch.getBatchNumber()
                                                + " rejected by FK constraint — skipped", e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;

                                case REMOVED:
                                    database.qualityBatchDao().delete(remoteBatch);
                                    Log.d(TAG, "❌ Quality batch deleted: " + remoteBatch.getBatchNumber());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Quality batches listener registered");
    }

    private void processPendingBatches(Long supplierId) {
        if (supplierId == null) return;

        List<QualityBatchEntity> toProcess = new ArrayList<>();
        for (QualityBatchEntity batch : pendingBatches) {
            if (batch.getSupplierId() != null && batch.getSupplierId().equals(supplierId)) {
                toProcess.add(batch);
            }
        }

        for (QualityBatchEntity batch : toProcess) {
            pendingBatches.remove(batch);
            // Insert the batch now that supplier exists
            database.qualityBatchDao().insert(batch);
            Log.d(TAG, "✅ Processed queued batch: " + batch.getBatchNumber());
        }
    }

    // ========================================
    // BATCH TRACEABILITY LISTENER
    // ========================================

    private void listenToBatchTraceability() {
        batchTraceabilityListener = db.collection("farms")
                .document(farmId)
                .collection("batch_traceability")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Batch traceability listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            BatchTraceabilityEntity remoteTraceability =
                                    EntityMapper.mapToBatchTraceability(dc.getDocument());
                            if (remoteTraceability == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // Existing quality-batch guard
                                    if (remoteTraceability.getQualityBatchId() != null) {
                                        QualityBatchEntity batch = database.qualityBatchDao()
                                                .getBatchById(remoteTraceability.getQualityBatchId());
                                        if (batch == null) {
                                            Log.w(TAG, "⚠️ Skipping traceability - quality batch " +
                                                    remoteTraceability.getQualityBatchId() + " not found");
                                            continue;
                                        }
                                    }

                                    // 3.6: revenue guard (renamed FK; same constraint class)
                                    if (remoteTraceability.getRevenueId() != null
                                            && remoteTraceability.getRevenueId() > 0) {
                                        if (database.revenueDao()
                                                .getRevenueById(remoteTraceability.getRevenueId()) == null) {
                                            Log.w(TAG, "Skipping traceability "
                                                    + remoteTraceability.getBatchNumber()
                                                    + " — revenue " + remoteTraceability.getRevenueId()
                                                    + " not present locally");
                                            continue;
                                        }
                                    }

                                    BatchTraceabilityEntity localTraceability =
                                            database.batchTraceabilityDao()
                                                    .getById(remoteTraceability.getId());

                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (localTraceability == null) {
                                            database.batchTraceabilityDao().insert(remoteTraceability);
                                            Log.d(TAG, "➕ New traceability: " + remoteTraceability.getBatchNumber());
                                        } else if (remoteTraceability.getUpdatedAt() > localTraceability.getUpdatedAt()) {
                                            database.batchTraceabilityDao().update(remoteTraceability);
                                            Log.d(TAG, "📥 Updated traceability: " + remoteTraceability.getBatchNumber());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped traceability (local newer): " +
                                                    remoteTraceability.getBatchNumber());
                                        }
                                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                                        Log.w(TAG, "Traceability " + remoteTraceability.getBatchNumber()
                                                + " rejected by FK constraint — skipped", e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;

                                case REMOVED:
                                    database.batchTraceabilityDao().delete(remoteTraceability);
                                    Log.d(TAG, "❌ Traceability deleted: " + remoteTraceability.getBatchNumber());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Batch traceability listener registered");
    }

    // ========================================
    // MASTER TRACEABILITY LISTENER
    // ========================================

    // Add this method
    private void listenToMasterTraceability() {
        masterTraceabilityListener = db.collection("farms")
                .document(farmId)
                .collection("master_traceability")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Master traceability listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            MasterBatchTraceabilityEntity remoteMaster =
                                    EntityMapper.mapToMasterTraceability(dc.getDocument());
                            if (remoteMaster == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // 3.6: symmetric parent guard
                                    if (remoteMaster.getFarmId() > 0
                                            && database.farmDao().getFarmById(remoteMaster.getFarmId()) == null) {
                                        Log.w(TAG, "Skipping master traceability "
                                                + remoteMaster.getMasterBatchNumber()
                                                + " — farm " + remoteMaster.getFarmId()
                                                + " not present locally");
                                        continue;
                                    }

                                    MasterBatchTraceabilityEntity localMaster =
                                            database.masterTraceabilityDao()
                                                    .getById(remoteMaster.getId());

                                    isApplyingRemoteUpdate = true;
                                    try {
                                        if (localMaster == null) {
                                            database.masterTraceabilityDao().insert(remoteMaster);
                                            Log.d(TAG, "➕ New master traceability: " + remoteMaster.getMasterBatchNumber());
                                        } else if (remoteMaster.getUpdatedAt() > localMaster.getUpdatedAt()) {
                                            database.masterTraceabilityDao().update(remoteMaster);
                                            Log.d(TAG, "📥 Updated master traceability: " + remoteMaster.getMasterBatchNumber());
                                        }
                                    } catch (android.database.sqlite.SQLiteConstraintException e) {
                                        Log.w(TAG, "Master traceability " + remoteMaster.getMasterBatchNumber()
                                                + " rejected by FK constraint — skipped", e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;

                                case REMOVED:
                                    database.masterTraceabilityDao().delete(remoteMaster);
                                    Log.d(TAG, "❌ Master traceability deleted: " + remoteMaster.getMasterBatchNumber());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Master traceability listener registered");
    }


    // Add certification listener method
    // In FirestoreSyncManager.java - update listenToCertifications()

    private void listenToCertifications() {
        certificationsListener = db.collection("farms")
                .document(farmId)
                .collection("certifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Certifications listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            // ✅ Use the correct method name: mapToCertification
                            CertificationEntity remoteCert =
                                    EntityMapper.mapToCertification(dc.getDocument());
                            if (remoteCert == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    CertificationEntity localCert =
                                            database.certificationDao()
                                                    .getCertificationById(remoteCert.getCertificationId());

                                    isApplyingRemoteUpdate = true;

                                    if (localCert == null) {
                                        database.certificationDao().insert(remoteCert);
                                        Log.d(TAG, "➕ New certification: " + remoteCert.getCertificationName());
                                        retryPendingChildren(remoteCert.getCertificationId());
                                    } else {
                                        if (remoteCert.getUpdatedAt() > localCert.getUpdatedAt()) {
                                            database.certificationDao().update(remoteCert);
                                            Log.d(TAG, "📥 Updated certification: " + remoteCert.getCertificationName());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped certification (local newer): " +
                                                    remoteCert.getCertificationName());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    // ✅ CASCADE DELETE: Delete all related records
                                    String certId = remoteCert.getCertificationId();
                                    Log.d(TAG, "🗑️ Certification removed from Firestore: " + certId);

                                    try {
                                        // 1. Delete all related records
                                        List<AuditEntity> audits = database.auditDao()
                                                .getAuditsByCertificationId(certId);
                                        for (AuditEntity audit : audits) {
                                            database.auditDao().delete(audit);
                                            Log.d(TAG, "🗑️ Deleted audit: " + audit.getAuditId());
                                        }

                                        List<InspectionEntity> inspections = database.inspectionDao()
                                                .getInspectionsByCertificationId(certId);
                                        for (InspectionEntity inspection : inspections) {
                                            database.inspectionDao().delete(inspection);
                                            Log.d(TAG, "🗑️ Deleted inspection: " + inspection.getInspectionId());
                                        }

                                        List<VerificationEntity> verifications = database.verificationDao()
                                                .getVerificationsByCertificationId(certId);
                                        for (VerificationEntity verification : verifications) {
                                            database.verificationDao().delete(verification);
                                            Log.d(TAG, "🗑️ Deleted verification: " + verification.getVerificationId());
                                        }

                                        List<DocumentEntity> documents = database.documentDao()
                                                .getDocumentsByCertificationId(certId);
                                        for (DocumentEntity document : documents) {
                                            database.documentDao().delete(document);
                                            Log.d(TAG, "🗑️ Deleted document: " + document.getDocumentId());
                                        }

                                        // 2. Delete the certification itself
                                        database.certificationDao().delete(remoteCert);
                                        Log.d(TAG, "✅ Certification and all related records deleted: " + certId);

                                    } catch (Exception e) {
                                        Log.e(TAG, "Error during cascade delete for certification: " + certId, e);
                                    }
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Certifications listener registered");
    }

    // Add listener method
    private void listenToInspections() {
        inspectionsListener = db.collection("farms")
                .document(farmId)
                .collection("inspections")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Inspections listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            InspectionEntity remoteInspection =
                                    EntityMapper.mapToInspection(dc.getDocument());
                            if (remoteInspection == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // ✅ Check if certification exists
                                    if (remoteInspection.getCertificationId() != null) {
                                        CertificationEntity certification = database.certificationDao()
                                                .getCertificationById(remoteInspection.getCertificationId());

                                        if (certification == null) {
                                            Log.w(TAG, "⚠️ Skipping inspection " + remoteInspection.getInspectionId() +
                                                    " - certification " + remoteInspection.getCertificationId() +
                                                    " not found in local DB. Will sync later.");
                                            pendingInspectionIds.add(remoteInspection.getInspectionId());
                                            continue;
                                        }
                                    }

                                    InspectionEntity localInspection =
                                            database.inspectionDao()
                                                    .getInspectionById(remoteInspection.getInspectionId());

                                    isApplyingRemoteUpdate = true;

                                    if (localInspection == null) {
                                        database.inspectionDao().insert(remoteInspection);
                                        Log.d(TAG, "➕ New inspection: " + remoteInspection.getInspectionId());
                                    } else {
                                        if (remoteInspection.getUpdatedAt() > localInspection.getUpdatedAt()) {
                                            database.inspectionDao().update(remoteInspection);
                                            Log.d(TAG, "📥 Updated inspection: " + remoteInspection.getInspectionId());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped inspection (local newer): " +
                                                    remoteInspection.getInspectionId());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    database.inspectionDao().delete(remoteInspection);
                                    Log.d(TAG, "❌ Inspection deleted: " + remoteInspection.getInspectionId());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Inspections listener registered");
    }


    // In FirestoreSyncManager.java - update listenToAudits()

    private void listenToAudits() {
        auditsListener = db.collection("farms")
                .document(farmId)
                .collection("audits")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Audits listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            AuditEntity remoteAudit =
                                    EntityMapper.mapToAudit(dc.getDocument());
                            if (remoteAudit == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // ✅ CRITICAL: Check if certification exists before inserting audit
                                    if (remoteAudit.getCertificationId() != null) {
                                        CertificationEntity certification = database.certificationDao()
                                                .getCertificationById(remoteAudit.getCertificationId());

                                        if (certification == null) {
                                            Log.w(TAG, "⚠️ Skipping audit " + remoteAudit.getAuditId() +
                                                    " - certification " + remoteAudit.getCertificationId() +
                                                    " not found in local DB. Will sync later.");
                                            pendingAuditIds.add(remoteAudit.getAuditId());
                                            continue;
                                        }
                                    }

                                    AuditEntity localAudit =
                                            database.auditDao()
                                                    .getAuditById(remoteAudit.getAuditId());

                                    isApplyingRemoteUpdate = true;

                                    if (localAudit == null) {
                                        database.auditDao().insert(remoteAudit);
                                        Log.d(TAG, "➕ New audit: " + remoteAudit.getAuditId());
                                    } else {
                                        if (remoteAudit.getUpdatedAt() > localAudit.getUpdatedAt()) {
                                            database.auditDao().update(remoteAudit);
                                            Log.d(TAG, "📥 Updated audit: " + remoteAudit.getAuditId());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped audit (local newer): " +
                                                    remoteAudit.getAuditId());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    database.auditDao().delete(remoteAudit);
                                    Log.d(TAG, "❌ Audit deleted: " + remoteAudit.getAuditId());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Audits listener registered");
    }

    private void listenToVerifications() {
        verificationsListener = db.collection("farms")
                .document(farmId)
                .collection("verifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Verifications listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            VerificationEntity remoteVerification =
                                    EntityMapper.mapToVerification(dc.getDocument());
                            if (remoteVerification == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // ✅ Check if certification exists
                                    if (remoteVerification.getCertificationId() != null) {
                                        CertificationEntity certification = database.certificationDao()
                                                .getCertificationById(remoteVerification.getCertificationId());

                                        if (certification == null) {
                                            Log.w(TAG, "⚠️ Skipping verification " + remoteVerification.getVerificationId() +
                                                    " - certification " + remoteVerification.getCertificationId() +
                                                    " not found in local DB. Will sync later.");
                                            pendingVerificationIds.add(remoteVerification.getVerificationId());
                                            continue;
                                        }
                                    }

                                    VerificationEntity localVerification =
                                            database.verificationDao()
                                                    .getVerificationById(remoteVerification.getVerificationId());

                                    isApplyingRemoteUpdate = true;

                                    if (localVerification == null) {
                                        database.verificationDao().insert(remoteVerification);
                                        Log.d(TAG, "➕ New verification: " + remoteVerification.getVerificationId());
                                    } else {
                                        if (remoteVerification.getUpdatedAt() > localVerification.getUpdatedAt()) {
                                            database.verificationDao().update(remoteVerification);
                                            Log.d(TAG, "📥 Updated verification: " + remoteVerification.getVerificationId());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped verification (local newer): " +
                                                    remoteVerification.getVerificationId());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    database.verificationDao().delete(remoteVerification);
                                    Log.d(TAG, "❌ Verification deleted: " + remoteVerification.getVerificationId());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Verifications listener registered");
    }

    private void listenToDocuments() {
        documentsListener = db.collection("farms")
                .document(farmId)
                .collection("documents")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Documents listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentEntity remoteDocument =
                                    EntityMapper.mapToDocument(dc.getDocument());
                            if (remoteDocument == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // ✅ Check if certification exists
                                    if (remoteDocument.getCertificationId() != null) {
                                        CertificationEntity certification = database.certificationDao()
                                                .getCertificationById(remoteDocument.getCertificationId());

                                        if (certification == null) {
                                            Log.w(TAG, "⚠️ Skipping document " + remoteDocument.getDocumentId() +
                                                    " - certification " + remoteDocument.getCertificationId() +
                                                    " not found in local DB. Will sync later.");
                                            pendingDocumentIds.add(remoteDocument.getDocumentId());
                                            continue;
                                        }
                                    }

                                    DocumentEntity localDocument =
                                            database.documentDao()
                                                    .getDocumentById(remoteDocument.getDocumentId());

                                    isApplyingRemoteUpdate = true;

                                    if (localDocument == null) {
                                        database.documentDao().insert(remoteDocument);
                                        Log.d(TAG, "➕ New document: " + remoteDocument.getDocumentId());
                                    } else {
                                        if (remoteDocument.getUpdatedAt() > localDocument.getUpdatedAt()) {
                                            database.documentDao().update(remoteDocument);
                                            Log.d(TAG, "📥 Updated document: " + remoteDocument.getDocumentId());
                                        } else {
                                            Log.d(TAG, "⏭️ Skipped document (local newer): " +
                                                    remoteDocument.getDocumentId());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    database.documentDao().delete(remoteDocument);
                                    Log.d(TAG, "❌ Document deleted: " + remoteDocument.getDocumentId());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "👂 Documents listener registered");
    }

    /**
     * Stage 1, step 6. Children whose parent certification wasn't local were
     * skipped and their IDs parked in the four pending sets. Now that the parent
     * has landed, re-fetch each one once and run it through the same insert path
     * the listener uses. Fire-and-forget: the .get() is async and its callback
     * re-enters the executor, same as the snapshot listeners.
     *
     * The ID leaves its pending set in every outcome — success, document gone,
     * map returned null — so a permanently unmappable record doesn't retry forever.
     */
    private void retryPendingChildren(String certificationId) {
        drainPendingAudits(certificationId);
        drainPendingInspections(certificationId);
        drainPendingVerifications(certificationId);
        drainPendingDocuments(certificationId);
    }

    private void drainPendingAudits(String certificationId) {
        if (pendingAuditIds.isEmpty()) return;
        List<String> toRetry = new ArrayList<>(pendingAuditIds);
        pendingAuditIds.clear();
        for (String auditId : toRetry) {
            Log.d(TAG, "Retrying skipped audit " + auditId + " (parent " + certificationId + " now local)");
            db.collection("farms").document(farmId)
                    .collection("audits").document(auditId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document == null || !document.exists()) {
                            Log.d(TAG, "Retried audit " + auditId + " — document no longer exists");
                            return;
                        }
                        AuditEntity remote = EntityMapper.mapToAudit(document);
                        if (remote == null) {
                            Log.d(TAG, "Retried audit " + auditId + " — map returned null, dropped");
                            return;
                        }
                        executor.execute(() -> {
                            if (database.certificationDao()
                                    .getCertificationById(remote.getCertificationId()) == null) {
                                Log.w(TAG, "Retried audit " + auditId + " — parent still missing, dropped");
                                return;
                            }
                            AuditEntity local = database.auditDao().getAuditById(remote.getAuditId());
                            isApplyingRemoteUpdate = true;
                            try {
                                if (local == null) {
                                    database.auditDao().insert(remote);
                                    Log.d(TAG, "➕ Retried new audit: " + remote.getAuditId());
                                } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                                    database.auditDao().update(remote);
                                    Log.d(TAG, "📥 Retried updated audit: " + remote.getAuditId());
                                }
                            } catch (android.database.sqlite.SQLiteConstraintException e) {
                                Log.w(TAG, "Retried audit " + auditId
                                        + " rejected by FK constraint — dropped", e);
                            } finally {
                                isApplyingRemoteUpdate = false;
                            }
                        });
                    })
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Retried audit " + auditId + " — fetch failed, dropped", e));
        }
    }

    private void drainPendingInspections(String certificationId) {
        if (pendingInspectionIds.isEmpty()) return;
        List<String> toRetry = new ArrayList<>(pendingInspectionIds);
        pendingInspectionIds.clear();
        for (String inspectionId : toRetry) {
            Log.d(TAG, "Retrying skipped inspection " + inspectionId + " (parent " + certificationId + " now local)");
            db.collection("farms").document(farmId)
                    .collection("inspections").document(inspectionId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document == null || !document.exists()) {
                            Log.d(TAG, "Retried inspection " + inspectionId + " — document no longer exists");
                            return;
                        }
                        InspectionEntity remote = EntityMapper.mapToInspection(document);
                        if (remote == null) {
                            Log.d(TAG, "Retried inspection " + inspectionId + " — map returned null, dropped");
                            return;
                        }
                        executor.execute(() -> {
                            if (database.certificationDao()
                                    .getCertificationById(remote.getCertificationId()) == null) {
                                Log.w(TAG, "Retried inspection " + inspectionId + " — parent still missing, dropped");
                                return;
                            }
                            InspectionEntity local = database.inspectionDao()
                                    .getInspectionById(remote.getInspectionId());
                            isApplyingRemoteUpdate = true;
                            try {
                                if (local == null) {
                                    database.inspectionDao().insert(remote);
                                    Log.d(TAG, "➕ Retried new inspection: " + remote.getInspectionId());
                                } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                                    database.inspectionDao().update(remote);
                                    Log.d(TAG, "📥 Retried updated inspection: " + remote.getInspectionId());
                                }
                            } catch (android.database.sqlite.SQLiteConstraintException e) {
                                Log.w(TAG, "Retried inspection " + inspectionId
                                        + " rejected by FK constraint — dropped", e);
                            } finally {
                                isApplyingRemoteUpdate = false;
                            }
                        });
                    })
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Retried inspection " + inspectionId + " — fetch failed, dropped", e));
        }
    }

    private void drainPendingVerifications(String certificationId) {
        if (pendingVerificationIds.isEmpty()) return;
        List<String> toRetry = new ArrayList<>(pendingVerificationIds);
        pendingVerificationIds.clear();
        for (String verificationId : toRetry) {
            Log.d(TAG, "Retrying skipped verification " + verificationId + " (parent " + certificationId + " now local)");
            db.collection("farms").document(farmId)
                    .collection("verifications").document(verificationId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document == null || !document.exists()) {
                            Log.d(TAG, "Retried verification " + verificationId + " — document no longer exists");
                            return;
                        }
                        VerificationEntity remote = EntityMapper.mapToVerification(document);
                        if (remote == null) {
                            Log.d(TAG, "Retried verification " + verificationId + " — map returned null, dropped");
                            return;
                        }
                        executor.execute(() -> {
                            if (database.certificationDao()
                                    .getCertificationById(remote.getCertificationId()) == null) {
                                Log.w(TAG, "Retried verification " + verificationId + " — parent still missing, dropped");
                                return;
                            }
                            VerificationEntity local = database.verificationDao()
                                    .getVerificationById(remote.getVerificationId());
                            isApplyingRemoteUpdate = true;
                            try {
                                if (local == null) {
                                    database.verificationDao().insert(remote);
                                    Log.d(TAG, "➕ Retried new verification: " + remote.getVerificationId());
                                } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                                    database.verificationDao().update(remote);
                                    Log.d(TAG, "📥 Retried updated verification: " + remote.getVerificationId());
                                }
                            } catch (android.database.sqlite.SQLiteConstraintException e) {
                                Log.w(TAG, "Retried verification " + verificationId
                                        + " rejected by FK constraint — dropped", e);
                            } finally {
                                isApplyingRemoteUpdate = false;
                            }
                        });
                    })
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Retried verification " + verificationId + " — fetch failed, dropped", e));
        }
    }

    private void drainPendingDocuments(String certificationId) {
        if (pendingDocumentIds.isEmpty()) return;
        List<String> toRetry = new ArrayList<>(pendingDocumentIds);
        pendingDocumentIds.clear();
        for (String documentId : toRetry) {
            Log.d(TAG, "Retrying skipped document " + documentId + " (parent " + certificationId + " now local)");
            db.collection("farms").document(farmId)
                    .collection("documents").document(documentId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document == null || !document.exists()) {
                            Log.d(TAG, "Retried document " + documentId + " — document no longer exists");
                            return;
                        }
                        DocumentEntity remote = EntityMapper.mapToDocument(document);
                        if (remote == null) {
                            Log.d(TAG, "Retried document " + documentId + " — map returned null, dropped");
                            return;
                        }
                        executor.execute(() -> {
                            if (database.certificationDao()
                                    .getCertificationById(remote.getCertificationId()) == null) {
                                Log.w(TAG, "Retried document " + documentId + " — parent still missing, dropped");
                                return;
                            }
                            DocumentEntity local = database.documentDao()
                                    .getDocumentById(remote.getDocumentId());
                            isApplyingRemoteUpdate = true;
                            try {
                                if (local == null) {
                                    database.documentDao().insert(remote);
                                    Log.d(TAG, "➕ Retried new document: " + remote.getDocumentId());
                                } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
                                    database.documentDao().update(remote);
                                    Log.d(TAG, "📥 Retried updated document: " + remote.getDocumentId());
                                }
                            } catch (android.database.sqlite.SQLiteConstraintException e) {
                                Log.w(TAG, "Retried document " + documentId
                                        + " rejected by FK constraint — dropped", e);
                            } finally {
                                isApplyingRemoteUpdate = false;
                            }
                        });
                    })
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Retried document " + documentId + " — fetch failed, dropped", e));
        }
    }

    // ========================================
    // TASK ASSIGNMENTS LISTENER with Conflict Resolution
    // ========================================

    private void listenToAssignments() {
        // Listen directly to the task_assignments subcollection
        assignmentsListener = db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection("task_assignments")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Assignments listener failed", error);
                        return;
                    }

                    if (snapshots == null) return;

                    executor.execute(() -> {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            TaskAssignmentHistoryEntity remoteAssignment =
                                    EntityMapper.fromFirestoreAssignment(dc.getDocument());
                            if (remoteAssignment == null) continue;

                            switch (dc.getType()) {
                                case ADDED:
                                case MODIFIED:
                                    // ✅ CRITICAL: Verify the task exists before inserting assignment
                                    TaskEntity task = database.taskDao().getTaskById(remoteAssignment.getTaskId());
                                    if (task == null) {
                                        Log.w(TAG, "⚠️ Skipping assignment for task " + remoteAssignment.getTaskId() +
                                                " - task not found in local DB (will sync when task arrives)");
                                        continue;
                                    }

                                    TaskAssignmentHistoryEntity localAssignment =
                                            database.taskAssignmentDao().getAssignmentById(remoteAssignment.getId());

                                    isApplyingRemoteUpdate = true;

                                    try {
                                        if (localAssignment == null) {
                                            database.taskAssignmentDao().insert(remoteAssignment);
                                            Log.d(TAG, "➕ New assignment synced: " + remoteAssignment.getId() +
                                                    " for task: " + remoteAssignment.getTaskId());
                                        } else {
                                            if (remoteAssignment.getAssignedAt() > localAssignment.getAssignedAt()) {
                                                database.taskAssignmentDao().update(remoteAssignment);
                                                Log.d(TAG, "📝 Updated assignment synced: " + remoteAssignment.getId());
                                            } else {
                                                Log.d(TAG, "⏭️ Skipped older assignment: " + remoteAssignment.getId());
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error processing assignment", e);
                                    } finally {
                                        isApplyingRemoteUpdate = false;
                                    }
                                    break;

                                case REMOVED:
                                    database.taskAssignmentDao().delete(remoteAssignment);
                                    Log.d(TAG, "❌ Assignment deleted: " + remoteAssignment.getId());
                                    break;
                            }
                        }
                    });
                });
        Log.d(TAG, "Assignments listener registered");
    }
}