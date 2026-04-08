package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.sync.FirestoreSyncManager;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PhotoRepository extends BaseRepository {
    private static final String TAG = "PhotoRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FirestoreSyncManager firestoreSyncManager;
    private FirebaseManager firebaseManager;

    public PhotoRepository(Context context, long farmId) {
        super(context, farmId);
        this.firestoreSyncManager = FirestoreSyncManager.getInstance(context);
        this.firebaseManager = FirebaseManager.getInstance();
    }

    public interface PhotoUploadCallback {
        void onSuccess(long id, String downloadUrl);
        void onError(String error);
        void onProgress(double progress);
    }

    /**
     * Get photo count for a block using CompletableFuture (for BlockDetailActivity)
     */
    public java.util.concurrent.CompletableFuture<Integer> getPhotoCountForBlockAsync(long blockId) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            return database.photoDao().getPhotoCountForBlock(blockId);
        }, executor);
    }

    // ========================================
    // CREATE
    // ========================================

    public void insertPhoto(PhotoEntity photo, File photoFile, PhotoUploadCallback callback) {
        executor.execute(() -> {
            try {
                // Verify file exists
                if (!photoFile.exists()) {
                    Log.e(TAG, "Photo file does not exist: " + photoFile.getAbsolutePath());
                    mainHandler.post(() -> callback.onError("Photo file does not exist"));
                    return;
                }

                Log.d(TAG, "Photo file exists, size: " + photoFile.length() + " bytes");

                // 1. Insert into local database first
                long id = database.photoDao().insert(photo);
                photo.setId(id);
                Log.d(TAG, "Photo saved locally with ID: " + id);

                // 2. Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("photos", id, SyncEntity.SyncAction.CREATE);
                    Log.d(TAG, "Queued photo for sync (local change)");
                }

                // 3. Upload to Firebase Storage with attachment context
                uploadToFirebaseStorage(photo, photoFile, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error inserting photo", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private void uploadToFirebaseStorage(PhotoEntity photo, File photoFile, PhotoUploadCallback callback) {
        final long photoId = photo.getId();  // Make a final copy for lambda

        firebaseManager.uploadPhoto(
                photoFile,
                String.valueOf(photoId),
                String.valueOf(farmId),
                photo.getBlockId(),
                photo.getTaskId(),
                photo.getExpenseId(),
                new FirebaseManager.PhotoUploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl) {
                        Log.d(TAG, "✅ Photo uploaded to Firebase Storage: " + downloadUrl);

                        firestoreHelper.savePhotoMetadata(photo, downloadUrl)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Photo metadata saved to Firestore: " + photoId);
                                    mainHandler.post(() -> callback.onSuccess(photoId, downloadUrl));
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Firestore metadata save failed: " + e.getMessage());
                                    mainHandler.post(() -> callback.onSuccess(photoId, null));
                                });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Firebase Storage upload failed: " + error);
                        mainHandler.post(() -> callback.onError(error));
                    }

                    @Override
                    public void onProgress(double progress) {
                        Log.d(TAG, "📤 Upload progress: " + (int) progress + "%");
                    }
                });
    }

    // ========================================
    // READ
    // ========================================
    public LiveData<List<PhotoEntity>> getAllPhotos() {
        return database.photoDao().getAllPhotosLiveData();
    }

    public LiveData<PhotoEntity> getPhotoById(long photoId) {
        return database.photoDao().getPhotoByIdLiveData(photoId);
    }

    public LiveData<List<PhotoEntity>> getPhotosForBlock(long blockId) {
        return database.photoDao().getPhotosByBlockIdLiveData(blockId);
    }

    public LiveData<List<PhotoEntity>> getPhotosForTask(long taskId) {
        return database.photoDao().getPhotosByTaskIdLiveData(taskId);
    }

    public LiveData<List<PhotoEntity>> getPhotosForExpense(long expenseId) {
        return database.photoDao().getPhotosByExpenseIdLiveData(expenseId);
    }

    public LiveData<List<PhotoEntity>> getPhotosForFarm() {
        return database.photoDao().getPhotosByFarmIdLiveData(farmId);
    }

    public LiveData<List<PhotoEntity>> getPhotosByFarm() {
        return database.photoDao().getPhotosByFarmIdLiveData(farmId);
    }

    public void getPhotoCountForBlock(long blockId, RepositoryCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                int count = database.photoDao().getPhotoCountForBlock(blockId);
                mainHandler.post(() -> callback.onSuccess(count));
            } catch (Exception e) {
                Log.e(TAG, "Error getting photo count", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get photo count for a task using CompletableFuture (for TaskDetailActivity)
     */
    public CompletableFuture<Integer> getPhotoCountForTaskAsync(long taskId) {
        return CompletableFuture.supplyAsync(() -> {
            return database.photoDao().getPhotoCountForTask(taskId);
        }, executor);
    }

    public CompletableFuture<Integer> getPhotoCountForExpenseAsync(long expenseId) {
        return CompletableFuture.supplyAsync(() -> {
            return database.photoDao().getPhotoCountForExpense(expenseId);
        }, executor);
    }

    // ========================================
    // UPDATE
    // ========================================

    public void updatePhotoCaption(long photoId, String newCaption, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                PhotoEntity photo = database.photoDao().getPhotoById(photoId);
                if (photo == null) {
                    mainHandler.post(() -> callback.onError("Photo not found"));
                    return;
                }

                photo.setCaption(newCaption.isEmpty() ? null : newCaption);
                database.photoDao().update(photo);

                Log.d(TAG, "Photo caption updated locally: " + photoId);

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("photos", photoId, SyncEntity.SyncAction.UPDATE);
                    Log.d(TAG, "Queued photo update for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Try to update Firestore
                firestoreHelper.updatePhotoMetadata(photo)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Photo metadata updated in Firestore");
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore update failed: " + e.getMessage());
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error updating photo caption", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // DELETE
    // ========================================

    public void deletePhoto(PhotoEntity photo, RepositoryCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                // Delete from local database first
                database.photoDao().delete(photo);
                Log.d(TAG, "Photo deleted locally: " + photo.getId());

                // Queue for sync (only if NOT a remote update)
                if (!firestoreSyncManager.isRemoteUpdate()) {
                    syncManager.queueForSync("photos", photo.getId(), SyncEntity.SyncAction.DELETE);
                    Log.d(TAG, "Queued photo deletion for sync (local change)");
                } else {
                    Log.d(TAG, "Skipping queue - this is a remote update");
                }

                // Delete from Firebase Storage
                String storagePath = "farms/" + farmId + "/photos/" + photo.getId() + ".jpg";
                firestoreHelper.deleteFromStorage(storagePath)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Photo deleted from Firebase Storage");

                            // Delete from Firestore metadata
                            firestoreHelper.deletePhotoMetadata(photo.getId())
                                    .addOnSuccessListener(aVoid2 -> {
                                        Log.d(TAG, "Photo metadata deleted from Firestore");
                                        mainHandler.post(() -> callback.onSuccess(true));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Firestore metadata delete failed: " + e.getMessage());
                                        mainHandler.post(() -> callback.onSuccess(true));
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Storage delete failed: " + e.getMessage());
                            mainHandler.post(() -> callback.onSuccess(true));
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting photo", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ========================================
    // SYNC FROM FIRESTORE
    // ========================================

    public void syncPhotoFromFirestore(DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                PhotoEntity photo = EntityMapper.mapToPhoto(document);
                if (photo != null) {
                    PhotoEntity existing = database.photoDao().getPhotoById(photo.getId());
                    if (existing == null) {
                        database.photoDao().insert(photo);
                        Log.d(TAG, "Synced new photo from Firestore: " + photo.getId());
                    } else {
                        if (photo.getUpdatedAt() > existing.getUpdatedAt()) {
                            database.photoDao().update(photo);
                            Log.d(TAG, "Synced updated photo from Firestore: " + photo.getId());
                        } else {
                            Log.d(TAG, "Skipped older photo update: " + photo.getId());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error syncing photo from Firestore", e);
            }
        });
    }

    public void deletePhotoFromSync(long photoId) {
        executor.execute(() -> {
            database.photoDao().deleteById(photoId);
            Log.d(TAG, "Deleted photo from sync: " + photoId);
        });
    }

    // For synchronous operations
    public List<PhotoEntity> getPhotosForBlockSync(long blockId) {
        return database.photoDao().getPhotosByBlockId(blockId);
    }
}