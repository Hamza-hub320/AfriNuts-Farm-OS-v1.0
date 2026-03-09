package com.afrinuts.farmos.ui.users;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.AuthUtils;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDetailActivity extends BaseActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private long userId;
    private UserEntity user;
    private AppDatabase database;

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
        super.onCreate(savedInstanceState); // This will trigger role check
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
        new Thread(() -> {
            user = database.userDao().getUserById(userId);

            runOnUiThread(() -> {
                if (user != null) {
                    displayUserData();
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }).start();
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
        String action = user.isActive() ? "deactivate" : "activate";

        new MaterialAlertDialogBuilder(this)
                .setTitle(user.isActive() ? "Deactivate User" : "Activate User")
                .setMessage("Are you sure you want to " + action + " " + user.getName() + "?")
                .setPositiveButton(user.isActive() ? "Deactivate" : "Activate",
                        (dialog, which) -> {
                            user.setActive(!user.isActive());
                            new Thread(() -> {
                                database.userDao().update(user);
                                runOnUiThread(() -> {
                                    displayUserData();
                                    Toast.makeText(this,
                                            "User " + action + "d successfully",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }).start();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getName() +
                        "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        database.userDao().delete(user);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResetPinDialog() {
        // In a real app, this would open a dialog to set a new PIN
        Toast.makeText(this, "Reset PIN coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showEditUserDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);

        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPhone);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmail);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        etName.setText(user.getName());
        etPhone.setText(user.getPhone());
        if (user.getEmail() != null) {
            etEmail.setText(user.getEmail());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit User")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    user.setName(newName);
                    user.setPhone(newPhone);

                    if (user.getAuthMethod() == UserEntity.AuthMethod.PASSWORD) {
                        user.setEmail(newEmail);

                        if (!TextUtils.isEmpty(newPassword)) {
                            if (newPassword.length() < 6) {
                                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (!newPassword.equals(confirmPassword)) {
                                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            user.setPasswordHash(AuthUtils.hash(newPassword));
                        }
                    }

                    new Thread(() -> {
                        database.userDao().update(user);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                            displayUserData();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}