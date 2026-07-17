package com.afrinuts.farmos.ui.users;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.afrinuts.farmos.utils.AuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditUserDialog extends DialogFragment {

    private static final String TAG = "EditUserDialog";
    private static final String ARG_USER_ID = "user_id";

    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private MaterialAutoCompleteTextView etRole;

    // Profile Fields
    private TextInputEditText etBio;
    private TextInputEditText etCompany;
    private TextInputEditText etTitle;

    // Password fields
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;

    private long userId;
    private UserEntity existingUser;
    private UserEntity.UserRole selectedRole;

    // Custom buttons
    private Button btnCancel;
    private Button btnSave;

    public interface OnUserUpdatedListener {
        void onUserUpdated();
    }

    private OnUserUpdatedListener listener;

    public static EditUserDialog newInstance(long userId) {
        EditUserDialog dialog = new EditUserDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnUserUpdatedListener(OnUserUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
        firebaseManager = FirebaseManager.getInstance();

        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID);
        }

        // Load user data
        executor.execute(() -> {
            existingUser = database.userDao().getUserById(userId);
            if (existingUser != null) {
                selectedRole = existingUser.getRole();
                requireActivity().runOnUiThread(() -> {
                    populateFields();
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_user, null);

        initViews(view);
        setupRoleDropdown();

        // Find custom buttons from layout
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("")
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            btnSave.setOnClickListener(v -> saveUser());
            btnCancel.setOnClickListener(v -> dismiss());
        });

        return dialog;
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.etName);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etRole = view.findViewById(R.id.etRole);

        // Profile Fields
        etBio = view.findViewById(R.id.etBio);
        etCompany = view.findViewById(R.id.etCompany);
        etTitle = view.findViewById(R.id.etTitle);

        etNewPassword = view.findViewById(R.id.etNewPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
    }

    private void setupRoleDropdown() {
        List<RoleItem> roleItems = new ArrayList<>();
        roleItems.add(new RoleItem(
                UserEntity.UserRole.OWNER,
                "Owner",
                R.drawable.ic_star
        ));
        roleItems.add(new RoleItem(
                UserEntity.UserRole.MANAGER,
                "Manager",
                R.drawable.ic_assignment
        ));
        roleItems.add(new RoleItem(
                UserEntity.UserRole.WORKER,
                "Worker",
                R.drawable.ic_person
        ));
        roleItems.add(new RoleItem(
                UserEntity.UserRole.INVESTOR,
                "Investor",
                R.drawable.ic_trending_up
        ));

        RoleAdapter adapter = new RoleAdapter(requireContext(), roleItems);
        etRole.setAdapter(adapter);

        // Set current role
        if (existingUser != null) {
            String currentRoleDisplay = existingUser.getRole().getDisplayName();
            for (RoleItem item : roleItems) {
                if (item.getDisplayName().equals(currentRoleDisplay)) {
                    etRole.setText(item.getDisplayName(), false);
                    selectedRole = item.getRole();
                    break;
                }
            }
        }

        etRole.setOnItemClickListener((parent, view, position, id) -> {
            RoleItem selectedItem = (RoleItem) parent.getItemAtPosition(position);
            selectedRole = selectedItem.getRole();
            etRole.setText(selectedItem.getDisplayName(), false);
        });
    }

    private void populateFields() {
        if (existingUser == null) return;

        etName.setText(existingUser.getName());
        etPhone.setText(existingUser.getPhone());
        etEmail.setText(existingUser.getEmail());

        // Populate profile fields
        etBio.setText(existingUser.getBio());
        etCompany.setText(existingUser.getCompany());
        etTitle.setText(existingUser.getTitle());
    }

    private void saveUser() {
        if (existingUser == null) {
            Toast.makeText(getContext(), "User data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate name
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        // Validate phone
        String phone = etPhone.getText().toString().trim();
        if (!AuthUtils.isValidPhone(phone)) {
            etPhone.setError("Invalid phone number");
            return;
        }

        // Validate email
        String email = etEmail.getText().toString().trim();
        if (!AuthUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email address");
            return;
        }

        // Get profile fields
        String bio = etBio.getText().toString().trim();
        String company = etCompany.getText().toString().trim();
        String title = etTitle.getText().toString().trim();

        // Handle password change
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!TextUtils.isEmpty(newPassword) || !TextUtils.isEmpty(confirmPassword)) {
            if (newPassword.length() < 6) {
                etNewPassword.setError("Password must be at least 6 characters");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Update user object
        existingUser.setName(name);
        existingUser.setPhone(phone);
        existingUser.setEmail(email);
        existingUser.setRole(selectedRole);

        // Set profile fields
        existingUser.setBio(bio.isEmpty() ? null : bio);
        existingUser.setCompany(company.isEmpty() ? null : company);
        existingUser.setTitle(title.isEmpty() ? null : title);

        existingUser.setUpdatedAt(System.currentTimeMillis());
        existingUser.setLastUpdatedAt(System.currentTimeMillis());

        // If password is changing, update it
        if (!TextUtils.isEmpty(newPassword)) {
            existingUser.setPasswordHash(AuthUtils.hash(newPassword));
            existingUser.setPasswordChangedAt(System.currentTimeMillis());
        }

        // Update in local database AND Firestore
        executor.execute(() -> {
            try {
                // Update local database
                database.userDao().update(existingUser);
                Log.d(TAG, "✅ User updated in local database");

                // Update Firestore
                updateUserInFirestore(existingUser);

                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");

                    Toast.makeText(getContext(), "User updated successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onUserUpdated();
                    }
                    dismiss();
                });
            } catch (Exception e) {
                Log.e(TAG, "❌ Error saving user", e);
                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(getContext(), "Error saving user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateUserInFirestore(UserEntity user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userMap = new HashMap<>();

        // Core fields
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole().name());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());

        // Profile fields
        userMap.put("bio", user.getBio());
        userMap.put("company", user.getCompany());
        userMap.put("title", user.getTitle());

        // Account status fields
        userMap.put("lastUpdatedAt", user.getLastUpdatedAt());
        userMap.put("updatedAt", System.currentTimeMillis());

        // If password changed, update the hash and timestamp
        if (user.getPasswordHash() != null) {
            userMap.put("passwordChangedAt", user.getPasswordChangedAt());
        }

        Log.d(TAG, "📤 Updating Firestore with fields: " + userMap.keySet());

        // Use set() with merge to add new fields without overwriting existing ones
        db.collection("users")
                .document(user.getUid())
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User updated in Firestore successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error updating user in Firestore", e);
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}