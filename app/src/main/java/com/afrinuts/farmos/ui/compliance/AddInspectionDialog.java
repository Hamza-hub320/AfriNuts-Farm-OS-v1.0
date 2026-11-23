package com.afrinuts.farmos.ui.compliance;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddInspectionDialog extends FarmDialogFragment {

    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_CERTIFICATION_ID = "certification_id";
    private static final String ARG_CORRECTION_OF = "correction_of";

    private long farmId;
    private String preselectedCertificationId;
    private String correctionOfId;
    private InspectionRepository inspectionRepository;
    private CertificationRepository certificationRepository;
    private PhotoRepository photoRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private List<CertificationEntity> certifications = new ArrayList<>();

    // UI Elements
    private MaterialAutoCompleteTextView etCertification;
    private MaterialAutoCompleteTextView etRequirementCategory;
    private MaterialAutoCompleteTextView etInspectionType;
    private TextInputEditText etInspectionDate;
    private TextInputEditText etInspectorName;
    private TextInputEditText etInspectorBody;
    private MaterialAutoCompleteTextView etStatus;
    private TextInputEditText etRemarks;
    private LinearLayout correctiveActionsContainer;
    private Button btnAddCorrectiveAction;
    private TextInputEditText etReportUrl;
    private LinearLayout inspectionPhotoPreviewContainer;
    private final List<File> pendingPhotoFiles = new ArrayList<>();
    private String currentPhotoPath;
    private static final int REQUEST_IMAGE_CAPTURE = 7501;
    private static final int REQUEST_IMAGE_PICK = 7502;
    private static final int REQUEST_CAMERA_PERMISSION = 7503;

    private Button btnCancel;
    private Button btnSave;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnInspectionAddedListener {
        void onInspectionAdded();
    }

    private OnInspectionAddedListener listener;

    public static AddInspectionDialog newInstance(long farmId) {
        return newInstance(farmId, null);
    }

    public static AddInspectionDialog newInstance(long farmId, String certificationId) {
        AddInspectionDialog dialog = new AddInspectionDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        if (certificationId != null) {
            args.putString(ARG_CERTIFICATION_ID, certificationId);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public static AddInspectionDialog newInstanceCorrection(long farmId, String originalInspectionId) {
        AddInspectionDialog dialog = new AddInspectionDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        args.putString(ARG_CORRECTION_OF, originalInspectionId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnInspectionAddedListener(OnInspectionAddedListener listener) {
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

        inspectionRepository = new InspectionRepository(requireContext(), farmId);
        certificationRepository = new CertificationRepository(requireContext(), farmId);
        photoRepository = new PhotoRepository(requireContext(), farmId);
        loadCertifications();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_inspection, null);

        initViews(view);
        setupDropdowns();
        setupDatePicker();
        setupCorrectiveActions();
        setupPhotoAttachments(view);

        if (correctionOfId != null) {
            prefillFromOriginal(correctionOfId);
        }

        android.widget.TextView dialogTitle = view.findViewById(R.id.tvDialogTitle);
        if (correctionOfId != null && dialogTitle != null) {
            dialogTitle.setText("Correcting " + correctionOfId);
        }
        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setText("Save");
        btnSave.setOnClickListener(v -> saveInspection());

        return new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false)
                .create();
    }

    private void prefillFromOriginal(String originalId) {
        executor.execute(() -> {
            InspectionEntity original = inspectionRepository.getInspectionByIdSync(originalId);
            if (original == null || getActivity() == null) return;

            List<CertificationEntity> certs = certificationRepository.getCertificationsSync();
            String certDisplay = null;
            for (CertificationEntity c : certs) {
                if (c.getCertificationId().equals(original.getCertificationId())) {
                    certDisplay = c.getCertificationName() + " (" + c.getCertificationId() + ")";
                    break;
                }
            }
            final String certDisplayFinal = certDisplay;

            getActivity().runOnUiThread(() -> {
                if (certDisplayFinal != null) {
                    etCertification.setText(certDisplayFinal, false);
                }
                etRequirementCategory.setText(original.getRequirementCategoryDisplay(), false);
                etInspectionType.setText(original.getInspectionType().getDisplayName(), false);
                etStatus.setText(original.getStatusDisplay(), false);

                // Correction inherits the original inspection's event date; createdAt
                // (set in the entity constructor) records when the correction was made.
                selectedDate.setTimeInMillis(
                        original.getInspectionDate() > 0
                                ? original.getInspectionDate()
                                : System.currentTimeMillis());
                updateDateLabel();

                etInspectorName.setText(original.getInspectorName() != null ? original.getInspectorName() : "");
                etInspectorBody.setText(original.getInspectorBody() != null ? original.getInspectorBody() : "");
                etRemarks.setText(original.getRemarks() != null ? original.getRemarks() : "");
                etReportUrl.setText(original.getReportUrl() != null ? original.getReportUrl() : "");

                List<InspectionEntity.CorrectiveAction> actions = original.getCorrectiveActions();
                if (actions != null) {
                    for (InspectionEntity.CorrectiveAction a : actions) {
                        addCorrectiveActionItem(a.description, a.assignedTo, a.status, a.dueDate);
                    }
                }
            });
        });
    }

    private void initViews(View view) {
        etCertification = view.findViewById(R.id.etCertification);
        etRequirementCategory = view.findViewById(R.id.etRequirementCategory);
        etInspectionType = view.findViewById(R.id.etInspectionType);
        etInspectionDate = view.findViewById(R.id.etInspectionDate);
        etInspectorName = view.findViewById(R.id.etInspectorName);
        etInspectorBody = view.findViewById(R.id.etInspectorBody);
        etStatus = view.findViewById(R.id.etStatus);
        etRemarks = view.findViewById(R.id.etRemarks);
        correctiveActionsContainer = view.findViewById(R.id.correctiveActionsContainer);
        btnAddCorrectiveAction = view.findViewById(R.id.btnAddCorrectiveAction);
        etReportUrl = view.findViewById(R.id.etReportUrl);
        inspectionPhotoPreviewContainer = view.findViewById(R.id.inspectionPhotoPreviewContainer);
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
        InspectionEntity.RequirementCategory[] categories = InspectionEntity.RequirementCategory.values();
        String[] categoryNames = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            categoryNames[i] = categories[i].getDisplayName();
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
        );
        etRequirementCategory.setAdapter(categoryAdapter);
        etRequirementCategory.setText(categoryNames[0], false);

        InspectionEntity.InspectionType[] types = InspectionEntity.InspectionType.values();
        String[] typeNames = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getDisplayName();
        }
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );
        etInspectionType.setAdapter(typeAdapter);
        etInspectionType.setText(typeNames[0], false);

        InspectionEntity.InspectionStatus[] statuses = InspectionEntity.InspectionStatus.values();
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
        etInspectionDate.setOnClickListener(v -> showDatePicker());
        etInspectionDate.setOnFocusChangeListener((v, hasFocus) -> {
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
        etInspectionDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupCorrectiveActions() {
        btnAddCorrectiveAction.setOnClickListener(v -> addCorrectiveActionItem(null, null));
    }

    private void setupPhotoAttachments(View view) {
        view.findViewById(R.id.btnAddInspectionPhoto).setOnClickListener(v -> showPhotoSourceDialog());
    }

    private void showPhotoSourceDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Photo")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) dispatchTakePictureIntent(); else dispatchPickPictureIntent();
                }).show();
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (capture.resolveActivity(requireContext().getPackageManager()) == null) {
            Toast.makeText(getContext(), "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File image = createImageFile();
            Uri uri = FileProvider.getUriForFile(requireContext(), "com.afrinuts.farmos.fileprovider", image);
            capture.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            capture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(capture, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            Log.e("AddInspectionDialog", "Unable to create photo file", e);
            Toast.makeText(getContext(), "Could not create photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchPickPictureIntent() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        startActivityForResult(pick, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        File directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) throw new IOException("Photo storage is unavailable");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create photo folder");
        File file = File.createTempFile("INSPECTION_PENDING_" + System.currentTimeMillis() + "_", ".jpg", directory);
        currentPhotoPath = file.getAbsolutePath();
        return file;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(getContext(), "Camera permission is required to take photos", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK) return;
        try {
            File image;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                image = currentPhotoPath == null ? null : new File(currentPhotoPath);
                if ((image == null || !image.exists() || image.length() == 0) && data != null && data.getExtras() != null) {
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    if (bitmap != null) image = saveBitmap(bitmap);
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                image = copyPickedPhoto(data.getData());
            } else return;
            if (image == null || !image.exists() || image.length() == 0) {
                Toast.makeText(getContext(), "Could not read the selected photo", Toast.LENGTH_SHORT).show();
                return;
            }
            pendingPhotoFiles.add(image);
            addPhotoPreview(image);
        } catch (Exception e) {
            Log.e("AddInspectionDialog", "Could not prepare inspection photo", e);
            Toast.makeText(getContext(), "Could not prepare photo", Toast.LENGTH_SHORT).show();
        }
    }

    private File saveBitmap(Bitmap bitmap) throws IOException {
        File file = createImageFile();
        try (FileOutputStream output = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
        }
        return file;
    }

    private File copyPickedPhoto(Uri uri) throws IOException {
        File file = createImageFile();
        try (InputStream input = requireContext().getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(file)) {
            if (input == null) throw new IOException("Cannot open selected photo");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return file;
    }

    private void addPhotoPreview(File image) {
        int size = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72,
                getResources().getDisplayMetrics());
        ImageView thumbnail = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                getResources().getDisplayMetrics()));
        thumbnail.setLayoutParams(params);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setContentDescription("Selected inspection photo");
        Glide.with(this).load(image).centerCrop().into(thumbnail);
        inspectionPhotoPreviewContainer.addView(thumbnail);
    }

    private void uploadPendingPhotos(String inspectionId) {
        for (File file : new ArrayList<>(pendingPhotoFiles)) {
            PhotoEntity photo = new PhotoEntity(farmId, null, null, null,
                    file.getAbsolutePath(), null, System.currentTimeMillis());
            photo.setInspectionId(inspectionId);
            photoRepository.insertPhoto(photo, file, new PhotoRepository.PhotoUploadCallback() {
                @Override public void onSuccess(long id, String downloadUrl) { }
                @Override public void onError(String error) {
                    Log.e("AddInspectionDialog", "Inspection photo upload failed: " + error);
                }
                @Override public void onProgress(double progress) { }
            });
        }
        pendingPhotoFiles.clear();
    }

    private void addCorrectiveActionItem(String description, String assignedTo) {
        addCorrectiveActionItem(description, assignedTo, "OPEN", 0L);
    }

    private void addCorrectiveActionItem(String description, String assignedTo, String status) {
        addCorrectiveActionItem(description, assignedTo, status, 0L);
    }

    private void addCorrectiveActionItem(String description, String assignedTo,
                                         String status, long dueDate) {
        View actionView = getLayoutInflater().inflate(R.layout.item_corrective_action_input, correctiveActionsContainer, false);

        TextInputEditText etDescription = actionView.findViewById(R.id.etActionDescription);
        TextInputEditText etAssignedTo = actionView.findViewById(R.id.etAssignedTo);
        TextInputEditText etDueDate = actionView.findViewById(R.id.etActionDueDate);
        Button btnRemove = actionView.findViewById(R.id.btnRemoveAction);

        if (description != null) etDescription.setText(description);
        if (assignedTo != null) etAssignedTo.setText(assignedTo);

        actionView.setTag(R.id.tag_action_status, status != null ? status : "OPEN");
        actionView.setTag(R.id.tag_action_due_date, dueDate);
        if (dueDate > 0) {
            etDueDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(dueDate)));
        }

        etDueDate.setOnClickListener(v -> {
            long current = (Long) actionView.getTag(R.id.tag_action_due_date);
            Calendar c = Calendar.getInstance();
            if (current > 0) c.setTimeInMillis(current);
            new DatePickerDialog(requireContext(),
                    (dp, year, month, day) -> {
                        Calendar picked = Calendar.getInstance();
                        picked.set(year, month, day, 0, 0, 0);
                        picked.set(Calendar.MILLISECOND, 0);
                        long millis = picked.getTimeInMillis();
                        actionView.setTag(R.id.tag_action_due_date, millis);
                        etDueDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(millis)));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        btnRemove.setOnClickListener(v -> correctiveActionsContainer.removeView(actionView));
        correctiveActionsContainer.addView(actionView);
    }

    private List<InspectionEntity.CorrectiveAction> collectCorrectiveActions() {
        List<InspectionEntity.CorrectiveAction> result = new ArrayList<>();

        for (int i = 0; i < correctiveActionsContainer.getChildCount(); i++) {
            View actionView = correctiveActionsContainer.getChildAt(i);
            TextInputEditText etDescription = actionView.findViewById(R.id.etActionDescription);
            TextInputEditText etAssignedTo = actionView.findViewById(R.id.etAssignedTo);

            String description = etDescription.getText().toString().trim();
            if (!TextUtils.isEmpty(description)) {
                InspectionEntity.CorrectiveAction action = new InspectionEntity.CorrectiveAction(
                        description,
                        etAssignedTo.getText().toString().trim()
                );
                Object statusTag = actionView.getTag(R.id.tag_action_status);
                if (statusTag instanceof String) action.status = (String) statusTag;
                Object dueTag = actionView.getTag(R.id.tag_action_due_date);
                if (dueTag instanceof Long) action.dueDate = (Long) dueTag;
                result.add(action);
            }
        }

        return result;
    }

    private InspectionEntity.RequirementCategory getCategoryFromDisplay(String display) {
        for (InspectionEntity.RequirementCategory category : InspectionEntity.RequirementCategory.values()) {
            if (display.contains(category.getDisplayName())) {
                return category;
            }
        }
        return InspectionEntity.RequirementCategory.FIELD;
    }

    private InspectionEntity.InspectionType getTypeFromDisplay(String display) {
        for (InspectionEntity.InspectionType type : InspectionEntity.InspectionType.values()) {
            if (display.equals(type.getDisplayName())) {
                return type;
            }
        }
        return InspectionEntity.InspectionType.EXTERNAL;
    }

    private InspectionEntity.InspectionStatus getStatusFromDisplay(String display) {
        for (InspectionEntity.InspectionStatus status : InspectionEntity.InspectionStatus.values()) {
            if (display.equals(status.getDisplayName())) {
                return status;
            }
        }
        return InspectionEntity.InspectionStatus.PASS;
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

    private void saveInspection() {
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

        String certificationName = etCertification.getText().toString().trim();
        int nameEnd = certificationName.indexOf(" (");
        if (nameEnd != -1) {
            certificationName = certificationName.substring(0, nameEnd);
        }

        String categoryStr = etRequirementCategory.getText().toString().trim();
        InspectionEntity.RequirementCategory category = getCategoryFromDisplay(categoryStr);

        String typeStr = etInspectionType.getText().toString().trim();
        InspectionEntity.InspectionType type = getTypeFromDisplay(typeStr);

        long inspectionDate = selectedDate.getTimeInMillis();
        if (inspectionDate <= 0) {
            etInspectionDate.setError("Please select an inspection date");
            return;
        }

        String inspectorName = etInspectorName.getText().toString().trim();
        if (TextUtils.isEmpty(inspectorName)) {
            etInspectorName.setError("Inspector name required");
            return;
        }

        String inspectorBody = etInspectorBody.getText().toString().trim();
        if (TextUtils.isEmpty(inspectorBody)) {
            etInspectorBody.setError("Inspector body required");
            return;
        }

        String statusStr = etStatus.getText().toString().trim();
        InspectionEntity.InspectionStatus status = getStatusFromDisplay(statusStr);

        String remarks = etRemarks.getText().toString().trim();
        List<InspectionEntity.CorrectiveAction> correctiveActions = collectCorrectiveActions();
        String reportUrl = etReportUrl.getText().toString().trim();

        InspectionEntity inspection = new InspectionEntity(
                certificationId,
                certificationName,
                farmId,
                category,
                type,
                inspectionDate,
                inspectorName,
                inspectorBody,
                status,
                remarks,
                correctiveActions,
                null, // photos
                reportUrl.isEmpty() ? null : reportUrl
        );

        if (correctionOfId != null) {
            inspection.setSupersedesId(correctionOfId);
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        inspectionRepository.insertInspection(inspection, new InspectionRepository.InsertCallback() {
            @Override
            public void onSuccess(String inspectionId) {
                uploadPendingPhotos(inspectionId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String label = correctionOfId != null ? "Correction added! ID: " : "Inspection added! ID: ";
                        Toast.makeText(getContext(), label + inspectionId, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onInspectionAdded();
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
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (inspectionRepository != null) {
            inspectionRepository.shutdown();
        }
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        if (photoRepository != null) {
            photoRepository.shutdown();
        }
        executor.shutdown();
    }
}
