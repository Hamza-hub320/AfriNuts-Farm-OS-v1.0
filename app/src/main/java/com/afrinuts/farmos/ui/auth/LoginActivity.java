package com.afrinuts.farmos.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private boolean isNavigating = false;

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
    private boolean isFirstTimeLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        database = AppDatabase.getInstance(this);
        firebaseManager = FirebaseManager.getInstance();
        initViews();

        // ✅ Check if already logged in via SessionManager
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "Already logged in via SessionManager, redirecting to Main");
            navigateToMain();
            return;
        }

        // ✅ Check if already logged in via Firebase
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentFirebaseUser != null) {
            Log.d(TAG, "Already logged in as: " + currentFirebaseUser.getUid());
            checkFirestoreUserAndNavigate(currentFirebaseUser);
            return;
        }

        animateLogoEntrance();
        loadLocalUsers();
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
    // USER LOADING
    // ========================================

    private void loadLocalUsers() {
        if (databaseExecutor.isShutdown() || databaseExecutor.isTerminated()) {
            Log.w(TAG, "loadLocalUsers called after executor shutdown");
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                List<UserEntity> userList = database.userDao().getAllUsers();

                Map<String, UserEntity> uniqueByUid = new LinkedHashMap<>();
                for (UserEntity user : userList) {
                    String key = user.getUid();
                    if (key == null || key.isEmpty()) {
                        key = user.getEmail();
                    }
                    if (!uniqueByUid.containsKey(key)) {
                        uniqueByUid.put(key, user);
                    }
                }

                List<UserEntity> uniqueUsers = new ArrayList<>(uniqueByUid.values());

                runOnUiThread(() -> {
                    users = uniqueUsers;
                    if (!users.isEmpty()) {
                        isFirstTimeLogin = false;
                        displayUserChips();
                        showLoginFieldsForReturningUser();
                    } else {
                        Log.d(TAG, "No local users → fetching from Firestore...");
                        fetchUsersFromFirestore();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading local users", e);
                runOnUiThread(() -> {
                    isFirstTimeLogin = true;
                    showLoginFieldsForFirstTimeUser();
                });
            }
        });
    }

    private void fetchUsersFromFirestore() {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        runOnUiThread(this::showLoginFieldsForFirstTimeUser);
                        return;
                    }

                    databaseExecutor.execute(() -> {
                        for (DocumentSnapshot doc : querySnapshot) {
                            UserEntity user = documentToUser(doc);
                            if (user != null && user.isActive()) {
                                database.userDao().insert(user);
                            }
                        }

                        runOnUiThread(this::loadLocalUsers);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch users", e);
                    runOnUiThread(this::showLoginFieldsForFirstTimeUser);
                });
    }

    private void showLoginFieldsForFirstTimeUser() {
        Log.d(TAG, "First-time login mode - hiding user selector");

        tvSelectUser.setVisibility(View.GONE);
        userSelectorDivider.setVisibility(View.GONE);
        userScrollView.setVisibility(View.GONE);

        layoutPassword.setVisibility(View.VISIBLE);
        layoutEmailPin.setHint("Email Address");
        etEmailPin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        etEmailPin.setText("");
        etPassword.setText("");

        Toast.makeText(this,
                "First-time login? Enter your email and password to access the system.\n\n" +
                        "If you don't have an account, please contact the farm owner.",
                Toast.LENGTH_LONG).show();

        layoutEmailPin.animate().alpha(1f).translationX(0f).setDuration(300).start();
    }

    private void showLoginFieldsForReturningUser() {
        Log.d(TAG, "Returning user mode - showing user selector");
        layoutPassword.setVisibility(View.GONE);
    }

    // ========================================
    // USER CHIPS DISPLAY
    // ========================================

    private void displayUserChips() {
        if (userChipsContainer == null || users == null || users.isEmpty()) return;

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

    // ========================================
    // LOGIN ATTEMPT
    // ========================================

    private void attemptLogin() {
        String emailPin = etEmailPin.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (isFirstTimeLogin || selectedUser == null) {
            if (TextUtils.isEmpty(emailPin)) {
                etEmailPin.setError("Email is required");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Password is required");
                return;
            }

            if (!AuthUtils.isValidEmail(emailPin)) {
                etEmailPin.setError("Invalid email format");
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Authenticating...");
            authenticateWithEmail(emailPin, password);
            return;
        }

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
            authenticateWithPin(emailPin);
        } else {
            authenticateWithEmail(emailPin, password);
        }
    }

    // ========================================
    // AUTHENTICATION METHODS
    // ========================================

    private void authenticateWithPin(String pin) {
        String hashedPin = AuthUtils.hash(pin);
        boolean authenticated = hashedPin != null && hashedPin.equals(selectedUser.getPinHash());

        if (authenticated) {
            loginSuccess();
        } else {
            loginFailed("Invalid PIN");
        }
    }

    private void authenticateWithEmail(String email, String password) {

        // First, check if user exists in local DB
        databaseExecutor.execute(() -> {
            UserEntity localUser = database.userDao().getUserByEmail(email);
            if (localUser != null) {
                Log.d(TAG, "Local user found: " + localUser.getName());
                Log.d(TAG, "Local user UID: " + localUser.getUid());
                Log.d(TAG, "Local user password hash: " + localUser.getPasswordHash());

                // Test hash locally
                String testHash = AuthUtils.hash(password);
                Log.d(TAG, "Test hash of entered password: " + testHash);
                Log.d(TAG, "Stored hash matches? " + testHash.equals(localUser.getPasswordHash()));
            } else {
                Log.d(TAG, "No local user found with email: " + email);
            }
        });

        firebaseManager.signInWithEmail(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d(TAG, "Firebase auth SUCCESS for UID: " + user.getUid());
                checkFirestoreUserAndNavigate(user);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Firebase auth FAILED: " + error);
                loginFailed("Authentication failed: " + error);
            }
        });
    }

    private void checkFirestoreUserAndNavigate(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "❌ User NOT found in Firestore. Access denied.");
                        FirebaseAuth.getInstance().signOut();
                        loginFailed("User not registered. Please contact farm owner.");
                        return;
                    }

                    Boolean isActive = documentSnapshot.getBoolean("isActive");
                    if (isActive == null || !isActive) {
                        Log.e(TAG, "❌ User is inactive");
                        FirebaseAuth.getInstance().signOut();
                        loginFailed("Your account is disabled");
                        return;
                    }

                    Log.d(TAG, "✅ User exists in Firestore");

                    UserEntity firestoreUser = documentToUser(documentSnapshot);
                    if (firestoreUser != null) {
                        saveUserToLocalAndLogin(firestoreUser);
                    } else {
                        loginFailed("Error loading user data");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check user in Firestore", e);
                    loginFailed("Network error. Please check your connection.");
                });
    }

    private void saveUserToLocalAndLogin(UserEntity firestoreUser) {
        databaseExecutor.execute(() -> {
            UserEntity existingLocal = database.userDao().getUserByUid(firestoreUser.getUid());

            long userId;
            if (existingLocal == null) {
                // Create local user from Firestore data
                userId = database.userDao().insert(firestoreUser);
                firestoreUser.setLocalId(userId);
                Log.d(TAG, "✅ Created local user from Firestore: " + firestoreUser.getName() + " with ID: " + userId);
            } else {
                // Update local user from Firestore
                existingLocal.setName(firestoreUser.getName());
                existingLocal.setRole(firestoreUser.getRole());
                existingLocal.setPhone(firestoreUser.getPhone());
                existingLocal.setActive(firestoreUser.isActive());
                if (firestoreUser.getPasswordHash() != null && !firestoreUser.getPasswordHash().isEmpty()) {
                    existingLocal.setPasswordHash(firestoreUser.getPasswordHash());
                }
                database.userDao().update(existingLocal);
                userId = existingLocal.getLocalId();
                firestoreUser.setLocalId(userId);
                Log.d(TAG, "📝 Updated local user from Firestore: " + firestoreUser.getName() + " with ID: " + userId);
            }

            final long finalUserId = userId;
            runOnUiThread(() -> {
                selectedUser = firestoreUser;

                // ✅ Verify user ID is valid before creating session
                if (finalUserId > 0) {
                    loginSuccess();
                } else {
                    Log.e(TAG, "Failed to get valid user ID");
                    loginFailed("Login error - please try again");
                }
            });
        });
    }

    private UserEntity documentToUser(DocumentSnapshot doc) {
        try {
            String uid = doc.getId();
            String name = doc.getString("name");
            String roleStr = doc.getString("role");
            String phone = doc.getString("phone");
            String email = doc.getString("email");
            String authMethodStr = doc.getString("authMethod");
            Boolean isActive = doc.getBoolean("isActive");
            Long createdAt = doc.getLong("createdAt");

            UserEntity.UserRole role;
            try {
                role = UserEntity.UserRole.valueOf(roleStr);
            } catch (Exception e) {
                Log.e(TAG, "Invalid role: " + roleStr);
                return null;
            }

            Object farmIdObj = doc.get("farmId");
            Long farmId = null;

            if (farmIdObj instanceof Long) {
                farmId = (Long) farmIdObj;
            } else if (farmIdObj instanceof String) {
                try {
                    farmId = Long.parseLong((String) farmIdObj);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid farmId format");
                }
            }

            if (farmId == null) {
                Log.e(TAG, "Missing farmId");
                return null;
            }

            UserEntity user = new UserEntity(
                    uid,
                    name,
                    role,
                    phone,
                    email,
                    "",
                    farmId
            );

            user.setAuthMethod(UserEntity.AuthMethod.valueOf(authMethodStr));
            user.setActive(isActive != null ? isActive : true);
            if (createdAt != null) user.setCreatedAt(createdAt);

            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to user", e);
            return null;
        }
    }

    // ========================================
    // LOGIN SUCCESS / FAILURE
    // ========================================

    private void loginSuccess() {
        // ✅ Set all login tracking fields
        selectedUser.setLastLoginAt(System.currentTimeMillis());
        selectedUser.setLastLoginIp(getIpAddress());
        selectedUser.setLastLoginDevice(getDeviceInfo());
        selectedUser.setEmailVerified(true);

        // ✅ Save to local database AND Firestore
        databaseExecutor.execute(() -> {
            // Update local database
            database.userDao().update(selectedUser);
            Log.d(TAG, "User updated in local database");

            // ✅ Update Firestore with all fields including login info
            updateUserInFirestore(selectedUser);

            runOnUiThread(() -> {
                SessionManager sessionManager = new SessionManager(LoginActivity.this);
                sessionManager.createLoginSession(selectedUser);

                Log.d(TAG, "Session created successfully with login tracking");
                navigateToMain();
            });
        });
    }

    private void updateUserInFirestore(UserEntity user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("lastLoginAt", user.getLastLoginAt());
        userMap.put("lastLoginIp", user.getLastLoginIp());
        userMap.put("lastLoginDevice", user.getLastLoginDevice());
        userMap.put("lastUpdatedAt", System.currentTimeMillis());
        userMap.put("emailVerified", user.isEmailVerified());
        userMap.put("updatedAt", System.currentTimeMillis());

        // Also update other fields if they were set
        if (user.getBio() != null) userMap.put("bio", user.getBio());
        if (user.getCompany() != null) userMap.put("company", user.getCompany());
        if (user.getTitle() != null) userMap.put("title", user.getTitle());

        db.collection("users")
                .document(user.getUid())
                .update(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore updated with login info for: " + user.getName());
                    Log.d(TAG, "   - Last Login: " + new java.util.Date(user.getLastLoginAt()));
                    Log.d(TAG, "   - IP: " + user.getLastLoginIp());
                    Log.d(TAG, "   - Device: " + user.getLastLoginDevice());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore with login info", e);
                });
    }

    private String getDeviceName() {
        String manufacturer = android.os.Build.MANUFACTURER;
        String model = android.os.Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private String getDeviceInfo() {
        String device = getDeviceName();
        String osVersion = android.os.Build.VERSION.RELEASE;
        return device + " (Android " + osVersion + ")";
    }

    private String getIpAddress() {
        try {
            java.net.InetAddress ip = java.net.InetAddress.getLocalHost();
            return ip.getHostAddress();
        } catch (Exception e) {
            Log.e(TAG, "Error getting IP address", e);
            return "Unknown";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private void loginFailed(String message) {
        runOnUiThread(() -> {
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");

            // Shake animation for error
            loginCard.animate().translationXBy(-10f).setDuration(50)
                    .withEndAction(() -> loginCard.animate().translationXBy(20f).setDuration(50)
                            .withEndAction(() -> loginCard.animate().translationXBy(-10f).setDuration(50)
                                    .withEndAction(() -> loginCard.animate().translationXBy(0f).setDuration(50).start())
                                    .start())
                            .start())
                    .start();

            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void navigateToMain() {
        if (isNavigating) return;
        isNavigating = true;

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            databaseExecutor.shutdownNow();
        }
    }
}