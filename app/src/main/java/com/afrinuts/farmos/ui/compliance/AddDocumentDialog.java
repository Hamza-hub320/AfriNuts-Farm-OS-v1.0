package com.afrinuts.farmos.ui.compliance;

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
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.DocumentRepository;
import com.afrinuts.farmos.utils.DocumentIdGenerator;
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

public class AddDocumentDialog extends DialogFragment {

    private static final String TAG = "AddDocumentDialog";
    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_CERTIFICATION_ID = "certification_id";

    private long farmId;
    private String preselectedCertificationId;
    private DocumentRepository documentRepository;
    private CertificationRepository certificationRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<CertificationEntity> certifications = new ArrayList<>();

    // UI Elements
    private MaterialAutoCompleteTextView etCertification;
    private MaterialAutoCompleteTextView etDocumentType;
    private TextInputEditText etDocumentName;
    private TextInputEditText etIssueDate;
    private TextInputEditText etExpiryDate;
    private TextInputEditText etFileUrl;
    private MaterialAutoCompleteTextView etStatus;
    private Button btnCancel;
    private Button btnSave;

    private Calendar issueCalendar = null;
    private Calendar expiryCalendar = null;

    public interface OnDocumentAddedListener {
        void onDocumentAdded();
    }

    private OnDocumentAddedListener listener;

    public static AddDocumentDialog newInstance(long farmId) {
        return newInstance(farmId, null);
    }

    public static AddDocumentDialog newInstance(long farmId, String certificationId) {
        AddDocumentDialog dialog = new AddDocumentDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        if (certificationId != null) {
            args.putString(ARG_CERTIFICATION_ID, certificationId);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDocumentAddedListener(OnDocumentAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
            preselectedCertificationId = getArguments().getString(ARG_CERTIFICATION_ID);
        }

        documentRepository = new DocumentRepository(requireContext(), farmId);
        certificationRepository = new CertificationRepository(requireContext(), farmId);
        loadCertifications();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_document, null);

        initViews(view);
        setupDropdowns();
        setupDatePickers();

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
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveDocument());
        });

        return dialog;
    }

    private void initViews(View view) {
        etCertification = view.findViewById(R.id.etCertification);
        etDocumentType = view.findViewById(R.id.etDocumentType);
        etDocumentName = view.findViewById(R.id.etDocumentName);
        etIssueDate = view.findViewById(R.id.etIssueDate);
        etExpiryDate = view.findViewById(R.id.etExpiryDate);
        etFileUrl = view.findViewById(R.id.etFileUrl);
        etStatus = view.findViewById(R.id.etStatus);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void loadCertifications() {
        executor.execute(() -> {
            try {
                certifications = certificationRepository.getCertificationsSync();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        List<String> certNames = new ArrayList<>();
                        List<String> certIds = new ArrayList<>();

                        for (CertificationEntity cert : certifications) {
                            certNames.add(cert.getCertificationName() + " (" + cert.getCertificationId() + ")");
                            certIds.add(cert.getCertificationId());
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
            } catch (Exception e) {
                Log.e(TAG, "Error loading certifications", e);
            }
        });
    }

    private void setupDropdowns() {
        // ✅ Document Type dropdown - show only display names (no icons in dropdown)
        DocumentEntity.DocumentType[] types = DocumentEntity.DocumentType.values();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );
        etDocumentType.setAdapter(typeAdapter);
        etDocumentType.setText(typeNames[0], false);

        // ✅ Status dropdown - show only display names (no icons in dropdown)
        DocumentEntity.DocumentStatus[] statuses = DocumentEntity.DocumentStatus.values();
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


    private void setupDatePickers() {
        etIssueDate.setOnClickListener(v -> showDatePicker(true));
        etIssueDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker(true);
        });

        etExpiryDate.setOnClickListener(v -> {
            if (expiryCalendar == null) {
                showDatePickerForExpiry();
            } else {
                expiryCalendar = null;
                etExpiryDate.setText("");
                etExpiryDate.setHint("Expiry Date (Optional)");
            }
        });

        etExpiryDate.setHint("Expiry Date (Optional)");
    }

    private void showDatePicker(boolean isIssueDate) {
        Calendar calendar = isIssueDate ?
                (issueCalendar != null ? issueCalendar : Calendar.getInstance()) :
                (expiryCalendar != null ? expiryCalendar : Calendar.getInstance());

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    if (isIssueDate) {
                        issueCalendar = selected;
                        updateDateLabel(etIssueDate, issueCalendar);
                    } else {
                        expiryCalendar = selected;
                        updateDateLabel(etExpiryDate, expiryCalendar);
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

    private DocumentEntity.DocumentType getTypeFromDisplay(String display) {
        for (DocumentEntity.DocumentType type : DocumentEntity.DocumentType.values()) {
            if (display.contains(type.getDisplayName())) {
                return type;
            }
        }
        return DocumentEntity.DocumentType.CERTIFICATE;
    }

    private DocumentEntity.DocumentStatus getStatusFromDisplay(String display) {
        for (DocumentEntity.DocumentStatus status : DocumentEntity.DocumentStatus.values()) {
            if (display.contains(status.getDisplayName())) {
                return status;
            }
        }
        return DocumentEntity.DocumentStatus.DRAFT;
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

    private void saveDocument() {
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

        // ✅ Verify the certification exists locally before proceeding
        boolean certificationExists = false;
        for (CertificationEntity cert : certifications) {
            if (cert.getCertificationId().equals(certificationId)) {
                certificationExists = true;
                break;
            }
        }

        if (!certificationExists) {
            Toast.makeText(getContext(), "Selected certification not found in local database. Please sync and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        String typeStr = etDocumentType.getText().toString().trim();
        DocumentEntity.DocumentType type = getTypeFromDisplay(typeStr);

        String documentName = etDocumentName.getText().toString().trim();
        if (TextUtils.isEmpty(documentName)) {
            etDocumentName.setError("Document name required");
            return;
        }

        long issueDate = issueCalendar != null ? issueCalendar.getTimeInMillis() : 0;
        if (issueDate <= 0) {
            etIssueDate.setError("Issue date is required");
            return;
        }

        long expiryDate = expiryCalendar != null ? expiryCalendar.getTimeInMillis() : 0;

        String fileUrl = etFileUrl.getText().toString().trim();
        String statusStr = etStatus.getText().toString().trim();
        DocumentEntity.DocumentStatus status = getStatusFromDisplay(statusStr);

        DocumentEntity document = new DocumentEntity(
                certificationId,
                farmId,
                type,
                documentName,
                issueDate,
                expiryDate,
                fileUrl.isEmpty() ? null : fileUrl,
                status
        );

        Log.d(TAG, "Saving document - CertId: " + certificationId + ", Issue: " + issueDate + ", Expiry: " + expiryDate);

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        documentRepository.insertDocument(document, new DocumentRepository.InsertCallback() {
            @Override
            public void onSuccess(String documentId) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "✅ Document added! ID: " + documentId, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onDocumentAdded();
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Add Document");
                        Toast.makeText(getContext(), "❌ Error: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error adding document: " + error);
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (documentRepository != null) {
            documentRepository.shutdown();
        }
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        executor.shutdown();
    }
}