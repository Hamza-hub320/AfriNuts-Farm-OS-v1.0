package com.afrinuts.farmos.ui.settings;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.utils.LanguageHelper;
import com.github.mikephil.charting.BuildConfig;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.AuthCredential;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.AuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends BaseActivity {

    private CardView cardChangePassword;
    private CardView cardChangePin;
    private CardView cardLanguage;
    private CardView cardUnits;
    private CardView cardSync;
    private CardView cardAbout;
    private TextView tvVersion;
    private TextView tvLanguage;
    private TextView tvUnits;

    private AppDatabase database;
    private SyncManager syncManager;
    private FirebaseManager firebaseManager;

    @Override
    protected boolean checkRoleAccess() {
        // Only owners and managers can access settings
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        database = AppDatabase.getInstance(this);
        syncManager = SyncManager.getInstance(this);
        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupToolbar();
        setupClickListeners();
        loadSettings();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cardChangePassword = findViewById(R.id.cardChangePassword);
        cardChangePin = findViewById(R.id.cardChangePin);
        cardLanguage = findViewById(R.id.cardLanguage);
        cardUnits = findViewById(R.id.cardUnits);
        cardSync = findViewById(R.id.cardSync);
        cardAbout = findViewById(R.id.cardAbout);
        tvVersion = findViewById(R.id.tvVersion);
        tvLanguage = findViewById(R.id.tvLanguage);
        tvUnits = findViewById(R.id.tvUnits);

        // Hide PIN change for non-PIN users
        if (sessionManager.getUserRole() != UserEntity.UserRole.WORKER) {
            cardChangePin.setVisibility(View.GONE);
        }

        // Hide password change for PIN users
        if (sessionManager.getUserRole() == UserEntity.UserRole.WORKER) {
            cardChangePassword.setVisibility(View.GONE);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Settings");
        }
    }

    private void setupClickListeners() {
        cardChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        cardChangePin.setOnClickListener(v -> showChangePinDialog());
        cardLanguage.setOnClickListener(v -> showLanguageDialog());
        cardUnits.setOnClickListener(v -> showUnitsDialog());

        cardSync.setOnClickListener(v -> {
            // Animate the card when clicked
            animateCardClick(cardSync);

            // Show visual feedback
            Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();

            // Start sync
            syncManager.forceSyncNow();
        });

        cardAbout.setOnClickListener(v -> showAboutDialog());
    }

    // Add this method to animate the card click
    private void animateCardClick(CardView card) {
        card.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    card.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void animateSyncIcon(ImageView icon, boolean start) {
        if (start) {
            icon.animate().rotationBy(360).setDuration(1000).withEndAction(() -> {
                icon.setRotation(0);
                if (syncManager != null && syncManager.isSyncing()) {
                    animateSyncIcon(icon, true);
                }
            }).start();
        } else {
            icon.animate().cancel();
            icon.setRotation(0);
        }
    }

    private String getAppVersion() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private void loadSettings() {

        String version = getAppVersion();
        tvVersion.setText("Version " + version);

        String language = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("language", "English");
        String units = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("units", "Metric (kg, XAF)");

        tvLanguage.setText(language);
        tvUnits.setText(units);
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);

        TextInputEditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);

        // Get strength indicator views
        ProgressBar passwordStrength = dialogView.findViewById(R.id.passwordStrength);
        TextView tvStrengthLabel = dialogView.findViewById(R.id.tvStrengthLabel);
        View layoutStrength = dialogView.findViewById(R.id.layoutStrength);

        // Add listener to check password strength as user types
        etNew.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                if (password.isEmpty()) {
                    layoutStrength.setVisibility(View.GONE);
                    return;
                }

                layoutStrength.setVisibility(View.VISIBLE);
                int strength = calculatePasswordStrength(password);
                passwordStrength.setProgress(strength);

                String strengthLabel = getStrengthLabel(strength);
                tvStrengthLabel.setText("Strength: " + strengthLabel);

                int color = getStrengthColor(strength);
                tvStrengthLabel.setTextColor(getColor(color));
                passwordStrength.setProgressTintList(android.content.res.ColorStateList.valueOf(getColor(color)));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });


        new MaterialAlertDialogBuilder(this)
                .setTitle("")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String current = etCurrent.getText().toString();
                    String newPass = etNew.getText().toString();
                    String confirm = etConfirm.getText().toString();

                    // Validate inputs
                    if (TextUtils.isEmpty(current)) {
                        Toast.makeText(this, "Current password required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (TextUtils.isEmpty(newPass)) {
                        Toast.makeText(this, "New password required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPass.equals(confirm)) {
                        Toast.makeText(this, "New passwords don't match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Show progress
                    Toast.makeText(this, "Verifying current password...", Toast.LENGTH_SHORT).show();

                    // Get current Firebase user
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser == null) {
                        Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Get user email
                    String email = firebaseUser.getEmail();
                    if (email == null) {
                        Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Re-authenticate with Firebase first
                    AuthCredential credential = EmailAuthProvider.getCredential(email, current);
                    firebaseUser.reauthenticate(credential)
                            .addOnSuccessListener(aVoid -> {
                                // Current password is correct with Firebase
                                Toast.makeText(this, "Password verified, updating...", Toast.LENGTH_SHORT).show();

                                // Update Firebase password
                                firebaseUser.updatePassword(newPass)
                                        .addOnSuccessListener(aVoid2 -> {
                                            // Update local database
                                            new Thread(() -> {
                                                UserEntity user = database.userDao().getUserById(sessionManager.getUserId());
                                                String newHash = AuthUtils.hash(newPass);
                                                user.setPasswordHash(newHash);
                                                database.userDao().update(user);

                                                runOnUiThread(() -> {
                                                    Toast.makeText(this,
                                                            "Password updated successfully!",
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                            }).start();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this,
                                                    "Failed to update password: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this,
                                        "Current password is incorrect",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePinDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_pin, null);

        TextInputEditText etCurrent = dialogView.findViewById(R.id.etCurrentPin);
        TextInputEditText etNew = dialogView.findViewById(R.id.etNewPin);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmPin);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change PIN")
                .setView(dialogView)
                .setPositiveButton("Change", (dialog, which) -> {
                    String current = etCurrent.getText().toString();
                    String newPin = etNew.getText().toString();
                    String confirm = etConfirm.getText().toString();

                    // Validate inputs
                    if (TextUtils.isEmpty(current)) {
                        Toast.makeText(this, "Current PIN required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!AuthUtils.isValidPin(newPin)) {
                        Toast.makeText(this, "PIN must be 4-6 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPin.equals(confirm)) {
                        Toast.makeText(this, "New PINs don't match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Verify current PIN
                    new Thread(() -> {
                        UserEntity user = database.userDao().getUserById(sessionManager.getUserId());

                        if (!AuthUtils.verify(current, user.getPinHash())) {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Current PIN incorrect", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        // Update local DB
                        String newHash = AuthUtils.hash(newPin);
                        user.setPinHash(newHash);
                        database.userDao().update(user);

                        runOnUiThread(() ->
                                Toast.makeText(this, "PIN updated successfully", Toast.LENGTH_SHORT).show());
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Français"};
        String[] codes = {"en", "fr"};
        int currentIndex = tvLanguage.getText().toString().equals("English") ? 0 : 1;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, currentIndex, (dialog, which) -> {
                    String selected = languages[which];
                    String code = codes[which];

                    tvLanguage.setText(selected);

                    // Save and apply language
                    LanguageHelper.setLocale(this, code);

                    // Show restart message
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Language Changed")
                            .setMessage("Language changed to " + selected + ". Please restart the app for changes to take effect.")
                            .setPositiveButton("Restart Now", (d, w) -> {
                                // Restart app
                                android.os.Process.killProcess(android.os.Process.myPid());
                                System.exit(0);
                            })
                            .setNegativeButton("Later", null)
                            .show();

                    dialog.dismiss();
                })
                .show();
    }

    private void showUnitsDialog() {
        String[] units = {"Metric (kg, ha, XAF)", "Imperial (lb, acres, USD)"};
        String[] unitCodes = {"metric", "imperial"};
        int currentIndex = tvUnits.getText().toString().contains("Metric") ? 0 : 1;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Units")
                .setSingleChoiceItems(units, currentIndex, (dialog, which) -> {
                    String selected = units[which];
                    String code = unitCodes[which];

                    tvUnits.setText(selected);

                    // Save preference
                    getSharedPreferences("settings", MODE_PRIVATE)
                            .edit()
                            .putString("units", code)
                            .apply();

                    Toast.makeText(this, "Units changed to " + selected, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= 8) strength += 25;
        else if (password.length() >= 6) strength += 15;

        if (password.matches(".*[A-Z].*")) strength += 25;
        if (password.matches(".*[a-z].*")) strength += 25;
        if (password.matches(".*\\d.*")) strength += 15;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) strength += 10;

        return Math.min(strength, 100);
    }


    private String getStrengthLabel(int strength) {
        if (strength < 30) return "Weak";
        if (strength < 60) return "Fair";
        if (strength < 80) return "Good";
        return "Strong";
    }

    private int getStrengthColor(int strength) {
        if (strength < 30) return R.color.dark_orange;
        if (strength < 60) return R.color.accent;
        if (strength < 80) return R.color.primary;
        return R.color.secondary;
    }

    private void showAboutDialog() {
        String version = getAppVersion();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about, null);

        TextView tvVersionValue = dialogView.findViewById(R.id.tvVersionValue);
        TextView tvBuildValue = dialogView.findViewById(R.id.tvBuildValue);

        tvVersionValue.setText(version);
        tvBuildValue.setText(String.valueOf(BuildConfig.VERSION_CODE));

        new MaterialAlertDialogBuilder(this)
                .setTitle("")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}