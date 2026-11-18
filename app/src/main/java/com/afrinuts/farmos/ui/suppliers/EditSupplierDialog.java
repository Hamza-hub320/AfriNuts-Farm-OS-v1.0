package com.afrinuts.farmos.ui.suppliers;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.ConsentScope;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditSupplierDialog extends DialogFragment {

    private static final String ARG_SUPPLIER_ID = "supplier_id";
    private static final String CONSENT_VERSION = "1.0";

    private long supplierId;
    private SupplierEntity supplier;
    private SupplierRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Basic Info
    private TextInputEditText etName;
    private TextInputEditText etContactPerson;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private TextInputEditText etLocation;
    private TextInputEditText etRegistrationNumber;
    private TextInputEditText etNotes;
    private MaterialAutoCompleteTextView etType;

    // Farmer Profile Fields
    private TextInputEditText etYearsFarming;
    private TextInputEditText etFarmSize;
    private TextInputEditText etQuote;
    private TextInputEditText etBio;
    private SwitchCompat switchFirebreak;
    private SwitchCompat switchSustainable;
    private SwitchCompat switchChildLabor;

    // ✅ Consent UI
    private TextInputEditText etCooperativeName;
    private TextInputEditText etCooperativeId;
    private TextInputEditText etFarmerIdNumber;

    private CheckBox cbConsentIdentity;
    private CheckBox cbConsentLocation;
    private CheckBox cbConsentPhoto;
    private LinearLayout layoutConsentMeta;
    private TextView tvConsentSignedBy;
    private TextView tvConsentDate;
    private TextView tvConsentWithdrawn;
    private SessionManager sessionManager;

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
        sessionManager = new SessionManager(requireContext());
        if (getArguments() != null) {
            supplierId = getArguments().getLong(ARG_SUPPLIER_ID);
        }

        long farmId = 1;
        repository = new SupplierRepository(requireContext(), farmId);

        executor.execute(() -> {
            supplier = repository.getSupplierByIdSync(supplierId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (supplier != null) {
                        populateFields();
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

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);

            view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveSupplier());
        });

        return dialog;
    }

    private void initViews(View view) {
        etName = view.findViewById(R.id.etName);
        etType = view.findViewById(R.id.etType);
        etContactPerson = view.findViewById(R.id.etContactPerson);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        etLocation = view.findViewById(R.id.etLocation);
        etRegistrationNumber = view.findViewById(R.id.etRegistrationNumber);
        etNotes = view.findViewById(R.id.etNotes);

        etYearsFarming = view.findViewById(R.id.etYearsFarming);
        etFarmSize = view.findViewById(R.id.etFarmSize);
        etQuote = view.findViewById(R.id.etQuote);
        etBio = view.findViewById(R.id.etBio);
        switchFirebreak = view.findViewById(R.id.switchFirebreak);
        switchSustainable = view.findViewById(R.id.switchSustainable);
        switchChildLabor = view.findViewById(R.id.switchChildLabor);

        // ✅ Cooperative linkage
        etCooperativeName = view.findViewById(R.id.etCooperativeName);
        etCooperativeId   = view.findViewById(R.id.etCooperativeId);
        etFarmerIdNumber  = view.findViewById(R.id.etFarmerIdNumber);

        // ✅ Consent UI
        cbConsentIdentity = view.findViewById(R.id.cbConsentIdentity);
        cbConsentLocation = view.findViewById(R.id.cbConsentLocation);
        cbConsentPhoto    = view.findViewById(R.id.cbConsentPhoto);
        layoutConsentMeta = view.findViewById(R.id.layoutConsentMeta);
        tvConsentSignedBy = view.findViewById(R.id.tvConsentSignedBy);
        tvConsentDate     = view.findViewById(R.id.tvConsentDate);
        tvConsentWithdrawn = view.findViewById(R.id.tvConsentWithdrawn);
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

        etName.setText(supplier.getName());
        etType.setText(supplier.getType().getDisplayName(), false);
        etContactPerson.setText(supplier.getContactPerson());
        etPhone.setText(supplier.getPhone());
        etEmail.setText(supplier.getEmail());
        etLocation.setText(supplier.getLocation());
        etRegistrationNumber.setText(supplier.getRegistrationNumber());
        etNotes.setText(supplier.getNotes());

        if (supplier.getYearsFarming() > 0) {
            etYearsFarming.setText(String.valueOf(supplier.getYearsFarming()));
        }
        if (supplier.getFarmSize() > 0) {
            etFarmSize.setText(String.valueOf(supplier.getFarmSize()));
        }
        etQuote.setText(supplier.getQuote());
        etBio.setText(supplier.getBio());

        switchFirebreak.setChecked(supplier.isFirebreakProtection());
        switchSustainable.setChecked(supplier.isSustainableHarvesting());
        switchChildLabor.setChecked(supplier.isNoChildLabor());

        // ✅ Consent checkboxes
        etCooperativeName.setText(supplier.getCooperativeName());
        etCooperativeId.setText(supplier.getCooperativeId());
        etFarmerIdNumber.setText(supplier.getFarmerIdNumber());

        List<String> scopes = supplier.getDataConsentScopes();
        cbConsentIdentity.setChecked(scopes.contains(ConsentScope.IDENTITY));
        cbConsentLocation.setChecked(scopes.contains(ConsentScope.LOCATION));
        cbConsentPhoto.setChecked(scopes.contains(ConsentScope.PHOTO));

        boolean hasConsentRecord = supplier.isDataConsentGiven() || supplier.getDataConsentDate() > 0;
        if (hasConsentRecord) {
            layoutConsentMeta.setVisibility(View.VISIBLE);

            String signedBy = supplier.getDataConsentSignedBy();
            tvConsentSignedBy.setText("Signed by: " +
                    (TextUtils.isEmpty(signedBy) ? supplier.getName() : signedBy) +
                    " (v" + (supplier.getDataConsentVersion() != null
                    ? supplier.getDataConsentVersion() : "—") + ")");

            if (supplier.getDataConsentDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                tvConsentDate.setText("Recorded on: " + sdf.format(new Date(supplier.getDataConsentDate())));
            } else {
                tvConsentDate.setText("Recorded: —");
            }

            boolean withdrawn = supplier.getDataConsentWithdrawnDate() != null
                    && !supplier.getDataConsentWithdrawnDate().isEmpty();
            if (withdrawn) {
                tvConsentWithdrawn.setVisibility(View.VISIBLE);
                String reason = supplier.getDataConsentWithdrawnReason();
                tvConsentWithdrawn.setText("Withdrawn on " + supplier.getDataConsentWithdrawnDate() +
                        (TextUtils.isEmpty(reason) ? "" : " — " + reason));
            } else {
                tvConsentWithdrawn.setVisibility(View.GONE);
            }
        } else {
            layoutConsentMeta.setVisibility(View.GONE);
        }
    }

    private void saveSupplier() {
        if (!sessionManager.canEditData()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Cannot perform this action")
                    .setMessage("Only Owners and Managers can edit supplier record.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        if (!validateInputs()) return;

        String name = etName.getText().toString().trim();
        String typeStr = etType.getText().toString().trim();
        String contactPerson = etContactPerson.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String registrationNumber = etRegistrationNumber.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String cooperativeName = etCooperativeName.getText().toString().trim();
        String cooperativeId   = etCooperativeId.getText().toString().trim();
        String farmerIdNumber  = etFarmerIdNumber.getText().toString().trim();

        supplier.setCooperativeName(cooperativeName.isEmpty() ? null : cooperativeName);
        supplier.setCooperativeId(cooperativeId.isEmpty() ? null : cooperativeId);
        supplier.setFarmerIdNumber(farmerIdNumber.isEmpty() ? null : farmerIdNumber);

        SupplierEntity.SupplierType type = getTypeFromDisplay(typeStr);

        int yearsFarming = 0;
        String yearsStr = etYearsFarming.getText().toString().trim();
        if (!TextUtils.isEmpty(yearsStr)) {
            try { yearsFarming = Integer.parseInt(yearsStr); } catch (NumberFormatException ignored) {}
        }

        double farmSize = 0;
        String farmSizeStr = etFarmSize.getText().toString().trim();
        if (!TextUtils.isEmpty(farmSizeStr)) {
            try { farmSize = Double.parseDouble(farmSizeStr); } catch (NumberFormatException ignored) {}
        }

        String quote = etQuote.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        boolean firebreakProtection = switchFirebreak.isChecked();
        boolean sustainableHarvesting = switchSustainable.isChecked();
        boolean noChildLabor = switchChildLabor.isChecked();

        // Update common fields
        supplier.setName(name);
        supplier.setType(type);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setLocation(location);
        supplier.setRegistrationNumber(registrationNumber);
        supplier.setNotes(notes);

        supplier.setYearsFarming(yearsFarming);
        supplier.setFarmSize(farmSize);
        supplier.setQuote(quote);
        supplier.setBio(bio);
        supplier.setFirebreakProtection(firebreakProtection);
        supplier.setSustainableHarvesting(sustainableHarvesting);
        supplier.setNoChildLabor(noChildLabor);

        List<String> newScopes = new ArrayList<>();
        if (cbConsentIdentity.isChecked()) newScopes.add(ConsentScope.IDENTITY);
        if (cbConsentLocation.isChecked()) newScopes.add(ConsentScope.LOCATION);
        if (cbConsentPhoto.isChecked())    newScopes.add(ConsentScope.PHOTO);

        boolean consentWasGiven = supplier.isDataConsentGiven();
        boolean consentNowGiven = !newScopes.isEmpty();

        String farmerName = !TextUtils.isEmpty(contactPerson) ? contactPerson : name;

        if (consentNowGiven) {
            supplier.setDataConsentScopes(newScopes);
            supplier.setDataConsentGiven(true);

            if (!consentWasGiven) {
                supplier.setDataConsentDate(System.currentTimeMillis());
                supplier.setDataConsentSignedBy(farmerName);
                supplier.setDataConsentVersion(CONSENT_VERSION);
            }
            supplier.setDataConsentText(
                    ConsentScope.buildConsentText(farmerName, newScopes, CONSENT_VERSION));

            supplier.setDataConsentWithdrawnDate(null);
            supplier.setDataConsentWithdrawnReason(null);

        } else if (consentWasGiven) {
            supplier.setDataConsentGiven(false);
            supplier.setDataConsentScopes(new ArrayList<>());
            supplier.setDataConsentWithdrawnDate(
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                            .format(new Date()));
            supplier.setDataConsentWithdrawnReason("Withdrawn by farmer");
        }

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
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show());
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
        if (repository != null) repository.shutdown();
        executor.shutdown();
    }
}