package com.afrinuts.farmos.ui.compliance;

import android.app.DatePickerDialog;
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
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.VerificationRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditVerificationDialog extends DialogFragment {

    private static final String ARG_VERIFICATION_ID = "verification_id";

    private String verificationId;
    private VerificationEntity verification;
    private VerificationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private MaterialAutoCompleteTextView etVerificationType;
    private TextInputEditText etVerificationDate;
    private TextInputEditText etVerifiedBy;
    private MaterialAutoCompleteTextView etStatus;
    private LinearLayout evidenceContainer;
    private Button btnAddEvidence;
    private TextInputEditText etComment;

    private Calendar selectedDate = Calendar.getInstance();
    private Button btnCancel;
    private Button btnSave;

    public interface OnVerificationUpdatedListener {
        void onVerificationUpdated();
    }

    private OnVerificationUpdatedListener listener;

    public static EditVerificationDialog newInstance(String verificationId) {
        EditVerificationDialog dialog = new EditVerificationDialog();
        Bundle args = new Bundle();
        args.putString(ARG_VERIFICATION_ID, verificationId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnVerificationUpdatedListener(OnVerificationUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            verificationId = getArguments().getString(ARG_VERIFICATION_ID);
        }

        long farmId = 1;
        repository = new VerificationRepository(requireContext(), farmId);

        executor.execute(() -> {
            verification = repository.getVerificationByIdSync(verificationId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (verification != null) {
                        populateFields();
                    } else {
                        Toast.makeText(getContext(), "Verification not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_verification, null);

        initViews(view);
        setupDropdowns();
        setupDatePicker();
        setupEvidence();

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
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveVerification());
        });

        return dialog;
    }

    private void initViews(View view) {
        etVerificationType = view.findViewById(R.id.etVerificationType);
        etVerificationDate = view.findViewById(R.id.etVerificationDate);
        etVerifiedBy = view.findViewById(R.id.etVerifiedBy);
        etStatus = view.findViewById(R.id.etStatus);
        evidenceContainer = view.findViewById(R.id.evidenceContainer);
        btnAddEvidence = view.findViewById(R.id.btnAddEvidence);
        etComment = view.findViewById(R.id.etComment);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void setupDropdowns() {
        // ✅ Verification Type dropdown - just display names (no icons in dropdown)
        VerificationEntity.VerificationType[] types = VerificationEntity.VerificationType.values();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();  // Just the display name
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );
        etVerificationType.setAdapter(typeAdapter);

        // Status dropdown
        VerificationEntity.VerificationStatus[] statuses = VerificationEntity.VerificationStatus.values();
        String[] statusNames = new String[statuses.length];
        for (int i = 0; i < statuses.length; i++) {
            statusNames[i] = statuses[i].getDisplayName();
        }
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                statusNames
        );
        etStatus.setAdapter(statusAdapter);
    }

    private void setupDatePicker() {
        etVerificationDate.setOnClickListener(v -> showDatePicker());
        etVerificationDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker();
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etVerificationDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupEvidence() {
        btnAddEvidence.setOnClickListener(v -> addEvidenceItem(null));

        // Load existing evidence
        if (verification != null) {
            List<String> evidence = verification.getEvidence();
            for (String url : evidence) {
                addEvidenceItem(url);
            }
        }
    }

    private void addEvidenceItem(String url) {
        View evidenceView = getLayoutInflater().inflate(R.layout.item_evidence_input, evidenceContainer, false);

        TextInputEditText etUrl = evidenceView.findViewById(R.id.etEvidenceUrl);
        Button btnRemove = evidenceView.findViewById(R.id.btnRemoveEvidence);

        if (url != null) {
            etUrl.setText(url);
        }

        btnRemove.setOnClickListener(v -> {
            evidenceContainer.removeView(evidenceView);
        });

        evidenceContainer.addView(evidenceView);
    }

    private void populateFields() {
        if (verification == null) return;

        // ✅ Set verification type - just the display name
        etVerificationType.setText(verification.getVerificationType().getDisplayName(), false);

        // ✅ Set status - just the display name
        etStatus.setText(verification.getStatus().getDisplayName(), false);

        if (verification.getVerificationDate() > 0) {
            selectedDate.setTimeInMillis(verification.getVerificationDate());
            updateDateLabel();
        }

        etVerifiedBy.setText(verification.getVerifiedBy());
        etComment.setText(verification.getComment());
    }

    private List<String> collectEvidence() {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < evidenceContainer.getChildCount(); i++) {
            View evidenceView = evidenceContainer.getChildAt(i);
            TextInputEditText etUrl = evidenceView.findViewById(R.id.etEvidenceUrl);
            String url = etUrl.getText().toString().trim();
            if (!TextUtils.isEmpty(url)) {
                result.add(url);
            }
        }

        return result;
    }

    private VerificationEntity.VerificationType getTypeFromDisplay(String display) {
        for (VerificationEntity.VerificationType type : VerificationEntity.VerificationType.values()) {
            if (display.contains(type.getDisplayName())) {
                return type;
            }
        }
        return VerificationEntity.VerificationType.DOCUMENTATION;
    }

    private VerificationEntity.VerificationStatus getStatusFromDisplay(String display) {
        for (VerificationEntity.VerificationStatus status : VerificationEntity.VerificationStatus.values()) {
            if (display.equals(status.getDisplayName())) {
                return status;
            }
        }
        return VerificationEntity.VerificationStatus.PENDING;
    }

    private void saveVerification() {
        String typeStr = etVerificationType.getText().toString().trim();
        VerificationEntity.VerificationType type = getTypeFromDisplay(typeStr);

        long verificationDate = selectedDate.getTimeInMillis();
        if (verificationDate <= 0) {
            etVerificationDate.setError("Please select a verification date");
            return;
        }

        String verifiedBy = etVerifiedBy.getText().toString().trim();
        if (TextUtils.isEmpty(verifiedBy)) {
            etVerifiedBy.setError("Verifier name required");
            return;
        }

        String statusStr = etStatus.getText().toString().trim();
        VerificationEntity.VerificationStatus status = getStatusFromDisplay(statusStr);

        List<String> evidence = collectEvidence();
        String comment = etComment.getText().toString().trim();

        // Update verification
        verification.setVerificationType(type);
        verification.setVerificationDate(verificationDate);
        verification.setVerifiedBy(verifiedBy);
        verification.setStatus(status);
        verification.setEvidence(evidence);
        verification.setComment(comment);
        verification.setUpdatedAt(System.currentTimeMillis());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        repository.updateVerification(verification, new VerificationRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Verification updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onVerificationUpdated();
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