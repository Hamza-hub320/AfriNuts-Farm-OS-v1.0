package com.afrinuts.farmos.ui.compliance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.photos.PhotoDetailActivity;
import com.afrinuts.farmos.utils.DateFormatter;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InspectionDetailActivity extends BaseActivity {

    public static final String EXTRA_INSPECTION_ID = "inspection_id";

    private String inspectionId;
    private InspectionEntity inspection;
    private CertificationEntity certification;
    private InspectionRepository inspectionRepository;
    private CertificationRepository certificationRepository;
    private AppDatabase database;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView tvInspectionId;
    private TextView tvRequirementCategory;
    private TextView tvStatus;
    private TextView tvCertification;
    private TextView tvInspectionType;
    private TextView tvInspectorName;
    private TextView tvInspectorBody;
    private TextView tvRemarks;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private TextView tvValidated;
    private TextView tvActionsCount;
    private LinearLayout actionsContainer;
    private CardView cardCorrectiveActions;
    private CardView cardPhotos;
    private LinearLayout photosContainer;
    private TextView tvPhotosTitle;
    private PhotoRepository photoRepository;
    private String currentPhotoPath;
    private static final int REQUEST_IMAGE_CAPTURE = 7401;
    private static final int REQUEST_IMAGE_PICK = 7402;
    private static final int REQUEST_CAMERA_PERMISSION = 7403;
    private MaterialButton btnCorrect;
    private TextView tvSuperseded;
    private TextView tvStatDate, tvStatCount, tvHeroSubtitle;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection_detail);
        com.afrinuts.farmos.ui.TabletPane.attachList(this, savedInstanceState, new com.afrinuts.farmos.ui.compliance.InspectionListFragment());

        inspectionId = getIntent().getStringExtra(EXTRA_INSPECTION_ID);
        if (inspectionId == null || inspectionId.isEmpty()) {
            Toast.makeText(this, "Inspection not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        initViews();
        setupToolbar();
        loadInspectionData();
    }

    private void initViews() {
        tvStatDate = findViewById(R.id.tvStatDate);
        tvStatCount = findViewById(R.id.tvStatCount);
        tvHeroSubtitle = findViewById(R.id.tvHeroSubtitle);
        tvInspectionId = findViewById(R.id.tvInspectionId);
        tvRequirementCategory = findViewById(R.id.tvRequirementCategory);
        tvStatus = findViewById(R.id.tvStatus);
        tvCertification = findViewById(R.id.tvCertification);
        tvInspectionType = findViewById(R.id.tvInspectionType);
        tvInspectorName = findViewById(R.id.tvInspectorName);
        tvInspectorBody = findViewById(R.id.tvInspectorBody);
        tvRemarks = findViewById(R.id.tvRemarks);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvValidated = findViewById(R.id.tvValidated);
        tvActionsCount = findViewById(R.id.tvActionsCount);
        actionsContainer = findViewById(R.id.actionsContainer);
        cardCorrectiveActions = findViewById(R.id.cardCorrectiveActions);
        cardPhotos = findViewById(R.id.cardPhotos);
        photosContainer = findViewById(R.id.photosContainer);
        tvPhotosTitle = findViewById(R.id.tvPhotosTitle);
        tvSuperseded = findViewById(R.id.tvSuperseded);

        btnCorrect = findViewById(R.id.btnCorrect);
        btnCorrect.setOnClickListener(v -> openCorrectionDialog());
        findViewById(R.id.btnAddPhoto).setOnClickListener(v -> showPhotoSourceDialog());

        loadSupersededState();
    }

    private void setupToolbar() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnEdit).setOnClickListener(v -> openCorrectionDialog());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadInspectionData() {
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                inspectionRepository = new InspectionRepository(this, farmId);
                certificationRepository = new CertificationRepository(this, farmId);

                inspection = inspectionRepository.getInspectionByIdSync(inspectionId);
                photoRepository = new PhotoRepository(this, farmId);

                String validatedByDisplayName = inspection == null || inspection.getValidatedAt() == null
                        ? null : resolveValidatorName(inspection.getValidatedBy());

                if (inspection != null && inspection.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(inspection.getCertificationId());
                }

                runOnUiThread(() -> {
                    if (inspection != null) {
                        displayInspectionData(validatedByDisplayName);
                        observeInspectionPhotos();
                        loadSupersededState();
                    } else {
                        Toast.makeText(this, "Inspection not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading inspection", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void observeInspectionPhotos() {
        if (inspection == null || photoRepository == null) return;
        photoRepository.getPhotosForInspection(inspection.getInspectionId())
                .observe(this, photos -> displayPhotos(photos));
    }

    private void displayPhotos(List<PhotoEntity> photos) {
        photosContainer.removeAllViews();
        if (photos == null || photos.isEmpty()) {
            cardPhotos.setVisibility(View.GONE);
            return;
        }
        cardPhotos.setVisibility(View.VISIBLE);
        tvPhotosTitle.setText("Photos (" + photos.size() + ")");
        int size = dp(96);
        for (PhotoEntity photo : photos) {
            ImageView thumbnail = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(dp(8));
            thumbnail.setLayoutParams(params);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setContentDescription("Inspection photo");
            thumbnail.setBackgroundResource(R.drawable.bg_icon_sand);
            thumbnail.setClipToOutline(true);
            Glide.with(this).load(photo.getFilePath()).placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_photo_error).centerCrop().into(thumbnail);
            thumbnail.setOnClickListener(v -> {
                Intent intent = new Intent(this, PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId());
                startActivity(intent);
            });
            photosContainer.addView(thumbnail);
        }
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private void showPhotoSourceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Photo")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) dispatchTakePictureIntent(); else dispatchPickPictureIntent();
                }).show();
    }

    private void dispatchTakePictureIntent() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (capture.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File image = createImageFile();
            Uri uri = FileProvider.getUriForFile(this, "com.afrinuts.farmos.fileprovider", image);
            capture.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            capture.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(capture, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            Log.e("InspectionPhotos", "Unable to create photo file", e);
            Toast.makeText(this, "Could not create photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) throw new IOException("Photo storage is unavailable");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create photo folder");
        File file = File.createTempFile("INSPECTION_" + System.currentTimeMillis() + "_", ".jpg", directory);
        currentPhotoPath = file.getAbsolutePath();
        return file;
    }

    private void dispatchPickPictureIntent() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        startActivityForResult(pick, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
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
                Toast.makeText(this, "Could not read the selected photo", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadInspectionPhoto(image, inspection.getInspectionId());
        } catch (Exception e) {
            Log.e("InspectionPhotos", "Could not prepare inspection photo", e);
            Toast.makeText(this, "Could not prepare photo", Toast.LENGTH_SHORT).show();
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
        try (InputStream input = getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(file)) {
            if (input == null) throw new IOException("Cannot open selected photo");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return file;
    }

    private void uploadInspectionPhoto(File file, String linkedInspectionId) {
        PhotoEntity photo = new PhotoEntity(farmId, null, null, null,
                file.getAbsolutePath(), null, System.currentTimeMillis());
        photo.setInspectionId(linkedInspectionId);
        photoRepository.insertPhoto(photo, file, new PhotoRepository.PhotoUploadCallback() {
            @Override public void onSuccess(long id, String downloadUrl) { }
            @Override public void onError(String error) {
                Log.e("InspectionPhotos", "Photo upload failed: " + error);
                runOnUiThread(() -> Toast.makeText(InspectionDetailActivity.this,
                        "Photo saved locally; upload failed", Toast.LENGTH_LONG).show());
            }
            @Override public void onProgress(double progress) { }
        });
    }

    private void loadSupersededState() {
        if (inspection == null) return;
        executor.execute(() -> {
            String supersedingId = database.inspectionDao()
                    .getSupersedingRecordId(inspection.getInspectionId());
            runOnUiThread(() -> {
                if (inspection.getSupersedesId() != null && !inspection.getSupersedesId().isEmpty()) {
                    tvSuperseded.setText("Supersedes an earlier inspection. This inspection replaces "
                            + inspection.getSupersedesId() + ". The earlier record stays on file and is never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else if (supersedingId != null) {
                    tvSuperseded.setText("This inspection was superseded by " + supersedingId + ". It stays on file and is never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else {
                    tvSuperseded.setVisibility(View.GONE);
                }
            });
        });
    }

    private String resolveValidatorName(String uid) {
        if (uid == null || uid.isEmpty()) return uid;
        try {
            UserEntity validator = database.userDao().getUserByUid(uid);
            String name = validator != null ? validator.getName() : null;
            return name != null && !name.trim().isEmpty() ? name : uid;
        } catch (Exception ignored) {
            return uid;
        }
    }

    private void displayInspectionData(String validatedByDisplayName) {
        // Basic Info
        tvInspectionId.setText(inspection.getInspectionId());
        tvRequirementCategory.setText(inspection.getRequirementCategoryDisplay() + " inspection");

        // Status with color
        String statusLabel = inspection.getValidatedAt() != null
                && inspection.getStatus() == InspectionEntity.InspectionStatus.PASS
                ? "Validated" : inspection.getStatusDisplay();
        tvStatus.setText(statusLabel);
        updateStatusColor();

        // Certification
        if (certification != null) {
            String certificationName = certification.getCertificationName();
            String certificationBody = certification.getCertificationBody();
            tvHeroSubtitle.setText(inspection.getRequirementCategoryDisplay() + " · " + inspection.getInspectionType().getDisplayName());
            tvCertification.setText(inspection.getRequirementCategoryDisplay());
        } else {
            tvHeroSubtitle.setText(inspection.getCertificationId() != null ? inspection.getCertificationId() : "Inspection");
            tvCertification.setText(inspection.getRequirementCategoryDisplay());
        }

        // Inspection Type
        tvInspectionType.setText(inspection.getInspectionType().getDisplayName());

        // Dates
        tvStatDate.setText(inspection.getFormattedInspectionDate());
        List<InspectionEntity.CorrectiveAction> actions = inspection.getCorrectiveActions();
        tvStatCount.setText(String.valueOf(actions == null ? 0 : actions.size()));

        // Inspector
        tvInspectorName.setText(inspection.getInspectorName() != null ? inspection.getInspectorName() : "Not specified");
        tvInspectorBody.setText(inspection.getInspectorBody() != null ? inspection.getInspectorBody() : "Not specified");

        // Remarks
        tvRemarks.setText(inspection.getRemarks() != null ? inspection.getRemarks() : "No remarks");

        // Timestamps
        tvCreatedAt.setText(dateFormat.format(inspection.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(inspection.getUpdatedAt()));

        // Validation
        if (inspection.getValidatedAt() == null) {
            tvValidated.setText("—");
        } else {
            tvValidated.setText("Validated by " + validatedByDisplayName
                    + " on " + DateFormatter.formatDateOrDash(inspection.getValidatedAt()));
        }

        // Corrective Actions
        displayCorrectiveActions();
    }

    private void updateStatusColor() {
        int backgroundColor;
        int textColor;
        switch (inspection.getStatus()) {
            case PASS:
                backgroundColor = android.graphics.Color.parseColor("#E3F0E7");
                textColor = android.graphics.Color.parseColor("#0A5530");
                break;
            case FAIL:
                backgroundColor = android.graphics.Color.parseColor("#FCE8E6");
                textColor = android.graphics.Color.parseColor("#A12820");
                break;
            case CONDITIONAL_PASS:
            default:
                backgroundColor = android.graphics.Color.parseColor("#F3EAD8");
                textColor = android.graphics.Color.parseColor("#59544C");
                break;
        }
        android.graphics.drawable.GradientDrawable pill = new android.graphics.drawable.GradientDrawable();
        pill.setColor(backgroundColor);
        pill.setCornerRadius(getResources().getDisplayMetrics().density * 999f);
        tvStatus.setBackground(pill);
        tvStatus.setTextColor(textColor);
    }

    private void displayCorrectiveActions() {
        List<InspectionEntity.CorrectiveAction> actions = inspection.getCorrectiveActions();

        if (actions == null || actions.isEmpty()) {
            cardCorrectiveActions.setVisibility(View.GONE);
            return;
        }

        cardCorrectiveActions.setVisibility(View.VISIBLE);
        tvActionsCount.setText("(" + actions.size() + ")");

        actionsContainer.removeAllViews();

        for (int i = 0; i < actions.size(); i++) {
            InspectionEntity.CorrectiveAction action = actions.get(i);
            View actionView = getLayoutInflater().inflate(R.layout.item_action, actionsContainer, false);

            TextView tvDescription = actionView.findViewById(R.id.tvActionDescription);
            TextView tvAssignedTo = actionView.findViewById(R.id.tvActionAssignedTo);
            TextView tvActionStatus = actionView.findViewById(R.id.tvActionStatus);

            tvDescription.setText(action.description != null ? action.description : "No description");

            if (action.assignedTo != null && !action.assignedTo.isEmpty()) {
                tvAssignedTo.setText("Assigned to: " + action.assignedTo);
                tvAssignedTo.setVisibility(View.VISIBLE);
            } else {
                tvAssignedTo.setVisibility(View.GONE);
            }

            // Get status with proper display
            String status = action.status != null ? action.status.toUpperCase() : "OPEN";
            String statusDisplay;
            int statusBackgroundColor;
            int statusTextColor;
            int colorRes;

            switch (status) {
                case "CLOSED":
                    statusDisplay = "CLOSED";
                    statusBackgroundColor = android.graphics.Color.parseColor("#0A5530");
                    statusTextColor = android.graphics.Color.WHITE;
                    break;
                case "IN_PROGRESS":
                    statusDisplay = "IN PROGRESS";
                    statusBackgroundColor = android.graphics.Color.parseColor("#F6ECD2");
                    statusTextColor = android.graphics.Color.parseColor("#8A6414");
                    break;
                default:
                    statusDisplay = "OPEN";
                    statusBackgroundColor = android.graphics.Color.parseColor("#F3EAD8");
                    statusTextColor = android.graphics.Color.parseColor("#59544C");
                    break;
            }

            tvActionStatus.setText(statusDisplay);
            android.graphics.drawable.GradientDrawable statusPill = new android.graphics.drawable.GradientDrawable();
            statusPill.setColor(statusBackgroundColor);
            statusPill.setCornerRadius(getResources().getDisplayMetrics().density * 999f);
            tvActionStatus.setBackground(statusPill);
            tvActionStatus.setTextColor(statusTextColor);

            // Make the status clickable to update it
            final int position = i;
            tvActionStatus.setOnClickListener(v -> showStatusUpdateDialog(action, position));

            actionsContainer.addView(actionView);
        }
    }

    private void showStatusUpdateDialog(InspectionEntity.CorrectiveAction action, int position) {
        String currentStatus = action.status != null ? action.status.toUpperCase() : "OPEN";
        UpdateActionStatusDialog dialog = UpdateActionStatusDialog.newInstance(currentStatus);
        dialog.setListener(newStatus -> updateActionStatus(position, newStatus));
        dialog.show(getSupportFragmentManager(), "UpdateActionStatusDialog");
    }

    private void updateActionStatus(int position, String newStatus) {
        List<InspectionEntity.CorrectiveAction> actions = inspection.getCorrectiveActions();

        if (position >= 0 && position < actions.size()) {
            InspectionEntity.CorrectiveAction action = actions.get(position);
            action.status = newStatus;

            // Update the inspection
            inspection.setCorrectiveActions(actions);
            inspection.setUpdatedAt(System.currentTimeMillis());

            // Show loading
            Toast.makeText(this, "Updating status...", Toast.LENGTH_SHORT).show();

            inspectionRepository.updateInspection(inspection, new InspectionRepository.RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    runOnUiThread(() -> {
                        Toast.makeText(InspectionDetailActivity.this,
                                "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                        // Refresh the display
                        displayCorrectiveActions();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(InspectionDetailActivity.this,
                                    "❌ Error updating status: " + error, Toast.LENGTH_LONG).show()
                    );
                }
            });
        }
    }

    private void openCorrectionDialog() {
        AddInspectionDialog correction = AddInspectionDialog.newInstanceCorrection(farmId, inspection.getInspectionId());
        correction.setOnInspectionAddedListener(this::loadInspectionData);
        correction.show(getSupportFragmentManager(), "AddInspectionCorrectionDialog");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (inspectionRepository != null) {
            inspectionRepository.shutdown();
        }
        if (certificationRepository != null) {
            certificationRepository.shutdown();
        }
        executor.shutdown();
    }
}
