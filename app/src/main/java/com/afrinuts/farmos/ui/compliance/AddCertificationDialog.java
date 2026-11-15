package com.afrinuts.farmos.ui.compliance;

import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddCertificationDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private CertificationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etCertificationName;
    private TextInputEditText etCertificationBody;
    private MaterialAutoCompleteTextView etStatus;
    private TextInputEditText etScope;
    private TextInputEditText etStandards;
    private TextInputEditText etTargetDate;
    private TextInputEditText etApprovalDate;
    private TextInputEditText etExpiryDate;
    private Button btnSave;
    private TextInputEditText etCertificationId;

    // ✅ Date calendars - initialize to null (not set)
    private Calendar targetCalendar = null;
    private Calendar approvalCalendar = null;
    private Calendar expiryCalendar = null;

    public interface OnCertificationAddedListener {
        void onCertificationAdded();
    }

    private OnCertificationAddedListener listener;

    public static AddCertificationDialog newInstance(long farmId) {
        AddCertificationDialog dialog = new AddCertificationDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnCertificationAddedListener(OnCertificationAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
        }

        repository = new CertificationRepository(requireContext(), farmId);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_certification, null);

        initViews(view);
        setupStatusDropdown();
        setupDatePickers();

        // ✅ Set hint text for optional dates
        etApprovalDate.setHint("Approval Date (Optional)");
        etExpiryDate.setHint("Expiry Date (Optional)");

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
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveCertification());
        });

        return dialog;
    }

    private void initViews(View view) {
        etCertificationName = view.findViewById(R.id.etCertificationName);
        etCertificationBody = view.findViewById(R.id.etCertificationBody);
        etStatus = view.findViewById(R.id.etStatus);
        etScope = view.findViewById(R.id.etScope);
        etStandards = view.findViewById(R.id.etStandards);
        etTargetDate = view.findViewById(R.id.etTargetDate);
        etApprovalDate = view.findViewById(R.id.etApprovalDate);
        etCertificationId = view.findViewById(R.id.etCertificationId);
        etCertificationId.setEnabled(false);
        etCertificationId.setHint("");
        etExpiryDate = view.findViewById(R.id.etExpiryDate);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void setupStatusDropdown() {
        // Get the display names directly from the enum
        String[] statusNames = new String[CertificationEntity.CertificationStatus.values().length];
        int i = 0;
        for (CertificationEntity.CertificationStatus status : CertificationEntity.CertificationStatus.values()) {
            statusNames[i++] = status.getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                statusNames
        );

        etStatus.setAdapter(adapter);
        etStatus.setText(statusNames[0], false);
    }

    private void setupDatePickers() {
        // ✅ Target Date - required
        etTargetDate.setOnClickListener(v -> showDatePicker(true));
        etTargetDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker(true);
        });

        // ✅ Approval Date - optional (click to set, click again to clear)
        etApprovalDate.setOnClickListener(v -> {
            if (approvalCalendar == null) {
                showDatePicker(false);
            } else {
                // Clear the date
                approvalCalendar = null;
                etApprovalDate.setText("");
                etApprovalDate.setHint("Approval Date (Optional)");
            }
        });

        // ✅ Expiry Date - optional (click to set, click again to clear)
        etExpiryDate.setOnClickListener(v -> {
            if (expiryCalendar == null) {
                showDatePickerForExpiry();
            } else {
                // Clear the date
                expiryCalendar = null;
                etExpiryDate.setText("");
                etExpiryDate.setHint("Expiry Date (Optional)");
            }
        });
    }

    private void showDatePicker(boolean isTarget) {
        Calendar calendar = isTarget ?
                (targetCalendar != null ? targetCalendar : Calendar.getInstance()) :
                (approvalCalendar != null ? approvalCalendar : Calendar.getInstance());

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    if (isTarget) {
                        targetCalendar = selected;
                        updateDateLabel(etTargetDate, targetCalendar);
                    } else {
                        approvalCalendar = selected;
                        updateDateLabel(etApprovalDate, approvalCalendar);
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showDatePickerForExpiry() {
        Calendar calendar = expiryCalendar != null ? expiryCalendar : Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    expiryCalendar = selected;
                    updateDateLabel(etExpiryDate, expiryCalendar);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateLabel(TextInputEditText editText, Calendar calendar) {
        if (calendar == null) {
            editText.setText("");
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        editText.setText(sdf.format(calendar.getTime()));
    }

    private CertificationEntity.CertificationStatus getStatusFromDisplay(String display) {
        for (CertificationEntity.CertificationStatus status : CertificationEntity.CertificationStatus.values()) {
            if (status.getDisplayName().equals(display)) {
                return status;
            }
        }
        return CertificationEntity.CertificationStatus.NOT_STARTED;
    }

    private void saveCertification() {
        String name = etCertificationName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            etCertificationName.setError("Certification name required");
            return;
        }

        String body = etCertificationBody.getText().toString().trim();
        if (TextUtils.isEmpty(body)) {
            etCertificationBody.setError("Certification body required");
            return;
        }

        String statusStr = etStatus.getText().toString().trim();
        CertificationEntity.CertificationStatus status = getStatusFromDisplay(statusStr);

        String scope = etScope.getText().toString().trim();
        String standardsStr = etStandards.getText().toString().trim();

        List<String> standards = new ArrayList<>();
        if (!TextUtils.isEmpty(standardsStr)) {
            standards = Arrays.asList(standardsStr.split("\\s*,\\s*"));
        }

        // ✅ Get dates - use 0 if not set (0 = not set)
        long targetDate = targetCalendar != null ? targetCalendar.getTimeInMillis() : 0;
        long approvalDate = approvalCalendar != null ? approvalCalendar.getTimeInMillis() : 0;
        long expiryDate = expiryCalendar != null ? expiryCalendar.getTimeInMillis() : 0;

        // ✅ Validate: Target date is required
        if (targetDate == 0) {
            etTargetDate.setError("Target date is required");
            return;
        }

        // ✅ Validate: If approval date is set, expiry date should also be set
        if (approvalDate > 0 && expiryDate == 0) {
            etExpiryDate.setError("Expiry date is required when approval date is set");
            return;
        }

        // ✅ Validate: If expiry date is set, approval date should also be set
        if (expiryDate > 0 && approvalDate == 0) {
            etApprovalDate.setError("Approval date is required when expiry date is set");
            return;
        }

        // ✅ Validate: Expiry date must be after approval date
        if (approvalDate > 0 && expiryDate > 0 && expiryDate <= approvalDate) {
            etExpiryDate.setError("Expiry date must be after approval date");
            return;
        }

        CertificationEntity certification = new CertificationEntity(
                farmId,
                name,
                body,
                status,
                scope,
                standards,
                targetDate,
                approvalDate,
                expiryDate
        );

        // ✅ Log the dates for debugging
        Log.d(TAG, "Saving certification with dates - Target: " + targetDate +
                ", Approval: " + approvalDate +
                ", Expiry: " + expiryDate);

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        repository.insertCertification(certification, new CertificationRepository.InsertCallback() {
            @Override
            public void onSuccess(String certificationId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "" + certificationId, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onCertificationAdded();
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Add Certification");
                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
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