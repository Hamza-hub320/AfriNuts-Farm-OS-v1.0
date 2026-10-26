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
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.repository.InspectionRepository;
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

public class EditInspectionDialog extends DialogFragment {

    private static final String ARG_INSPECTION_ID = "inspection_id";

    private String inspectionId;
    private InspectionEntity inspection;
    private InspectionRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
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

    private Button btnCancel;
    private Button btnSave;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnInspectionUpdatedListener {
        void onInspectionUpdated();
    }

    private OnInspectionUpdatedListener listener;

    public static EditInspectionDialog newInstance(String inspectionId) {
        EditInspectionDialog dialog = new EditInspectionDialog();
        Bundle args = new Bundle();
        args.putString(ARG_INSPECTION_ID, inspectionId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnInspectionUpdatedListener(OnInspectionUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            inspectionId = getArguments().getString(ARG_INSPECTION_ID);
        }

        long farmId = 1;
        repository = new InspectionRepository(requireContext(), farmId);

        executor.execute(() -> {
            inspection = repository.getInspectionByIdSync(inspectionId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (inspection != null) {
                        populateFields();
                    } else {
                        Toast.makeText(getContext(), "Inspection not found", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_inspection, null);

        initViews(view);
        setupDropdowns();
        setupDatePicker();
        setupCorrectiveActions();

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
            view.findViewById(R.id.btnSave).setOnClickListener(v -> saveInspection());
        });

        return dialog;
    }

    private void initViews(View view) {
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
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void setupDropdowns() {
        // ✅ Requirement Category dropdown - show just display names
        InspectionEntity.RequirementCategory[] categories = InspectionEntity.RequirementCategory.values();
        String[] categoryNames = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            categoryNames[i] = categories[i].getDisplayName();  // Just the name, no icon
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
        );
        etRequirementCategory.setAdapter(categoryAdapter);

        // Inspection Type dropdown
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

        // Status dropdown
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

        // Load existing corrective actions
        if (inspection != null) {
            List<InspectionEntity.CorrectiveAction> actions = inspection.getCorrectiveActions();
            for (InspectionEntity.CorrectiveAction action : actions) {
                addCorrectiveActionItem(action.description, action.assignedTo);
            }
        }
    }

    private void addCorrectiveActionItem(String description, String assignedTo) {
        View actionView = getLayoutInflater().inflate(R.layout.item_corrective_action_input, correctiveActionsContainer, false);

        TextInputEditText etDescription = actionView.findViewById(R.id.etActionDescription);
        TextInputEditText etAssignedTo = actionView.findViewById(R.id.etAssignedTo);
        Button btnRemove = actionView.findViewById(R.id.btnRemoveAction);

        if (description != null) {
            etDescription.setText(description);
        }
        if (assignedTo != null) {
            etAssignedTo.setText(assignedTo);
        }

        btnRemove.setOnClickListener(v -> {
            correctiveActionsContainer.removeView(actionView);
        });

        correctiveActionsContainer.addView(actionView);
    }

    private void populateFields() {
        if (inspection == null) return;

        etRequirementCategory.setText(
                inspection.getRequirementCategory().getDisplayName(), false);
        etInspectionType.setText(inspection.getInspectionType().getDisplayName(), false);
        etStatus.setText(inspection.getStatus().getDisplayName(), false);

        // Set date
        if (inspection.getInspectionDate() > 0) {
            selectedDate.setTimeInMillis(inspection.getInspectionDate());
            updateDateLabel();
        }

        etInspectorName.setText(inspection.getInspectorName());
        etInspectorBody.setText(inspection.getInspectorBody());
        etRemarks.setText(inspection.getRemarks());
        etReportUrl.setText(inspection.getReportUrl());
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

    private void saveInspection() {
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

        // Update inspection
        inspection.setRequirementCategory(category);
        inspection.setInspectionType(type);
        inspection.setInspectionDate(inspectionDate);
        inspection.setInspectorName(inspectorName);
        inspection.setInspectorBody(inspectorBody);
        inspection.setStatus(status);
        inspection.setRemarks(remarks);
        inspection.setCorrectiveActions(correctiveActions);
        inspection.setReportUrl(reportUrl.isEmpty() ? null : reportUrl);
        inspection.setUpdatedAt(System.currentTimeMillis());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        repository.updateInspection(inspection, new InspectionRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Inspection updated!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onInspectionUpdated();
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