package com.afrinuts.farmos.ui.auth;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // ✅ SYNC ALL FARM DATA BEFORE LOADING LOCAL USERS
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

    // ========================================
    // AUTO-SYNC ALL FARM DATA FROM FIRESTORE
    // ========================================

    private void syncUsersFromFirestore() {
        Log.d(TAG, "Syncing users from Firestore...");

        String farmId = "1";
        Log.d(TAG, "Looking for users with farmId: " + farmId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("farmId", farmId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " users with farmId=" + farmId);

                    databaseExecutor.execute(() -> {
                        int addedCount = 0;
                        int updatedCount = 0;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            UserEntity user = documentToUser(doc);
                            if (user != null) {
                                // Check if user exists by uid
                                UserEntity existing = database.userDao().getUserByUid(user.getUid());
                                if (existing == null) {
                                    // Insert new user
                                    database.userDao().insert(user);
                                    addedCount++;
                                    Log.d(TAG, "Added user: " + user.getEmail());
                                } else {
                                    // Update existing user, preserving localId
                                    user.setLocalId(existing.getLocalId());
                                    database.userDao().update(user);
                                    updatedCount++;
                                    Log.d(TAG, "Updated user: " + user.getEmail());
                                }
                            }
                        }

                        final int finalAddedCount = addedCount;
                        final int finalUpdatedCount = updatedCount;
                        runOnUiThread(() -> {
                            Log.d(TAG, "Synced users: " + finalAddedCount + " added, " +
                                    finalUpdatedCount + " updated");

                            // AFTER users are synced, load local users
                            loadLocalUsers();
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync users from Firestore", e);
                    loadLocalUsers();
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
            if (owner == null) {
                // Create owner in local DB if not exists
                String hashedPassword = AuthUtils.hash("admin123");

                // Use email as UID consistently
                String ownerUid = "owner@afrinuts.com";

                owner = new UserEntity(
                        ownerUid,  // Explicitly set UID
                        "Farm Owner",
                        UserEntity.UserRole.OWNER,
                        "+225 00 00 00 00",
                        "owner@afrinuts.com",
                        hashedPassword,
                        1L // farmId
                );

                // Insert into local DB
                long localId = database.userDao().insert(owner);
                owner.setLocalId(localId);

                Log.d(TAG, "Created owner in local DB with UID: " + ownerUid);

                // Save to Firestore
                saveOwnerToFirestore(owner);
            } else {
                Log.d(TAG, "Owner already exists in local DB");

                // Ensure existing owner has a UID
                if (owner.getUid() == null || owner.getUid().isEmpty()) {
                    owner.setUid(owner.getEmail());
                    database.userDao().update(owner);
                    Log.d(TAG, "Updated existing owner with UID: " + owner.getEmail());
                }

                // Optionally ensure it exists in Firestore too
                saveOwnerToFirestore(owner);
            }
        });
    }

    private void saveOwnerToFirestore(UserEntity owner) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Ensure UID is not null - if it is, use email as fallback
        final String uid;  // Declare as final
        if (owner.getUid() == null || owner.getUid().isEmpty()) {
            uid = owner.getEmail();  // Fallback to email
            owner.setUid(uid);       // Update the entity
            Log.w(TAG, "Owner UID was null, using email as UID: " + uid);
        } else {
            uid = owner.getUid();
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("localId", owner.getLocalId());
        userMap.put("name", owner.getName());
        userMap.put("role", owner.getRole().name());
        userMap.put("phone", owner.getPhone());
        userMap.put("email", owner.getEmail());
        userMap.put("authMethod", owner.getAuthMethod().name());
        userMap.put("isActive", owner.isActive());
        userMap.put("createdAt", owner.getCreatedAt());
        userMap.put("farmId", String.valueOf(owner.getFarmId()));

        Log.d(TAG, "Saving owner to Firestore with UID: " + uid);

        db.collection("users")
                .document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    // Use the final uid variable here - it's effectively final
                    Log.d(TAG, "Owner saved to Firestore with UID: " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving owner to Firestore", e);
                });
    }

    private void loadLocalUsers() {
        databaseExecutor.execute(() -> {
            List<UserEntity> userList = database.userDao().getAllUsers();

            runOnUiThread(() -> {
                users = userList;
                if (users.isEmpty()) {
                    createDefaultOwner();
                } else {
                    displayUserChips();
                }
            });
        });
    }

    private void createDefaultOwner() {
        String defaultEmail = "owner@afrinuts.com";
        String defaultPassword = "admin123";

        Toast.makeText(this, "Creating default owner...", Toast.LENGTH_SHORT).show();

        firebaseManager.signUpWithEmail(defaultEmail, defaultPassword,
                new FirebaseManager.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        Log.d(TAG, "Firebase user created: " + user.getUid());
                        createLocalUser(defaultEmail, defaultPassword);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Firebase user creation failed: " + error);
                        if (error != null && error.contains("email-already-exists")) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this,
                                            "Owner already exists in Firebase", Toast.LENGTH_SHORT).show());
                            createLocalUser(defaultEmail, defaultPassword);
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this,
                                            "Firebase error: " + error, Toast.LENGTH_LONG).show());
                            createLocalUser(defaultEmail, defaultPassword);
                        }
                    }
                });
    }

    private void createLocalUser(String email, String password) {
        String hashedPassword = AuthUtils.hash(password);
        UserEntity owner = new UserEntity(
                email,  // use email as UID
                "Farm Owner",
                UserEntity.UserRole.OWNER,
                "+225 00 00 00 00",
                email,
                hashedPassword,
                1L // farmId
        );

        databaseExecutor.execute(() -> {
            UserEntity existing = database.userDao().getUserByEmail(email);
            if (existing == null) {
                database.userDao().insert(owner);
                owner.setLocalId(database.userDao().getUserByUid(email).getLocalId());
            }

            runOnUiThread(() -> {
                Toast.makeText(LoginActivity.this,
                        "Default owner ready. Email: owner@afrinuts.com, Password: admin123",
                        Toast.LENGTH_LONG).show();
                loadLocalUsers();
            });
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
                    for (int i = 0; i < userChipsContainer.getChildCount(); i++) {
                        Chip otherChip = (Chip) userChipsContainer.getChildAt(i);
                        if (otherChip != buttonView) {
                            otherChip.setChecked(false);
                        }
                    }
                    selectedUser = user;
                    updateLoginFieldsForUser(user);
                }
            });

            userChipsContainer.addView(chip);
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

        btnLogin.setEnabled(false);
        btnLogin.setText("Authenticating...");

        if (isPinMode) {
            String hashedPin = AuthUtils.hash(emailPin);
            boolean authenticated = hashedPin != null && hashedPin.equals(selectedUser.getPinHash());

            if (authenticated) {
                selectedUser.setLastLoginAt(System.currentTimeMillis());

                databaseExecutor.execute(() -> {
                    database.userDao().update(selectedUser);

                    SessionManager sessionManager = new SessionManager(LoginActivity.this);
                    sessionManager.createLoginSession(selectedUser);

                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this,
                                "Welcome " + selectedUser.getName(), Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    });
                });
            } else {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    Toast.makeText(LoginActivity.this, "Invalid PIN", Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            firebaseManager.signInWithEmail(emailPin, password,
                    new FirebaseManager.AuthCallback() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            selectedUser.setLastLoginAt(System.currentTimeMillis());

                            databaseExecutor.execute(() -> {
                                database.userDao().update(selectedUser);

                                SessionManager sessionManager = new SessionManager(LoginActivity.this);
                                sessionManager.createLoginSession(selectedUser);

                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this,
                                            "Welcome " + selectedUser.getName(), Toast.LENGTH_SHORT).show();
                                    navigateToMain();
                                });
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                btnLogin.setEnabled(true);
                                btnLogin.setText("Login");
                                Toast.makeText(LoginActivity.this,
                                        "Authentication failed: " + error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
    }
}