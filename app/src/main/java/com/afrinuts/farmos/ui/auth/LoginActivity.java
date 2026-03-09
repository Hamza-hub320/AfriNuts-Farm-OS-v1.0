package com.afrinuts.farmos.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
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
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.utils.AuthUtils;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private AppDatabase database;

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
        initViews();
        loadUsers();
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

    private void loadUsers() {
        new Thread(() -> {
            users = database.userDao().getAllUsers();

            runOnUiThread(() -> {
                if (users.isEmpty()) {
                    // No users yet - create default owner
                    createDefaultOwner();
                } else {
                    displayUserChips();
                }
            });
        }).start();
    }

    private void createDefaultOwner() {
        // Create a default owner account for first launch
        String defaultPassword = AuthUtils.hash("admin123");
        UserEntity owner = new UserEntity(
                "Farm Owner",
                UserEntity.UserRole.OWNER,
                "+225 00 00 00 00",
                "owner@afrinuts.com",
                defaultPassword
        );

        new Thread(() -> {
            database.userDao().insert(owner);
            runOnUiThread(() -> {
                Toast.makeText(this,
                        "Default owner created. Email: owner@afrinuts.com, Password: admin123",
                        Toast.LENGTH_LONG).show();
                loadUsers(); // Reload users
            });
        }).start();
    }

    private void displayUserChips() {
        if (userChipsContainer == null) return;

        userChipsContainer.removeAllViews();

        for (UserEntity user : users) {
            if (!user.isActive()) continue; // Skip inactive users

            // Create chip with vector icon
            Chip chip = new Chip(this);
            chip.setText(user.getName());

            // Set icon based on role
            int iconResId = getRoleIcon(user.getRole());
            chip.setChipIconResource(iconResId);
            chip.setChipIconTint(ContextCompat.getColorStateList(this, R.color.primary));
            chip.setChipIconVisible(true);

            // Style the chip
            chip.setChipBackgroundColorResource(R.color.warm_grey);
            chip.setTextColor(getColor(R.color.text_dark));
            chip.setChipStrokeColorResource(R.color.primary);
            chip.setChipStrokeWidth(1.0f);
            chip.setClickable(true);
            chip.setCheckable(true);
            chip.setChecked(false);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Uncheck all others
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
            case OWNER:
                return R.drawable.ic_star;
            case MANAGER:
                return R.drawable.ic_assignment;
            case WORKER:
                return R.drawable.ic_person;
            case INVESTOR:
                return R.drawable.ic_trending_up;
            default:
                return R.drawable.ic_person;
        }
    }

    private void updateLoginFieldsForUser(UserEntity user) {
        if (user == null) return;

        if (user.getAuthMethod() == UserEntity.AuthMethod.PIN) {
            // PIN mode
            layoutEmailPin.setHint("Enter PIN");
            etEmailPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            layoutPassword.setVisibility(View.GONE);
            isPinMode = true;

            // Clear any previous values
            etEmailPin.setText("");
        } else {
            // Email/Password mode
            layoutEmailPin.setHint("Email");
            etEmailPin.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            layoutPassword.setVisibility(View.VISIBLE);
            isPinMode = false;

            // Pre-fill email if available
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

        new Thread(() -> {
            boolean authenticated = false;

            if (isPinMode) {
                // Verify PIN
                String hashedPin = AuthUtils.hash(emailPin);
                authenticated = hashedPin != null && hashedPin.equals(selectedUser.getPinHash());
            } else {
                // Verify email and password
                if (emailPin.equals(selectedUser.getEmail())) {
                    String hashedPassword = AuthUtils.hash(password);
                    authenticated = hashedPassword != null &&
                            hashedPassword.equals(selectedUser.getPasswordHash());
                }
            }

            boolean finalAuthenticated = authenticated;
            runOnUiThread(() -> {
                if (finalAuthenticated) {
                    // Update last login
                    selectedUser.setLastLoginAt(System.currentTimeMillis());

                    new Thread(() -> {
                        database.userDao().update(selectedUser);

                        // Store logged in user
                        SessionManager sessionManager = new SessionManager(LoginActivity.this);
                        sessionManager.createLoginSession(selectedUser);

                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this,
                                    "Welcome " + selectedUser.getName(), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    }).start();
                } else {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    Toast.makeText(LoginActivity.this,
                            isPinMode ? "Invalid PIN" : "Invalid email or password",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}