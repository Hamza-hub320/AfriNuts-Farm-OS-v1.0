package com.afrinuts.farmos.data.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierDisputeEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
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
    private static final String SUBCOLLECTION_SUPPLIERS = "suppliers";


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

        // ✅ Add new fields for procurement tracking
        expenseMap.put("batchId", expense.getBatchId());
        expenseMap.put("supplierId", expense.getSupplierId());
        expenseMap.put("batchNumber", expense.getBatchNumber());
        expenseMap.put("supplierName", expense.getSupplierName());

        expenseMap.put("category", expense.getCategory().name());
        expenseMap.put("amount", expense.getAmount());
        expenseMap.put("date", expense.getDate());
        expenseMap.put("description", expense.getDescription());
        expenseMap.put("receiptPath", expense.getReceiptPath());
        expenseMap.put("firestoreId", expense.getFirestoreId());
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

    // In FirebaseManager.java - Add this method

    public void saveExpenseMap(Map<String, Object> expenseMap, String farmId, long expenseId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_EXPENSES)
                    .document(String.valueOf(expenseId))
                    .set(expenseMap));
            Log.d(TAG, "Expense saved to Firestore: " + expenseId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving expense: " + expenseId, e);
            throw new Exception("Failed to save expense to Firestore", e);
        }
    }

// ========================================
// REVENUE OPERATIONS - UPDATED to use subcollections
// ========================================

    public void saveRevenue(RevenueEntity revenue, String farmId) throws Exception {
        Map<String, Object> revenueMap = new HashMap<>();
        revenueMap.put("id", revenue.getId());
        revenueMap.put("farmId", farmId);
        revenueMap.put("blockId", revenue.getBlockId());

        // ✅ Add new fields for procurement tracking
        revenueMap.put("batchId", revenue.getBatchId());
        revenueMap.put("supplierId", revenue.getSupplierId());
        revenueMap.put("batchNumber", revenue.getBatchNumber());
        revenueMap.put("supplierName", revenue.getSupplierName());

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

        Log.d(TAG, "Uploading photo to: " + finalPath);

        UploadTask uploadTask = storageRef.putFile(Uri.fromFile(photoFile));

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        }).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "Photo uploaded successfully: " + uri.toString());
                callback.onSuccess(uri.toString());
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get download URL", e);
                callback.onError(e.getMessage());
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Upload failed", e);
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
        Log.d(TAG, "Saving photo metadata to: farms/" + farmId + "/photos/" + docId);

        db.collection("farms")
                .document(farmId)
                .collection("photos")
                .document(docId)
                .set(photoMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Photo metadata saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save metadata", e));
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
// SUPPLIER OPERATIONS
// ========================================

    /**
     * Save supplier to Firestore
     */
    public void saveSupplier(SupplierEntity supplier, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.supplierToMap(supplier);
        Task<Void> task = db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_SUPPLIERS)
                .document(String.valueOf(supplier.getId()))
                .set(data);

        Tasks.await(task);
        Log.d(TAG, "Saved supplier to Firestore: " + supplier.getName());
    }

    /**
     * Delete supplier from Firestore
     */
    public void deleteSupplier(String farmId, long supplierId) throws Exception {
        Task<Void> task = db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_SUPPLIERS)
                .document(String.valueOf(supplierId))
                .delete();

        Tasks.await(task);
        Log.d(TAG, "Deleted supplier from Firestore: " + supplierId);
    }

    public Task<QuerySnapshot> getSuppliersForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_SUPPLIERS)
                .get();
    }

    // ========================================
    // BATCH TRACEABILITY OPERATIONS
    // ========================================

    public void saveBatchTraceability(BatchTraceabilityEntity traceability, String farmId) throws Exception {
        Map<String, Object> traceabilityMap = EntityMapper.batchTraceabilityToMap(traceability);

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection("batch_traceability")
                    .document(String.valueOf(traceability.getId()))
                    .set(traceabilityMap));
            Log.d(TAG, "Batch traceability saved: " + traceability.getBatchNumber());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving batch traceability", e);
            throw new Exception("Failed to save batch traceability to Firestore", e);
        }
    }

    public void deleteBatchTraceability(String farmId, long traceabilityId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection("batch_traceability")
                    .document(String.valueOf(traceabilityId))
                    .delete());
            Log.d(TAG, "Batch traceability deleted: " + traceabilityId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting batch traceability", e);
            throw new Exception("Failed to delete batch traceability from Firestore", e);
        }
    }

    public Task<QuerySnapshot> getBatchTraceabilityForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection("batch_traceability")
                .get();
    }


// ========================================
// SUPPLIER COMMITMENT OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_SUPPLIER_COMMITMENTS = "supplier_commitments";

    public void saveSupplierCommitment(SupplierCommitmentEntity c, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.commitmentToMap(c);
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_SUPPLIER_COMMITMENTS)
                    .document(String.valueOf(c.getId()))
                    .set(data));
            Log.d(TAG, "Commitment saved: " + c.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving commitment: " + c.getId(), e);
            throw new Exception("Failed to save commitment to Firestore", e);
        }
    }

    public void deleteSupplierCommitment(String farmId, long commitmentId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_SUPPLIER_COMMITMENTS)
                    .document(String.valueOf(commitmentId))
                    .delete());
            Log.d(TAG, "Commitment deleted: " + commitmentId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting commitment", e);
            throw new Exception("Failed to delete commitment from Firestore", e);
        }
    }

// ========================================
// SUPPLIER INSPECTION OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_SUPPLIER_INSPECTIONS = "supplier_inspections";

    public void saveSupplierInspection(SupplierInspectionEntity i, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.supplierInspectionToMap(i);
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_SUPPLIER_INSPECTIONS)
                    .document(String.valueOf(i.getId()))
                    .set(data));
            Log.d(TAG, "Inspection saved: " + i.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving inspection: " + i.getId(), e);
            throw new Exception("Failed to save inspection to Firestore", e);
        }
    }

    public void deleteSupplierInspection(String farmId, long inspectionId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_SUPPLIER_INSPECTIONS)
                    .document(String.valueOf(inspectionId))
                    .delete());
            Log.d(TAG, "Inspection deleted: " + inspectionId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting inspection", e);
            throw new Exception("Failed to delete inspection from Firestore", e);
        }
    }

// ========================================
// SUPPLIER DISPUTE OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_SUPPLIER_DISPUTES = "supplier_disputes";

    public void saveSupplierDispute(SupplierDisputeEntity d, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.supplierDisputeToMap(d);
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_SUPPLIER_DISPUTES)
                    .document(String.valueOf(d.getId()))
                    .set(data));
            Log.d(TAG, "Dispute saved: " + d.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving dispute: " + d.getId(), e);
            throw new Exception("Failed to save dispute to Firestore", e);
        }
    }

    public void deleteSupplierDispute(String farmId, long disputeId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_SUPPLIER_DISPUTES)
                    .document(String.valueOf(disputeId))
                    .delete());
            Log.d(TAG, "Dispute deleted: " + disputeId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting dispute", e);
            throw new Exception("Failed to delete dispute from Firestore", e);
        }
    }

// ========================================
// SNAPSHOTS (write-through for tests; the
// engine writes these portal-side in production)
// ========================================

    private static final String SUBCOLLECTION_SCORE_HISTORY = "supplier_score_history";

    public void saveScoreSnapshot(SupplierScoreSnapshotEntity s, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.supplierScoreSnapshotToMap(s);
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_SCORE_HISTORY)
                    .document(String.valueOf(s.getId()))
                    .set(data));
            Log.d(TAG, "Snapshot saved: " + s.getId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving snapshot: " + s.getId(), e);
            throw new Exception("Failed to save snapshot to Firestore", e);
        }
    }

// ========================================
// AD-HOC EVIDENCE PHOTO UPLOAD
// ========================================
// Mirrors PhotoRepository.uploadToFirebaseStorage's mechanics, but is not
// tied to PhotoEntity: file → Storage → download URL. Nothing is written
// back to Room from here; the caller rewrites the evidenceUrls array on its
// own record. The point of having one method is so that upload ordering
// lives in exactly one place.

    public interface EvidenceUploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String error);
    }

    /**
     * Uploads one evidence file to Storage under the given record path and
     * returns the download URL. The path is structured so the portal can find
     * evidence by (farmId, entityType, entityId, filename).
     *
     * Example path:
     *   farms/1/supplier_inspections/42/evidence/1730000000_a.jpg
     */
    public void uploadEvidenceFile(File file,
                                   String farmId,
                                   String entityType,
                                   long entityId,
                                   String evidenceFileName,
                                   EvidenceUploadCallback callback) {
        String path = "farms/" + farmId
                + "/" + entityType
                + "/" + entityId
                + "/evidence/" + evidenceFileName;

        StorageReference ref = storage.getReference().child(path);
        Log.d(TAG, "Uploading evidence to: " + path);

        ref.putFile(Uri.fromFile(file))
                .addOnSuccessListener(snapshot -> ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Log.d(TAG, "Evidence uploaded: " + uri);
                            callback.onSuccess(uri.toString());
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Evidence upload failed", e);
                    callback.onError(e.getMessage());
                });
    }

// ========================================
// MASTER TRACEABILITY OPERATIONS
// ========================================

    public void saveMasterTraceability(MasterBatchTraceabilityEntity master, String farmId) throws Exception {
        Map<String, Object> masterMap = EntityMapper.masterTraceabilityToMap(master);

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection("master_traceability")
                    .document(String.valueOf(master.getId()))
                    .set(masterMap));
            Log.d(TAG, "Master traceability saved: " + master.getMasterBatchNumber());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving master traceability", e);
            throw new Exception("Failed to save master traceability to Firestore", e);
        }
    }

    public void deleteMasterTraceability(String farmId, long masterId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection("master_traceability")
                    .document(String.valueOf(masterId))
                    .delete());
            Log.d(TAG, "Master traceability deleted: " + masterId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting master traceability", e);
            throw new Exception("Failed to delete master traceability from Firestore", e);
        }
    }

    public Task<QuerySnapshot> getMasterTraceabilityForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection("master_traceability")
                .get();
    }


// In FirebaseManager.java - Add certification methods

// ========================================
// CERTIFICATION OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_CERTIFICATIONS = "certifications";

    public void saveCertification(CertificationEntity certification, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.certificationToMap(certification);

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_CERTIFICATIONS)
                    .document(certification.getCertificationId())
                    .set(data));
            Log.d(TAG, "✅ Certification saved to Firestore: " + certification.getCertificationId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving certification", e);
            throw new Exception("Failed to save certification to Firestore", e);
        }
    }

    public void deleteCertification(String farmId, String certificationId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_CERTIFICATIONS)
                    .document(certificationId)
                    .delete());
            Log.d(TAG, "✅ Certification deleted from Firestore: " + certificationId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting certification", e);
            throw new Exception("Failed to delete certification from Firestore", e);
        }
    }

    public Task<QuerySnapshot> getCertificationsForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_CERTIFICATIONS)
                .get();
    }

    // ========================================
// AUDIT OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_AUDITS = "audits";

    public void saveAudit(AuditEntity audit, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.auditToMap(audit);

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_AUDITS)
                    .document(audit.getAuditId())
                    .set(data));
            Log.d(TAG, "✅ Audit saved to Firestore: " + audit.getAuditId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving audit", e);
            throw new Exception("Failed to save audit to Firestore", e);
        }
    }

    // In FirebaseManager.java - add these methods if missing

    public void deleteAudit(String farmId, String auditId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection("audits")
                    .document(auditId)
                    .delete());
            Log.d(TAG, "Audit deleted from Firestore: " + auditId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting audit", e);
            throw new Exception("Failed to delete audit from Firestore", e);
        }
    }

    public Task<QuerySnapshot> getAuditsForFarm(String farmId) {
        return db.collection(COLLECTION_FARMS)
                .document(farmId)
                .collection(SUBCOLLECTION_AUDITS)
                .get();
    }

    // ========================================
// INSPECTION OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_INSPECTIONS = "inspections";

    public void saveInspection(InspectionEntity inspection, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.inspectionToMap(inspection);

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_INSPECTIONS)
                    .document(inspection.getInspectionId())
                    .set(data));
            Log.d(TAG, "Inspection saved to Firestore: " + inspection.getInspectionId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving inspection", e);
            throw new Exception("Failed to save inspection to Firestore", e);
        }
    }

    public void deleteInspection(String farmId, String inspectionId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_INSPECTIONS)
                    .document(inspectionId)
                    .delete());
            Log.d(TAG, "Inspection deleted from Firestore: " + inspectionId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting inspection", e);
            throw new Exception("Failed to delete inspection from Firestore", e);
        }
    }

    // ========================================
// VERIFICATION OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_VERIFICATIONS = "verifications";

    public void saveVerification(VerificationEntity verification, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.verificationToMap(verification);

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_VERIFICATIONS)
                    .document(verification.getVerificationId())
                    .set(data));
            Log.d(TAG, "Verification saved to Firestore: " + verification.getVerificationId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving verification", e);
            throw new Exception("Failed to save verification to Firestore", e);
        }
    }

    public void deleteVerification(String farmId, String verificationId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_VERIFICATIONS)
                    .document(verificationId)
                    .delete());
            Log.d(TAG, "Verification deleted from Firestore: " + verificationId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting verification", e);
            throw new Exception("Failed to delete verification from Firestore", e);
        }
    }


    // ========================================
// DOCUMENT OPERATIONS
// ========================================

    private static final String SUBCOLLECTION_DOCUMENTS = "documents";

    public void saveDocument(DocumentEntity document, String farmId) throws Exception {
        Map<String, Object> data = EntityMapper.documentToMap(document);

        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_DOCUMENTS)
                    .document(document.getDocumentId())
                    .set(data));
            Log.d(TAG, "✅ Document saved to Firestore: " + document.getDocumentId());
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error saving document", e);
            throw new Exception("Failed to save document to Firestore", e);
        }
    }

    public void deleteDocument(String farmId, String documentId) throws Exception {
        try {
            Tasks.await(db.collection(COLLECTION_FARMS)
                    .document(farmId)
                    .collection(SUBCOLLECTION_DOCUMENTS)
                    .document(documentId)
                    .delete());
            Log.d(TAG, "✅ Document deleted from Firestore: " + documentId);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error deleting document", e);
            throw new Exception("Failed to delete document from Firestore", e);
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

    // In FirebaseManager.java - add logging
    public Task<Void> saveBlockMap(Map<String, Object> blockMap, String farmId, long blockId) {
        // ✅ Log what's being saved
        Log.d(TAG, "📤 saveBlockMap: blockId=" + blockId +
                " harvestCount=" + blockMap.get("harvestCount") +
                " totalHarvest=" + blockMap.get("totalHarvestQuantity"));

        return db.collection("farms")
                .document(farmId)
                .collection("blocks")
                .document(String.valueOf(blockId))
                .set(blockMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Block saved to Firestore successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save block to Firestore", e);
                });
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