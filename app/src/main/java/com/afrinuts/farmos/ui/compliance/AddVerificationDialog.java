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
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
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

public class AddVerificationDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_CERTIFICATION_ID = "certification_id";

    private long farmId;
    private String preselectedCertificationId;
    private static final String ARG_CORRECTION_OF = "correction_of";
    private String correctionOfId;
    private VerificationRepository verificationRepository;
    private CertificationRepository certificationRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<CertificationEntity> certifications = new ArrayList<>();

    // UI Elements
    private MaterialAutoCompleteTextView etCertification;
    private MaterialAutoCompleteTextView etVerificationType;
    private TextInputEditText etVerificationDate;
    private TextInputEditText etVerifiedBy;
    private MaterialAutoCompleteTextView etStatus;
    private LinearLayout evidenceContainer;
    private Button btnAddEvidence;
    private Button btnCancel;
    private Button btnSave;
    private TextInputEditText etComment;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnVerificationAddedListener {
        void onVerificationAdded();
    }

    private OnVerificationAddedListener listener;

    public static AddVerificationDialog newInstance(long farmId) {
        return newInstance(farmId, null);
    }

    public static AddVerificationDialog newInstance(long farmId, String certificationId) {
        AddVerificationDialog dialog = new AddVerificationDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        if (certificationId != null) {
            args.putString(ARG_CERTIFICATION_ID, certificationId);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public static AddVerificationDialog newInstanceCorrection(long farmId, String originalVerificationId) {
        AddVerificationDialog dialog = new AddVerificationDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        args.putString(ARG_CORRECTION_OF, originalVerificationId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnVerificationAddedListener(OnVerificationAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
            preselectedCertificationId = getArguments().getString(ARG_CERTIFICATION_ID);
            correctionOfId = getArguments().getString(ARG_CORRECTION_OF);
        }

        verificationRepository = new VerificationRepository(requireContext(), farmId);
        certificationRepository = new CertificationRepository(requireContext(), farmId);
        loadCertifications();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_verification, null);

        initViews(view);
        setupDropdowns();
        setupDatePicker();
        setupEvidence();

        String title = correctionOfId != null ? "Correcting " + correctionOfId : "Add Verification";
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setTitle(title)
                .setCancelable(false);

        if (correctionOfId != null) {
            prefillFromOriginal(correctionOfId);
        }

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

    private void prefillFromOriginal(String originalId) {
        executor.execute(() -> {
            VerificationEntity original = verificationRepository.getVerificationByIdSync(originalId);
            if (original == null || getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                preselectedCertificationId = original.getCertificationId();
                loadCertifications();

                etVerificationType.setText(original.getVerificationTypeDisplay(), false);
                etStatus.setText(original.getStatusDisplay(), false);

                // Correction's verification date defaults to TODAY.
                selectedDate.setTimeInMillis(original.getVerificationDate() > 0 ? original.getVerificationDate() : System.currentTimeMillis());
                updateDateLabel();

                etVerifiedBy.setText(original.getVerifiedBy() != null ? original.getVerifiedBy() : "");
                etComment.setText(original.getComment() != null ? original.getComment() : "");

                List<String> evidence = original.getEvidence();
                if (evidence != null) {
                    for (String url : evidence) {
                        addEvidenceItem(url);
                    }
                }
            });
        });
    }

    private void initViews(View view) {
        etCertification = view.findViewById(R.id.etCertification);
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

    private void loadCertifications() {
        executor.execute(() -> {
            certifications = certificationRepository.getCertificationsSync();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<String> certNames = new ArrayList<>();
                    for (CertificationEntity cert : certifications) {
                        certNames.add(cert.getCertificationName() + " (" + cert.getCertificationId() + ")");
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            certNames
                    );
                    etCertification.setAdapter(adapter);

                    if (preselectedCertificationId != null) {
                        for (int i = 0; i < certifications.size(); i++) {
                            CertificationEntity cert = certifications.get(i);
                            if (cert.getCertificationId().equals(preselectedCertificationId)) {
                                etCertification.setText(cert.getCertificationName() + " (" + cert.getCertificationId() + ")", false);
                                break;
                            }
                        }
                    }
                });
            }
        });
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
        etVerificationType.setText(typeNames[0], false);

        // ✅ Status dropdown - just display names
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
        etStatus.setText(statusNames[0], false);
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

    private String getCertificationIdFromDisplay(String display) {
        if (display == null) return null;
        int start = display.indexOf("(");
        int end = display.indexOf(")");
        if (start != -1 && end != -1 && end > start) {
            return display.substring(start + 1, end);
        }
        return null;
    }

    private void saveVerification() {
        String certDisplay = etCertification.getText().toString().trim();
        if (TextUtils.isEmpty(certDisplay)) {
            etCertification.setError("Please select a certification");
            return;
        }

        String certificationId = getCertificationIdFromDisplay(certDisplay);
        if (certificationId == null) {
            etCertification.setError("Invalid certification selection");
            return;
        }

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

        VerificationEntity verification = new VerificationEntity(
                certificationId,
                farmId,
                type,
                verificationDate,
                verifiedBy,
                status,
                evidence,
                comment
        );

        if (correctionOfId != null) {
            verification.setSupersedesId(correctionOfId);
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        verificationRepository.insertVerification(verification, new VerificationRepository.InsertCallback() {
            @Override
            public void onSuccess(String verificationId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Verification added! ID: " + verificationId, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onVerificationAdded();
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Add Verification");
                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (verificationRepository != null) {
            verificationRepository.shutdown();
        }
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        executor.shutdown();
    }
}