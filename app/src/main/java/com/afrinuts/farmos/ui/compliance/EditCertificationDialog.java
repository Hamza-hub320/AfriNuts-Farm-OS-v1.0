package com.afrinuts.farmos.ui.compliance;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditCertificationDialog extends DialogFragment {

    private static final String TAG = "EditCertificationDialog";
    private static final String ARG_CERTIFICATION_ID = "certification_id";

    private String certificationId;
    private CertificationEntity certification;
    private CertificationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etCertificationName;
    private TextInputEditText etCertificationBody;
    private TextInputEditText etScope;
    private TextInputEditText etStandards;
    private TextInputEditText etTargetDate;
    private TextInputEditText etApprovalDate;
    private TextInputEditText etExpiryDate;

    private Button btnSave;
    private Button btnCancel;

    // ✅ Initialize calendars to null (not set)
    private Calendar targetCalendar = null;
    private Calendar approvalCalendar = null;
    private Calendar expiryCalendar = null;

    public interface OnCertificationUpdatedListener {
        void onCertificationUpdated();
    }

    private OnCertificationUpdatedListener listener;

    public static EditCertificationDialog newInstance(String certificationId) {
        EditCertificationDialog dialog = new EditCertificationDialog();
        Bundle args = new Bundle();
        args.putString(ARG_CERTIFICATION_ID, certificationId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnCertificationUpdatedListener(OnCertificationUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            certificationId = getArguments().getString(ARG_CERTIFICATION_ID);
        }

        long farmId = 1;
        repository = new CertificationRepository(requireContext(), farmId);

        // Load certification data
        executor.execute(() -> {
            certification = repository.getCertificationByIdSync(certificationId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (certification != null) {
                        populateFields();
                    } else {
                        Toast.makeText(getContext(), "Certification not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_certification, null);

        initViews(view);
        setupDatePickers();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSaveDialog = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancelDialog = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            btnSaveDialog.setVisibility(View.GONE);
            btnCancelDialog.setVisibility(View.GONE);

            btnCancel.setOnClickListener(v -> dismiss());
            btnSave.setOnClickListener(v -> saveCertification());
        });

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window != null) {
            int horizontalInset = (int) (32 * getResources().getDisplayMetrics().density + 0.5f);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - horizontalInset,
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void initViews(View view) {
        etCertificationName = view.findViewById(R.id.etCertificationName);
        etCertificationBody = view.findViewById(R.id.etCertificationBody);
        etScope = view.findViewById(R.id.etScope);
        etStandards = view.findViewById(R.id.etStandards);
        etTargetDate = view.findViewById(R.id.etTargetDate);
        etApprovalDate = view.findViewById(R.id.etApprovalDate);
        etExpiryDate = view.findViewById(R.id.etExpiryDate);

        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        // ✅ Set hint text for optional dates
        etApprovalDate.setHint("Approval Date (Optional)");
        etExpiryDate.setHint("Expiry Date (Optional)");
    }

    private String currentActor() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return "app";
        if (u.getEmail() != null && !u.getEmail().isEmpty()) return u.getEmail();
        if (u.getUid() != null && !u.getUid().isEmpty()) return u.getUid();
        return "app";
    }

    private void setupDatePickers() {
        // ✅ Target Date - always required
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

    private void populateFields() {
        if (certification == null) return;

        etCertificationName.setText(certification.getCertificationName());
        etCertificationBody.setText(certification.getCertificationBody());
        etScope.setText(certification.getScope());
        etStandards.setText(certification.getStandardsDisplay());

        // ✅ Only set dates if they exist (value > 0)
        if (certification.getTargetDate() > 0) {
            targetCalendar = Calendar.getInstance();
            targetCalendar.setTimeInMillis(certification.getTargetDate());
            updateDateLabel(etTargetDate, targetCalendar);
        }

        if (certification.getApprovalDate() > 0) {
            approvalCalendar = Calendar.getInstance();
            approvalCalendar.setTimeInMillis(certification.getApprovalDate());
            updateDateLabel(etApprovalDate, approvalCalendar);
        } else {
            etApprovalDate.setText("");
            etApprovalDate.setHint("Approval Date (Optional)");
        }

        if (certification.getExpiryDate() > 0) {
            expiryCalendar = Calendar.getInstance();
            expiryCalendar.setTimeInMillis(certification.getExpiryDate());
            updateDateLabel(etExpiryDate, expiryCalendar);
        } else {
            etExpiryDate.setText("");
            etExpiryDate.setHint("Expiry Date (Optional)");
        }
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

        String scope = etScope.getText().toString().trim();
        String standardsStr = etStandards.getText().toString().trim();

        List<String> standards = new ArrayList<>();
        if (!TextUtils.isEmpty(standardsStr)) {
            standards = Arrays.asList(standardsStr.split("\\s*,\\s*"));
        }

        // ✅ Get dates - use 0 if not set
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

        // Update certification
        certification.setCertificationName(name);
        certification.setCertificationBody(body);
        certification.setScope(scope);
        certification.setApplicableStandards(standards);
        certification.setTargetDate(targetDate);
        certification.setApprovalDate(approvalDate);
        certification.setExpiryDate(expiryDate);
        certification.setUpdatedAt(System.currentTimeMillis());

        // ✅ Log the dates for debugging
        Log.d(TAG, "Saving certification with dates - Target: " + targetDate +
                ", Approval: " + approvalDate +
                ", Expiry: " + expiryDate);

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        repository.updateCertification(certification, new CertificationRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Certification updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onCertificationUpdated();
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
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
