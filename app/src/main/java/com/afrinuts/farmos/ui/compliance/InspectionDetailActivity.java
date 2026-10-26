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
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

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
    private TextView tvInspectionDate;
    private TextView tvInspectorName;
    private TextView tvInspectorBody;
    private TextView tvRemarks;
    private TextView tvReportUrl;
    private TextView tvCreatedAt;
    private TextView tvUpdatedAt;
    private TextView tvActionsCount;
    private LinearLayout actionsContainer;
    private CardView cardStatus;
    private CardView cardCorrectiveActions;

    private MaterialButton btnEdit;
    private MaterialButton btnDelete;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection_detail);

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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvInspectionId = findViewById(R.id.tvInspectionId);
        tvRequirementCategory = findViewById(R.id.tvRequirementCategory);
        tvStatus = findViewById(R.id.tvStatus);
        tvCertification = findViewById(R.id.tvCertification);
        tvInspectionType = findViewById(R.id.tvInspectionType);
        tvInspectionDate = findViewById(R.id.tvInspectionDate);
        tvInspectorName = findViewById(R.id.tvInspectorName);
        tvInspectorBody = findViewById(R.id.tvInspectorBody);
        tvRemarks = findViewById(R.id.tvRemarks);
        tvReportUrl = findViewById(R.id.tvReportUrl);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvActionsCount = findViewById(R.id.tvActionsCount);
        actionsContainer = findViewById(R.id.actionsContainer);
        cardStatus = findViewById(R.id.cardStatus);
        cardCorrectiveActions = findViewById(R.id.cardCorrectiveActions);

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v -> showEditDialog());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Inspection Details");
        }
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

                if (inspection != null && inspection.getCertificationId() != null) {
                    certification = certificationRepository.getCertificationByIdSync(inspection.getCertificationId());
                }

                runOnUiThread(() -> {
                    if (inspection != null) {
                        displayInspectionData();
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

    private void displayInspectionData() {
        // Basic Info
        tvInspectionId.setText(inspection.getInspectionId());
        tvRequirementCategory.setText(inspection.getRequirementCategoryDisplay());

        // Status with color
        tvStatus.setText(inspection.getStatusDisplay());
        updateStatusColor();

        // Certification
        if (certification != null) {
            tvCertification.setText(certification.getCertificationName() + " (" + certification.getCertificationId() + ")");
        } else {
            tvCertification.setText(inspection.getCertificationId());
        }

        // Inspection Type
        tvInspectionType.setText(inspection.getInspectionType().getDisplayName());

        // Dates
        tvInspectionDate.setText(inspection.getFormattedInspectionDate());

        // Inspector
        tvInspectorName.setText(inspection.getInspectorName() != null ? inspection.getInspectorName() : "Not specified");
        tvInspectorBody.setText(inspection.getInspectorBody() != null ? inspection.getInspectorBody() : "Not specified");

        // Remarks
        tvRemarks.setText(inspection.getRemarks() != null ? inspection.getRemarks() : "No remarks");

        // Report URL
        if (inspection.getReportUrl() != null && !inspection.getReportUrl().isEmpty()) {
            tvReportUrl.setText(inspection.getReportUrl());
            tvReportUrl.setVisibility(View.VISIBLE);
        } else {
            tvReportUrl.setVisibility(View.GONE);
        }

        // Timestamps
        tvCreatedAt.setText(dateFormat.format(inspection.getCreatedAt()));
        tvUpdatedAt.setText(dateFormat.format(inspection.getUpdatedAt()));

        // Corrective Actions
        displayCorrectiveActions();

        // Role-based UI
        if (!sessionManager.canEditData()) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void updateStatusColor() {
        int colorRes;
        switch (inspection.getStatus()) {
            case PASS:
                colorRes = R.color.status_certified;
                break;
            case CONDITIONAL_PASS:
                colorRes = R.color.status_audit_ready;
                break;
            case FAIL:
                colorRes = R.color.status_expired;
                break;
            default:
                colorRes = R.color.status_not_started;
                break;
        }
        cardStatus.setCardBackgroundColor(getColor(colorRes));
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
            int bgRes;
            int colorRes;

            switch (status) {
                case "CLOSED":
                    statusDisplay = "CLOSED";
                    bgRes = R.drawable.bg_status_finding_closed;
                    break;
                case "IN_PROGRESS":
                    statusDisplay = "IN PROGRESS";
                    bgRes = R.drawable.bg_status_finding_in_progress;
                    break;
                default:
                    statusDisplay = "OPEN";
                    bgRes = R.drawable.bg_status_finding_open;
                    break;
            }

            tvActionStatus.setText(statusDisplay);
            tvActionStatus.setBackgroundResource(bgRes);

            // Make the status clickable to update it
            final int position = i;
            tvActionStatus.setOnClickListener(v -> showStatusUpdateDialog(action, position));

            actionsContainer.addView(actionView);
        }
    }

    private void showStatusUpdateDialog(InspectionEntity.CorrectiveAction action, int position) {
        String[] statusOptions = {
                "OPEN - Not addressed yet",
                "IN PROGRESS - Being addressed",
                "CLOSED - Resolved"
        };

        String[] statusValues = {"OPEN", "IN PROGRESS", "CLOSED"};

        // Find current selection
        int currentSelection = 0;
        String currentStatus = action.status != null ? action.status.toUpperCase() : "OPEN";
        for (int i = 0; i < statusValues.length; i++) {
            if (statusValues[i].equals(currentStatus)) {
                currentSelection = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Action Status")
                .setSingleChoiceItems(statusOptions, currentSelection, (dialog, which) -> {
                    String newStatus = statusValues[which];
                    updateActionStatus(position, newStatus);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    private void showEditDialog() {
        if (inspection == null) return;

        EditInspectionDialog dialog = EditInspectionDialog.newInstance(inspection.getInspectionId());
        dialog.setOnInspectionUpdatedListener(() -> {
            loadInspectionData();
            Toast.makeText(this, "Inspection updated!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditInspectionDialog");
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Only owners can delete inspections", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Inspection")
                .setMessage("Are you sure you want to delete this inspection record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteInspection())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteInspection() {
        Toast.makeText(this, "Deleting inspection...", Toast.LENGTH_SHORT).show();

        inspectionRepository.deleteInspection(inspection, new InspectionRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(InspectionDetailActivity.this,
                            "Inspection deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(InspectionDetailActivity.this,
                                "Error: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
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