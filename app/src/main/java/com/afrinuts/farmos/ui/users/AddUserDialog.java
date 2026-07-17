package com.afrinuts.farmos.ui.users;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddUserDialog extends DialogFragment {

    private static final String TAG = "AddUserDialog";
    private AppDatabase database;
    private FirebaseManager firebaseManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private MaterialAutoCompleteTextView etRole;
    private MaterialButtonToggleGroup toggleAuthMethod;
    private LinearLayout layoutPassword;
    private TextInputEditText etPassword;
    private LinearLayout layoutPin;
    private TextInputEditText etPin;
    private TextInputEditText etConfirmPin;

    // ✅ New Profile Fields
    private TextInputEditText etBio;
    private TextInputEditText etCompany;
    private TextInputEditText etTitle;
    private TextInputEditText etDeactivationReason;

    private long farmId;

    private UserEntity.UserRole selectedRole = UserEntity.UserRole.WORKER;
    private UserEntity.AuthMethod selectedAuthMethod = UserEntity.AuthMethod.PIN;

    // Custom buttons
    private Button btnCancel;
    private Button btnSave;

    public interface OnUserAddedListener {
        void onUserAdded();
    }

    private OnUserAddedListener listener;

    public static AddUserDialog newInstance() {
        return new AddUserDialog();
    }

    public static AddUserDialog newInstance(long farmId) {
        AddUserDialog dialog = new AddUserDialog();
        Bundle args = new Bundle();
        args.putLong("farm_id", farmId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnUserAddedListener(OnUserAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
        firebaseManager = FirebaseManager.getInstance();

        if (getArguments() != null) {
            farmId = getArguments().getLong("farm_id", 1);
        } else {
            farmId = 1;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_user, null);

        initViews(view);
        setupRoleDropdown();
        setupAuthMethodToggle();

        // Find custom buttons from layout
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        // Create dialog without default buttons
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("")
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        // Set custom button click listeners after dialog is created
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
        toggleAuthMethod = view.findViewById(R.id.toggleAuthMethod);
        layoutPassword = view.findViewById(R.id.layoutPassword);
        etPassword = view.findViewById(R.id.etPassword);
        layoutPin = view.findViewById(R.id.layoutPin);
        etPin = view.findViewById(R.id.etPin);
        etConfirmPin = view.findViewById(R.id.etConfirmPin);

        // ✅ New Profile Fields
        etBio = view.findViewById(R.id.etBio);
        etCompany = view.findViewById(R.id.etCompany);
        etTitle = view.findViewById(R.id.etTitle);
        etDeactivationReason = view.findViewById(R.id.etDeactivationReason);

        // Default to PIN for workers
        toggleAuthMethod.check(R.id.btnPin);
        layoutPin.setVisibility(View.VISIBLE);
        layoutPassword.setVisibility(View.GONE);
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

        etRole.setText(roleItems.get(2).getDisplayName(), false);

        etRole.setOnItemClickListener((parent, view, position, id) -> {
            RoleItem selectedItem = (RoleItem) parent.getItemAtPosition(position);
            selectedRole = selectedItem.getRole();
            etRole.setText(selectedItem.getDisplayName(), false);

            if (selectedRole == UserEntity.UserRole.WORKER) {
                toggleAuthMethod.check(R.id.btnPin);
                layoutPin.setVisibility(View.VISIBLE);
                layoutPassword.setVisibility(View.GONE);
                selectedAuthMethod = UserEntity.AuthMethod.PIN;
            } else {
                toggleAuthMethod.check(R.id.btnPassword);
                layoutPassword.setVisibility(View.VISIBLE);
                layoutPin.setVisibility(View.GONE);
                selectedAuthMethod = UserEntity.AuthMethod.PASSWORD;
            }
        });
    }

    private void setupAuthMethodToggle() {
        toggleAuthMethod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnPin) {
                    selectedAuthMethod = UserEntity.AuthMethod.PIN;
                    layoutPin.setVisibility(View.VISIBLE);
                    layoutPassword.setVisibility(View.GONE);
                } else {
                    selectedAuthMethod = UserEntity.AuthMethod.PASSWORD;
                    layoutPassword.setVisibility(View.VISIBLE);
                    layoutPin.setVisibility(View.GONE);
                }
            }
        });
    }

    private void saveUser() {
        // ✅ REQUIRED FIELDS (Entered by Admin)
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        // Phone is required
        String phone = etPhone.getText().toString().trim();
        if (!AuthUtils.isValidPhone(phone)) {
            etPhone.setError("Invalid phone number");
            return;
        }

        // ✅ OPTIONAL FIELDS (Entered by Admin)
        String bio = etBio.getText().toString().trim();
        String company = etCompany.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String deactivationReason = etDeactivationReason.getText().toString().trim();

        if (selectedAuthMethod == UserEntity.AuthMethod.PIN) {
            // Validate PIN
            String pin = etPin.getText().toString().trim();
            String confirmPin = etConfirmPin.getText().toString().trim();

            if (!AuthUtils.isValidPin(pin)) {
                etPin.setError("PIN must be 4-6 digits");
                return;
            }

            if (!pin.equals(confirmPin)) {
                etConfirmPin.setError("PINs do not match");
                return;
            }

            // Create PIN user (local only)
            createLocalPinUser(name, phone, pin, bio, company, title, deactivationReason);

        } else {
            // Validate email
            String email = etEmail.getText().toString().trim();
            if (!AuthUtils.isValidEmail(email)) {
                etEmail.setError("Invalid email address");
                return;
            }

            // Validate password
            String password = etPassword.getText().toString().trim();
            if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                return;
            }

            // Create in Firebase FIRST
            createFirebaseUser(name, phone, email, password, bio, company, title, deactivationReason);
        }
    }

    private void createLocalPinUser(String name, String phone, String pin,
                                    String bio, String company, String title,
                                    String deactivationReason) {
        Toast.makeText(getContext(), "Creating PIN user...", Toast.LENGTH_SHORT).show();

        btnSave.setEnabled(false);
        btnSave.setText("Creating...");

        executor.execute(() -> {
            String pinHash = AuthUtils.hash(pin);

            // ✅ Create user with all fields
            UserEntity user = new UserEntity(
                    0, // localId will be auto-generated
                    null, // uid (no Firebase)
                    name,
                    selectedRole,
                    phone,
                    null, // email (no email for PIN users)
                    null, // passwordHash (not used for PIN)
                    pinHash,
                    UserEntity.AuthMethod.PIN,
                    0, // lastLoginAt
                    null, // lastLoginIp
                    null, // lastLoginDevice
                    false, // emailVerified
                    System.currentTimeMillis(), // passwordChangedAt
                    0, // failedLoginAttempts
                    0, // lockedUntil
                    false, // twoFactorEnabled
                    null, // twoFactorSecret
                    System.currentTimeMillis(), // lastUpdatedAt
                    0, // deactivatedAt
                    null, // deactivatedBy
                    deactivationReason.isEmpty() ? null : deactivationReason,
                    bio.isEmpty() ? null : bio,
                    company.isEmpty() ? null : company,
                    title.isEmpty() ? null : title,
                    "UTC", // timezone (default)
                    System.currentTimeMillis(), // createdAt
                    System.currentTimeMillis(), // updatedAt
                    true, // isActive
                    farmId
            );

            long id = database.userDao().insert(user);

            requireActivity().runOnUiThread(() -> {
                btnSave.setEnabled(true);
                btnSave.setText("Add User");

                if (id > 0) {
                    Toast.makeText(getContext(),
                            "PIN user added successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onUserAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(),
                            "Error adding PIN user", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void createFirebaseUser(String name, String phone, String email, String password,
                                    String bio, String company, String title,
                                    String deactivationReason) {
        Toast.makeText(getContext(), "Creating user in Firebase...", Toast.LENGTH_SHORT).show();

        btnSave.setEnabled(false);
        btnSave.setText("Creating...");

        firebaseManager.signUpWithEmail(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                Log.d(TAG, "Firebase user created: " + firebaseUser.getUid());

                // Now create in local database
                createLocalUser(name, phone, email, password, firebaseUser.getUid(),
                        bio, company, title, deactivationReason);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Firebase creation failed: " + error);
                requireActivity().runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Add User");

                    String errorMessage;
                    if (error.contains("email-already-in-use")) {
                        errorMessage = "This email is already registered";
                    } else if (error.contains("invalid-email")) {
                        errorMessage = "Invalid email format";
                    } else if (error.contains("weak-password")) {
                        errorMessage = "Password is too weak";
                    } else {
                        errorMessage = "Firebase error: " + error;
                    }

                    Toast.makeText(getContext(),
                            errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void createLocalUser(String name, String phone, String email, String password,
                                 String firebaseUid, String bio, String company, String title,
                                 String deactivationReason) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();

            // ✅ Create user with all fields
            UserEntity user = new UserEntity(
                    0, // localId will be auto-generated
                    firebaseUid,
                    name,
                    selectedRole,
                    phone,
                    email,
                    AuthUtils.hash(password), // passwordHash
                    null, // pinHash
                    UserEntity.AuthMethod.PASSWORD,
                    0, // lastLoginAt
                    null, // lastLoginIp
                    null, // lastLoginDevice
                    false, // emailVerified
                    now, // passwordChangedAt
                    0, // failedLoginAttempts
                    0, // lockedUntil
                    false, // twoFactorEnabled
                    null, // twoFactorSecret
                    now, // lastUpdatedAt
                    0, // deactivatedAt
                    null, // deactivatedBy
                    deactivationReason.isEmpty() ? null : deactivationReason,
                    bio.isEmpty() ? null : bio,
                    company.isEmpty() ? null : company,
                    title.isEmpty() ? null : title,
                    "UTC", // timezone (default)
                    now, // createdAt
                    now, // updatedAt
                    true, // isActive
                    farmId
            );

            long id = database.userDao().insert(user);
            user.setLocalId(id);

            // Save to Firestore
            saveUserToFirestore(user, firebaseUid);

            requireActivity().runOnUiThread(() -> {
                btnSave.setEnabled(true);
                btnSave.setText("Add User");

                if (id > 0) {
                    Toast.makeText(getContext(),
                            "Team member added successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onUserAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(),
                            "Error adding team member to local database", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveUserToFirestore(UserEntity user, String firebaseUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userMap = new HashMap<>();

        // ✅ Core fields (Admin Entered - Required)
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole().name());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());

        // ✅ Core fields (Admin Entered - Optional)
        userMap.put("bio", user.getBio());
        userMap.put("company", user.getCompany());
        userMap.put("title", user.getTitle());
        userMap.put("deactivationReason", user.getDeactivationReason());

        // ✅ Auto-Generated fields
        userMap.put("id", user.getLocalId());
        userMap.put("uid", firebaseUid);
        userMap.put("farmId", String.valueOf(user.getFarmId()));
        userMap.put("authMethod", user.getAuthMethod().name());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("lastUpdatedAt", user.getLastUpdatedAt());
        userMap.put("emailVerified", user.isEmailVerified());
        userMap.put("failedLoginAttempts", user.getFailedLoginAttempts());
        userMap.put("lockedUntil", user.getLockedUntil());
        userMap.put("twoFactorEnabled", user.isTwoFactorEnabled());
        userMap.put("twoFactorSecret", user.getTwoFactorSecret());
        userMap.put("lastLoginAt", user.getLastLoginAt());
        userMap.put("lastLoginIp", user.getLastLoginIp());
        userMap.put("lastLoginDevice", user.getLastLoginDevice());
        userMap.put("deactivatedAt", user.getDeactivatedAt());

        // ✅ Admin Action + Auto fields
        userMap.put("isActive", user.isActive());
        userMap.put("deactivatedBy", user.getDeactivatedBy());

        // Audit
        userMap.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(firebaseUid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved to Firestore with all fields");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user to Firestore", e);
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}