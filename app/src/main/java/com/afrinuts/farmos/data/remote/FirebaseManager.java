package com.afrinuts.farmos.data.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
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
    public static final String COLLECTION_FARMS = "farms";
    private static final String COLLECTION_USERS = "users";

    // Subcollection names (under farms/{farmId}/)
    private static final String SUBCOLLECTION_BLOCKS = "blocks";
    private static final String SUBCOLLECTION_EXPENSES = "expenses";
    private static final String SUBCOLLECTION_REVENUES = "revenues";
    public static final String SUBCOLLECTION_TASKS = "tasks";
    private static final String SUBCOLLECTION_PHOTOS = "photos";
    public static final String SUBCOLLECTION_TASK_ASSIGNMENTS = "task_assignments";
    private static final String SUBCOLLECTION_QUALITY_BATCHES = "quality_batches";

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

        // ALWAYS store both uid and localId
        if (user.getUid() != null && !user.getUid().isEmpty()) {
            userMap.put("uid", user.getUid());
        } else {
            // For PIN users, store null uid
            userMap.put("uid", null);
        }

        userMap.put("localId", user.getLocalId());
        userMap.put("farmId", farmId);
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole().name());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("authMethod", user.getAuthMethod().name());
        userMap.put("isActive", user.isActive());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", System.currentTimeMillis());

        // Improved document ID selection:
        String docId;

        // ALWAYS prefer uid if available (prevents duplicates)
        if (user.getUid() != null && !user.getUid().isEmpty()) {
            docId = user.getUid();
            Log.d(TAG, "Saving Firebase Auth user with UID docId: " + docId);
        } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            // Fallback: use email if UID missing (important for migration cases)
            docId = user.getEmail();
            Log.d(TAG, "Saving user with EMAIL docId (fallback): " + docId);
        } else {
            // Last fallback: local compound key
            docId = farmId + "_" + user.getLocalId();
            Log.d(TAG, "Saving PIN user with LOCAL docId: " + docId);
        }

        db.collection(COLLECTION_USERS).document(docId)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User saved: " + docId))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user", e));
    }

    // ========================================
    // BLOCK OPERATIONS - UPDATED to use subcollections
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

        // Save under farms/{farmId}/blocks/{blockId}
        db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_BLOCKS)
                .document(String.valueOf(block.getId()))
                .set(blockMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Block saved: " + block.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving block", e));
    }

    public Task<QuerySnapshot> getBlocksForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_BLOCKS)
                .get();
    }

    // ========================================
    // EXPENSE OPERATIONS - UPDATED to use subcollections
    // ========================================

    public void saveExpense(ExpenseEntity expense, String farmId) throws Exception {
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

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_EXPENSES)
                    .document(String.valueOf(expense.getId()))
                    .set(expenseMap));
            Log.d(TAG, "Expense saved: " + expense.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving expense: " + expense.getId(), e);
            throw new Exception("Failed to save expense to Firestore", e);
        }
    }

    public Task<QuerySnapshot> getExpensesForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_EXPENSES)
                .get();
    }

    // ========================================
    // REVENUE OPERATIONS - UPDATED to use subcollections
    // ========================================

    public void saveRevenue(RevenueEntity revenue, String farmId) throws Exception {
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
        revenueMap.put("harvestCycleId", revenue.getHarvestCycleId());
        revenueMap.put("harvestYear", revenue.getHarvestYear());
        revenueMap.put("harvestSeason", revenue.getHarvestSeason());

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_REVENUES)
                    .document(String.valueOf(revenue.getId()))
                    .set(revenueMap));
            Log.d(TAG, "Revenue saved: " + revenue.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving revenue: " + revenue.getId(), e);
            throw new Exception("Failed to save revenue to Firestore", e);
        }
    }

    public Task<QuerySnapshot> getRevenuesForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_REVENUES)
                .get();
    }

    // ========================================
    // TASK OPERATIONS - UPDATED to use subcollections
    // ========================================

    public void saveTask(TaskEntity task, String farmId) throws Exception {
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

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_TASKS)
                    .document(String.valueOf(task.getId()))
                    .set(taskMap));
            Log.d(TAG, "Task saved: " + task.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving task: " + task.getId(), e);
            throw new Exception("Failed to save task to Firestore", e);
        }
    }

    public Task<QuerySnapshot> getTasksForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_TASKS)
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

    // Callback for Firestore operations (success/error, no return value)
    public interface FirestoreCallback {
        void onSuccess();
        void onError(String error);
    }

    // In FirebaseManager.java
    public void uploadPhoto(File photoFile, String photoId, String farmId,
                            Long blockId, Long taskId, Long expenseId,
                            PhotoUploadCallback callback) {

        // Build the path without reassigning the reference
        String path;

        // Determine the correct path based on attachment type
        if (blockId != null && blockId > 0) {
            path = "farms/" + farmId + "/blocks/" + blockId + "/photos/" + photoId + ".jpg";
        } else if (taskId != null && taskId > 0) {
            path = "farms/" + farmId + "/tasks/" + taskId + "/photos/" + photoId + ".jpg";
        } else if (expenseId != null && expenseId > 0) {
            path = "farms/" + farmId + "/expenses/" + expenseId + "/photos/" + photoId + ".jpg";
        } else {
            path = "farms/" + farmId + "/photos/" + photoId + ".jpg";
        }

        final String finalPath = path;  // Make it final for lambda

        StorageReference storageRef = storage.getReference().child(finalPath);

        Log.d(TAG, "📤 Uploading photo to: " + finalPath);

        UploadTask uploadTask = storageRef.putFile(Uri.fromFile(photoFile));

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        }).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "✅ Photo uploaded successfully: " + uri.toString());
                callback.onSuccess(uri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to get download URL", e);
                callback.onError(e.getMessage());
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Upload failed", e);
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
        photoMap.put("filePath", downloadUrl);
        photoMap.put("caption", photo.getCaption());
        photoMap.put("takenAt", photo.getTakenAt());
        photoMap.put("photoType", photo.getPhotoType());
        photoMap.put("createdAt", photo.getCreatedAt());
        photoMap.put("updatedAt", System.currentTimeMillis());

        String docId = String.valueOf(photo.getId());
        Log.d(TAG, "📝 Saving photo metadata to: farms/" + farmId + "/photos/" + docId);

        db.collection("farms")
                .document(farmId)
                .collection("photos")
                .document(docId)
                .set(photoMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Photo metadata saved"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to save metadata", e));
    }

    // Overloaded version with callback
    public void savePhotoMetadata(PhotoEntity photo, String farmId, String downloadUrl, FirestoreCallback callback) {
        Map<String, Object> photoMap = new HashMap<>();
        photoMap.put("id", photo.getId());
        photoMap.put("farmId", farmId);
        photoMap.put("blockId", photo.getBlockId());
        photoMap.put("taskId", photo.getTaskId());
        photoMap.put("expenseId", photo.getExpenseId());
        photoMap.put("filePath", downloadUrl);
        photoMap.put("caption", photo.getCaption());
        photoMap.put("takenAt", photo.getTakenAt());
        photoMap.put("photoType", photo.getPhotoType());
        photoMap.put("createdAt", photo.getCreatedAt());
        photoMap.put("updatedAt", System.currentTimeMillis());

        String docId = String.valueOf(photo.getId());
        Log.d(TAG, "📝 Saving photo metadata to: farms/" + farmId + "/photos/" + docId);

        db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_PHOTOS)
                .document(docId)
                .set(photoMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Photo metadata saved");
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save metadata", e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public Task<QuerySnapshot> getPhotosForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_PHOTOS)
                .get();
    }

    public Task<QuerySnapshot> getPhotosForBlock(String farmId, long blockId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_PHOTOS)
                .whereEqualTo("blockId", blockId)
                .get();
    }

    // ========================================
   //  TASK ASSIGNMENT OPERATIONS
   // ========================================

    public void saveAssignment(TaskAssignmentHistoryEntity assignment, String farmId) throws Exception {
        Map<String, Object> assignmentMap = new HashMap<>();
        assignmentMap.put("id", assignment.getId());
        assignmentMap.put("taskId", assignment.getTaskId());
        assignmentMap.put("assignedTo", assignment.getAssignedTo());
        assignmentMap.put("assignedAt", assignment.getAssignedAt());
        assignmentMap.put("unassignedAt", assignment.getUnassignedAt());
        assignmentMap.put("farmId", farmId);
        assignmentMap.put("updatedAt", System.currentTimeMillis());

        try {
            // Save under farms/{farmId}/task_assignments/{assignmentId}
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_TASK_ASSIGNMENTS)
                    .document(String.valueOf(assignment.getId()))
                    .set(assignmentMap));
            Log.d(TAG, "Assignment saved to Firestore: " + assignment.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving assignment", e);
            throw new Exception("Failed to save assignment to Firestore", e);
        }
    }

    public void deleteAssignment(String farmId, long assignmentId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection("task_assignments")
                    .document(String.valueOf(assignmentId))
                    .delete());
            Log.d(TAG, "✅ Assignment deleted: " + assignmentId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "❌ Error deleting assignment", e);
            throw new Exception("Failed to delete assignment from Firestore", e);
        }
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
                    callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // ========================================
        // QUALITY BATCH OPERATIONS
    // ========================================

    public void saveQualityBatch(QualityBatchEntity batch, String farmId) throws Exception {
        Map<String, Object> batchMap = EntityMapper.qualityBatchToMap(batch);

        String docId = farmId + "_" + batch.getId();

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_QUALITY_BATCHES)
                    .document(String.valueOf(batch.getId()))
                    .set(batchMap));
            Log.d(TAG, "Quality batch saved: " + batch.getBatchNumber());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving quality batch", e);
            throw new Exception("Failed to save quality batch to Firestore", e);
        }
    }

    public Task<QuerySnapshot> getQualityBatchesForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_QUALITY_BATCHES)
                .get();
    }

    public void deleteQualityBatch(String farmId, long batchId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_QUALITY_BATCHES)
                    .document(String.valueOf(batchId))
                    .delete());
            Log.d(TAG, "Quality batch deleted: " + batchId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting quality batch", e);
            throw new Exception("Failed to delete quality batch from Firestore", e);
        }
    }

    // ========================================
    // DELETE OPERATIONS - UPDATED to use subcollections
    // ========================================

    public void deleteFarm(String farmId) {
        db.collection(COLLECTION_FARMS).document(farmId).delete();
    }

    public void deleteBlock(String farmId, long blockId) {
        db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_BLOCKS)
                .document(String.valueOf(blockId))
                .delete();
    }

    public void deleteExpense(String farmId, long expenseId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_EXPENSES)
                    .document(String.valueOf(expenseId))
                    .delete());
            Log.d(TAG, "Expense deleted: " + expenseId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting expense: " + expenseId, e);
            throw new Exception("Failed to delete expense from Firestore", e);
        }
    }

    public void deleteRevenue(String farmId, long revenueId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_REVENUES)
                    .document(String.valueOf(revenueId))
                    .delete());
            Log.d(TAG, "Revenue deleted: " + revenueId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting revenue: " + revenueId, e);
            throw new Exception("Failed to delete revenue from Firestore", e);
        }
    }

    public void deleteTask(String farmId, long taskId, List<TaskAssignmentHistoryEntity> assignments) throws Exception {
        try {
            // Step 1: Fetch ALL assignments linked to this task from Firestore
            QuerySnapshot snapshot = Tasks.await(
                    db.collection(COLLECTION_FARMS)
                            .document(farmId)
                            .collection(SUBCOLLECTION_TASK_ASSIGNMENTS)
                            .whereEqualTo("taskId", taskId)
                            .get()
            );

            // Step 2: Delete each assignment found
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Tasks.await(doc.getReference().delete());
                Log.d(TAG, "✅ Assignment deleted: " + doc.getId());
            }

            // Step 3: Delete the task document
            Tasks.await(
                    db.collection(COLLECTION_FARMS)
                            .document(farmId)
                            .collection(SUBCOLLECTION_TASKS)
                            .document(String.valueOf(taskId))
                            .delete()
            );

            Log.d(TAG, "✅ Task deleted: " + taskId);

        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting task: " + taskId, e);
            throw new Exception("Failed to delete task from Firestore", e);
        }
    }

    public void saveTaskAssignment(TaskAssignmentHistoryEntity assignment, String farmId) throws Exception {
        Map<String, Object> assignmentMap = new HashMap<>();
        assignmentMap.put("id", assignment.getId());
        assignmentMap.put("taskId", assignment.getTaskId());
        assignmentMap.put("assignedTo", assignment.getAssignedTo());
        assignmentMap.put("assignedAt", assignment.getAssignedAt());
        assignmentMap.put("unassignedAt", assignment.getUnassignedAt());
        assignmentMap.put("farmId", farmId);
        assignmentMap.put("createdAt", assignment.getCreatedAt());
        assignmentMap.put("updatedAt", System.currentTimeMillis());

        try {
            // ✅ Simpler path: farms/{farmId}/task_assignments/{assignmentId}
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection("task_assignments")  // Root-level collection under farm
                    .document(String.valueOf(assignment.getId()))
                    .set(assignmentMap));
            Log.d(TAG, "✅ Assignment saved to Firestore: " + assignment.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "❌ Error saving assignment", e);
            throw new Exception("Failed to save assignment to Firestore", e);
        }
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
            db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_PHOTOS)
                    .document(String.valueOf(photoId))
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