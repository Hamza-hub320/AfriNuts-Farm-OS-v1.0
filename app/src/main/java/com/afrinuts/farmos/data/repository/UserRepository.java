package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class UserRepository extends BaseRepository {
    private static final String TAG = "UserRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirestoreSyncManager firestoreSyncManager;  // Add this

    public UserRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertUser(UserEntity user, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.userDao().insert(user);
                user.setLocalId(id);
                Log.d(TAG, "User saved locally with ID: " + id);

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("users", id, SyncEntity.SyncAction.CREATE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "Queued user for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                mainHandler.post(() -> callback.onSuccess(id));

            } catch (Exception e) {
                Log.e(TAG, "Error inserting user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // READ
    // ========================================

    public LiveData<List<UserEntity>> getAllUsers() {
        return database.userDao().getAllUsersLiveData();
    }

    public LiveData<UserEntity> getUserById(long userId) {
        return database.userDao().getUserByIdLiveData(userId);
    }

    public LiveData<List<UserEntity>> getUsersByRole(UserEntity.UserRole role) {
        return database.userDao().getUsersByRoleLiveData(role);
    }

    public LiveData<List<UserEntity>> getActiveUsers() {
        return database.userDao().getActiveUsersLiveData();
    }

    // Synchronous methods for when you need immediate results
    public UserEntity getUserByEmailSync(String email) {
        return database.userDao().getUserByEmail(email);
    }

    public List<UserEntity> getAllUsersSync() {
        return database.userDao().getAllUsers();
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updateUser(UserEntity user, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.userDao().update(user);
                Log.d(TAG, "User updated locally: " + user.getLocalId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("users", user.getLocalId(), SyncEntity.SyncAction.UPDATE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "Queued user update for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error updating user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deleteUser(UserEntity user, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                database.userDao().delete(user);
                Log.d(TAG, "User deleted locally: " + user.getLocalId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("users", user.getLocalId(), SyncEntity.SyncAction.DELETE);
                    syncManager.triggerAutoSync();
                    Log.d(TAG, "Queued user deletion for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                mainHandler.post(() -> callback.onSuccess(true));

            } catch (Exception e) {
                Log.e(TAG, "Error deleting user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncUserFromFirestore(DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                UserEntity remoteUser = EntityMapper.mapToUser(document);
                if (remoteUser == null) return;

                // Check if user exists by UID (Firebase UID) instead of email
                UserEntity existing = database.userDao().getUserByUid(remoteUser.getUid());

                if (existing == null) {
                    // New user - insert
                    database.userDao().insert(remoteUser);
                    Log.d(TAG, "Synced new user from Firestore: " + remoteUser.getEmail());
                } else {
                    // Existing user - compare timestamps
                    if (remoteUser.getUpdatedAt() > existing.getUpdatedAt()) {
                        // Remote is newer - update local, preserving local ID
                        remoteUser.setLocalId(existing.getLocalId());
                        database.userDao().update(remoteUser);
                        Log.d(TAG, "Synced updated user from Firestore (remote newer): " + remoteUser.getEmail());
                    } else {
                        // Local is newer - ignore remote
                        Log.d(TAG, "Skipped user update (local newer): " + remoteUser.getEmail());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing user from Firestore", e);
            }
        });
    }

    public void deleteUserFromSync(long userId) {
        executor.execute(() -> {
            database.userDao().deleteById(userId);
            Log.d(TAG, "Deleted user from sync: " + userId);
        });
    }
}