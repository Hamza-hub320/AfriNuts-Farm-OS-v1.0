package com.afrinuts.farmos.ui.suppliers;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditSupplierDialog extends DialogFragment {

    private static final String ARG_SUPPLIER_ID = "supplier_id";

    private long supplierId;
    private SupplierEntity supplier;
    private SupplierRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements - Basic Info
    private TextInputEditText etName;
    private TextInputEditText etContactPerson;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private TextInputEditText etLocation;
    private TextInputEditText etRegistrationNumber;
    private TextInputEditText etNotes;
    private MaterialAutoCompleteTextView etType;

    // ✅ Farmer Profile Fields
    private TextInputEditText etYearsFarming;
    private TextInputEditText etFarmSize;
    private TextInputEditText etQuote;
    private TextInputEditText etBio;
    private SwitchCompat switchFirebreak;
    private SwitchCompat switchSustainable;
    private SwitchCompat switchChildLabor;

    public interface OnSupplierUpdatedListener {
        void onSupplierUpdated();
    }

    private OnSupplierUpdatedListener listener;

    public static EditSupplierDialog newInstance(long supplierId) {
        EditSupplierDialog dialog = new EditSupplierDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_SUPPLIER_ID, supplierId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnSupplierUpdatedListener(OnSupplierUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
        }

        long farmId = 1; // TODO: Get from current farm context
        repository = new SupplierRepository(requireContext(), farmId);

        // Load supplier data
        executor.execute(() -> {
            supplier = repository.getSupplierByIdSync(supplierId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (supplier != null) {
                        // Populate UI when view is created
                    } else {
                        Toast.makeText(getContext(), "Supplier not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_supplier, null);

        initViews(view);
        setupTypeDropdown();
        populateFields();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);

            // Use custom buttons from layout
            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveSupplier());
        });

        return dialog;
    }

    private void initViews(View view) {
        // Basic Info
        etName = view.findViewById(R.id.etName);
        etType = view.findViewById(R.id.etType);
        etContactPerson = view.findViewById(R.id.etContactPerson);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etLocation = view.findViewById(R.id.etLocation);
        etRegistrationNumber = view.findViewById(R.id.etRegistrationNumber);
        etNotes = view.findViewById(R.id.etNotes);

        // ✅ Farmer Profile Fields
        etYearsFarming = view.findViewById(R.id.etYearsFarming);
        etFarmSize = view.findViewById(R.id.etFarmSize);
        etQuote = view.findViewById(R.id.etQuote);
        etBio = view.findViewById(R.id.etBio);
        switchFirebreak = view.findViewById(R.id.switchFirebreak);
        switchSustainable = view.findViewById(R.id.switchSustainable);
        switchChildLabor = view.findViewById(R.id.switchChildLabor);
    }

    private void setupTypeDropdown() {
        SupplierEntity.SupplierType[] types = SupplierEntity.SupplierType.values();
        String[] typeNames = new String[types.length];

        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );

        etType.setAdapter(adapter);
    }

    private void populateFields() {
        if (supplier == null) return;

        // Basic Info
        etName.setText(supplier.getName());
        String typeDisplay = supplier.getType().getDisplayName();
        etType.setText(typeDisplay, false);
        etContactPerson.setText(supplier.getContactPerson());
        etPhone.setText(supplier.getPhone());
        etEmail.setText(supplier.getEmail());
        etLocation.setText(supplier.getLocation());
        etRegistrationNumber.setText(supplier.getRegistrationNumber());
        etNotes.setText(supplier.getNotes());

        // ✅ Farmer Profile Fields
        if (supplier.getYearsFarming() > 0) {
            etYearsFarming.setText(String.valueOf(supplier.getYearsFarming()));
        }
        if (supplier.getFarmSize() > 0) {
            etFarmSize.setText(String.valueOf(supplier.getFarmSize()));
        }
        etQuote.setText(supplier.getQuote());
        etBio.setText(supplier.getBio());

        // Switches
        switchFirebreak.setChecked(supplier.isFirebreakProtection());
        switchSustainable.setChecked(supplier.isSustainableHarvesting());
        switchChildLabor.setChecked(supplier.isNoChildLabor());
    }

    private void saveSupplier() {
        if (!validateInputs()) return;

        String name = etName.getText().toString().trim();
        String typeStr = etType.getText().toString().trim();
        String contactPerson = etContactPerson.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String registrationNumber = etRegistrationNumber.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Get supplier type from display
        SupplierEntity.SupplierType type = getTypeFromDisplay(typeStr);

        // ✅ Get farmer profile fields
        int yearsFarming = 0;
        String yearsStr = etYearsFarming.getText().toString().trim();
        if (!TextUtils.isEmpty(yearsStr)) {
            try {
                yearsFarming = Integer.parseInt(yearsStr);
            } catch (NumberFormatException e) {
                // Ignore, keep as 0
            }
        }

        double farmSize = 0;
        String farmSizeStr = etFarmSize.getText().toString().trim();
        if (!TextUtils.isEmpty(farmSizeStr)) {
            try {
                farmSize = Double.parseDouble(farmSizeStr);
            } catch (NumberFormatException e) {
                // Ignore, keep as 0
            }
        }

        String quote = etQuote.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        boolean firebreakProtection = switchFirebreak.isChecked();
        boolean sustainableHarvesting = switchSustainable.isChecked();
        boolean noChildLabor = switchChildLabor.isChecked();

        // Update supplier
        supplier.setName(name);
        supplier.setType(type);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setLocation(location);
        supplier.setRegistrationNumber(registrationNumber);
        supplier.setNotes(notes);

        // ✅ Set farmer profile fields
        supplier.setYearsFarming(yearsFarming);
        supplier.setFarmSize(farmSize);
        supplier.setQuote(quote);
        supplier.setBio(bio);
        supplier.setFirebreakProtection(firebreakProtection);
        supplier.setSustainableHarvesting(sustainableHarvesting);
        supplier.setNoChildLabor(noChildLabor);

        supplier.setUpdatedAt(System.currentTimeMillis());

        executor.execute(() -> {
            repository.updateSupplier(supplier, new SupplierRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Supplier updated successfully!", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onSupplierUpdated();
                            dismiss();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        });
    }

    private SupplierEntity.SupplierType getTypeFromDisplay(String display) {
        for (SupplierEntity.SupplierType type : SupplierEntity.SupplierType.values()) {
            if (display.contains(type.getDisplayName())) {
                return type;
            }
        }
        return SupplierEntity.SupplierType.COOPERATIVE;
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etName.getText())) {
            etName.setError("Supplier name required");
            return false;
        }
        if (TextUtils.isEmpty(etType.getText())) {
            etType.setError("Supplier type required");
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
        executor.shutdown();
    }
}