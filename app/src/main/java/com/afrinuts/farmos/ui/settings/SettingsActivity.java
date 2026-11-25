package com.afrinuts.farmos.ui.settings;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.auth.LoginActivity;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.afrinuts.farmos.utils.AuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends BaseActivity {

    private View cardChangePassword;
    private View cardSync;
    private View cardAbout;
    private TextView tvVersion;
    private AppDatabase database;
    private SyncManager syncManager;

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        database = AppDatabase.getInstance(this);
        syncManager = SyncManager.getInstance(this);
        cardChangePassword = findViewById(R.id.cardChangePassword);
        cardSync = findViewById(R.id.cardSync);
        cardAbout = findViewById(R.id.cardAbout);
        tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Version " + getAppVersion());

        // About is a read-only row; the version is displayed inline.
        cardAbout.setClickable(false);
        cardAbout.setFocusable(false);
        cardAbout.setForeground(null);
        if (cardAbout instanceof ViewGroup) {
            ViewGroup aboutRow = (ViewGroup) cardAbout;
            if (aboutRow.getChildCount() > 0) {
                aboutRow.getChildAt(aboutRow.getChildCount() - 1).setVisibility(View.GONE);
            }
        }

        findViewById(R.id.toolbarBack).setOnClickListener(v -> finish());
        cardChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        cardSync.setOnClickListener(v -> {
            cardSync.animate().cancel();
            cardSync.animate().scaleX(.97f).scaleY(.97f).setDuration(90)
                    .withEndAction(() -> cardSync.animate().scaleX(1f).scaleY(1f).setDuration(90).start()).start();
            Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();
            syncManager.forceSyncNow();
        });
    }

    private String getAppVersion() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            return version == null ? "1.0" : version;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return "1.0";
        }
    }

    private void showChangePasswordDialog() {
        new ChangePasswordDialog().show(getSupportFragmentManager(), "change_password");
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
        if (strength < 60) return R.color.accent_ink;
        if (strength < 80) return R.color.primary;
        return R.color.secondary;
    }

    private LinearLayout createStrengthSegments(View oldBar) {
        float density = getResources().getDisplayMetrics().density;
        ViewGroup parent = (ViewGroup) oldBar.getParent();
        int index = parent.indexOfChild(oldBar);
        parent.removeView(oldBar);

        LinearLayout segments = new LinearLayout(this);
        segments.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(8 * density));
        parent.addView(segments, index, rowParams);

        for (int i = 0; i < 4; i++) {
            View segment = new View(this);
            segment.setBackground(strengthSegmentBackground(0xFFF3EAD8, density));
            LinearLayout.LayoutParams segmentParams = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) segmentParams.leftMargin = Math.round(5 * density);
            segments.addView(segment, segmentParams);
        }
        return segments;
    }

    private GradientDrawable strengthSegmentBackground(int color, float density) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(999 * density);
        return background;
    }

    private void updateStrengthSegments(LinearLayout segments, int score) {
        float density = getResources().getDisplayMetrics().density;
        int activeCount = score <= 0 ? 0 : Math.min(4, (score + 24) / 25);
        int activeColor = score < 30 ? 0xFFB3402A : score < 60 ? 0xFFE8912D : 0xFF0E6B3C;
        for (int i = 0; i < segments.getChildCount(); i++) {
            int color = i < activeCount ? activeColor : 0xFFF3EAD8;
            if (score >= 60 && score < 80 && i == activeCount - 1) color = 0xFFC99B20;
            segments.getChildAt(i).setBackground(strengthSegmentBackground(color, density));
        }
    }

    private void submitPasswordChange(String current, String newPass, String confirm, DialogFragment dialog) {
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

        dialog.dismiss();
        Toast.makeText(this, "Verifying current password...", Toast.LENGTH_SHORT).show();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = firebaseUser.getEmail();
        if (email == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, current);
        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Password verified, updating...", Toast.LENGTH_SHORT).show();
                    firebaseUser.updatePassword(newPass)
                            .addOnSuccessListener(aVoid2 -> new Thread(() -> {
                                UserEntity user = database.userDao().getUserById(sessionManager.getUserId());
                                if (user != null) {
                                    user.setPasswordHash(AuthUtils.hash(newPass));
                                    database.userDao().update(user);
                                }
                                runOnUiThread(() -> {
                                    Toast.makeText(SettingsActivity.this,
                                            "Password updated. Please sign in again.", Toast.LENGTH_LONG).show();
                                    FirebaseAuth.getInstance().signOut();
                                    sessionManager.logout();
                                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            }).start())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Failed to update password: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Current password is incorrect", Toast.LENGTH_SHORT).show());
    }

    public static class ChangePasswordDialog extends FarmDialogFragment {
        @NonNull
        @Override
        public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null, false);
            TextInputEditText current = view.findViewById(R.id.etCurrentPassword);
            TextInputEditText newPassword = view.findViewById(R.id.etNewPassword);
            TextInputEditText confirm = view.findViewById(R.id.etConfirmPassword);
            LinearLayout strengthSegments = ((SettingsActivity) requireActivity())
                    .createStrengthSegments(view.findViewById(R.id.passwordStrength));
            TextView strengthLabel = view.findViewById(R.id.tvStrengthLabel);
            View strengthLayout = view.findViewById(R.id.layoutStrength);

            newPassword.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String password = s.toString();
                    if (password.isEmpty()) {
                        strengthLayout.setVisibility(View.GONE);
                        return;
                    }
                    strengthLayout.setVisibility(View.VISIBLE);
                    SettingsActivity activity = (SettingsActivity) requireActivity();
                    int score = activity.calculatePasswordStrength(password);
                    int color = activity.getColor(activity.getStrengthColor(score));
                    activity.updateStrengthSegments(strengthSegments, score);
                    strengthLabel.setText(activity.getStrengthLabel(score));
                    strengthLabel.setTextColor(color);
                }
                @Override public void afterTextChanged(android.text.Editable s) { }
            });

            Button cancel = view.findViewById(R.id.btnCancelPassword);
            Button change = view.findViewById(R.id.btnChangePassword);
            cancel.setOnClickListener(v -> dismiss());
            change.setOnClickListener(v -> ((SettingsActivity) requireActivity()).submitPasswordChange(
                    value(current), value(newPassword), value(confirm), this));

            View content = view instanceof android.widget.ScrollView
                    ? ((android.widget.ScrollView) view).getChildAt(0) : view;
            if (content.getParent() instanceof ViewGroup) {
                ((ViewGroup) content.getParent()).removeView(content);
            }
            return new MaterialAlertDialogBuilder(requireContext()).setView(content).create();
        }

        private String value(TextInputEditText editText) {
            return editText.getText() == null ? "" : editText.getText().toString();
        }

        @Override
        public void onStart() {
            super.onStart();
            Button change = getDialog() == null ? null : getDialog().findViewById(R.id.btnChangePassword);
            if (change != null) {
                float density = getResources().getDisplayMetrics().density;
                GradientDrawable background = new GradientDrawable();
                background.setColor(0xFF0E6B3C);
                background.setCornerRadius(999 * density);
                change.setBackground(background);
                change.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF0E6B3C));
                change.setTextColor(0xFFFFFFFF);
                change.setMinHeight(Math.round(60 * density));
                change.setAllCaps(false);
                change.setElevation(0);
                change.setTranslationZ(0);
                change.setStateListAnimator(null);
            }
        }
    }
}
