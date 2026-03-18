package com.afrinuts.farmos.data.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.remote.EntityMapper;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.List;

public class PhotoRepository extends BaseRepository {
    private static final String TAG = "PhotoRepository";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Change constructor to accept long farmId
    public PhotoRepository(Context context, long farmId) {
        super(context, farmId); // now matches BaseRepository(long)
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
                // 1. Insert into local database first
                long id = database.photoDao().insert(photo);
                photo.setId(id);

                Log.d(TAG, "Photo saved locally with ID: " + id);

                // 2. Upload to Firebase Storage
                uploadToFirebaseStorage(photo, photoFile, callback);

            } catch (Exception e) {
                Log.e(TAG, "Error inserting photo", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private void uploadToFirebaseStorage(PhotoEntity photo, File photoFile, PhotoUploadCallback callback) {
        StorageReference storageRef = firestoreHelper.getStorageReference()
                .child("farms")
                .child(String.valueOf(farmId)) // must convert long to String for Firestore path
                .child("photos")
                .child(photo.getId() + ".jpg");

        UploadTask uploadTask = storageRef.putFile(Uri.fromFile(photoFile));

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            mainHandler.post(() -> callback.onProgress(progress));
        }).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                Log.d(TAG, "Photo uploaded to Firebase Storage: " + downloadUrl);

                // 3. Save metadata to Firestore
                firestoreHelper.savePhotoMetadata(photo, downloadUrl)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Photo metadata saved to Firestore: " + photo.getId());
                            mainHandler.post(() -> callback.onSuccess(photo.getId(), downloadUrl));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore metadata save failed, queuing for sync", e);
                            syncManager.queueForSync("photos", photo.getId(), SyncEntity.SyncAction.CREATE);
                            mainHandler.post(() -> callback.onSuccess(photo.getId(), null));
                        });
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Firebase Storage upload failed, queuing for sync", e);
            syncManager.queueForSync("photos", photo.getId(), SyncEntity.SyncAction.CREATE);
            mainHandler.post(() -> callback.onError(e.getMessage()));
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
        // Remove parseLong, farmId is already long
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

                // Try to update Firestore
                firestoreHelper.updatePhotoMetadata(photo)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Photo metadata updated in Firestore");
                            mainHandler.post(() -> callback.onSuccess(true));
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Firestore update failed, queuing for sync", e);
                            syncManager.queueForSync("photos", photoId, SyncEntity.SyncAction.UPDATE);
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
                // Delete from Firebase Storage first
                String storagePath = "farms/" + farmId + "/photos/" + photo.getId() + ".jpg";
                firestoreHelper.deleteFromStorage(storagePath)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Photo deleted from Firebase Storage");

                            // Then delete from local database
                            database.photoDao().delete(photo);

                            // Try to delete from Firestore
                            firestoreHelper.deletePhotoMetadata(photo.getId())
                                    .addOnSuccessListener(aVoid2 -> {
                                        Log.d(TAG, "Photo metadata deleted from Firestore");
                                        mainHandler.post(() -> callback.onSuccess(true));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Firestore delete failed, queuing for sync", e);
                                        syncManager.queueForSync("photos", photo.getId(), SyncEntity.SyncAction.DELETE);
                                        mainHandler.post(() -> callback.onSuccess(true));
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Storage delete failed", e);
                            mainHandler.post(() -> callback.onError(e.getMessage()));
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

    public void syncPhotoFromFirestore(com.google.firebase.firestore.DocumentSnapshot document) {
        executor.execute(() -> {
            try {
                PhotoEntity photo = EntityMapper.mapToPhoto(document);
                if (photo != null) {
                    PhotoEntity existing = database.photoDao().getPhotoById(photo.getId());
                    if (existing == null) {
                        database.photoDao().insert(photo);
                        Log.d(TAG, "Synced new photo: " + photo.getId());
                    } else {
                        database.photoDao().update(photo);
                        Log.d(TAG, "Synced updated photo: " + photo.getId());
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