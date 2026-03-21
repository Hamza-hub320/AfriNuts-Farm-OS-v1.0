package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;

import java.util.List;

public class TaskRepository extends BaseRepository {
    private static final String TAG = "TaskRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirestoreSyncManager firestoreSyncManager;  // Add this

    public TaskRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertTask(TaskEntity task, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.taskDao().insertTask(task);
                task.setId(id);

                Log.d(TAG, "Task saved locally with ID: " + id);

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("tasks", id, SyncEntity.SyncAction.CREATE);
                    Log.d(TAG, "Queued task for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to upload to Firestore immediately
                firestoreHelper.uploadTask(task)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Task uploaded to Firestore: " + id);
                            mainHandler.post(() -> callback.onSuccess(id));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore upload failed", e);
                            // Queue already exists, just return success
                            mainHandler.post(() -> callback.onSuccess(id));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error inserting task", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<TaskEntity>> getAllTasks() {
        return database.taskDao().getAllTasksLiveData();
    }

    public LiveData<TaskEntity> getTaskById(long taskId) {
        return database.taskDao().getTaskByIdLiveData(taskId);
    }

    public LiveData<List<TaskEntity>> getTasksForFarm() {
        return database.taskDao().getTasksByFarmIdLiveData(farmId);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateTask(TaskEntity task, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.taskDao().updateTask(task);
                Log.d(TAG, "Task updated locally: " + task.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("tasks", task.getId(), SyncEntity.SyncAction.UPDATE);
                    Log.d(TAG, "Queued task update for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to update Firestore
                firestoreHelper.updateTask(task)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Task updated in Firestore: " + task.getId());
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore update failed", e);
                            // Queue already exists, just return success
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error updating task", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteTask(TaskEntity task, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.taskDao().deleteTask(task);
                Log.d(TAG, "Task deleted locally: " + task.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("tasks", task.getId(), SyncEntity.SyncAction.DELETE);
                    Log.d(TAG, "Queued task deletion for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to delete from Firestore
                firestoreHelper.deleteTask(task.getId())
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Task deleted from Firestore: " + task.getId());
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore delete failed", e);
                            // Queue already exists, just return success
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting task", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // SYNC FROM FIRESTORE (called by listeners)
    // ========================================

    public void syncTaskFromFirestore(com.google.firebase.firestore.DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                TaskEntity task = EntityMapper.mapToTask(document);
                if (task != null) {
                    TaskEntity existing = database.taskDao().getTaskById(task.getId());
                    if (existing == null) {
                        database.taskDao().insertTask(task);
                        Log.d(TAG, "Synced new task from Firestore: " + task.getId());
                    } else {
                        // Compare timestamps to avoid unnecessary updates
                        if (task.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.taskDao().updateTask(task);
                            Log.d(TAG, "Synced updated task from Firestore: " + task.getId());
                        } else {
                            Log.d(TAG, "Skipped older task update: " + task.getId());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing task from Firestore", e);
            }
        });
    }

    public void deleteTaskFromSync(long taskId) {
        executor.execute(() -> {
            database.taskDao().deleteTaskById(taskId);
            Log.d(TAG, "Deleted task from sync: " + taskId);
        });
    }

    // For synchronous operations (when you need immediate result)
    public List<TaskEntity> getTasksSync() {
        return database.taskDao().getTasksByFarmId(farmId);
    }
}