package com.afrinuts.farmos.data.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Collection names
    private static final String COLLECTION_FARMS = "farms";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_BLOCKS = "blocks";
    private static final String COLLECTION_EXPENSES = "expenses";
    private static final String COLLECTION_REVENUES = "revenues";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_PHOTOS = "photos";
    private static final String COLLECTION_TASK_ASSIGNMENTS = "task_assignments";

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ========================================
    // AUTHENTICATION
    // ========================================

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String error);
    }

    public void signInWithEmail(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public void signUpWithEmail(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    public void signOut() {
        auth.signOut();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    // ========================================
    // FARM OPERATIONS
    // ========================================

    public void saveFarm(FarmEntity farm, String farmId) {
        Map<String, Object> farmMap = new HashMap<>();
        farmMap.put("id", farmId);
        farmMap.put("name", farm.getName());
        farmMap.put("location", farm.getLocation());
        farmMap.put("totalHectares", farm.getTotalHectares());
        farmMap.put("cashewHectares", farm.getCashewHectares());
        farmMap.put("treesPerHectare", farm.getTreesPerHectare());
        farmMap.put("plantingYear", farm.getPlantingYear());
        farmMap.put("createdAt", farm.getCreatedAt());
        farmMap.put("updatedAt", System.currentTimeMillis());

        db.collection(COLLECTION_FARMS).document(farmId)
                .set(farmMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Farm saved: " + farmId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving farm", e));
    }

    public Task<DocumentSnapshot> getFarm(String farmId) {
        return db.collection(COLLECTION_FARMS).document(farmId).get();
    }

    // ========================================
    // USER OPERATIONS
    // ========================================

    public void saveUser(UserEntity user, String farmId) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("farmId", farmId);
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole().name());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("authMethod", user.getAuthMethod().name());
        userMap.put("isActive", user.isActive());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", System.currentTimeMillis());

        String docId = farmId + "_" + user.getId();
        db.collection(COLLECTION_USERS).document(docId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user", e));
    }

    public Task<QuerySnapshot> getUsersForFarm(String farmId) {
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("farmId", farmId)
                .get();
    }

    // ========================================
    // BLOCK OPERATIONS
    // ========================================

    public void saveBlock(BlockEntity block, String farmId) {
        Map<String, Object> blockMap = new HashMap<>();
        blockMap.put("id", block.getId());
        blockMap.put("farmId", farmId);
        blockMap.put("blockName", block.getBlockName());
        blockMap.put("hectareSize", block.getHectareSize());
        blockMap.put("status", block.getStatus().name());
        blockMap.put("clearedDate", block.getClearedDate());
        blockMap.put("plowedDate", block.getPlowedDate());
        blockMap.put("plantedDate", block.getPlantedDate());
        blockMap.put("survivalRate", block.getSurvivalRate());
        blockMap.put("replacementCount", block.getReplacementCount());
        blockMap.put("notes", block.getNotes());
        blockMap.put("createdAt", block.getCreatedAt());
        blockMap.put("updatedAt", System.currentTimeMillis());

        String docId = farmId + "_" + block.getBlockName();
        db.collection(COLLECTION_BLOCKS).document(docId)
                .set(blockMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Block saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving block", e));
    }

    public Task<QuerySnapshot> getBlocksForFarm(String farmId) {
        return db.collection(COLLECTION_BLOCKS)
                .whereEqualTo("farmId", farmId)
                .get();
    }

    // ========================================
    // EXPENSE OPERATIONS
    // ========================================

    public void saveExpense(ExpenseEntity expense, String farmId) {
        Map<String, Object> expenseMap = new HashMap<>();
        expenseMap.put("id", expense.getId());
        expenseMap.put("farmId", farmId);
        expenseMap.put("blockId", expense.getBlockId());
        expenseMap.put("category", expense.getCategory().name());
        expenseMap.put("amount", expense.getAmount());
        expenseMap.put("date", expense.getDate());
        expenseMap.put("description", expense.getDescription());
        expenseMap.put("receiptPath", expense.getReceiptPath());
        expenseMap.put("createdAt", expense.getCreatedAt());
        expenseMap.put("updatedAt", System.currentTimeMillis());

        String docId = farmId + "_" + expense.getId();
        db.collection(COLLECTION_EXPENSES).document(docId)
                .set(expenseMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Expense saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving expense", e));
    }

    public Task<QuerySnapshot> getExpensesForFarm(String farmId) {
        return db.collection(COLLECTION_EXPENSES)
                .whereEqualTo("farmId", farmId)
                .get();
    }

    // ========================================
    // REVENUE OPERATIONS
    // ========================================

    public void saveRevenue(RevenueEntity revenue, String farmId) {
        Map<String, Object> revenueMap = new HashMap<>();
        revenueMap.put("id", revenue.getId());
        revenueMap.put("farmId", farmId);
        revenueMap.put("blockId", revenue.getBlockId());
        revenueMap.put("harvestDate", revenue.getHarvestDate());
        revenueMap.put("quantityKg", revenue.getQuantityKg());
        revenueMap.put("pricePerKg", revenue.getPricePerKg());
        revenueMap.put("totalAmount", revenue.getTotalAmount());
        revenueMap.put("buyer", revenue.getBuyer());
        revenueMap.put("quality", revenue.getQuality().name());
        revenueMap.put("notes", revenue.getNotes());
        revenueMap.put("createdAt", revenue.getCreatedAt());
        revenueMap.put("updatedAt", System.currentTimeMillis());

        String docId = farmId + "_" + revenue.getId();
        db.collection(COLLECTION_REVENUES).document(docId)
                .set(revenueMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Revenue saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving revenue", e));
    }

    public Task<QuerySnapshot> getRevenuesForFarm(String farmId) {
        return db.collection(COLLECTION_REVENUES)
                .whereEqualTo("farmId", farmId)
                .get();
    }

    // ========================================
    // TASK OPERATIONS
    // ========================================

    public void saveTask(TaskEntity task, String farmId) {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("farmId", farmId);
        taskMap.put("blockId", task.getBlockId());
        taskMap.put("title", task.getTitle());
        taskMap.put("description", task.getDescription());
        taskMap.put("status", task.getStatus().name());
        taskMap.put("type", task.getType().name());
        taskMap.put("dueDate", task.getDueDate());
        taskMap.put("completedDate", task.getCompletedDate());
        taskMap.put("createdAt", task.getCreatedAt());
        taskMap.put("updatedAt", System.currentTimeMillis());

        String docId = farmId + "_" + task.getId();
        db.collection(COLLECTION_TASKS).document(docId)
                .set(taskMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving task", e));
    }

    public Task<QuerySnapshot> getTasksForFarm(String farmId) {
        return db.collection(COLLECTION_TASKS)
                .whereEqualTo("farmId", farmId)
                .get();
    }

    // ========================================
    // PHOTO OPERATIONS
    // ========================================

    public interface PhotoUploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
        void onProgress(double progress);
    }

    public void uploadPhoto(File photoFile, String photoId, String farmId, PhotoUploadCallback callback) {
        StorageReference storageRef = storage.getReference()
                .child("farms")
                .child(farmId)
                .child("photos")
                .child(photoId + ".jpg");

        UploadTask uploadTask = storageRef.putFile(Uri.fromFile(photoFile));

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        }).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                callback.onSuccess(uri.toString());
            });
        }).addOnFailureListener(e -> {
            callback.onError(e.getMessage());
        });
    }

    public void savePhotoMetadata(PhotoEntity photo, String farmId, String downloadUrl) {
        Map<String, Object> photoMap = new HashMap<>();
        photoMap.put("id", photo.getId());
        photoMap.put("farmId", farmId);
        photoMap.put("blockId", photo.getBlockId());
        photoMap.put("taskId", photo.getTaskId());
        photoMap.put("expenseId", photo.getExpenseId());
        photoMap.put("filePath", downloadUrl); // Store cloud URL instead of local path
        photoMap.put("thumbnailPath", photo.getThumbnailPath());
        photoMap.put("caption", photo.getCaption());
        photoMap.put("takenAt", photo.getTakenAt());
        photoMap.put("latitude", photo.getLatitude());
        photoMap.put("longitude", photo.getLongitude());
        photoMap.put("photoType", photo.getPhotoType());
        photoMap.put("createdAt", photo.getCreatedAt());
        photoMap.put("updatedAt", System.currentTimeMillis());

        String docId = farmId + "_" + photo.getId();
        db.collection(COLLECTION_PHOTOS).document(docId)
                .set(photoMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Photo metadata saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving photo metadata", e));
    }

    public Task<QuerySnapshot> getPhotosForFarm(String farmId) {
        return db.collection(COLLECTION_PHOTOS)
                .whereEqualTo("farmId", farmId)
                .get();
    }

    public Task<QuerySnapshot> getPhotosForBlock(String farmId, long blockId) {
        return db.collection(COLLECTION_PHOTOS)
                .whereEqualTo("farmId", farmId)
                .whereEqualTo("blockId", blockId)
                .get();
    }

    // ========================================
    // SYNC OPERATIONS
    // ========================================

    public interface SyncCallback {
        void onComplete();
        void onError(String error);
    }

    public void syncAllData(String farmId, SyncCallback callback) {
        List<Task<?>> tasks = new ArrayList<>();

        // Add all data fetching tasks
        tasks.add(getBlocksForFarm(farmId));
        tasks.add(getExpensesForFarm(farmId));
        tasks.add(getRevenuesForFarm(farmId));
        tasks.add(getTasksForFarm(farmId));
        tasks.add(getPhotosForFarm(farmId));

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(results -> {
                    // Process results and update local database
                    // This would call your existing DAOs to insert/update
                    callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // ========================================
    // DELETE OPERATIONS
    // ========================================

    public void deleteFarm(String farmId) {
        db.collection(COLLECTION_FARMS).document(farmId).delete();
    }

    public void deleteBlock(String farmId, String blockName) {
        String docId = farmId + "_" + blockName;
        db.collection(COLLECTION_BLOCKS).document(docId).delete();
    }

    public void deleteExpense(String farmId, long expenseId) {
        String docId = farmId + "_" + expenseId;
        db.collection(COLLECTION_EXPENSES).document(docId).delete();
    }

    public void deletePhoto(String farmId, long photoId, PhotoUploadCallback callback) {
        // Delete from Storage first
        StorageReference photoRef = storage.getReference()
                .child("farms")
                .child(farmId)
                .child("photos")
                .child(photoId + ".jpg");

        photoRef.delete().addOnSuccessListener(aVoid -> {
            // Then delete metadata from Firestore
            String docId = farmId + "_" + photoId;
            db.collection(COLLECTION_PHOTOS).document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid2 -> {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onError(e.getMessage());
                        }
                    });
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        });
    }
}