package com.afrinuts.farmos.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.utils.AuthUtils;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private AppDatabase database;
    private FirebaseManager firebaseManager;

    // UI Elements
    private ImageView logoImage;
    private TextView appNameText;
    private CardView loginCard;
    private TextInputLayout layoutEmailPin;
    private TextInputEditText etEmailPin;
    private TextInputLayout layoutPassword;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private TextView tvSelectUser;
    private View userSelectorDivider;
    private HorizontalScrollView userScrollView;
    private LinearLayout userChipsContainer;

    private List<UserEntity> users;
    private UserEntity selectedUser;
    private boolean isPinMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        database = AppDatabase.getInstance(this);
        firebaseManager = FirebaseManager.getInstance();
        initViews();

        cleanupDuplicateUsers();

        cleanupDuplicateLocalUsers();

        fixExistingUserFarmIds();

        // ✅ Add Firebase Auth state listener
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    Log.d(TAG, "Auth state changed: User logged in - UID: " + currentUser.getUid());
                    // Ensure this user exists in Firestore
                    ensureUserInFirestore(currentUser);
                }
            }
        });

        animateLogoEntrance();
        syncUsersFromFirestore();
        ensureOwnerExists();
    }

    private void initViews() {
        logoImage = findViewById(R.id.logoImage);
        appNameText = findViewById(R.id.appNameText);
        loginCard = findViewById(R.id.loginCard);
        layoutEmailPin = findViewById(R.id.layoutEmailPin);
        etEmailPin = findViewById(R.id.etEmailPin);
        layoutPassword = findViewById(R.id.layoutPassword);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSelectUser = findViewById(R.id.tvSelectUser);
        userSelectorDivider = findViewById(R.id.userSelectorDivider);
        userScrollView = findViewById(R.id.userScrollView);
        userChipsContainer = findViewById(R.id.userChipsContainer);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void ensureUserInFirestore(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.w(TAG, "User exists in Auth but not in Firestore! Re-creating...");

                        // ✅ First check local database for existing user
                        databaseExecutor.execute(() -> {
                            UserEntity localUser = database.userDao().getUserByEmail(email);

                            // Determine role and name from local DB
                            String role = "OWNER"; // Default to OWNER for owner@afrinuts.com
                            String userName = "Farm Owner";
                            String phone = "+225 00 00 00 00";
                            long localId = 0;

                            if (localUser != null) {
                                // Use existing local user data
                                role = localUser.getRole().name();
                                userName = localUser.getName();
                                phone = localUser.getPhone() != null ? localUser.getPhone() : "";
                                localId = localUser.getLocalId();
                                Log.d(TAG, "Found local user: " + userName + " with role: " + role);
                            } else {
                                Log.d(TAG, "No local user found, creating new owner");
                            }

                            final String finalRole = role;
                            final String finalUserName = userName;
                            final String finalPhone = phone;
                            final long finalLocalId = localId;

                            runOnUiThread(() -> {
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("uid", uid);
                                userMap.put("email", email);
                                userMap.put("name", finalUserName);
                                userMap.put("role", finalRole);
                                userMap.put("phone", finalPhone);
                                userMap.put("authMethod", "PASSWORD");
                                userMap.put("isActive", true);
                                userMap.put("farmId", "1");
                                userMap.put("createdAt", System.currentTimeMillis());
                                userMap.put("updatedAt", System.currentTimeMillis());
                                if (finalLocalId > 0) {
                                    userMap.put("localId", finalLocalId);
                                }

                                db.collection("users").document(uid)
                                        .set(userMap)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ Re-created user in Firestore: " + uid + " with role: " + finalRole);
                                            loadLocalUsers();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ Failed to re-create user in Firestore", e);
                                        });
                            });
                        });
                    } else {
                        Log.d(TAG, "User exists in Firestore: " + uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check user existence in Firestore", e);
                });
    }

    private void saveToFirestore(String uid, Map<String, Object> userMap) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Re-created user in Firestore: " + uid);
                    // Refresh local users
                    loadLocalUsers();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to re-create user in Firestore", e);
                });
    }

    private void animateLogoEntrance() {
        logoImage.setAlpha(0f);
        logoImage.setScaleX(0.8f);
        logoImage.setScaleY(0.8f);

        logoImage.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        appNameText.setAlpha(0f);
        appNameText.setTranslationY(20f);
        appNameText.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(200)
                .start();

        loginCard.setAlpha(0f);
        loginCard.setTranslationY(30f);
        loginCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(300)
                .start();
    }

    // ========================================
    // AUTO-SYNC ALL FARM DATA FROM FIRESTORE
    // ========================================

    private void syncUsersFromFirestore() {
        Log.d(TAG, "========== SYNCING USERS FROM FIRESTORE ==========");

        String farmId = "1";
        Log.d(TAG, "Looking for users with farmId: " + farmId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "✅ SUCCESS: Found " + queryDocumentSnapshots.size() + " users");

                    // Check if executor is still alive
                    if (databaseExecutor.isShutdown() || databaseExecutor.isTerminated()) {
                        Log.w(TAG, "Executor already shut down, skipping user sync");
                        loadLocalUsers();
                        return;
                    }

                    databaseExecutor.execute(() -> {
                        try {
                            int addedCount = 0;
                            int updatedCount = 0;

                            Set<String> firestoreUids = new HashSet<>();

                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                UserEntity user = documentToUser(doc);
                                if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
                                    firestoreUids.add(user.getUid());

                                    UserEntity existing = database.userDao().getUserByUid(user.getUid());

                                    if (existing == null) {
                                        existing = database.userDao().getUserByEmail(user.getEmail());
                                    }

                                    if (existing == null) {
                                        long id = database.userDao().insert(user);
                                        addedCount++;
                                        Log.d(TAG, "✅ Added user: " + user.getEmail());
                                    } else {
                                        existing.setName(user.getName());
                                        existing.setRole(user.getRole());
                                        existing.setPhone(user.getPhone());
                                        existing.setActive(user.isActive());
                                        if (existing.getUid() == null || existing.getUid().isEmpty()) {
                                            existing.setUid(user.getUid());
                                        }
                                        if (existing.getFarmId() != 1L) {
                                            existing.setFarmId(1L);
                                        }
                                        database.userDao().update(existing);
                                        updatedCount++;
                                        Log.d(TAG, "📝 Updated user: " + user.getEmail());
                                    }
                                }
                            }

                            List<UserEntity> localUsers = database.userDao().getAllUsers();
                            for (UserEntity localUser : localUsers) {
                                if (localUser.getUid() != null && !localUser.getUid().isEmpty()) {
                                    if (!firestoreUids.contains(localUser.getUid())) {
                                        Log.w(TAG, "⚠️ User exists locally but NOT in Firestore: " + localUser.getEmail());
                                        saveUserToFirestore(localUser);
                                    }
                                }
                            }

                            final int finalAddedCount = addedCount;
                            final int finalUpdatedCount = updatedCount;

                            runOnUiThread(() -> {
                                Log.d(TAG, "Added: " + finalAddedCount + ", Updated: " + finalUpdatedCount);
                                loadLocalUsers();
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Error in user sync", e);
                            runOnUiThread(() -> loadLocalUsers());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to sync users", e);
                    loadLocalUsers();
                });
    }

    // ✅ Add this helper method
    private void saveUserToFirestore(UserEntity user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("localId", user.getLocalId());
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole().name());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("authMethod", user.getAuthMethod().name());
        userMap.put("isActive", user.isActive());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", System.currentTimeMillis());
        userMap.put("farmId", String.valueOf(user.getFarmId()));

        db.collection("users")
                .document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Saved missing user to Firestore: " + user.getEmail());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save missing user: " + user.getEmail(), e);
                });
    }

    private void syncFarmData() {
        Log.d(TAG, "Syncing all farm data from Firestore...");

        String farmId = "1";
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First, ensure farm exists
        ensureFarmExistsInLocalDB(farmId);

        // Sync blocks
        syncBlocks(db, farmId);

        // Sync expenses
        syncExpenses(db, farmId);

        // Sync revenues
        syncRevenues(db, farmId);

        // Sync tasks
        syncTasks(db, farmId);
    }

    private void ensureFarmExistsInLocalDB(String farmIdStr) {
        databaseExecutor.execute(() -> {
            long farmId = Long.parseLong(farmIdStr);
            FarmEntity farm = database.farmDao().getFarmById(farmId);
            if (farm == null) {
                // Create default farm if it doesn't exist
                farm = new FarmEntity(
                        "AfriNuts Odienné",
                        "Odienné, Côte d'Ivoire",
                        35.0,
                        35.0,
                        100,
                        2024
                );
                farm.setId(farmId);
                database.farmDao().insert(farm);
                Log.d(TAG, "Created default farm in local DB: " + farmId);
            }
        });
    }

    private void syncBlocks(FirebaseFirestore db, String farmId) {
        db.collection("blocks")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    databaseExecutor.execute(() -> {
                        int addedCount = 0;
                        int updatedCount = 0;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            BlockEntity block = documentToBlock(doc);
                            if (block != null) {
                                BlockEntity existing = database.blockDao().getBlockById(block.getId());
                                if (existing == null) {
                                    database.blockDao().insert(block);
                                    addedCount++;
                                } else {
                                    database.blockDao().update(block);
                                    updatedCount++;
                                }
                            }
                        }
                        Log.d(TAG, "Synced blocks: " + addedCount + " added, " + updatedCount + " updated");
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync blocks", e));
    }

    private void syncExpenses(FirebaseFirestore db, String farmId) {
        db.collection("expenses")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    databaseExecutor.execute(() -> {
                        int addedCount = 0;
                        int updatedCount = 0;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ExpenseEntity expense = documentToExpense(doc);
                            if (expense != null) {
                                ExpenseEntity existing = database.expenseDao().getExpenseById(expense.getId());
                                if (existing == null) {
                                    database.expenseDao().insert(expense);
                                    addedCount++;
                                } else {
                                    database.expenseDao().update(expense);
                                    updatedCount++;
                                }
                            }
                        }
                        Log.d(TAG, "Synced expenses: " + addedCount + " added, " + updatedCount + " updated");
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync expenses", e));
    }

    private void syncRevenues(FirebaseFirestore db, String farmId) {
        db.collection("revenues")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    databaseExecutor.execute(() -> {
                        int addedCount = 0;
                        int updatedCount = 0;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            RevenueEntity revenue = documentToRevenue(doc);
                            if (revenue != null) {
                                RevenueEntity existing = database.revenueDao().getRevenueById(revenue.getId());
                                if (existing == null) {
                                    database.revenueDao().insert(revenue);
                                    addedCount++;
                                } else {
                                    database.revenueDao().update(revenue);
                                    updatedCount++;
                                }
                            }
                        }
                        Log.d(TAG, "Synced revenues: " + addedCount + " added, " + updatedCount + " updated");
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync revenues", e));
    }

    private void syncTasks(FirebaseFirestore db, String farmId) {
        db.collection("tasks")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    databaseExecutor.execute(() -> {
                        int addedCount = 0;
                        int updatedCount = 0;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            TaskEntity task = documentToTask(doc);
                            if (task != null) {
                                TaskEntity existing = database.taskDao().getTaskById(task.getId());
                                if (existing == null) {
                                    database.taskDao().insertTask(task);
                                    addedCount++;
                                } else {
                                    database.taskDao().updateTask(task);
                                    updatedCount++;
                                }
                            }
                        }
                        Log.d(TAG, "Synced tasks: " + addedCount + " added, " + updatedCount + " updated");
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync tasks", e));
    }

    private void cleanupDuplicateLocalUsers() {
        databaseExecutor.execute(() -> {
            List<UserEntity> allUsers = database.userDao().getAllUsers();
            Log.d(TAG, "Cleaning up " + allUsers.size() + " local users");

            // Map to track unique UIDs and emails
            Map<String, UserEntity> uniqueByUid = new HashMap<>();
            Map<String, UserEntity> uniqueByEmail = new HashMap<>();
            List<UserEntity> toDelete = new ArrayList<>();

            for (UserEntity user : allUsers) {
                String uid = user.getUid();
                String email = user.getEmail();

                // Check by UID
                if (uid != null && !uid.isEmpty()) {
                    if (uniqueByUid.containsKey(uid)) {
                        // Duplicate UID found - mark for deletion
                        toDelete.add(user);
                        Log.d(TAG, "Duplicate by UID: " + uid + " (keeping " + uniqueByUid.get(uid).getLocalId() + ", deleting " + user.getLocalId() + ")");
                    } else {
                        uniqueByUid.put(uid, user);
                    }
                }

                // Check by email (for users without UID)
                if (email != null && !email.isEmpty()) {
                    if (uniqueByEmail.containsKey(email)) {
                        // Duplicate email found - mark for deletion
                        if (!toDelete.contains(user)) {
                            toDelete.add(user);
                            Log.d(TAG, "Duplicate by email: " + email + " (keeping " + uniqueByEmail.get(email).getLocalId() + ", deleting " + user.getLocalId() + ")");
                        }
                    } else {
                        uniqueByEmail.put(email, user);
                    }
                }
            }

            // Delete duplicates
            for (UserEntity user : toDelete) {
                database.userDao().delete(user);
                Log.d(TAG, "Deleted duplicate user: " + user.getName() + " (ID: " + user.getLocalId() + ")");
            }

            // Verify owner exists and has correct data
            UserEntity owner = database.userDao().getUserByEmail("owner@afrinuts.com");
            if (owner != null) {
                Log.d(TAG, "Owner exists: " + owner.getName() + " (ID: " + owner.getLocalId() + ", UID: " + owner.getUid() + ")");

                // Ensure owner has correct phone number
                if (owner.getPhone() == null || owner.getPhone().isEmpty()) {
                    owner.setPhone("+225 00 00 00 00");
                    database.userDao().update(owner);
                    Log.d(TAG, "Updated owner phone number");
                }
            }

            runOnUiThread(() -> {
                Log.d(TAG, "Cleanup complete. Removed " + toDelete.size() + " duplicates.");
                loadLocalUsers();
            });
        });
    }

    // ========================================
    // DOCUMENT CONVERTERS
    // ========================================

    private UserEntity documentToUser(DocumentSnapshot doc) {
        try {
            // Get Firebase UID from document ID
            String uid = doc.getId();

            // Get fields safely
            String name = doc.getString("name");
            String roleStr = doc.getString("role");
            String phone = doc.getString("phone");
            String email = doc.getString("email");
            String authMethodStr = doc.getString("authMethod");
            Boolean isActive = doc.getBoolean("isActive");
            Long createdAt = doc.getLong("createdAt");

            // Get farmId (could be String or Long)
            long farmId = 1; // default
            Object farmIdObj = doc.get("farmId");
            if (farmIdObj instanceof Long) {
                farmId = (Long) farmIdObj;
            } else if (farmIdObj instanceof String) {
                try {
                    farmId = Long.parseLong((String) farmIdObj);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid farmId format: " + farmIdObj);
                }
            }

            // Use the Firebase Auth constructor with uid and farmId
            UserEntity user = new UserEntity(
                    uid,
                    name,
                    UserEntity.UserRole.valueOf(roleStr),
                    phone,
                    email,
                    "",
                    farmId
            );

            // Set additional fields
            user.setAuthMethod(UserEntity.AuthMethod.valueOf(authMethodStr));
            user.setActive(isActive != null ? isActive : true);
            if (createdAt != null) user.setCreatedAt(createdAt);

            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to user", e);
            return null;
        }
    }

    private BlockEntity documentToBlock(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");

            // Enhanced farmId parsing with error handling
            String farmIdStr = doc.getString("farmId");
            long farmId = 0;
            if (farmIdStr != null) {
                try {
                    farmId = Long.parseLong(farmIdStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid farmId format for block: " + farmIdStr);
                    return null;
                }
            } else {
                Log.e(TAG, "Missing farmId for block: " + id);
                return null;
            }

            String blockName = doc.getString("blockName");
            if (blockName == null) {
                Log.e(TAG, "Missing blockName for block: " + id);
                return null;
            }

            double hectareSize = doc.getDouble("hectareSize");
            String status = doc.getString("status");
            if (status == null) {
                Log.e(TAG, "Missing status for block: " + id);
                return null;
            }

            Long clearedDate = doc.getLong("clearedDate");
            Long plowedDate = doc.getLong("plowedDate");
            Long plantedDate = doc.getLong("plantedDate");
            double survivalRate = doc.getDouble("survivalRate");
            long replacementCount = doc.getLong("replacementCount");
            String notes = doc.getString("notes");

            BlockEntity block = new BlockEntity(farmId, blockName, hectareSize, notes);
            block.setId(id);
            block.setStatus(BlockEntity.BlockStatus.valueOf(status));
            block.setClearedDate(clearedDate);
            block.setPlowedDate(plowedDate);
            block.setPlantedDate(plantedDate);
            block.setSurvivalRate(survivalRate);
            block.setReplacementCount((int) replacementCount);

            return block;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to block", e);
            return null;
        }
    }

    private ExpenseEntity documentToExpense(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");

            // Enhanced farmId parsing with error handling
            String farmIdStr = doc.getString("farmId");
            long farmId = 0;
            if (farmIdStr != null) {
                try {
                    farmId = Long.parseLong(farmIdStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid farmId format for expense: " + farmIdStr);
                    return null;
                }
            } else {
                Log.e(TAG, "Missing farmId for expense: " + id);
                return null;
            }

            Long blockId = doc.getLong("blockId");
            String category = doc.getString("category");
            if (category == null) {
                Log.e(TAG, "Missing category for expense: " + id);
                return null;
            }

            double amount = doc.getDouble("amount");
            long date = doc.getLong("date");
            String description = doc.getString("description");
            String receiptPath = doc.getString("receiptPath");
            long createdAt = doc.getLong("createdAt");

            ExpenseEntity.ExpenseCategory expenseCategory;
            try {
                expenseCategory = ExpenseEntity.ExpenseCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid expense category: " + category);
                return null;
            }

            ExpenseEntity expense = new ExpenseEntity(
                    farmId, blockId, expenseCategory, amount, date, description
            );
            expense.setId(id);
            expense.setReceiptPath(receiptPath);
            expense.setCreatedAt(createdAt);

            return expense;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to expense", e);
            return null;
        }
    }

    private RevenueEntity documentToRevenue(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");

            // Enhanced farmId parsing with error handling
            String farmIdStr = doc.getString("farmId");
            long farmId = 0;
            if (farmIdStr != null) {
                try {
                    farmId = Long.parseLong(farmIdStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid farmId format for revenue: " + farmIdStr);
                    return null;
                }
            } else {
                Log.e(TAG, "Missing farmId for revenue: " + id);
                return null;
            }

            Long blockId = doc.getLong("blockId");
            long harvestDate = doc.getLong("harvestDate");
            double quantityKg = doc.getDouble("quantityKg");
            double pricePerKg = doc.getDouble("pricePerKg");
            double totalAmount = doc.getDouble("totalAmount");
            String buyer = doc.getString("buyer");
            String quality = doc.getString("quality");
            if (quality == null) {
                Log.e(TAG, "Missing quality for revenue: " + id);
                return null;
            }

            String notes = doc.getString("notes");
            long createdAt = doc.getLong("createdAt");
            long updatedAt = doc.getLong("updatedAt");

            RevenueEntity.QualityGrade qualityGrade;
            try {
                qualityGrade = RevenueEntity.QualityGrade.valueOf(quality);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid quality grade: " + quality);
                return null;
            }

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    blockId,
                    harvestDate,
                    quantityKg,
                    pricePerKg,
                    buyer,
                    qualityGrade,
                    notes
            );
            revenue.setId(id);
            revenue.setCreatedAt(createdAt);
            revenue.setUpdatedAt(updatedAt);

            return revenue;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to revenue", e);
            return null;
        }
    }

    private TaskEntity documentToTask(DocumentSnapshot doc) {
        try {
            long id = doc.getLong("id");

            // Enhanced farmId parsing with error handling
            String farmIdStr = doc.getString("farmId");
            long farmId = 0;
            if (farmIdStr != null) {
                try {
                    farmId = Long.parseLong(farmIdStr);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid farmId format for task: " + farmIdStr);
                    return null;
                }
            } else {
                Log.e(TAG, "Missing farmId for task: " + id);
                return null;
            }

            Long blockId = doc.getLong("blockId");
            String title = doc.getString("title");
            if (title == null) {
                Log.e(TAG, "Missing title for task: " + id);
                return null;
            }

            String description = doc.getString("description");
            String status = doc.getString("status");
            if (status == null) {
                Log.e(TAG, "Missing status for task: " + id);
                return null;
            }

            String type = doc.getString("type");
            if (type == null) {
                Log.e(TAG, "Missing type for task: " + id);
                return null;
            }

            long dueDate = doc.getLong("dueDate");
            Long completedDate = doc.getLong("completedDate");

            TaskEntity.TaskStatus taskStatus;
            try {
                taskStatus = TaskEntity.TaskStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid task status: " + status);
                return null;
            }

            TaskEntity.TaskType taskType;
            try {
                taskType = TaskEntity.TaskType.valueOf(type);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid task type: " + type);
                return null;
            }

            TaskEntity task = new TaskEntity(
                    farmId,
                    blockId,
                    title,
                    description,
                    taskType,
                    taskStatus,
                    dueDate
            );
            task.setId(id);
            task.setCompletedDate(completedDate);

            return task;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to task", e);
            return null;
        }
    }

    // ========================================
    // USER MANAGEMENT
    // ========================================

    private void ensureOwnerExists() {
        databaseExecutor.execute(() -> {
            UserEntity owner = database.userDao().getUserByEmail("owner@afrinuts.com");

            // Get current Firebase user (if logged in)
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

            if (owner == null) {
                // Create owner in local DB if not exists
                String hashedPassword = AuthUtils.hash("admin123");

                // ✅ Ensure role is OWNER and name is "Farm Owner"
                owner = new UserEntity(
                        "", // Temporary UID, will be updated
                        "Farm Owner",  // ← Correct name
                        UserEntity.UserRole.OWNER,  // ← Correct role
                        "+225 00 00 00 00",
                        "owner@afrinuts.com",
                        hashedPassword,
                        1L // farmId
                );

                // Insert into local DB
                long localId = database.userDao().insert(owner);
                owner.setLocalId(localId);
                Log.d(TAG, "✅ Created owner in local DB with name: Farm Owner, role: OWNER");
            } else {
                // ✅ Ensure existing owner has correct role and name
                boolean needsUpdate = false;

                if (owner.getRole() != UserEntity.UserRole.OWNER) {
                    owner.setRole(UserEntity.UserRole.OWNER);
                    needsUpdate = true;
                    Log.d(TAG, "Fixed owner role to OWNER");
                }

                if (!"Farm Owner".equals(owner.getName())) {
                    owner.setName("Farm Owner");
                    needsUpdate = true;
                    Log.d(TAG, "Fixed owner name to Farm Owner");
                }

                if (owner.getFarmId() != 1L) {
                    owner.setFarmId(1L);
                    needsUpdate = true;
                    Log.d(TAG, "Fixed owner farmId to 1");
                }

                if (needsUpdate) {
                    database.userDao().update(owner);
                    Log.d(TAG, "✅ Updated existing owner with correct data");
                } else {
                    Log.d(TAG, "Owner already exists in local DB with correct data");
                }
            }

            // If we have a Firebase user, update the UID and save to Firestore
            if (firebaseUser != null && owner != null) {
                boolean needsUpdate = false;
                String realUid = firebaseUser.getUid();

                if (!realUid.equals(owner.getUid())) {
                    owner.setUid(realUid);
                    needsUpdate = true;
                    Log.d(TAG, "Updated local owner with real Firebase UID: " + realUid);
                }

                if (owner.getFarmId() != 1L) {
                    owner.setFarmId(1L);
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    database.userDao().update(owner);
                }

                // Save to Firestore with correct UID and role
                saveOwnerToFirestore(owner);
            } else {
                Log.d(TAG, "No Firebase user yet, will save after authentication");
            }
        });
    }

    private void saveOwnerToFirestore(UserEntity owner) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get the current Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot save owner to Firestore: No authenticated user");
            return;
        }

        // CRITICAL: Use the REAL Firebase UID as document ID
        String realUid = currentUser.getUid();

        // ✅ Ensure role is OWNER
        String role = "OWNER";
        String name = "Farm Owner";

        if (owner != null) {
            if (owner.getRole() == UserEntity.UserRole.OWNER) {
                role = "OWNER";
            } else {
                Log.w(TAG, "Owner role was " + owner.getRole() + ", forcing to OWNER");
            }
            name = owner.getName();
            if (name == null || name.isEmpty() || !name.equals("Farm Owner")) {
                name = "Farm Owner";
            }
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", realUid);
        userMap.put("localId", owner != null ? owner.getLocalId() : 1);
        userMap.put("name", name);
        userMap.put("role", role);  // ← Ensure role is OWNER
        userMap.put("phone", owner != null ? owner.getPhone() : "+225 00 00 00 00");
        userMap.put("email", "owner@afrinuts.com");
        userMap.put("authMethod", "PASSWORD");
        userMap.put("isActive", true);
        userMap.put("createdAt", owner != null ? owner.getCreatedAt() : System.currentTimeMillis());
        userMap.put("farmId", "1");

        Log.d(TAG, "Saving owner to Firestore with UID: " + realUid + ", role: " + role + ", name: " + name);

        db.collection("users")
                .document(realUid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Owner saved to Firestore with UID: " + realUid + ", role: OWNER");
                    if (owner != null && !realUid.equals(owner.getUid())) {
                        owner.setUid(realUid);
                        database.userDao().update(owner);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error saving owner to Firestore", e);
                });
    }

    private void authenticateWithEmail(String email, String password) {
        Log.d(TAG, "Attempting login with email: " + email);

        // First check if user exists locally and verify password
        databaseExecutor.execute(() -> {
            UserEntity localUser = database.userDao().getUserByEmail(email);
            if (localUser != null) {
                Log.d(TAG, "Local user found: " + localUser.getName() + ", role: " + localUser.getRole());
                Log.d(TAG, "Local password hash: " + localUser.getPasswordHash());
                Log.d(TAG, "Entered password hash: " + AuthUtils.hash(password));

                if (localUser.getPasswordHash() != null &&
                        localUser.getPasswordHash().equals(AuthUtils.hash(password))) {
                    Log.d(TAG, "Local password verification SUCCESS");
                    // Proceed with Firebase auth
                } else {
                    Log.w(TAG, "Local password verification FAILED");
                }
            } else {
                Log.d(TAG, "No local user found with email: " + email);
            }
        });

        firebaseManager.signInWithEmail(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Firebase auth SUCCESS for UID: " + user.getUid());
                // After Firebase success, ensure user exists in local DB
                ensureUserInLocalDB(user);
                loginSuccess();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Firebase auth FAILED: " + error);
                loginFailed("Authentication failed: " + error);
            }
        });
    }

    private void ensureUserInLocalDB(FirebaseUser firebaseUser) {
        databaseExecutor.execute(() -> {
            UserEntity existingUser = database.userDao().getUserByUid(firebaseUser.getUid());

            if (existingUser == null) {
                // User doesn't exist in local DB, create from Firebase data
                Log.d(TAG, "Creating local user from Firebase data");

                String email = firebaseUser.getEmail();
                String name = email != null ? email.split("@")[0] : "User";

                // Check if this is the owner email
                if ("owner@afrinuts.com".equals(email)) {
                    name = "Farm Owner";
                    UserEntity owner = new UserEntity(
                            firebaseUser.getUid(),
                            name,
                            UserEntity.UserRole.OWNER,
                            "+225 00 00 00 00",
                            email,
                            AuthUtils.hash("admin123"), // Default password hash
                            1L
                    );
                    database.userDao().insert(owner);
                    Log.d(TAG, "✅ Created owner in local DB with password hash for 'admin123'");
                } else {
                    // For other users, create with temporary data
                    UserEntity user = new UserEntity(
                            firebaseUser.getUid(),
                            name,
                            UserEntity.UserRole.MANAGER,
                            "",
                            email,
                            "", // No password hash for now
                            1L
                    );
                    database.userDao().insert(user);
                    Log.d(TAG, "Created local user: " + name);
                }
            } else {
                Log.d(TAG, "User already exists in local DB");
            }
        });
    }

    private void fixExistingUserFarmIds() {
        databaseExecutor.execute(() -> {
            List<UserEntity> allUsers = database.userDao().getAllUsers();
            boolean anyFixed = false;

            for (UserEntity user : allUsers) {
                if (user.getFarmId() != 1L) {
                    user.setFarmId(1L);
                    database.userDao().update(user);
                    anyFixed = true;
                    Log.d(TAG, "Fixed farmId for user: " + user.getEmail() + " from " + user.getFarmId() + " to 1");
                }
            }

            if (anyFixed) {
                Log.d(TAG, "Fixed farmIds for " + allUsers.size() + " users");
            }
        });
    }

    private void loadLocalUsers() {
        // Check if executor is already shut down
        if (databaseExecutor.isShutdown() || databaseExecutor.isTerminated()) {
            Log.w(TAG, "loadLocalUsers called after executor shutdown");
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                List<UserEntity> userList = database.userDao().getAllUsers();

                // Remove any potential duplicates by UID (keep the first occurrence)
                Map<String, UserEntity> uniqueByUid = new LinkedHashMap<>();
                for (UserEntity user : userList) {
                    String key = user.getUid();
                    if (key == null || key.isEmpty()) {
                        // For users without UID (PIN users), use email
                        key = user.getEmail();
                    }
                    if (!uniqueByUid.containsKey(key)) {
                        uniqueByUid.put(key, user);
                    }
                }

                List<UserEntity> uniqueUsers = new ArrayList<>(uniqueByUid.values());

                runOnUiThread(() -> {
                    users = uniqueUsers;
                    if (users.isEmpty()) {
                        createDefaultOwner();
                    } else {
                        displayUserChips();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading local users", e);
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Error loading users", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void createDefaultOwner() {
        String defaultEmail = "owner@afrinuts.com";
        String defaultPassword = "admin123";

        // Log the hash for debugging
        String hashedPassword = AuthUtils.hash(defaultPassword);
        Log.d(TAG, "Creating default owner with password hash: " + hashedPassword);
        Log.d(TAG, "Expected password: admin123");

        Toast.makeText(this, "Creating default owner...", Toast.LENGTH_SHORT).show();

        firebaseManager.signUpWithEmail(defaultEmail, defaultPassword,
                new FirebaseManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        Log.d(TAG, "Firebase user created with UID: " + user.getUid());
                        createLocalUser(defaultEmail, defaultPassword, user.getUid());
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Firebase user creation failed: " + error);
                        if (error != null && error.contains("email-already-exists")) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this,
                                            "Owner already exists in Firebase", Toast.LENGTH_SHORT).show());
                            // Try to sign in instead
                            firebaseManager.signInWithEmail(defaultEmail, defaultPassword,
                                    new FirebaseManager.AuthCallback() {
                                        @Override
                                        public void onSuccess(FirebaseUser existingUser) {
                                            createLocalUser(defaultEmail, defaultPassword, existingUser.getUid());
                                        }

                                        @Override
                                        public void onError(String signInError) {
                                            Log.e(TAG, "Sign in failed: " + signInError);
                                        }
                                    });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this,
                                            "Firebase error: " + error, Toast.LENGTH_LONG).show());
                        }
                    }
                });
    }

    private void createLocalUser(String email, String password, String firebaseUid) {
        String hashedPassword = AuthUtils.hash(password);

        // ✅ Use default farmId = 1
        long defaultFarmId = 1L;

        UserEntity owner = new UserEntity(
                firebaseUid,  // Use REAL Firebase UID
                "Farm Owner",
                UserEntity.UserRole.OWNER,
                "+225 00 00 00 00",
                email,
                hashedPassword,
                defaultFarmId
        );

        databaseExecutor.execute(() -> {
            UserEntity existing = database.userDao().getUserByEmail(email);

            if (existing == null) {
                // No existing user - insert new
                long id = database.userDao().insert(owner);
                owner.setLocalId(id);
                Log.d(TAG, "✅ Created new owner in local DB");
            } else {
                // User exists - check what needs updating
                boolean needsUpdate = false;

                // Update UID if missing
                if (existing.getUid() == null || existing.getUid().isEmpty()) {
                    existing.setUid(firebaseUid);
                    needsUpdate = true;
                    Log.d(TAG, "   Updated UID: " + firebaseUid);
                }

                // Update role to OWNER if not already
                if (existing.getRole() != UserEntity.UserRole.OWNER) {
                    existing.setRole(UserEntity.UserRole.OWNER);
                    needsUpdate = true;
                    Log.d(TAG, "   Updated role to OWNER");
                }

                // Update name to "Farm Owner" if different
                if (!"Farm Owner".equals(existing.getName())) {
                    existing.setName("Farm Owner");
                    needsUpdate = true;
                    Log.d(TAG, "   Updated name to Farm Owner");
                }

                // Update phone if missing
                if (existing.getPhone() == null || existing.getPhone().isEmpty()) {
                    existing.setPhone("+225 00 00 00 00");
                    needsUpdate = true;
                    Log.d(TAG, "   Updated phone");
                }

                // Update farmId if incorrect
                if (existing.getFarmId() != 1L) {
                    existing.setFarmId(1L);
                    needsUpdate = true;
                    Log.d(TAG, "   Updated farmId to 1");
                }

                if (needsUpdate) {
                    database.userDao().update(existing);
                    Log.d(TAG, "📝 Updated existing user with correct data");
                } else {
                    Log.d(TAG, "✅ Existing user already has correct data");
                }
            }

            runOnUiThread(() -> {
                Toast.makeText(LoginActivity.this,
                        "Default owner ready. Email: owner@afrinuts.com, Password: admin123",
                        Toast.LENGTH_LONG).show();
                loadLocalUsers();
            });
        });
    }

    private void forceSyncOwnerFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String ownerEmail = "owner@afrinuts.com";

        // Get current Firebase user UID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No Firebase user, cannot force sync owner");
            return;
        }

        String ownerUid = currentUser.getUid();

        db.collection("users").document(ownerUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserEntity firestoreOwner = documentToUser(documentSnapshot);
                        if (firestoreOwner != null) {
                            databaseExecutor.execute(() -> {
                                UserEntity localOwner = database.userDao().getUserByEmail(ownerEmail);

                                if (localOwner != null) {
                                    // Force update local owner with Firestore data
                                    boolean needsUpdate = false;

                                    if (!"Farm Owner".equals(firestoreOwner.getName())) {
                                        localOwner.setName("Farm Owner");
                                        needsUpdate = true;
                                    }

                                    if (localOwner.getRole() != UserEntity.UserRole.OWNER) {
                                        localOwner.setRole(UserEntity.UserRole.OWNER);
                                        needsUpdate = true;
                                    }

                                    if (localOwner.getPhone() == null || localOwner.getPhone().isEmpty()) {
                                        localOwner.setPhone("+225 00 00 00 00");
                                        needsUpdate = true;
                                    }

                                    if (localOwner.getUid() == null || localOwner.getUid().isEmpty()) {
                                        localOwner.setUid(ownerUid);
                                        needsUpdate = true;
                                    }

                                    if (needsUpdate) {
                                        database.userDao().update(localOwner);
                                        Log.d(TAG, "✅ Force synced owner from Firestore");
                                    }
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to force sync owner", e));
    }

    private void cleanupDuplicateUsers() {
        databaseExecutor.execute(() -> {
            // Get all users
            List<UserEntity> allUsers = database.userDao().getAllUsers();

            // Find users with same email
            Map<String, List<UserEntity>> usersByEmail = new HashMap<>();
            for (UserEntity user : allUsers) {
                String email = user.getEmail();
                if (email != null && !email.isEmpty()) {
                    usersByEmail.computeIfAbsent(email, k -> new ArrayList<>()).add(user);
                }
            }

            for (Map.Entry<String, List<UserEntity>> entry : usersByEmail.entrySet()) {
                List<UserEntity> duplicates = entry.getValue();
                if (duplicates.size() > 1) {
                    Log.d(TAG, "Found " + duplicates.size() + " users with email: " + entry.getKey());

                    // Keep the one with the correct UID (if available)
                    UserEntity keeper = null;
                    for (UserEntity user : duplicates) {
                        if (user.getUid() != null && !user.getUid().isEmpty()) {
                            keeper = user;
                            break;
                        }
                    }

                    // If no UID, keep the one with the phone number
                    if (keeper == null) {
                        for (UserEntity user : duplicates) {
                            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                                keeper = user;
                                break;
                            }
                        }
                    }

                    // Delete all others
                    if (keeper != null) {
                        for (UserEntity user : duplicates) {
                            if (user.getLocalId() != keeper.getLocalId()) {
                                database.userDao().delete(user);
                                Log.d(TAG, "Deleted duplicate user: " + user.getName() + " (ID: " + user.getLocalId() + ")");
                            }
                        }
                    }
                }
            }

            // Ensure owner has correct data
            UserEntity owner = database.userDao().getUserByEmail("owner@afrinuts.com");
            if (owner != null) {
                boolean needsUpdate = false;

                if (owner.getRole() != UserEntity.UserRole.OWNER) {
                    owner.setRole(UserEntity.UserRole.OWNER);
                    needsUpdate = true;
                }

                if (!"Farm Owner".equals(owner.getName())) {
                    owner.setName("Farm Owner");
                    needsUpdate = true;
                }

                if (owner.getPhone() == null || owner.getPhone().isEmpty()) {
                    owner.setPhone("+225 00 00 00 00");
                    needsUpdate = true;
                }

                if (needsUpdate) {
                    database.userDao().update(owner);
                    Log.d(TAG, "Updated owner with correct data");
                }
            }

            // Reload users
            runOnUiThread(this::loadLocalUsers);
        });
    }

    private void displayUserChips() {
        if (userChipsContainer == null || users == null) return;

        userChipsContainer.removeAllViews();

        for (UserEntity user : users) {
            if (!user.isActive()) continue;

            Chip chip = new Chip(this);
            chip.setText(user.getName());

            int iconResId = getRoleIcon(user.getRole());
            chip.setChipIconResource(iconResId);
            chip.setChipIconTint(ContextCompat.getColorStateList(this, R.color.primary));
            chip.setChipIconVisible(true);

            chip.setChipBackgroundColorResource(R.color.warm_grey);
            chip.setTextColor(getColor(R.color.text_dark));
            chip.setChipStrokeColorResource(R.color.primary);
            chip.setChipStrokeWidth(1.0f);
            chip.setClickable(true);
            chip.setCheckable(true);
            chip.setChecked(false);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Animate selection
                    buttonView.animate().scaleX(1.02f).scaleY(1.02f).setDuration(150).start();

                    for (int i = 0; i < userChipsContainer.getChildCount(); i++) {
                        Chip otherChip = (Chip) userChipsContainer.getChildAt(i);
                        if (otherChip != buttonView) {
                            otherChip.setChecked(false);
                            otherChip.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                        }
                    }
                    selectedUser = user;
                    updateLoginFieldsForUser(user);
                }
            });

            userChipsContainer.addView(chip);
        }

        // Animate chips entrance
        for (int i = 0; i < userChipsContainer.getChildCount(); i++) {
            View chip = userChipsContainer.getChildAt(i);
            chip.setAlpha(0f);
            chip.setTranslationX(-20f);
            chip.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .setStartDelay(i * 50)
                    .start();
        }

        tvSelectUser.setVisibility(View.VISIBLE);
        userSelectorDivider.setVisibility(View.VISIBLE);
        userScrollView.setVisibility(View.VISIBLE);
    }

    private int getRoleIcon(UserEntity.UserRole role) {
        switch (role) {
            case OWNER: return R.drawable.ic_star;
            case MANAGER: return R.drawable.ic_assignment;
            case WORKER: return R.drawable.ic_person;
            case INVESTOR: return R.drawable.ic_trending_up;
            default: return R.drawable.ic_person;
        }
    }

    private void updateLoginFieldsForUser(UserEntity user) {
        if (user == null) return;

        // Animate field transition
        layoutEmailPin.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            if (user.getAuthMethod() == UserEntity.AuthMethod.PIN) {
                layoutEmailPin.setHint("Enter PIN");
                etEmailPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                layoutPassword.setVisibility(View.GONE);
                isPinMode = true;
                etEmailPin.setText("");
            } else {
                layoutEmailPin.setHint("Email");
                etEmailPin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                layoutPassword.setVisibility(View.VISIBLE);
                isPinMode = false;
                if (user.getEmail() != null) {
                    etEmailPin.setText(user.getEmail());
                } else {
                    etEmailPin.setText("");
                }
                etPassword.setText("");
            }
            layoutEmailPin.animate().alpha(1f).setDuration(150).start();
        }).start();
    }

    private void attemptLogin() {
        if (selectedUser == null) {
            Toast.makeText(this, "Please select a user", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailPin = etEmailPin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(emailPin)) {
            etEmailPin.setError(isPinMode ? "PIN is required" : "Email is required");
            return;
        }

        if (!isPinMode && TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        // Disable button and show loading
        btnLogin.setEnabled(false);
        btnLogin.setText("Authenticating...");
        btnLogin.animate().scaleX(0.98f).scaleY(0.98f).setDuration(150).start();

        if (isPinMode) {
            authenticateWithPin(emailPin);
        } else {
            authenticateWithEmail(emailPin, password);
        }
    }

    private void authenticateWithPin(String pin) {
        String hashedPin = AuthUtils.hash(pin);
        boolean authenticated = hashedPin != null && hashedPin.equals(selectedUser.getPinHash());

        if (authenticated) {
            loginSuccess();
        } else {
            loginFailed("Invalid PIN");
        }
    }

    // After successful login
    private void loginSuccess() {
        selectedUser.setLastLoginAt(System.currentTimeMillis());

        databaseExecutor.execute(() -> {
            database.userDao().update(selectedUser);
            SessionManager sessionManager = new SessionManager(LoginActivity.this);
            sessionManager.createLoginSession(selectedUser);

            runOnUiThread(() -> {
                // ✅ Force sync owner data after login
                if ("owner@afrinuts.com".equals(selectedUser.getEmail())) {
                    forceSyncOwnerFromFirestore();
                }

                // Success animation
                btnLogin.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150)
                        .withEndAction(() -> {
                            btnLogin.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                            navigateToMain();
                        }).start();
            });
        });
    }

    private void loginFailed(String message) {
        runOnUiThread(() -> {
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
            btnLogin.animate().scaleX(1f).scaleY(1f).setDuration(150).start();

            // Shake animation for error
            loginCard.animate().translationXBy(-10f).setDuration(50)
                    .withEndAction(() -> loginCard.animate().translationXBy(20f).setDuration(50)
                            .withEndAction(() -> loginCard.animate().translationXBy(-10f).setDuration(50)
                                    .withEndAction(() -> loginCard.animate().translationXBy(0f).setDuration(50).start())
                                    .start())
                            .start())
                    .start();

            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        // Window transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown executor gracefully
        databaseExecutor.shutdown();
        // Optionally await termination
        try {
            if (!databaseExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            databaseExecutor.shutdownNow();
        }
    }
}