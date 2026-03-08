package com.afrinuts.farmos.ui.users;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.afrinuts.farmos.utils.AuthUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddUserDialog extends DialogFragment {

    private AppDatabase database;

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

    private UserEntity.UserRole selectedRole = UserEntity.UserRole.WORKER;
    private UserEntity.AuthMethod selectedAuthMethod = UserEntity.AuthMethod.PIN;

    public interface OnUserAddedListener {
        void onUserAdded();
    }

    private OnUserAddedListener listener;

    public static AddUserDialog newInstance() {
        return new AddUserDialog();
    }

    public void setOnUserAddedListener(OnUserAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_user, null);

        initViews(view);
        setupRoleDropdown();
        setupAuthMethodToggle();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Team Member")
                .setView(view)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> saveUser());
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

        // Default to PIN for workers
        toggleAuthMethod.check(R.id.btnPin);
        layoutPin.setVisibility(View.VISIBLE);
        layoutPassword.setVisibility(View.GONE);
    }

    private void setupRoleDropdown() {
        UserEntity.UserRole[] roles = UserEntity.UserRole.values();
        String[] roleNames = new String[roles.length];

        for (int i = 0; i < roles.length; i++) {
            roleNames[i] = roles[i].getIcon() + " " + roles[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                roleNames
        );

        etRole.setAdapter(adapter);
        etRole.setText(roleNames[2], false); // Default to Worker

        etRole.setOnItemClickListener((parent, view, position, id) -> {
            selectedRole = roles[position];
            // Workers default to PIN, others default to Password
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

        UserEntity user = null;

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

            String pinHash = AuthUtils.hash(pin);
            user = new UserEntity(name, selectedRole, phone, pinHash);

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

            String passwordHash = AuthUtils.hash(password);
            user = new UserEntity(name, selectedRole, phone, email, passwordHash);
        }

        // Save to database
        Toast.makeText(getContext(), "Adding team member...", Toast.LENGTH_SHORT).show();

        final UserEntity finalUser = user;
        new Thread(() -> {
            long id = database.userDao().insert(finalUser);

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
                            "Error adding team member", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}