package com.afrinuts.farmos.ui.tasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTaskDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;

    // UI Elements
    private TextInputEditText etTaskTitle;
    private TextInputEditText etTaskDescription;
    private MaterialAutoCompleteTextView etTaskType;
    private MaterialButtonToggleGroup toggleTaskScope;
    private Button btnFarmWide;
    private Button btnPerBlock;
    private TextInputLayout layoutBlockSelector;
    private MaterialAutoCompleteTextView etBlock;
    private TextInputEditText etDueDate;
    private TextInputEditText etStatus;

    private Calendar dueDateCalendar = Calendar.getInstance();
    private boolean isFarmWide = true; // Default to farm-wide

    public interface OnTaskAddedListener {
        void onTaskAdded();
    }

    private OnTaskAddedListener listener;

    public static AddTaskDialog newInstance(long farmId) {
        AddTaskDialog dialog = new AddTaskDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnTaskAddedListener(OnTaskAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
        }

        database = AppDatabase.getInstance(requireContext());

        // Load data in background
        new Thread(() -> {
            currentFarm = database.farmDao().getFarmById(farmId);
            allBlocks = database.blockDao().getBlocksByFarmId(farmId);
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);

        initViews(view);
        setupTaskTypeDropdown();
        setupDatePicker();
        setupTaskScopeToggle();
        loadBlocksForDropdown();

        // Set default due date to tomorrow
        dueDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
        updateDateLabel();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            btnSave.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
        });

        return dialog;
    }

    private void initViews(View view) {
        etTaskTitle = view.findViewById(R.id.etTaskTitle);
        etTaskDescription = view.findViewById(R.id.etTaskDescription);
        etTaskType = view.findViewById(R.id.etTaskType);
        toggleTaskScope = view.findViewById(R.id.toggleTaskScope);
        btnFarmWide = view.findViewById(R.id.btnFarmWide);
        btnPerBlock = view.findViewById(R.id.btnPerBlock);
        layoutBlockSelector = view.findViewById(R.id.layoutBlockSelector);
        etBlock = view.findViewById(R.id.etBlock);
        etDueDate = view.findViewById(R.id.etDueDate);
        etStatus = view.findViewById(R.id.etStatus);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveTask());

        // Set status to PENDING (read-only)
        etStatus.setText("PENDING");

        // Default to farm-wide selected
        toggleTaskScope.check(R.id.btnFarmWide);
        layoutBlockSelector.setVisibility(View.GONE);
    }

    private void setupTaskTypeDropdown() {
        TaskEntity.TaskType[] types = TaskEntity.TaskType.values();
        String[] typeNames = new String[types.length];

        for (int i = 0; i < types.length; i++) {
            typeNames[i] = types[i].getIcon() + " " + types[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                typeNames
        );

        etTaskType.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etDueDate.setOnClickListener(v -> showDatePicker());
        etDueDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    dueDateCalendar.set(Calendar.YEAR, year);
                    dueDateCalendar.set(Calendar.MONTH, month);
                    dueDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateLabel();
                },
                dueDateCalendar.get(Calendar.YEAR),
                dueDateCalendar.get(Calendar.MONTH),
                dueDateCalendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etDueDate.setText(sdf.format(dueDateCalendar.getTime()));
    }

    private void setupTaskScopeToggle() {
        toggleTaskScope.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFarmWide) {
                    isFarmWide = true;
                    layoutBlockSelector.setVisibility(View.GONE);
                } else {
                    isFarmWide = false;
                    layoutBlockSelector.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadBlocksForDropdown() {
        new Thread(() -> {
            if (allBlocks == null) {
                allBlocks = database.blockDao().getBlocksByFarmId(farmId);
            }

            List<String> blockNames = new ArrayList<>();
            for (BlockEntity block : allBlocks) {
                blockNames.add(block.getBlockName() + " - " + block.getStatus().getDisplayName());
            }

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        blockNames
                );
                etBlock.setAdapter(adapter);
            });
        }).start();
    }

    private void saveTask() {
        // Validate title
        String title = etTaskTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            etTaskTitle.setError("Title is required");
            return;
        }

        // Validate task type
        String typeStr = etTaskType.getText().toString().trim();
        if (TextUtils.isEmpty(typeStr)) {
            etTaskType.setError("Task type is required");
            return;
        }

        // Parse task type
        TaskEntity.TaskType selectedType = null;
        for (TaskEntity.TaskType type : TaskEntity.TaskType.values()) {
            if (typeStr.contains(type.getDisplayName())) {
                selectedType = type;
                break;
            }
        }

        if (selectedType == null) {
            Toast.makeText(getContext(), "Invalid task type", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate block if per block
        Long blockId = null;
        if (!isFarmWide) {
            String blockStr = etBlock.getText().toString().trim();
            if (TextUtils.isEmpty(blockStr)) {
                etBlock.setError("Please select a block");
                return;
            }

            String blockName = blockStr.split(" - ")[0];
            for (BlockEntity block : allBlocks) {
                if (block.getBlockName().equals(blockName)) {
                    blockId = block.getId();
                    break;
                }
            }

            if (blockId == null) {
                Toast.makeText(getContext(), "Invalid block selection", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Get description (optional)
        String description = etTaskDescription.getText().toString().trim();

        // Get due date
        long dueDate = dueDateCalendar.getTimeInMillis();

        // Create task (always PENDING for new tasks)
        TaskEntity task = new TaskEntity(
                farmId,
                blockId,
                title,
                description.isEmpty() ? null : description,
                selectedType,
                TaskEntity.TaskStatus.PENDING,
                dueDate
        );

        Toast.makeText(getContext(), "Creating task...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            long id = database.taskDao().insertTask(task);

            requireActivity().runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(getContext(),
                            "Task created successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onTaskAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(),
                            "Error creating task", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}