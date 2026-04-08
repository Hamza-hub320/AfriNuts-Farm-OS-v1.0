package com.afrinuts.farmos.data.sync;

import static com.afrinuts.farmos.data.remote.FirebaseManager.COLLECTION_FARMS;
import static com.afrinuts.farmos.data.remote.FirebaseManager.SUBCOLLECTION_TASKS;
import static com.afrinuts.farmos.data.remote.FirebaseManager.SUBCOLLECTION_TASK_ASSIGNMENTS;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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
    public void startListening(String farmId) {
        this.farmId = farmId;
        Log.d(TAG, "Starting Firestore listeners for farm: " + farmId);

        listenToBlocks();
        listenToExpenses();
        listenToRevenues();
        listenToTasks();
        listenToPhotos();
        listenToUsers();
        listenToAssignments();
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
                                    ExpenseEntity localExpense = database.expenseDao().getExpenseById(remoteExpense.getId());

                                    isApplyingRemoteUpdate = true;

                                    if (localExpense == null) {
                                        database.expenseDao().insert(remoteExpense);
                                        Log.d(TAG, "New expense: " + remoteExpense.getId());
                                    } else {
                                        if (remoteExpense.getUpdatedAt() > localExpense.getUpdatedAt()) {
                                            database.expenseDao().update(remoteExpense);
                                            Log.d(TAG, "Updated expense (remote newer): " + remoteExpense.getId());
                                        } else {
                                            Log.d(TAG, "Skipped expense (local newer): " + remoteExpense.getId());
                                        }
                                    }

                                    isApplyingRemoteUpdate = false;
                                    break;

                                case REMOVED:
                                    database.expenseDao().delete(remoteExpense);
                                    Log.d(TAG, "Expense deleted: " + remoteExpense.getId());
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

                            // ✅ If block-specific, ensure block exists
                            if (remoteRevenue.getBlockId() != null) {
                                BlockEntity block = database.blockDao().getBlockById(remoteRevenue.getBlockId());
                                if (block == null) {
                                    Log.w(TAG, "⚠️ Skipping revenue " + remoteRevenue.getId() +
                                            " - block " + remoteRevenue.getBlockId() + " not found");
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
                                            Log.d(TAG, "✅ New revenue: " + remoteRevenue.getId());
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