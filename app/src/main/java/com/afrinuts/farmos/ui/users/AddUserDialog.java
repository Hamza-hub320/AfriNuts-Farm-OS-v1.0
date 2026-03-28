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

    private long farmId;

    private UserEntity.UserRole selectedRole = UserEntity.UserRole.WORKER;
    private UserEntity.AuthMethod selectedAuthMethod = UserEntity.AuthMethod.PIN;

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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("")
                .setView(view);

        AlertDialog dialog = builder.create();

        // Wire up custom buttons from the layout
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnSave.setOnClickListener(v -> saveUser());
        btnCancel.setOnClickListener(v -> dismiss());

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

            // PIN users are local only
            createLocalPinUser(name, phone, pin);

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
            createFirebaseUser(name, phone, email, password);
        }
    }

    private void createLocalPinUser(String name, String phone, String pin) {
        Toast.makeText(getContext(), "Creating PIN user...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            String pinHash = AuthUtils.hash(pin);
            // FIXED: Add farmId parameter
            UserEntity user = new UserEntity(name, selectedRole, phone, pinHash, farmId);

            long id = database.userDao().insert(user);

            requireActivity().runOnUiThread(() -> {
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

    private void createFirebaseUser(String name, String phone, String email, String password) {
        Toast.makeText(getContext(), "Creating user in Firebase...", Toast.LENGTH_SHORT).show();

        firebaseManager.signUpWithEmail(email, password, new FirebaseManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                Log.d(TAG, "Firebase user created: " + firebaseUser.getUid());

                // Now create in local database
                createLocalUser(name, phone, email, password, firebaseUser.getUid());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Firebase creation failed: " + error);
                requireActivity().runOnUiThread(() -> {
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

    private void createLocalUser(String name, String phone, String email, String password, String firebaseUid) {
        executor.execute(() -> {
            String passwordHash = AuthUtils.hash(password);
            UserEntity user = new UserEntity(name, selectedRole, phone, email, passwordHash);

            long id = database.userDao().insert(user);
            user.setLocalId(id);

            // Save to Firestore
            saveUserToFirestore(user, firebaseUid);

            requireActivity().runOnUiThread(() -> {
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
        userMap.put("id", user.getLocalId());
        userMap.put("name", user.getName());
        userMap.put("role", user.getRole().name());
        userMap.put("phone", user.getPhone());
        userMap.put("email", user.getEmail());
        userMap.put("authMethod", user.getAuthMethod().name());
        userMap.put("isActive", user.isActive());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("farmId", "1");

        db.collection("users")
                .document(firebaseUid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved to Firestore");
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