package com.afrinuts.farmos.ui.users;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.utils.AuthUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserDetailActivity extends BaseActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private long userId;
    private UserEntity user;
    private AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvUserInitials;
    private TextView tvUserName;
    private TextView tvUserRole;
    private Chip chipUserStatus;
    private TextView tvUserPhone;
    private TextView tvUserEmail;
    private LinearLayout layoutEmail;
    private LinearLayout layoutPin;
    private TextView tvAccessRole;
    private TextView tvAccessLevel;
    private ProgressBar accessProgress;
    private TextView tvAccessDescription;
    private TextView tvMemberSince;
    private TextView tvLastLogin;
    private TextView tvAuthMethod;
    private MaterialButton btnEdit;
    private Button btnDeactivate;
    private MaterialButton btnDelete;
    private MaterialButton btnResetPin;
    private Button btnCall;
    private Button btnEmail;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        // Only Owners can access UserDetailActivity
        return sessionManager.isOwner();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        // Get user ID from intent
        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1);
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load data
        loadUserData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvUserInitials = findViewById(R.id.tvUserInitials);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserRole = findViewById(R.id.tvUserRole);
        chipUserStatus = findViewById(R.id.chipUserStatus);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPin = findViewById(R.id.layoutPin);
        tvAccessRole = findViewById(R.id.tvAccessRole);
        tvAccessLevel = findViewById(R.id.tvAccessLevel);
        accessProgress = findViewById(R.id.accessProgress);
        tvAccessDescription = findViewById(R.id.tvAccessDescription);
        tvMemberSince = findViewById(R.id.tvMemberSince);
        tvLastLogin = findViewById(R.id.tvLastLogin);
        tvAuthMethod = findViewById(R.id.tvAuthMethod);
        btnEdit = findViewById(R.id.btnEdit);
        btnDeactivate = findViewById(R.id.btnDeactivate);
        btnDelete = findViewById(R.id.btnDelete);
        btnResetPin = findViewById(R.id.btnResetPin);
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);

        // Setup click listeners
        btnEdit.setOnClickListener(v -> showEditUserDialog());
        btnDeactivate.setOnClickListener(v -> toggleUserStatus());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnResetPin.setOnClickListener(v -> showResetPinDialog());

        btnCall.setOnClickListener(v -> {
            if (user != null && user.getPhone() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + user.getPhone()));
                startActivity(intent);
            }
        });

        btnEmail.setOnClickListener(v -> {
            if (user != null && user.getEmail() != null) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + user.getEmail()));
                startActivity(intent);
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Team Details");
        }
    }

    private void loadUserData() {
        executor.execute(() -> {
            UserEntity loadedUser = database.userDao().getUserById(userId);

            runOnUiThread(() -> {
                user = loadedUser;
                if (user != null) {
                    displayUserData();
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void displayUserData() {
        // Set initials
        tvUserInitials.setText(user.getInitials());

        // Basic info
        tvUserName.setText(user.getName());
        tvUserRole.setText(user.getDisplayRole());

        // Status
        if (user.isActive()) {
            chipUserStatus.setText("Active");
            chipUserStatus.setChipBackgroundColorResource(R.color.primary);
        } else {
            chipUserStatus.setText("Inactive");
            chipUserStatus.setChipBackgroundColorResource(android.R.color.holo_red_dark);
        }

        // Contact info
        tvUserPhone.setText(user.getPhone() != null ? user.getPhone() : "No phone");

        if (user.getAuthMethod() == UserEntity.AuthMethod.PASSWORD) {
            layoutEmail.setVisibility(View.VISIBLE);
            layoutPin.setVisibility(View.GONE);
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
        } else {
            layoutEmail.setVisibility(View.GONE);
            layoutPin.setVisibility(View.VISIBLE);
        }

        // Check if this is the current user (yourself)
        boolean isCurrentUser = user.getId() == sessionManager.getUserId();

        // Owners cannot deactivate themselves
        if (user.getRole() == UserEntity.UserRole.OWNER && isCurrentUser) {
            btnDeactivate.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE); // Also hide delete for yourself
        } else {
            btnDeactivate.setVisibility(View.VISIBLE);

            // Only owners can delete other users
            if (sessionManager.isOwner()) {
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        }

        // Access level
        tvAccessRole.setText(user.getRole().getDisplayName());
        int accessLevel = user.getRole().getAccessLevel();
        tvAccessLevel.setText(accessLevel + " (" + getAccessLevelDescription(accessLevel) + ")");
        accessProgress.setProgress(accessLevel);
        tvAccessDescription.setText(user.getRole().getDescription());

        // Account info
        tvMemberSince.setText(dateFormat.format(new Date(user.getCreatedAt())));

        if (user.getLastLoginAt() > 0) {
            tvLastLogin.setText(dateTimeFormat.format(new Date(user.getLastLoginAt())));
        } else {
            tvLastLogin.setText("Never");
        }

        tvAuthMethod.setText(user.getAuthMethod().getIcon() + " " +
                user.getAuthMethod().getDisplayName());
    }

    private String getAccessLevelDescription(int level) {
        if (level >= 80) return "Full Access";
        if (level >= 50) return "Limited Access";
        if (level >= 20) return "View Only";
        return "Restricted";
    }

    private void toggleUserStatus() {
        // Prevent deactivating yourself
        boolean isCurrentUser = user.getId() == sessionManager.getUserId();
        if (isCurrentUser) {
            Toast.makeText(this, "You cannot deactivate your own account", Toast.LENGTH_SHORT).show();
            return;
        }

        String action = user.isActive() ? "deactivate" : "activate";

        new MaterialAlertDialogBuilder(this)
                .setTitle(user.isActive() ? "Deactivate User" : "Activate User")
                .setMessage("Are you sure you want to " + action + " " + user.getName() + "?")
                .setPositiveButton(user.isActive() ? "Deactivate" : "Activate",
                        (dialog, which) -> {
                            user.setActive(!user.isActive());
                            executor.execute(() -> {
                                database.userDao().update(user);
                                runOnUiThread(() -> {
                                    displayUserData();
                                    Toast.makeText(this,
                                            "User " + action + "d successfully",
                                            Toast.LENGTH_SHORT).show();
                                });
                            });
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete() {
        // Prevent deleting yourself
        boolean isCurrentUser = user.getId() == sessionManager.getUserId();
        if (isCurrentUser) {
            Toast.makeText(this, "You cannot delete your own account", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() +
                        "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executor.execute(() -> {
                        database.userDao().delete(user);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResetPinDialog() {
        Toast.makeText(this, "Reset PIN coming soon", Toast.LENGTH_SHORT).show();
    }

    // Updates only local database
    private void updateLocalUserOnly() {
        executor.execute(() -> {
            database.userDao().update(user);
            runOnUiThread(() -> {
                Toast.makeText(this, "User updated successfully!", Toast.LENGTH_SHORT).show();
                displayUserData();
            });
        });
    }

    // Add this interface inside your UserDetailActivity class
    private interface OnReauthListener {
        void onSuccess();
        void onFailure(String error);
    }

    // Add this method inside your UserDetailActivity class
    private void reauthenticateUser(String email, String password, OnReauthListener listener) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            listener.onFailure("No user logged in");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure(e.getMessage()));
    }

    // Helper method for current user updates that require logout
    private void updateLocalUserAndLogout() {
        executor.execute(() -> {
            database.userDao().update(user);
            runOnUiThread(() -> {
                Toast.makeText(this,
                        "✓ Credentials updated! Please log in with your new credentials.",
                        Toast.LENGTH_LONG).show();

                // Force logout
                sessionManager.logout();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    private void showEditUserDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);

        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPhone);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        TextView tvHelper = dialogView.findViewById(R.id.tvHelperText);

        etName.setText(user.getName());
        etPhone.setText(user.getPhone());

        boolean isCurrentUser = user.getId() == sessionManager.getUserId();

        // Show appropriate helper text
        tvHelper.setVisibility(View.VISIBLE);

        if (isCurrentUser) {
            tvHelper.setText("You are updating YOUR OWN account. You can change your password here. Email cannot be changed.");
            // Disable email field for current user (keep using owner@afrinuts.com)
            etEmail.setEnabled(false);
            etEmail.setHint("Email (cannot be changed)");
        } else {
            tvHelper.setText("You are updating another user. Only name/phone can be changed. They must change their own password in Settings.");
            // Disable email/password for other users
            etEmail.setEnabled(false);
            etNewPassword.setEnabled(false);
            etConfirmPassword.setEnabled(false);
            etEmail.setHint("Email (user must change in Settings)");
        }

        // Always show email if available
        if (user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit User")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update local user object first
                    user.setName(newName);
                    user.setPhone(newPhone);

                    // IF THIS IS THE CURRENT USER, CHECK FOR PASSWORD CHANGE
                    if (isCurrentUser) {
                        // Check if password was provided
                        boolean passwordChanged = !TextUtils.isEmpty(newPassword);

                        if (passwordChanged) {
                            // Validate password
                            if (newPassword.length() < 6) {
                                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (!newPassword.equals(confirmPassword)) {
                                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Update Firebase password
                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

                            if (firebaseUser == null) {
                                Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Show progress
                            Toast.makeText(this, "Updating your password...", Toast.LENGTH_SHORT).show();

                            // Update password in Firebase
                            firebaseUser.updatePassword(newPassword)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update local password hash
                                        user.setPasswordHash(AuthUtils.hash(newPassword));
                                        // Save to local DB and logout
                                        updateLocalUserAndLogout();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this,
                                                "Password update failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            // No password change, just update local profile
                            updateLocalUserOnly();
                            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Updating another user - only update local DB
                        updateLocalUserOnly();
                        Toast.makeText(this, "User profile updated successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}