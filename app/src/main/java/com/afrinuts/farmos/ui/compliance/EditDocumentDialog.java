package com.afrinuts.farmos.ui.compliance;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.repository.DocumentRepository;
import com.afrinuts.farmos.data.remote.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;

public class EditDocumentDialog extends FarmDialogFragment {

    private static final String ARG_DOCUMENT_ID = "document_id";

    private String documentId;
    private DocumentEntity document;
    private DocumentRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private MaterialAutoCompleteTextView etDocumentType;
    private TextInputEditText etDocumentName;
    private TextInputEditText etIssueDate;
    private TextInputEditText etExpiryDate;
    private TextInputEditText etFileUrl;
    private MaterialAutoCompleteTextView etStatus;
    private Button btnCancel;
    private Button btnSave;
    private MaterialButton btnUploadFile;
    private final ActivityResultLauncher<String> pickDocument = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::uploadPickedDocument);

    // ✅ Initialize calendars to NULL (not with current date)
    private Calendar issueCalendar = null;
    private Calendar expiryCalendar = null;

    public interface OnDocumentUpdatedListener {
        void onDocumentUpdated();
    }

    private OnDocumentUpdatedListener listener;

    public static EditDocumentDialog newInstance(String documentId) {
        EditDocumentDialog dialog = new EditDocumentDialog();
        Bundle args = new Bundle();
        args.putString(ARG_DOCUMENT_ID, documentId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDocumentUpdatedListener(OnDocumentUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            documentId = getArguments().getString(ARG_DOCUMENT_ID);
        }

        long farmId = 1;
        repository = new DocumentRepository(requireContext(), farmId);

        executor.execute(() -> {
            document = repository.getDocumentByIdSync(documentId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (document != null) {
                        populateFields();
                    } else {
                        Toast.makeText(getContext(), "Document not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_document, null);

        initViews(view);
        setupDropdowns();
        setupDatePickers();

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveDocument());
        btnUploadFile.setOnClickListener(v -> pickDocument.launch("*/*"));

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();
    }

    private void initViews(View view) {
        etDocumentType = view.findViewById(R.id.etDocumentType);
        etDocumentName = view.findViewById(R.id.etDocumentName);
        etIssueDate = view.findViewById(R.id.etIssueDate);
        etExpiryDate = view.findViewById(R.id.etExpiryDate);
        etFileUrl = view.findViewById(R.id.etFileUrl);
        etStatus = view.findViewById(R.id.etStatus);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
        btnUploadFile = view.findViewById(R.id.btnUploadFile);
    }

    private void uploadPickedDocument(Uri uri) {
        if (uri == null) return;
        if (document == null) {
            Toast.makeText(getContext(), "Document is still loading", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!DocumentUploadHelper.isSupported(requireContext(), uri)) {
            Toast.makeText(getContext(), "Choose a PDF or image", Toast.LENGTH_SHORT).show();
            return;
        }
        btnUploadFile.setEnabled(false);
        btnUploadFile.setText("Uploading…");
        executor.execute(() -> {
            try {
                File file = DocumentUploadHelper.copyToCache(requireContext(), uri);
                String filename = DocumentUploadHelper.fileName(requireContext(), uri);
                String path = "farms/" + document.getFarmId() + "/certification_documents/"
                        + documentId + "/" + filename;
                FirebaseManager.getInstance().uploadDocumentFile(file, path,
                        new FirebaseManager.EvidenceUploadCallback() {
                            @Override public void onSuccess(String downloadUrl) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    etFileUrl.setText(downloadUrl);
                                    btnUploadFile.setEnabled(true);
                                    btnUploadFile.setText("Upload file");
                                    file.delete();
                                });
                            }
                            @Override public void onError(String error) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    btnUploadFile.setEnabled(true);
                                    btnUploadFile.setText("Upload file");
                                    Toast.makeText(getContext(), "File upload failed", Toast.LENGTH_LONG).show();
                                    file.delete();
                                });
                            }
                        });
            } catch (Exception error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnUploadFile.setEnabled(true);
                    btnUploadFile.setText("Upload file");
                    Toast.makeText(getContext(), "Could not prepare file", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setupDropdowns() {
        // Document Type dropdown
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

        // Status dropdown
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
    }

    private void setupDatePickers() {
        etIssueDate.setOnClickListener(v -> showDatePicker(true));
        etIssueDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker(true);
        });

        etExpiryDate.setOnClickListener(v -> showDatePicker(false));
        etExpiryDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker(false);
        });
    }

    private void showDatePicker(boolean isIssueDate) {
        Calendar calendar;
        TextInputEditText editText;

        if (isIssueDate) {
            // ✅ If issueCalendar is null, create a new one with today's date
            if (issueCalendar == null) {
                issueCalendar = Calendar.getInstance();
            }
            calendar = issueCalendar;
            editText = etIssueDate;
        } else {
            // ✅ If expiryCalendar is null, create a new one with today's date
            if (expiryCalendar == null) {
                expiryCalendar = Calendar.getInstance();
            }
            calendar = expiryCalendar;
            editText = etExpiryDate;
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel(editText, calendar);
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
            editText.setHint("Select date");
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        editText.setText(sdf.format(calendar.getTime()));
    }

    private void populateFields() {
        if (document == null) return;

        // Set document type - just the display name
        etDocumentType.setText(document.getDocumentType().getDisplayName(), false);
        etDocumentName.setText(document.getDocumentName());

        // Set status - just the display name
        etStatus.setText(document.getStatus().getDisplayName(), false);

        // ✅ Set issue date (required)
        if (document.getIssueDate() > 0) {
            issueCalendar = Calendar.getInstance();
            issueCalendar.setTimeInMillis(document.getIssueDate());
            updateDateLabel(etIssueDate, issueCalendar);
        } else {
            issueCalendar = null;
            etIssueDate.setText("");
            etIssueDate.setHint("Issue Date (Required)");
        }

        // ✅ Set expiry date (optional) - ONLY if it exists
        if (document.getExpiryDate() > 0) {
            expiryCalendar = Calendar.getInstance();
            expiryCalendar.setTimeInMillis(document.getExpiryDate());
            updateDateLabel(etExpiryDate, expiryCalendar);
        } else {
            // ✅ IMPORTANT: Clear the calendar so it doesn't auto-populate
            expiryCalendar = null;
            etExpiryDate.setText("");
            etExpiryDate.setHint("Expiry Date (Optional)");
        }

        etFileUrl.setText(document.getFileUrl());
    }

    private DocumentEntity.DocumentType getDocumentTypeFromDisplay(String displayName) {
        for (DocumentEntity.DocumentType type : DocumentEntity.DocumentType.values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return DocumentEntity.DocumentType.CERTIFICATE;
    }

    private DocumentEntity.DocumentStatus getDocumentStatusFromDisplay(String displayName) {
        for (DocumentEntity.DocumentStatus status : DocumentEntity.DocumentStatus.values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        return DocumentEntity.DocumentStatus.DRAFT;
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

    private void saveDocument() {
        String typeStr = etDocumentType.getText().toString().trim();
        DocumentEntity.DocumentType type = getTypeFromDisplay(typeStr);

        String documentName = etDocumentName.getText().toString().trim();
        if (TextUtils.isEmpty(documentName)) {
            etDocumentName.setError("Document name required");
            return;
        }

        // ✅ Get issue date - required
        long issueDate = issueCalendar != null ? issueCalendar.getTimeInMillis() : 0;
        if (issueDate <= 0) {
            etIssueDate.setError("Issue date is required");
            return;
        }

        // ✅ Get expiry date - optional (0 if not set)
        long expiryDate = expiryCalendar != null ? expiryCalendar.getTimeInMillis() : 0;

        String fileUrl = etFileUrl.getText().toString().trim();
        String statusStr = etStatus.getText().toString().trim();
        DocumentEntity.DocumentStatus status = getStatusFromDisplay(statusStr);

        // ✅ Validate: expiry date must be after issue date (if set)
        if (expiryDate > 0 && expiryDate <= issueDate) {
            etExpiryDate.setError("Expiry date must be after issue date");
            return;
        }

        // Update document
        document.setDocumentType(type);
        document.setDocumentName(documentName);
        document.setIssueDate(issueDate);
        document.setExpiryDate(expiryDate);
        document.setFileUrl(fileUrl.isEmpty() ? null : fileUrl);
        document.setStatus(status);
        document.setUpdatedAt(System.currentTimeMillis());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        repository.updateDocument(document, new DocumentRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Document updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onDocumentUpdated();
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
