package com.afrinuts.farmos.ui.compliance;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.DateFormatter;
import com.google.android.material.button.MaterialButton;

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
        tvSuperseded = findViewById(R.id.tvSuperseded);

        btnCorrect = findViewById(R.id.btnCorrect);
        btnCorrect.setOnClickListener(v -> openCorrectionDialog());

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

                String validatedByDisplayName = inspection == null || inspection.getValidatedAt() == null
                        ? null : resolveValidatorName(inspection.getValidatedBy());

                if (inspection != null && inspection.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(inspection.getCertificationId());
                }

                runOnUiThread(() -> {
                    if (inspection != null) {
                        displayInspectionData(validatedByDisplayName);
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
