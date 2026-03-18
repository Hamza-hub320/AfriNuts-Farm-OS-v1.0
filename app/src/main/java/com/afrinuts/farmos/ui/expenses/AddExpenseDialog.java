package com.afrinuts.farmos.ui.expenses;

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
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
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

public class AddExpenseDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private ExpenseRepository expenseRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etAmount;
    private MaterialAutoCompleteTextView etCategory;
    private TextInputEditText etDate;
    private MaterialButtonToggleGroup toggleExpenseType;
    private Button btnFarmWide;
    private Button btnBlockSpecific;
    private TextInputLayout layoutBlockSelector;
    private MaterialAutoCompleteTextView etBlock;
    private TextInputEditText etDescription;

    private Calendar selectedDate = Calendar.getInstance();
    private boolean isFarmWide = true; // Default to farm-wide

    // Store the selected category
    private ExpenseEntity.ExpenseCategory selectedCategory = null;

    // Store the display names for the dropdown
    private String[] categoryDisplayNames;
    private ExpenseEntity.ExpenseCategory[] categories;

    public interface OnExpenseAddedListener {
        void onExpenseAdded();
    }

    private OnExpenseAddedListener listener;

    public static AddExpenseDialog newInstance(long farmId) {
        AddExpenseDialog dialog = new AddExpenseDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnExpenseAddedListener(OnExpenseAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
        }

        // Initialize repository
        expenseRepository = new ExpenseRepository(requireContext(), farmId);

        // Load data in background
        executor.execute(() -> {
            // We still need direct DB access for blocks until we create BlockRepository
            // For now, we'll keep direct DB access
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_expense, null);

        initViews(view);
        setupCategoryDropdown();
        setupDatePicker();
        setupExpenseTypeToggle();
        loadBlocksForDropdown();

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
        etAmount = view.findViewById(R.id.etAmount);
        etCategory = view.findViewById(R.id.etCategory);
        etDate = view.findViewById(R.id.etDate);
        toggleExpenseType = view.findViewById(R.id.toggleExpenseType);
        btnFarmWide = view.findViewById(R.id.btnFarmWide);
        btnBlockSpecific = view.findViewById(R.id.btnBlockSpecific);
        layoutBlockSelector = view.findViewById(R.id.layoutBlockSelector);
        etBlock = view.findViewById(R.id.etBlock);
        etDescription = view.findViewById(R.id.etDescription);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveExpense());

        // Set default date to today
        updateDateLabel();

        // Default to farm-wide selected
        toggleExpenseType.check(R.id.btnFarmWide);
        layoutBlockSelector.setVisibility(View.GONE);
    }

    private void setupCategoryDropdown() {
        categories = ExpenseEntity.ExpenseCategory.values();

        // Create array of display names
        categoryDisplayNames = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            categoryDisplayNames[i] = categories[i].getDisplayName();
        }

        // Create a simple ArrayAdapter with display names
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryDisplayNames) {

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_category_dropdown, parent, false);
                }

                ImageView iconView = convertView.findViewById(R.id.categoryIcon);
                TextView textView = convertView.findViewById(R.id.categoryName);

                // Get the actual category enum for this position
                ExpenseEntity.ExpenseCategory category = categories[position];

                // Set vector icon using ExpenseIconHelper
                iconView.setImageResource(ExpenseIconHelper.getCategoryIconResId(category));
                iconView.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary));

                // Set display name
                textView.setText(categoryDisplayNames[position]);

                return convertView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return getView(position, convertView, parent);
            }
        };

        etCategory.setAdapter(adapter);

        // Set item click listener to capture the selected category
        etCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];

            // Auto-fill description if available
            if (selectedCategory.getDefaultDescription() != null &&
                    TextUtils.isEmpty(etDescription.getText())) {
                etDescription.setText(selectedCategory.getDefaultDescription());
            }
        });
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker();
            }
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
        etDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupExpenseTypeToggle() {
        toggleExpenseType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFarmWide) {
                    isFarmWide = true;
                    layoutBlockSelector.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnBlockSpecific) {
                    isFarmWide = false;
                    layoutBlockSelector.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadBlocksForDropdown() {
        executor.execute(() -> {
            // For now, use direct DB access until we create BlockRepository
            AppDatabase database = AppDatabase.getInstance(requireContext());
            allBlocks = database.blockDao().getBlocksByFarmId(farmId);

            List<String> blockNames = new ArrayList<>();
            for (BlockEntity block : allBlocks) {
                blockNames.add(block.getBlockName() + " - " + block.getStatus().getDisplayName());
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            blockNames
                    );
                    etBlock.setAdapter(adapter);
                });
            }
        });
    }

    private void saveExpense() {
        // Validate amount
        String amountStr = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("Amount is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            return;
        }

        // Validate category was selected
        if (selectedCategory == null) {
            Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate block if block-specific
        Long blockId = null;
        if (!isFarmWide) {
            String blockStr = etBlock.getText().toString().trim();
            if (TextUtils.isEmpty(blockStr)) {
                etBlock.setError("Please select a block");
                return;
            }

            // Find block ID from name
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

        // Get description
        String description = etDescription.getText().toString().trim();
        if (TextUtils.isEmpty(description) && selectedCategory.getDefaultDescription() != null) {
            description = selectedCategory.getDefaultDescription();
        }

        long date = selectedDate.getTimeInMillis();

        // Create expense object
        ExpenseEntity expense = new ExpenseEntity(
                farmId,
                blockId,
                selectedCategory,
                amount,
                date,
                description
        );

        // Show loading state
        Button btnSave = getDialog() != null ? getDialog().findViewById(R.id.btnSave) : null;
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
        }

        // Use repository to save
        expenseRepository.insertExpense(expense, new BaseRepository.InsertCallback() {
            @Override
            public void onSuccess(long id) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Expense added successfully!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onExpenseAdded();
                        }
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Error adding expense: " + error, Toast.LENGTH_LONG).show();
                        if (btnSave != null) {
                            btnSave.setEnabled(true);
                            btnSave.setText("Save Expense");
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (expenseRepository != null) {
            expenseRepository.shutdown();
        }
        executor.shutdown();
    }
}