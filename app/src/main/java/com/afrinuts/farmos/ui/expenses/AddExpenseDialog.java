package com.afrinuts.farmos.ui.expenses;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
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
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
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

public class AddExpenseDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;

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

        // Set default description based on category selection
        setupCategoryDescriptionListener();
    }

    // Custom adapter for category dropdown with icons
    private class CategoryAdapter extends ArrayAdapter<ExpenseEntity.ExpenseCategory> {

        public CategoryAdapter(Context context, List<ExpenseEntity.ExpenseCategory> categories) {
            super(context, 0, categories);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, R.layout.item_category_dropdown);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, R.layout.item_category_dropdown);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(resource, parent, false);
            }

            ExpenseEntity.ExpenseCategory category = getItem(position);

            ImageView iconView = view.findViewById(R.id.categoryIcon);
            TextView textView = view.findViewById(R.id.categoryName);

            if (category != null) {
                // Set icon based on category
                int iconRes = getIconForCategory(category);
                iconView.setImageResource(iconRes);
                iconView.setColorFilter(getContext().getColor(R.color.primary));

                textView.setText(category.getDisplayName());
            }

            return view;
        }

        private int getIconForCategory(ExpenseEntity.ExpenseCategory category) {
            switch (category) {
                case LAND_CLEARING:
                    return R.drawable.ic_construction;
                case PLOWING:
                    return R.drawable.ic_grain;
                case SEEDLINGS:
                    return R.drawable.ic_grass;
                case LABOR:
                    return R.drawable.ic_people;
                case SECURITY:
                    return R.drawable.ic_security;
                case FENCING:
                    return R.drawable.ic_fence;
                case FERTILIZER:
                    return R.drawable.ic_fertilizer;
                case IRRIGATION:
                    return R.drawable.ic_water_drop;
                case EQUIPMENT:
                    return R.drawable.ic_construction;
                case MAINTENANCE:
                    return R.drawable.ic_construction;
                case PROCESSING_CENTER:
                    return R.drawable.ic_factory;
                case OTHER:
                    return R.drawable.ic_more_horiz;
                default:
                    return R.drawable.ic_attach_money;
            }
        }
    }

    private void setupCategoryDropdown() {
        ExpenseEntity.ExpenseCategory[] categories = ExpenseEntity.ExpenseCategory.values();
        List<ExpenseEntity.ExpenseCategory> categoryList = new ArrayList<>();
        for (ExpenseEntity.ExpenseCategory category : categories) {
            categoryList.add(category);
        }

        CategoryAdapter adapter = new CategoryAdapter(requireContext(), categoryList);
        etCategory.setAdapter(adapter);
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

    private void setupCategoryDescriptionListener() {
        etCategory.setOnItemClickListener((parent, view, position, id) -> {
            ExpenseEntity.ExpenseCategory selectedCategory = (ExpenseEntity.ExpenseCategory)
                    parent.getItemAtPosition(position);

            if (selectedCategory.getDefaultDescription() != null &&
                    TextUtils.isEmpty(etDescription.getText())) {
                etDescription.setText(selectedCategory.getDefaultDescription());
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

        // Validate category
        ExpenseEntity.ExpenseCategory selectedCategory =
                (ExpenseEntity.ExpenseCategory) etCategory.getAdapter().getItem(
                        etCategory.getListSelection());

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

        // Create and save expense
        ExpenseEntity expense = new ExpenseEntity(
                farmId,
                blockId,
                selectedCategory,
                amount,
                date,
                description
        );

        Toast.makeText(getContext(), "Saving expense...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            long id = database.expenseDao().insert(expense);

            requireActivity().runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(getContext(),
                            "Expense added successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onExpenseAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(),
                            "Error adding expense", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}