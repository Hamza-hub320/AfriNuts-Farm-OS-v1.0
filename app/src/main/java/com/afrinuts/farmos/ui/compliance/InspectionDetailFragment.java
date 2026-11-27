package com.afrinuts.farmos.ui.compliance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.ui.photos.PhotoDetailActivity;
import com.afrinuts.farmos.utils.DateFormatter;
import com.afrinuts.farmos.utils.SessionManager;
import com.bumptech.glide.Glide;
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

/** Renders inspection detail content directly in the Compliance tablet pane. */
public class InspectionDetailFragment extends Fragment {

    private static final String ARG_INSPECTION_ID = "inspection_id";
    private static final int REQUEST_CAMERA_PERMISSION = 7403;

    public static InspectionDetailFragment newInstance(String inspectionId) {
        InspectionDetailFragment fragment = new InspectionDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INSPECTION_ID, inspectionId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private String inspectionId;
    private long farmId = 1;
    private AppDatabase database;
    private InspectionRepository inspectionRepository;
    private CertificationRepository certificationRepository;
    private PhotoRepository photoRepository;
    private InspectionEntity inspection;
    private CertificationEntity certification;
    private Uri cameraPhotoUri;
    private File cameraPhotoFile;

    private TextView tvInspectionId, tvRequirementCategory, tvHeroSubtitle, tvStatus;
    private TextView tvStatDate, tvStatCount, tvInspectionType, tvCertification;
    private TextView tvInspectorName, tvInspectorBody, tvRemarks, tvCreatedAt, tvValidated, tvUpdatedAt, tvSuperseded;
    private TextView tvActionsCount, tvPhotosTitle;
    private LinearLayout actionsContainer, photosContainer;
    private CardView cardCorrectiveActions, cardPhotos;
    private MaterialButton btnCorrect, btnAddPhoto;
    private ImageButton btnEdit;
    private boolean canEdit;

    private final ActivityResultLauncher<Uri> takePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraPhotoFile != null && cameraPhotoFile.exists()) uploadPhoto(cameraPhotoFile);
            });
    private final ActivityResultLauncher<String> pickPicture = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                executor.execute(() -> {
                    try {
                        File file = copyPickedPhoto(uri);
                        if (isAdded()) requireActivity().runOnUiThread(() -> uploadPhoto(file));
                    } catch (IOException e) {
                        if (isAdded()) requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Could not prepare photo", Toast.LENGTH_SHORT).show());
                    }
                });
            });
    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_inspection_detail_content, container, false);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(28));
        view.setBackground(background);
        view.setClipToOutline(true);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        inspectionId = getArguments() == null ? null : getArguments().getString(ARG_INSPECTION_ID);
        database = AppDatabase.getInstance(requireContext());
        canEdit = new SessionManager(requireContext()).canEditData();
        bindViews(view);
        View scroll = ((ViewGroup) view).getChildAt(1);
        if (scroll instanceof android.widget.ScrollView) {
            android.widget.ScrollView scrollView = (android.widget.ScrollView) scroll;
            scrollView.setPadding(scrollView.getPaddingLeft(), dp(2),
                    scrollView.getPaddingRight(), scrollView.getPaddingBottom());
        }
        loadInspectionData();
    }

    private void bindViews(View view) {
        tvInspectionId = view.findViewById(R.id.tvInspectionId);
        tvRequirementCategory = view.findViewById(R.id.tvRequirementCategory);
        tvHeroSubtitle = view.findViewById(R.id.tvHeroSubtitle);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvStatDate = view.findViewById(R.id.tvStatDate);
        tvStatCount = view.findViewById(R.id.tvStatCount);
        tvInspectionType = view.findViewById(R.id.tvInspectionType);
        tvCertification = view.findViewById(R.id.tvCertification);
        tvInspectorName = view.findViewById(R.id.tvInspectorName);
        tvInspectorBody = view.findViewById(R.id.tvInspectorBody);
        tvRemarks = view.findViewById(R.id.tvRemarks);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        tvValidated = view.findViewById(R.id.tvValidated);
        tvUpdatedAt = view.findViewById(R.id.tvUpdatedAt);
        tvSuperseded = view.findViewById(R.id.tvSuperseded);
        tvActionsCount = view.findViewById(R.id.tvActionsCount);
        tvPhotosTitle = view.findViewById(R.id.tvPhotosTitle);
        actionsContainer = view.findViewById(R.id.actionsContainer);
        photosContainer = view.findViewById(R.id.photosContainer);
        cardCorrectiveActions = view.findViewById(R.id.cardCorrectiveActions);
        cardPhotos = view.findViewById(R.id.cardPhotos);
        cardPhotos.setVisibility(View.VISIBLE);
        stylePhotosCard();
        btnCorrect = view.findViewById(R.id.btnCorrect);
        btnAddPhoto = view.findViewById(R.id.btnAddPhoto);
        addDetailBottomDivider();

        View btnBack = view.findViewById(R.id.btnBack);
        btnEdit = view.findViewById(R.id.btnEdit);
        ViewGroup phoneHeader = (ViewGroup) btnBack.getParent();
        phoneHeader.removeView(btnEdit);
        phoneHeader.setVisibility(View.GONE);
        moveHeaderIntoHero(btnEdit);
        styleTabletKpis(tvStatDate);
        styleTabletText(view);
        btnEdit.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        btnAddPhoto.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        btnCorrect.setVisibility(canEdit ? View.VISIBLE : View.GONE);
        styleTabletActions(btnCorrect);
        btnEdit.setOnClickListener(v -> openCorrectionDialog());
        btnCorrect.setOnClickListener(v -> openCorrectionDialog());
        btnAddPhoto.setOnClickListener(v -> showPhotoSourceDialog());
    }

    private void moveHeaderIntoHero(ImageButton editButton) {
        ViewGroup heroContent = (ViewGroup) tvInspectionId.getParent();
        heroContent.removeView(tvInspectionId);
        heroContent.removeView(tvRequirementCategory);
        heroContent.removeView(tvHeroSubtitle);
        heroContent.removeView(tvStatus);

        FrameLayout header = new FrameLayout(requireContext());
        LinearLayout textStack = new LinearLayout(requireContext());
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.setPadding(0, 0, dp(62), 0);
        header.addView(textStack, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.START));
        tvInspectionId.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvInspectionId);
        tvRequirementCategory.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        textStack.addView(tvRequirementCategory);
        LinearLayout metadataRow = new LinearLayout(requireContext());
        metadataRow.setGravity(Gravity.CENTER_VERTICAL);
        tvHeroSubtitle.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        metadataRow.addView(tvHeroSubtitle);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.setMarginStart(dp(8));
        tvStatus.setLayoutParams(statusParams);
        tvStatus.setSingleLine(true);
        metadataRow.addView(tvStatus);
        LinearLayout.LayoutParams metadataParams = new LinearLayout.LayoutParams(-1, -2);
        metadataParams.topMargin = dp(2);
        textStack.addView(metadataRow, metadataParams);

        editButton.setImageResource(R.drawable.ic_farm_edit);
        editButton.setColorFilter(0xFF1C1B1A);
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setShape(GradientDrawable.OVAL);
        buttonBackground.setColor(0xFFF6F1E7);
        buttonBackground.setStroke(dp(1), 0xFFDDC9A4);
        editButton.setBackground(buttonBackground);
        header.addView(editButton, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END));
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        heroContent.addView(header, 0, headerParams);
        heroContent.setPadding(heroContent.getPaddingLeft(), heroContent.getPaddingTop(),
                heroContent.getPaddingRight(), dp(4));
        View heroCard = (View) heroContent.getParent();
        ViewGroup.LayoutParams cardParams = heroCard.getLayoutParams();
        if (cardParams instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) cardParams).bottomMargin = dp(4);
            heroCard.setLayoutParams(cardParams);
        }
    }

    private void styleTabletKpis(TextView firstStat) {
        ViewGroup row = (ViewGroup) firstStat.getParent().getParent().getParent();
        if (row instanceof LinearLayout) ((LinearLayout) row).setGravity(Gravity.START);
        row.post(() -> {
            int gap = dp(12);
            int width = Math.min(dp(280), Math.max(dp(240), (row.getWidth() - gap) / 2));
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (!(child instanceof CardView)) continue;
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
                params.width = width;
                params.weight = 0;
                params.setMarginStart(i == 0 ? 0 : gap);
                params.setMarginEnd(0);
                child.setLayoutParams(params);
                if (i > 0) ((CardView) child).setCardBackgroundColor(0xFFF6F1E7);
                ViewGroup content = (ViewGroup) ((CardView) child).getChildAt(0);
                if (content != null && content.getChildCount() > 1) {
                    View label = content.getChildAt(0), value = content.getChildAt(1);
                    if (label instanceof TextView) ((TextView) label).setTextSize(14);
                    if (value instanceof TextView) ((TextView) value).setTextSize(21);
                }
            }
        });
    }

    private void styleTabletText(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getCurrentTextColor() == 0xFF6B675F) text.setTextColor(0xFF59544C);
            else if (view.getId() != R.id.tvInspectionId && text.getCurrentTextColor() == 0xFF0E6B3C) {
                text.setTextColor(0xFF151413);
            } else if (view.getId() != R.id.tvInspectionId && text.getCurrentTextColor() == 0xFF1C1B1A) {
                text.setTextColor(0xFF151413);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) styleTabletText(group.getChildAt(i));
        }
        if (view.getId() == R.id.tvCreatedAt || view.getId() == R.id.tvValidated
                || view.getId() == R.id.tvUpdatedAt) view.setAlpha(1f);
    }

    private void styleTabletActions(MaterialButton correctButton) {
        LinearLayout content = (LinearLayout) correctButton.getParent();
        int buttonIndex = content.indexOfChild(correctButton);
        content.removeView(correctButton);
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        content.addView(row, buttonIndex, new LinearLayout.LayoutParams(-1, -2));
        MaterialButton editAction = new MaterialButton(requireContext());
        editAction.setText("Edit");
        editAction.setTextColor(0xFF1C1B1A);
        editAction.setAllCaps(false);
        editAction.setTextSize(16);
        editAction.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.manrope_bold));
        editAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        editAction.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE9E1CF));
        editAction.setStrokeWidth(dp(2));
        editAction.setCornerRadius(dp(999));
        editAction.setInsetTop(0);
        editAction.setInsetBottom(0);
        editAction.setOnClickListener(v -> openCorrectionDialog());
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(dp(128), dp(58));
        editParams.setMarginEnd(dp(8));
        row.addView(editAction, editParams);
        correctButton.setLayoutParams(new LinearLayout.LayoutParams(dp(172), dp(58)));
        row.addView(correctButton);
        editAction.setVisibility(canEdit ? View.VISIBLE : View.GONE);
    }

    private void addDetailBottomDivider() {
        View row = (View) tvUpdatedAt.getParent();
        if (!(row.getParent() instanceof LinearLayout)) return;
        LinearLayout details = (LinearLayout) row.getParent();
        int index = details.indexOfChild(row);
        View divider = new View(requireContext());
        divider.setBackgroundColor(0xFFE9E1CF);
        details.addView(divider, index + 1, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void loadInspectionData() {
        if (inspectionId == null || inspectionId.isEmpty()) return;
        Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                farmId = database.farmDao().getFirstFarm().getId();
                if (inspectionRepository == null) inspectionRepository = new InspectionRepository(appContext, farmId);
                if (certificationRepository == null) certificationRepository = new CertificationRepository(appContext, farmId);
                if (photoRepository == null) photoRepository = new PhotoRepository(appContext, farmId);
                inspection = inspectionRepository.getInspectionByIdSync(inspectionId);
                certification = inspection == null || inspection.getCertificationId() == null ? null
                        : certificationRepository.getCertificationByIdSync(inspection.getCertificationId());
                String validatorName = inspection == null || inspection.getValidatedAt() == null
                        ? null : resolveValidatorName(inspection.getValidatedBy());
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (inspection == null) {
                        Toast.makeText(requireContext(), "Inspection not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    displayInspectionData(validatorName);
                    loadSupersededState();
                    photoRepository.getPhotosForInspection(inspectionId)
                            .observe(getViewLifecycleOwner(), this::displayPhotos);
                });
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() -> {
                    if (isAdded()) Toast.makeText(requireContext(), "Error loading inspection", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String resolveValidatorName(String uid) {
        if (uid == null || uid.isEmpty()) return uid;
        try {
            UserEntity user = database.userDao().getUserByUid(uid);
            String name = user == null ? null : user.getName();
            return name == null || name.trim().isEmpty() ? uid : name;
        } catch (Exception ignored) { return uid; }
    }

    private void displayInspectionData(String validatorName) {
        tvInspectionId.setText(inspection.getInspectionId());
        tvRequirementCategory.setText(inspection.getRequirementCategoryDisplay() + " inspection");
        String status = inspection.getValidatedAt() != null
                && inspection.getStatus() == InspectionEntity.InspectionStatus.PASS
                ? "Validated" : inspection.getStatusDisplay();
        tvStatus.setText(status.toUpperCase(Locale.ROOT));
        updateStatusStyle();
        if (certification != null) {
            tvHeroSubtitle.setText(inspection.getRequirementCategoryDisplay() + " · "
                    + inspection.getInspectionType().getDisplayName());
            tvCertification.setText(inspection.getRequirementCategoryDisplay());
        } else {
            tvHeroSubtitle.setText(inspection.getCertificationId() == null ? "Inspection" : inspection.getCertificationId());
            tvCertification.setText(inspection.getRequirementCategoryDisplay());
        }
        tvStatDate.setText(inspection.getFormattedInspectionDate());
        List<InspectionEntity.CorrectiveAction> actions = inspection.getCorrectiveActions();
        tvStatCount.setText(String.valueOf(actions == null ? 0 : actions.size()));
        tvInspectionType.setText(inspection.getInspectionType().getDisplayName());
        tvInspectorName.setText(inspection.getInspectorName() == null ? "Not specified" : inspection.getInspectorName());
        tvInspectorBody.setText(inspection.getInspectorBody() == null ? "Not specified" : inspection.getInspectorBody());
        tvRemarks.setText(inspection.getRemarks() == null ? "No remarks" : inspection.getRemarks());
        tvCreatedAt.setText(dateFormat.format(inspection.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(inspection.getUpdatedAt()));
        tvValidated.setText(inspection.getValidatedAt() == null ? "—"
                : "Validated by " + validatorName + " on " + DateFormatter.formatDateOrDash(inspection.getValidatedAt()));
        displayCorrectiveActions();
    }

    private void updateStatusStyle() {
        int background, foreground;
        switch (inspection.getStatus()) {
            case PASS: background = 0xFFE3F0E7; foreground = 0xFF0A5530; break;
            case FAIL: background = 0xFFFCE8E6; foreground = 0xFFA12820; break;
            case CONDITIONAL_PASS:
            default: background = 0xFFF3EAD8; foreground = 0xFF59544C; break;
        }
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(background);
        pill.setCornerRadius(dp(999));
        tvStatus.setBackground(pill);
        tvStatus.setTextColor(foreground);
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
            View item = getLayoutInflater().inflate(R.layout.item_action, actionsContainer, false);
            TextView description = item.findViewById(R.id.tvActionDescription);
            TextView assignedTo = item.findViewById(R.id.tvActionAssignedTo);
            TextView status = item.findViewById(R.id.tvActionStatus);
            description.setText(action.description == null ? "No description" : action.description);
            if (action.assignedTo == null || action.assignedTo.isEmpty()) assignedTo.setVisibility(View.GONE);
            else assignedTo.setText("Assigned to: " + action.assignedTo);
            String normalized = action.status == null ? "OPEN" : action.status.toUpperCase(Locale.ROOT);
            int bg = "CLOSED".equals(normalized) ? 0xFF0A5530
                    : "IN_PROGRESS".equals(normalized) ? 0xFFF6ECD2 : 0xFFF3EAD8;
            int fg = "CLOSED".equals(normalized) ? Color.WHITE
                    : "IN_PROGRESS".equals(normalized) ? 0xFF8A6414 : 0xFF59544C;
            status.setText("IN_PROGRESS".equals(normalized) ? "IN PROGRESS" : normalized);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(bg);
            pill.setCornerRadius(dp(999));
            status.setBackground(pill);
            status.setTextColor(fg);
            final int position = i;
            if (canEdit) status.setOnClickListener(v -> showActionStatusDialog(action, position));
            actionsContainer.addView(item);
            if (i < actions.size() - 1) {
                View divider = new View(requireContext());
                divider.setBackgroundColor(0xFFE9E1CF);
                actionsContainer.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        View bottomDivider = new View(requireContext());
        bottomDivider.setBackgroundColor(0xFFE9E1CF);
        actionsContainer.addView(bottomDivider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void showActionStatusDialog(InspectionEntity.CorrectiveAction action, int position) {
        UpdateActionStatusDialog dialog = UpdateActionStatusDialog.newInstance(
                action.status == null ? "OPEN" : action.status.toUpperCase(Locale.ROOT));
        dialog.setListener(status -> updateActionStatus(position, status));
        dialog.show(getParentFragmentManager(), "UpdateActionStatusDialog");
    }

    private void updateActionStatus(int position, String newStatus) {
        List<InspectionEntity.CorrectiveAction> actions = inspection.getCorrectiveActions();
        if (actions == null || position < 0 || position >= actions.size()) return;
        actions.get(position).status = newStatus;
        inspection.setCorrectiveActions(actions);
        inspection.setUpdatedAt(System.currentTimeMillis());
        inspectionRepository.updateInspection(inspection, new InspectionRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) { if (isAdded()) displayCorrectiveActions(); }
            @Override public void onError(String error) {
                if (isAdded()) Toast.makeText(requireContext(), "Error updating status: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayPhotos(List<PhotoEntity> photos) {
        photosContainer.removeAllViews();
        cardPhotos.setVisibility(View.VISIBLE);
        tvPhotosTitle.setText("Photos");
        int size = dp(96);
        int photoCount = photos == null ? 0 : photos.size();
        for (int i = 0; i < Math.max(photoCount, 3); i++) {
            if (i >= photoCount) {
                photosContainer.addView(createPhotoPlaceholder(size));
                continue;
            }
            PhotoEntity photo = photos.get(i);
            ImageView thumbnail = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(dp(10));
            thumbnail.setLayoutParams(params);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setContentDescription("Inspection photo");
            thumbnail.setBackgroundResource(R.drawable.bg_inspection_photo_thumbnail);
            thumbnail.setClipToOutline(true);
            Glide.with(this).load(photo.getFilePath()).placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_photo_error).centerCrop().into(thumbnail);
            thumbnail.setOnClickListener(v -> {
                com.afrinuts.farmos.ui.photos.PhotoNavigation.openPhoto(requireContext(), photo.getId());
            });
            photosContainer.addView(thumbnail);
        }
    }

    private ImageView createPhotoPlaceholder(int size) {
        ImageView placeholder = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(dp(10));
        placeholder.setLayoutParams(params);
        placeholder.setScaleType(ImageView.ScaleType.CENTER);
        placeholder.setContentDescription("Empty inspection photo slot");
        placeholder.setImageResource(R.drawable.ic_photo_placeholder);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFFF6F1E7);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), 0xFFE9E1CF);
        placeholder.setBackground(background);
        placeholder.setColorFilter(0xFF8F8A80);
        placeholder.setClipToOutline(true);
        return placeholder;
    }

    private void stylePhotosCard() {
        cardPhotos.setRadius(dp(24));
        cardPhotos.setCardBackgroundColor(0xFFE9E1CF);
        cardPhotos.setContentPadding(dp(1), dp(1), dp(1), dp(1));
        View content = cardPhotos.getChildAt(0);
        if (content instanceof LinearLayout) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.WHITE);
            background.setCornerRadius(dp(23));
            content.setBackground(background);
            content.setClipToOutline(true);
        }
    }

    private void showPhotoSourceDialog() {
        PhotoSourceDialog dialog = new PhotoSourceDialog();
        dialog.setListener(source -> {
            if (source == PhotoSourceDialog.SOURCE_CAMERA) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) launchCamera();
                else cameraPermission.launch(Manifest.permission.CAMERA);
            } else pickPicture.launch("image/*");
        });
        dialog.show(getParentFragmentManager(), "InspectionPhotoSourceDialog");
    }

    private void launchCamera() {
        try {
            File directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directory == null) throw new IOException("Photo storage unavailable");
            if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create photo folder");
            cameraPhotoFile = File.createTempFile("INSPECTION_" + System.currentTimeMillis() + "_", ".jpg", directory);
            cameraPhotoUri = FileProvider.getUriForFile(requireContext(), "com.afrinuts.farmos.fileprovider", cameraPhotoFile);
            takePicture.launch(cameraPhotoUri);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not create photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private File copyPickedPhoto(Uri uri) throws IOException {
        File directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) throw new IOException("Photo storage unavailable");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Could not create photo folder");
        File file = File.createTempFile("INSPECTION_" + System.currentTimeMillis() + "_", ".jpg", directory);
        try (InputStream input = requireContext().getContentResolver().openInputStream(uri);
             OutputStream output = new FileOutputStream(file)) {
            if (input == null) throw new IOException("Cannot open selected photo");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return file;
    }

    private void uploadPhoto(File file) {
        if (inspection == null || photoRepository == null || !canEdit) return;
        PhotoEntity photo = new PhotoEntity(farmId, null, null, null,
                file.getAbsolutePath(), null, System.currentTimeMillis());
        photo.setInspectionId(inspection.getInspectionId());
        photoRepository.insertPhoto(photo, file, new PhotoRepository.PhotoUploadCallback() {
            @Override public void onSuccess(long id, String downloadUrl) { }
            @Override public void onError(String error) {
                if (isAdded()) requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "Photo saved locally; upload failed", Toast.LENGTH_LONG).show());
            }
            @Override public void onProgress(double progress) { }
        });
    }

    private void loadSupersededState() {
        InspectionEntity current = inspection;
        executor.execute(() -> {
            String supersedingId = database.inspectionDao().getSupersedingRecordId(current.getInspectionId());
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || inspection != current) return;
                if (current.getSupersedesId() != null && !current.getSupersedesId().isEmpty()) {
                    tvSuperseded.setText("Supersedes an earlier inspection. This inspection replaces "
                            + current.getSupersedesId() + ". The earlier record stays on file and is never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else if (supersedingId != null) {
                    tvSuperseded.setText("This inspection was superseded by " + supersedingId
                            + ". It stays on file and is never deleted.");
                    tvSuperseded.setVisibility(View.VISIBLE);
                } else tvSuperseded.setVisibility(View.GONE);
            });
        });
    }

    private void openCorrectionDialog() {
        if (inspection == null || !canEdit) return;
        AddInspectionDialog dialog = AddInspectionDialog.newInstanceCorrection(farmId, inspection.getInspectionId());
        dialog.setOnInspectionAddedListener(this::loadInspectionData);
        dialog.show(getParentFragmentManager(), "AddInspectionCorrectionDialog");
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (inspectionRepository != null) inspectionRepository.shutdown();
        if (certificationRepository != null) certificationRepository.shutdown();
        if (photoRepository != null) photoRepository.shutdown();
        executor.shutdown();
    }
}
