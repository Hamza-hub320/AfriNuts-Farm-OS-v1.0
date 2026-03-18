package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class UserRepository extends BaseRepository {
    private static final String TAG = "UserRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public UserRepository(Context context, long farmId) {
        super(context, farmId);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertUser(UserEntity user, InsertCallback callback) {
        executor.execute(() -> {
            try {
                long id = database.userDao().insert(user);
                user.setId(id);
                Log.d(TAG, "User saved locally with ID: " + id);

                // Queue for sync
                syncManager.queueForSync("users", id, SyncEntity.SyncAction.CREATE);
                syncManager.triggerAutoSync();

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
                Log.d(TAG, "User updated locally: " + user.getId());

                // Queue for sync
                syncManager.queueForSync("users", user.getId(), SyncEntity.SyncAction.UPDATE);
                syncManager.triggerAutoSync();

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
                Log.d(TAG, "User deleted locally: " + user.getId());

                // Queue for sync
                syncManager.queueForSync("users", user.getId(), SyncEntity.SyncAction.DELETE);
                syncManager.triggerAutoSync();

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
                UserEntity user = EntityMapper.mapToUser(document);
                if (user != null) {
                    UserEntity existing = database.userDao().getUserByEmail(user.getEmail());
                    if (existing == null) {
                        database.userDao().insert(user);
                        Log.d(TAG, "Synced new user: " + user.getEmail());
                    } else {
                        user.setId(existing.getId());
                        database.userDao().update(user);
                        Log.d(TAG, "Synced updated user: " + user.getEmail());
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