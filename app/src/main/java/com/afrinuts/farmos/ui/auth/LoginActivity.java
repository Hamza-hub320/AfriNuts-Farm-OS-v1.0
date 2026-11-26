package com.afrinuts.farmos.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.core.content.res.ResourcesCompat;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.utils.AuthUtils;
import com.afrinuts.farmos.utils.SessionManager;
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
    private TextInputEditText etPassword;
    private View passwordFieldGroup;
    private Button btnLogin;
    private TextView tvSelectUser;
    private TextView tvEmailLabel;
    private HorizontalScrollView userScrollView;
    private LinearLayout userChipsContainer;
    private View selectedUserChip;

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
        setContentColumnWidth();

        // ✅ Check if already logged in via SessionManager
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            if (sessionManager.getUserRole() == UserEntity.UserRole.WORKER) {
                Log.w(TAG, "Saved session belongs to a WORKER — clearing");
                FirebaseAuth.getInstance().signOut();
                sessionManager.logout();
                // fall through to the login UI
            } else {
                Log.d(TAG, "Already logged in via SessionManager, redirecting to Main");
                navigateToMain();
                return;
            }
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
        passwordFieldGroup = findViewById(R.id.passwordFieldGroup);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSelectUser = findViewById(R.id.tvSelectUser);
        tvEmailLabel = findViewById(R.id.tvEmailLabel);
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
                        Log.d(TAG, "No local users → showing first-time email and password login");
                        isFirstTimeLogin = true;
                        showLoginFieldsForFirstTimeUser();
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

    private void showLoginFieldsForFirstTimeUser() {
        Log.d(TAG, "First-time login mode - hiding user selector");

        tvSelectUser.setVisibility(View.GONE);
        userScrollView.setVisibility(View.GONE);

        passwordFieldGroup.setVisibility(View.VISIBLE);
        tvEmailLabel.setText("Email");
        etEmailPin.setHint("Email");
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
        passwordFieldGroup.setVisibility(View.GONE);
        tvEmailLabel.setText("Email");
        etEmailPin.setHint("Email");
    }

    // ========================================
    // USER CHIPS DISPLAY
    // ========================================

    private void displayUserChips() {
        if (userChipsContainer == null || users == null || users.isEmpty()) return;

        userChipsContainer.removeAllViews();

        for (UserEntity user : users) {
            if (!user.isActive()) continue;
            if (user.getRole() == UserEntity.UserRole.WORKER) continue;   // §9.3 — not a sign-in role

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.HORIZONTAL);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(8), dp(6), dp(14), dp(6));
            chip.setBackground(userChipBackground(false));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setContentDescription(user.getName() + ", " + user.getRole().name());

            TextView initials = new TextView(this);
            initials.setGravity(Gravity.CENTER);
            initials.setText(user.getInitials());
            initials.setTextColor(Color.rgb(10, 85, 48));
            initials.setTextSize(13);
            initials.setTypeface(font(R.font.manrope_extrabold));
            initials.setBackground(avatarBackground(false));
            chip.addView(initials, new LinearLayout.LayoutParams(dp(36), dp(36)));

            int iconResId = getRoleIcon(user.getRole());
            ImageView roleIcon = new ImageView(this);
            roleIcon.setImageResource(iconResId);
            roleIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary));
            LinearLayout.LayoutParams roleIconParams = new LinearLayout.LayoutParams(dp(18), dp(18));
            roleIconParams.setMargins(dp(8), 0, dp(7), 0);
            chip.addView(roleIcon, roleIconParams);

            TextView name = new TextView(this);
            name.setText(user.getName());
            name.setTextColor(Color.rgb(28, 27, 26));
            name.setTextSize(16);
            name.setTypeface(font(R.font.manrope_bold));
            name.setMaxLines(1);
            chip.addView(name, new LinearLayout.LayoutParams(-2, -2));

            chip.setOnClickListener(view -> {
                if (selectedUserChip != null && selectedUserChip != view) {
                    setUserChipSelected(selectedUserChip, false);
                }
                selectedUserChip = view;
                setUserChipSelected(view, true);
                selectedUser = user;
                updateLoginFieldsForUser(user);
            });

            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(-2, dp(58));
            chipParams.setMargins(0, dp(2), dp(8), dp(2));
            userChipsContainer.addView(chip, chipParams);
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
        userScrollView.setVisibility(View.VISIBLE);
    }

    private GradientDrawable userChipBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(selected ? Color.rgb(227, 240, 231) : Color.rgb(246, 241, 231));
        background.setCornerRadius(dp(999));
        background.setStroke(dp(1), selected ? Color.rgb(14, 107, 60) : Color.rgb(233, 225, 207));
        return background;
    }

    private void setUserChipSelected(View chipView, boolean selected) {
        if (!(chipView instanceof LinearLayout)) return;
        LinearLayout chip = (LinearLayout) chipView;
        chip.setBackground(userChipBackground(selected));
        if (chip.getChildCount() > 0 && chip.getChildAt(0) instanceof TextView) {
            TextView initials = (TextView) chip.getChildAt(0);
            initials.setTextColor(selected ? Color.WHITE : Color.rgb(10, 85, 48));
            initials.setBackground(avatarBackground(selected));
        }
        for (int i = 0; i < chip.getChildCount(); i++) {
            if (chip.getChildAt(i) instanceof TextView && i > 0) {
                ((TextView) chip.getChildAt(i)).setTextColor(Color.rgb(28, 27, 26));
            }
        }
    }

    private GradientDrawable avatarBackground(boolean selected) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(selected ? Color.rgb(14, 107, 60) : Color.rgb(227, 240, 231));
        background.setShape(GradientDrawable.OVAL);
        return background;
    }

    private Typeface font(int fontId) {
        Typeface typeface = ResourcesCompat.getFont(this, fontId);
        return typeface == null ? Typeface.DEFAULT_BOLD : typeface;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setContentColumnWidth() {
        View contentColumn = findViewById(R.id.contentColumn);
        int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(48);
        ViewGroup.LayoutParams params = contentColumn.getLayoutParams();
        params.width = Math.min(availableWidth, dp(480));
        contentColumn.setLayoutParams(params);
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
                tvEmailLabel.setText("Enter PIN");
                etEmailPin.setHint("Enter PIN");
                etEmailPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                passwordFieldGroup.setVisibility(View.GONE);
                isPinMode = true;
                etEmailPin.setText("");
            } else {
                tvEmailLabel.setText("Email");
                etEmailPin.setHint("Email");
                etEmailPin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                passwordFieldGroup.setVisibility(View.VISIBLE);
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
                        if (firestoreUser.getRole() == UserEntity.UserRole.WORKER) {
                            Log.w(TAG, "Worker sign-in refused: " + firestoreUser.getUid());
                            FirebaseAuth.getInstance().signOut();
                            loginFailed("Field workers don't sign in — your supervisor records your work.");
                            return;
                        }
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
                // Replace with the full Firestore profile while retaining local credentials and row identity.
                firestoreUser.setLocalId(existingLocal.getLocalId());
                firestoreUser.setPasswordHash(existingLocal.getPasswordHash());
                firestoreUser.setPinHash(existingLocal.getPinHash());
                database.userDao().update(firestoreUser);
                userId = existingLocal.getLocalId();
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
            String bio = doc.getString("bio");
            String company = doc.getString("company");
            String title = doc.getString("title");
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
            user.setBio(bio);
            user.setCompany(company);
            user.setTitle(title);

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
