package com.afrinuts.farmos.ui.tasks;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.TaskRepository;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTaskDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private String farmIdString;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;
    private TaskRepository taskRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
    private Button btnCancel;
    private Button btnSave;

    // Store the selected task type
    private TaskEntity.TaskType selectedTaskType = null;

    private Calendar dueDateCalendar = Calendar.getInstance();

    // 42: backdated-work support
    private androidx.appcompat.widget.SwitchCompat swAlreadyDone;
    private View cardDueDate;           // existing due-date card, hidden when ON
    private View cardWorkDates;         // new work-dates card, shown when ON
    private TextInputEditText etStartDate;
    private TextInputEditText etFinishDate;
    private Calendar startDateCalendar = Calendar.getInstance();
    private Calendar finishDateCalendar = Calendar.getInstance();
    private boolean isPastWork = false;
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
            farmIdString = String.valueOf(farmId);
        }

        database = AppDatabase.getInstance(requireContext());
        taskRepository = new TaskRepository(requireContext(), farmId);

        // Load data in background
        executor.execute(() -> {
            currentFarm = database.farmDao().getFarmById(farmId);
            allBlocks = database.blockDao().getBlocksByFarmId(farmId);
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_task, null);

        initViews(view);
        setupTaskTypeDropdown();
        setupDatePicker();
        setupWorkDates();
        setupAlreadyDoneToggle();
        setupTaskScopeToggle();
        loadBlocksForDropdown();

        // Set default due date to tomorrow
        dueDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
        updateDateLabel();

        // Use setView with custom buttons - don't use setPositiveButton/setNegativeButton
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("")
                .setView(view)
                .setCancelable(false); // Prevent dismissal by tapping outside

        AlertDialog dialog = builder.create();

        // Set up custom buttons after dialog is created
        dialog.setOnShowListener(dialogInterface -> {
            // Find and set up custom buttons from the layout
            btnCancel.setOnClickListener(v -> dismiss());
            btnSave.setOnClickListener(v -> saveTask());
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
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        // Set status to PENDING (read-only)
        etStatus.setText("PENDING");

        // Default to farm-wide selected
        toggleTaskScope.check(R.id.btnFarmWide);
        layoutBlockSelector.setVisibility(View.GONE);
        toggleTaskScope.post(this::roundTaskScope);

        swAlreadyDone = view.findViewById(R.id.swAlreadyDone);
        cardDueDate   = view.findViewById(R.id.cardDueDate);
        cardWorkDates = view.findViewById(R.id.cardWorkDates);
        etStartDate   = view.findViewById(R.id.etStartDate);
        etFinishDate  = view.findViewById(R.id.etFinishDate);
    }

    private void setupTaskTypeDropdown() {
        TaskEntity.TaskType[] types = TaskEntity.TaskType.values();

        // Create a custom adapter that shows vector icons and text
        ArrayAdapter<TaskEntity.TaskType> adapter = new ArrayAdapter<TaskEntity.TaskType>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                types) {

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_task_type_dropdown, parent, false);
                }

                TaskEntity.TaskType type = getItem(position);

                ImageView iconView = convertView.findViewById(R.id.typeIcon);
                TextView textView = convertView.findViewById(R.id.typeName);

                // Set vector icon
                iconView.setImageResource(getIconForTaskType(type));
                iconView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));

                // Set text
                textView.setText(type.getDisplayName());

                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return getView(position, convertView, parent);
            }
        };

        etTaskType.setAdapter(adapter);

        // Set item click listener to capture the selected type
        etTaskType.setOnItemClickListener((parent, view, position, id) -> {
            selectedTaskType = types[position];
        });
    }

    private int getIconForTaskType(TaskEntity.TaskType type) {
        switch (type) {
            case CLEARING:
                return R.drawable.ic_tractor;
            case PLOWING:
                return R.drawable.ic_grain;
            case PLANTING:
                return R.drawable.ic_grass;
            case REPLACEMENT:
                return R.drawable.ic_sync;
            case FERTILIZING:
                return R.drawable.ic_fertilizer;
            case PRUNING:
                return R.drawable.ic_cut;
            case WEEDING:
                return R.drawable.ic_grass;
            case HARVEST:
                return R.drawable.ic_download;
            case IRRIGATION:
                return R.drawable.ic_water_drop;
            case MAINTENANCE:
                return R.drawable.ic_construction;
            case OTHER:
            default:
                return R.drawable.ic_note;
        }
    }

    private void setupDatePicker() {
        etDueDate.setOnClickListener(v -> showDatePicker());
        etDueDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
        });
    }

    /** 42: pre-populate the work-dates fields (finish = today, start = finish). */
    private void setupWorkDates() {
        finishDateCalendar = Calendar.getInstance();
        startDateCalendar  = (Calendar) finishDateCalendar.clone();

        updateWorkDatesLabels();

        etFinishDate.setOnClickListener(v -> showFinishDatePicker());
        etFinishDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showFinishDatePicker();
        });

        etStartDate.setOnClickListener(v -> showStartDatePicker());
        etStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showStartDatePicker();
        });
    }

    private void showFinishDatePicker() {
        DatePickerDialog d = new DatePickerDialog(
                requireContext(),
                (v, y, m, day) -> {
                    finishDateCalendar.set(y, m, day);
                    // Keep start <= finish: if start now exceeds finish, pull it back.
                    if (startDateCalendar.after(finishDateCalendar)) {
                        startDateCalendar = (Calendar) finishDateCalendar.clone();
                    }
                    updateWorkDatesLabels();
                },
                finishDateCalendar.get(Calendar.YEAR),
                finishDateCalendar.get(Calendar.MONTH),
                finishDateCalendar.get(Calendar.DAY_OF_MONTH));
        d.getDatePicker().setMaxDate(System.currentTimeMillis());
        d.show();
    }

    private void showStartDatePicker() {
        DatePickerDialog d = new DatePickerDialog(
                requireContext(),
                (v, y, m, day) -> {
                    startDateCalendar.set(y, m, day);
                    updateWorkDatesLabels();
                },
                startDateCalendar.get(Calendar.YEAR),
                startDateCalendar.get(Calendar.MONTH),
                startDateCalendar.get(Calendar.DAY_OF_MONTH));
        // Start cannot be in the future and cannot exceed finish.
        d.getDatePicker().setMaxDate(finishDateCalendar.getTimeInMillis());
        d.show();
    }

    private void updateWorkDatesLabels() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etStartDate.setText(sdf.format(startDateCalendar.getTime()));
        etFinishDate.setText(sdf.format(finishDateCalendar.getTime()));
    }

    /**
     * 42: 'Work already done' mode. Swaps which date block is visible and what
     * the read-only status field says. The normal path (toggle OFF) is
     * byte-for-byte unchanged: due date visible, PENDING status, no start date.
     */
    private void setupAlreadyDoneToggle() {
        swAlreadyDone.setOnCheckedChangeListener((btn, checked) -> {
            isPastWork = checked;
            if (checked) {
                cardDueDate.setVisibility(View.GONE);
                cardWorkDates.setVisibility(View.VISIBLE);
                etStatus.setText("COMPLETED");
            } else {
                cardDueDate.setVisibility(View.VISIBLE);
                cardWorkDates.setVisibility(View.GONE);
                etStatus.setText("PENDING");
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

    private void roundTaskScope() {
        if (toggleTaskScope == null) return;
        toggleTaskScope.setBackgroundResource(R.drawable.bg_expense_scope_toggle);
        toggleTaskScope.setClipToOutline(true);
        float radius = 16f * getResources().getDisplayMetrics().density;
        for (int i = 0; i < toggleTaskScope.getChildCount(); i++) {
            View child = toggleTaskScope.getChildAt(i);
            if (child instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton button =
                        (com.google.android.material.button.MaterialButton) child;
                button.setShapeAppearanceModel(button.getShapeAppearanceModel().toBuilder()
                        .setAllCornerSizes(radius).build());
                button.setInsetTop(0);
                button.setInsetBottom(0);
                button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
                button.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            }
        }
    }

    private void loadBlocksForDropdown() {
        executor.execute(() -> {
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
        });
    }

    private void saveTask() {
        // Validate title
        String title = etTaskTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            etTaskTitle.setError("Title is required");
            return;
        }

        // Validate task type was selected
        if (selectedTaskType == null) {
            Toast.makeText(getContext(), "Please select a task type", Toast.LENGTH_SHORT).show();
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

        long dueDate;
        TaskEntity.TaskStatus initialStatus;
        Long startDateMillis = null;
        Long completedDateMillis = null;

        if (isPastWork) {
            // 42: backdated work.
            //   - finish must be in the past or today
            //   - start must be <= finish
            long finishMs = finishDateCalendar.getTimeInMillis();
            long startMs  = startDateCalendar.getTimeInMillis();

            if (finishMs > System.currentTimeMillis()) {
                Toast.makeText(getContext(),
                        "Finish date cannot be in the future", Toast.LENGTH_LONG).show();
                return;
            }
            if (startMs > finishMs) {
                Toast.makeText(getContext(),
                        "Start date must be on or before the finish date", Toast.LENGTH_LONG).show();
                return;
            }

            dueDate              = finishMs;
            initialStatus        = TaskEntity.TaskStatus.COMPLETED;
            startDateMillis      = startMs;
            completedDateMillis  = finishMs;
        } else {
            // Normal path — unchanged.
            dueDate              = dueDateCalendar.getTimeInMillis();
            initialStatus        = TaskEntity.TaskStatus.PENDING;
        }

        TaskEntity task = new TaskEntity(
                farmId,
                blockId,
                title,
                description.isEmpty() ? null : description,
                selectedTaskType,
                initialStatus,
                dueDate
        );

        // 42: in past-work mode, set the two dates AFTER construction and BEFORE
        // insert, in this exact order. The constructor nulls completedDate; we
        // set it explicitly and never call setStatus() afterwards, so
        // setCompletedDate here is the record. createdAt stays 'now' — that is
        // the log date, which is correct.
        if (isPastWork) {
            task.setStartDate(startDateMillis);
            task.setCompletedDate(completedDateMillis);
        }

        // Disable save button
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Use repository to save
        taskRepository.insertTask(task, new BaseRepository.InsertCallback() {
            @Override
            public void onSuccess(long id) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Task created successfully!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onTaskAdded();
                        }
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Create Task");
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (taskRepository != null) {
            taskRepository.shutdown();
        }
        executor.shutdown();
    }
}
