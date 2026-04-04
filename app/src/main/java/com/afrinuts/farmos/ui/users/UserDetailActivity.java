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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.UserRepository;
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
    private UserRepository userRepository;
    private long farmId = 1L;
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
        userRepository = new UserRepository(this, farmId);

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
        userRepository.getUserById(userId).observe(this, loadedUser -> {
            user = loadedUser;
            if (user != null) {
                displayUserData();
            } else {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                finish();
            }
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
        boolean isCurrentUser = user.getLocalId() == sessionManager.getUserId();

        // Owners cannot deactivate themselves
        if (user.getRole() == UserEntity.UserRole.OWNER && isCurrentUser) {
            btnDeactivate.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
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

        tvAuthMethod.setText(user.getAuthMethod().getDisplayName());
    }

    private String getAccessLevelDescription(int level) {
        if (level >= 80) return "Full Access";
        if (level >= 50) return "Limited Access";
        if (level >= 20) return "View Only";
        return "Restricted";
    }

    private void toggleUserStatus() {
        // Prevent deactivating yourself
        boolean isCurrentUser = user.getLocalId() == sessionManager.getUserId();
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
                            userRepository.updateUser(user, new BaseRepository.RepositoryCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    runOnUiThread(() -> {
                                        displayUserData();
                                        Toast.makeText(UserDetailActivity.this,
                                                "User " + action + "d successfully",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() ->
                                            Toast.makeText(UserDetailActivity.this,
                                                    "Error: " + error, Toast.LENGTH_SHORT).show()
                                    );
                                }
                            });
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete() {
        boolean isCurrentUser = user.getLocalId() == sessionManager.getUserId();
        if (isCurrentUser) {
            Toast.makeText(this, "You cannot delete your own account", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    userRepository.deleteUser(user, new BaseRepository.RepositoryCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            runOnUiThread(() -> {
                                Toast.makeText(UserDetailActivity.this,
                                        "User deleted successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                    Toast.makeText(UserDetailActivity.this,
                                            "Error deleting user: " + error, Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResetPinDialog() {
        Toast.makeText(this, "Reset PIN coming soon", Toast.LENGTH_SHORT).show();
    }

    private void updateLocalUserOnly() {
        userRepository.updateUser(user, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(UserDetailActivity.this,
                            "User updated successfully!", Toast.LENGTH_SHORT).show();
                    displayUserData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(UserDetailActivity.this,
                                "Error: " + error, Toast.LENGTH_SHORT).show()
                );
            }
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

        // Find custom buttons
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        etName.setText(user.getName());
        etPhone.setText(user.getPhone());

        boolean isCurrentUser = user.getLocalId() == sessionManager.getUserId();

        tvHelper.setVisibility(View.VISIBLE);

        if (isCurrentUser) {
            tvHelper.setText("You are updating YOUR OWN account.");
            etEmail.setEnabled(false);
            etEmail.setHint("Email (cannot be changed)");

            // For current user, password change is allowed
            etNewPassword.setEnabled(true);
            etConfirmPassword.setEnabled(true);
        } else {
            tvHelper.setText("You are updating another user.");
            etEmail.setEnabled(false);
            etNewPassword.setEnabled(false);
            etConfirmPassword.setEnabled(false);
            etEmail.setHint("Email (cannot be changed)");
        }

        if (user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        }

        // Create custom dialog without default buttons
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("")
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set custom button click listeners
        dialog.setOnShowListener(dialogInterface -> {
            btnSave.setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                String newPhone = etPhone.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(newName)) {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                    return;
                }

                user.setName(newName);
                user.setPhone(newPhone);

                // Handle password change for current user
                if (isCurrentUser && !TextUtils.isEmpty(newPassword)) {
                    if (newPassword.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Show loading state
                    btnSave.setEnabled(false);
                    btnSave.setText("Saving...");

                    // Update password in Firebase
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser()
                            .updatePassword(newPassword)
                            .addOnSuccessListener(aVoid -> {
                                // Update local password hash
                                user.setPasswordHash(AuthUtils.hash(newPassword));
                                userRepository.updateUser(user, new BaseRepository.RepositoryCallback<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean result) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(UserDetailActivity.this,
                                                    "Password updated! Please log in with your new password.",
                                                    Toast.LENGTH_LONG).show();

                                            // Log out and redirect to login
                                            sessionManager.logout();
                                            Intent intent = new Intent(UserDetailActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        runOnUiThread(() -> {
                                            btnSave.setEnabled(true);
                                            btnSave.setText("Save Changes");
                                            Toast.makeText(UserDetailActivity.this,
                                                    "Error: " + error, Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                runOnUiThread(() -> {
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Save Changes");
                                    Toast.makeText(UserDetailActivity.this,
                                            "Failed to update password: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                            });
                } else {
                    // Update profile only (no password change)
                    updateLocalUserOnly();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
        });

        dialog.show();
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