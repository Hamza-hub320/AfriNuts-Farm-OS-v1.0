package com.afrinuts.farmos.ui.settings;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

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
            syncManager.forceSyncNow();
            Toast.makeText(this, "Sync started", Toast.LENGTH_SHORT).show();
        });
        cardAbout.setOnClickListener(v -> showAboutDialog());
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
        // Get version from PackageManager instead of BuildConfig
        String version = getAppVersion();
        tvVersion.setText("Version " + version);

        // Load preferences from SharedPreferences
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

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change Password")
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

                    // Verify current password
                    new Thread(() -> {
                        UserEntity user = database.userDao().getUserById(sessionManager.getUserId());

                        if (!AuthUtils.verify(current, user.getPasswordHash())) {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Current password incorrect", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        // Update local DB
                        String newHash = AuthUtils.hash(newPass);
                        user.setPasswordHash(newHash);
                        database.userDao().update(user);

                        // Update Firebase
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.updatePassword(newPass)
                                    .addOnSuccessListener(aVoid -> {
                                        runOnUiThread(() ->
                                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> {
                                        runOnUiThread(() ->
                                                Toast.makeText(this, "Firebase update failed: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show());
                                    });
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Password updated locally", Toast.LENGTH_SHORT).show());
                        }
                    }).start();
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
        int currentIndex = tvLanguage.getText().toString().equals("English") ? 0 : 1;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, currentIndex, (dialog, which) -> {
                    String selected = languages[which];
                    tvLanguage.setText(selected);

                    // Save to SharedPreferences
                    getSharedPreferences("settings", MODE_PRIVATE)
                            .edit()
                            .putString("language", selected)
                            .apply();

                    Toast.makeText(this, "Language changed to " + selected, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    private void showUnitsDialog() {
        String[] units = {"Metric (kg, XAF)", "Imperial (lb, USD)"};
        int currentIndex = tvUnits.getText().toString().contains("Metric") ? 0 : 1;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Units")
                .setSingleChoiceItems(units, currentIndex, (dialog, which) -> {
                    String selected = units[which];
                    tvUnits.setText(selected);

                    // Save to SharedPreferences
                    getSharedPreferences("settings", MODE_PRIVATE)
                            .edit()
                            .putString("units", selected)
                            .apply();

                    Toast.makeText(this, "Units changed to " + selected, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .show();
    }

    private void showAboutDialog() {
        String version = getAppVersion();
        String aboutMessage =
                "AfriNuts Farm OS v" + version + "\n\n" +
                        "© 2026 AfriNuts Export\n" +
                        "Odienné, Côte d'Ivoire\n\n" +
                        "A complete farm management system for cashew plantations.\n\n" +
                        "Features:\n" +
                        "• 35-hectare block management\n" +
                        "• Expense & revenue tracking\n" +
                        "• Task assignment\n" +
                        "• Photo documentation\n" +
                        "• PDF reports\n" +
                        "• Multi-user support\n" +
                        "• Cloud sync\n\n" +
                        "For support: support@afrinuts.com";

        new MaterialAlertDialogBuilder(this)
                .setTitle("About AfriNuts Farm OS")
                .setMessage(aboutMessage)
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