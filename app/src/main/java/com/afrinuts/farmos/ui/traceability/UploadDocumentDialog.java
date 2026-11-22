package com.afrinuts.farmos.ui.traceability;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.utils.FileUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UploadDocumentDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String ARG_TRACEABILITY_ID = "traceability_id";
    private static final int REQUEST_TAKE_PHOTO = 1001;
    private static final int REQUEST_PICK_FILE = 1002;

    private long traceabilityId;
    private BatchTraceabilityEntity traceability;
    private BatchTraceabilityRepository repository;

    private TextInputEditText etDocumentName;
    private TextView tvSelectedFile;
    private Button btnChooseFile;
    private Button btnTakePhoto;
    private Button btnSave;
    private Button btnCancel;

    private Uri selectedFileUri;

    public interface OnDocumentUploadedListener {
        void onDocumentUploaded();
    }

    private OnDocumentUploadedListener listener;

    public static UploadDocumentDialog newInstance(long traceabilityId) {
        UploadDocumentDialog dialog = new UploadDocumentDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_TRACEABILITY_ID, traceabilityId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnDocumentUploadedListener(OnDocumentUploadedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            traceabilityId = getArguments().getLong(ARG_TRACEABILITY_ID);
        }

        repository = new BatchTraceabilityRepository(requireContext(), 1);

        // Load traceability
        new Thread(() -> {
            traceability = repository.getTraceabilityByIdSync(traceabilityId);
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_upload_document, null);

        initViews(view);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)

                .setCancelable(false);

        AlertDialog dialog = builder.create();
        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> uploadDocument());

        return dialog;
    }

    private void initViews(View view) {
        etDocumentName = view.findViewById(R.id.etDocumentName);
        tvSelectedFile = view.findViewById(R.id.tvSelectedFile);
        btnChooseFile = view.findViewById(R.id.btnChooseFile);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);

        // ✅ Add null checks for safety
        if (btnChooseFile != null) {
            btnChooseFile.setOnClickListener(v -> chooseFile());
        }

        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> takePhoto());
        }
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        String[] mimeTypes = {"image/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        startActivityForResult(Intent.createChooser(intent, "Select Document"), REQUEST_PICK_FILE);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            try {
                File photoFile = FileUtils.createImageFile(requireContext());
                if (photoFile != null) {
                    selectedFileUri = FileProvider.getUriForFile(requireContext(),
                            requireContext().getPackageName() + ".fileprovider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedFileUri);
                    startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                } else {
                    Toast.makeText(getContext(), "Failed to create image file", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error creating image file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getContext(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == REQUEST_PICK_FILE && data != null) {
                selectedFileUri = data.getData();
                String fileName = FileUtils.getFileName(requireContext(), selectedFileUri);
                if (tvSelectedFile != null) {
                    tvSelectedFile.setText("📎 " + fileName);
                    tvSelectedFile.setVisibility(View.VISIBLE);
                }
            } else if (requestCode == REQUEST_TAKE_PHOTO && selectedFileUri != null) {
                String fileName = new File(selectedFileUri.getPath()).getName();
                if (tvSelectedFile != null) {
                    tvSelectedFile.setText("📸 " + fileName);
                    tvSelectedFile.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void uploadDocument() {
        if (selectedFileUri == null) {
            Toast.makeText(getContext(), "Please select a document", Toast.LENGTH_SHORT).show();
            return;
        }

        String documentName = etDocumentName.getText().toString().trim();
        if (documentName.isEmpty()) {
            documentName = "Document_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        }

        // Show loading state
        if (btnChooseFile != null) btnChooseFile.setEnabled(false);
        if (btnTakePhoto != null) btnTakePhoto.setEnabled(false);
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Uploading...");
        }

        Toast.makeText(getContext(), "Uploading document...", Toast.LENGTH_SHORT).show();

        final String finalDocumentName = documentName;
        final Uri finalSelectedFileUri = selectedFileUri;

        new Thread(() -> {
            try {
                BatchTraceabilityEntity currentTraceability = repository.getTraceabilityByIdSync(traceabilityId);
                if (currentTraceability == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Traceability record not found", Toast.LENGTH_SHORT).show();
                        resetButtons();
                    });
                    return;
                }

                // TODO: Upload to Firebase Storage
                // For now, simulate upload and update traceability
                String documentUrl = finalSelectedFileUri.toString();

                BatchTraceabilityEntity.Documents docs = currentTraceability.getDocumentsObject();

                String docNameLower = finalDocumentName.toLowerCase();
                if (docNameLower.contains("quality") || docNameLower.contains("certificate")) {
                    docs.setQualityCertificate(documentUrl);
                } else if (docNameLower.contains("phyto") || docNameLower.contains("sanitary")) {
                    docs.setPhytoSanitary(documentUrl);
                } else if (docNameLower.contains("lading") || docNameLower.contains("bill")) {
                    docs.setBillOfLading(documentUrl);
                } else {
                    docs.getAdditionalDocs().add(documentUrl);
                }

                currentTraceability.setDocumentsObject(docs);
                currentTraceability.setUpdatedAt(System.currentTimeMillis());

                final BatchTraceabilityEntity finalTraceability = currentTraceability;

                repository.updateTraceability(finalTraceability, new BatchTraceabilityRepository.RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Document uploaded!", Toast.LENGTH_SHORT).show();
                                resetButtons();
                                if (listener != null) listener.onDocumentUploaded();
                                dismiss();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                                resetButtons();
                            });
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        resetButtons();
                    });
                }
            }
        }).start();
    }

    private void resetButtons() {
        if (btnChooseFile != null) {
            btnChooseFile.setEnabled(true);
        }
        if (btnTakePhoto != null) {
            btnTakePhoto.setEnabled(true);
        }
        if (btnSave != null) {
            btnSave.setEnabled(true);
            btnSave.setText("Upload");
        }
    }

    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }
}
