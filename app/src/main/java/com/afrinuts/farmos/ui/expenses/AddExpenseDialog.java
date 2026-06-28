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
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
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
    private List<QualityBatchEntity> allBatches;
    private List<SupplierEntity> allSuppliers;
    private ExpenseRepository expenseRepository;
    private QualityBatchRepository batchRepository;
    private SupplierRepository supplierRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextInputEditText etAmount;
    private MaterialAutoCompleteTextView etCategory;
    private TextInputEditText etDate;

    // Expense Scope
    private View layoutExpenseScope;  // ✅ Container for expense scope
    private MaterialButtonToggleGroup toggleExpenseType;
    private Button btnFarmWide;
    private Button btnBlockSpecific;
    private TextInputLayout layoutBlockSelector;
    private MaterialAutoCompleteTextView etBlock;

    private TextInputEditText etDescription;

    // ✅ Procurement UI Elements
    private View layoutProcurementSection;  // ✅ Container for procurement section
    private TextInputLayout layoutSupplierSelector;
    private MaterialAutoCompleteTextView etSupplier;
    private TextInputLayout layoutBatchSelector;
    private MaterialAutoCompleteTextView etBatch;

    private Calendar selectedDate = Calendar.getInstance();
    private boolean isFarmWide = true;

    private ExpenseEntity.ExpenseCategory selectedCategory = null;
    private Long selectedBatchId = null;
    private Long selectedSupplierId = null;
    private String selectedBatchNumber = null;
    private String selectedSupplierName = null;

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

        expenseRepository = new ExpenseRepository(requireContext(), farmId);
        batchRepository = new QualityBatchRepository(requireContext(), farmId);
        supplierRepository = new SupplierRepository(requireContext(), farmId);
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
        loadSuppliersForDropdown();

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

        // Expense Scope
        layoutExpenseScope = view.findViewById(R.id.layoutExpenseScope);
        toggleExpenseType = view.findViewById(R.id.toggleExpenseType);
        btnFarmWide = view.findViewById(R.id.btnFarmWide);
        btnBlockSpecific = view.findViewById(R.id.btnBlockSpecific);
        layoutBlockSelector = view.findViewById(R.id.layoutBlockSelector);
        etBlock = view.findViewById(R.id.etBlock);

        etDescription = view.findViewById(R.id.etDescription);

        // ✅ Procurement UI Elements
        layoutProcurementSection = view.findViewById(R.id.layoutProcurementSection);
        layoutSupplierSelector = view.findViewById(R.id.layoutSupplierSelector);
        etSupplier = view.findViewById(R.id.etSupplier);
        layoutBatchSelector = view.findViewById(R.id.layoutBatchSelector);
        etBatch = view.findViewById(R.id.etBatch);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveExpense());

        // Set default date to today
        updateDateLabel();

        // Default to farm-wide selected
        toggleExpenseType.check(R.id.btnFarmWide);
        layoutBlockSelector.setVisibility(View.GONE);

        // Hide procurement section initially
        layoutProcurementSection.setVisibility(View.GONE);
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

    private void setupCategoryDropdown() {
        ExpenseEntity.ExpenseCategory[] categories = ExpenseEntity.ExpenseCategory.values();
        String[] categoryDisplayNames = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            categoryDisplayNames[i] = categories[i].getDisplayName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryDisplayNames
        );

        etCategory.setAdapter(adapter);

        etCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];

            // ✅ Check if category is procurement-related
            boolean isProcurement = selectedCategory == ExpenseEntity.ExpenseCategory.PROCUREMENT ||
                    selectedCategory == ExpenseEntity.ExpenseCategory.TRANSPORT ||
                    selectedCategory == ExpenseEntity.ExpenseCategory.QUALITY_TESTING ||
                    selectedCategory == ExpenseEntity.ExpenseCategory.STORAGE;

            if (isProcurement) {
                // ✅ Hide Expense Scope for procurement
                layoutExpenseScope.setVisibility(View.GONE);

                // ✅ Show Procurement Section
                layoutProcurementSection.setVisibility(View.VISIBLE);
                layoutSupplierSelector.setVisibility(View.VISIBLE);
                layoutBatchSelector.setVisibility(View.VISIBLE);

                // Load suppliers if not loaded
                if (allSuppliers == null || allSuppliers.isEmpty()) {
                    loadSuppliersForDropdown();
                }
            } else {
                // ✅ Show Expense Scope for farm expenses
                layoutExpenseScope.setVisibility(View.VISIBLE);

                // ✅ Hide Procurement Section
                layoutProcurementSection.setVisibility(View.GONE);
                layoutSupplierSelector.setVisibility(View.GONE);
                layoutBatchSelector.setVisibility(View.GONE);

                // Clear procurement selections
                selectedSupplierId = null;
                selectedSupplierName = null;
                selectedBatchId = null;
                selectedBatchNumber = null;
                etSupplier.setText("");
                etBatch.setText("");
            }
        });
    }

    // ✅ Load suppliers for dropdown
    private void loadSuppliersForDropdown() {
        executor.execute(() -> {
            allSuppliers = supplierRepository.getSuppliersSync();

            List<String> supplierNames = new ArrayList<>();
            for (SupplierEntity supplier : allSuppliers) {
                supplierNames.add(supplier.getName() + " (" + supplier.getType().getDisplayName() + ")");
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            supplierNames
                    );
                    etSupplier.setAdapter(adapter);

                    // ✅ When supplier is selected, load their batches
                    etSupplier.setOnItemClickListener((parent, view, position, id) -> {
                        SupplierEntity selectedSupplier = allSuppliers.get(position);
                        selectedSupplierId = selectedSupplier.getId();
                        selectedSupplierName = selectedSupplier.getName();

                        // Load batches for this supplier
                        loadBatchesForSupplier(selectedSupplierId);
                    });
                });
            }
        });
    }

    // ✅ Load batches for a specific supplier
    private void loadBatchesForSupplier(long supplierId) {
        executor.execute(() -> {
            List<QualityBatchEntity> supplierBatches =
                    batchRepository.getBatchesBySupplierIdSync(supplierId);

            List<String> batchDisplayNames = new ArrayList<>();
            for (QualityBatchEntity batch : supplierBatches) {
                batchDisplayNames.add(batch.getBatchNumber() + " - " +
                        batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)");
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            batchDisplayNames
                    );
                    etBatch.setAdapter(adapter);

                    etBatch.setOnItemClickListener((parent, view, position, id) -> {
                        QualityBatchEntity selectedBatch = supplierBatches.get(position);
                        selectedBatchId = selectedBatch.getId();
                        selectedBatchNumber = selectedBatch.getBatchNumber();
                    });
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

        if (selectedCategory == null) {
            Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Check if procurement category
        boolean isProcurement = selectedCategory == ExpenseEntity.ExpenseCategory.PROCUREMENT ||
                selectedCategory == ExpenseEntity.ExpenseCategory.TRANSPORT ||
                selectedCategory == ExpenseEntity.ExpenseCategory.QUALITY_TESTING ||
                selectedCategory == ExpenseEntity.ExpenseCategory.STORAGE;

        Long blockId = null;

        if (isProcurement) {
            // ✅ Validate procurement fields
            if (selectedSupplierId == null) {
                Toast.makeText(getContext(), "Please select a supplier", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedBatchId == null) {
                Toast.makeText(getContext(), "Please select a batch", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // ✅ Validate block if block-specific (farm expenses only)
            if (!isFarmWide) {
                String blockStr = etBlock.getText().toString().trim();
                if (TextUtils.isEmpty(blockStr)) {
                    etBlock.setError("Please select a block");
                    return;
                }

                // Find block ID from name
                if (allBlocks != null) {
                    String blockName = blockStr.split(" - ")[0];
                    for (BlockEntity block : allBlocks) {
                        if (block.getBlockName().equals(blockName)) {
                            blockId = block.getId();
                            break;
                        }
                    }
                }

                if (blockId == null) {
                    Toast.makeText(getContext(), "Invalid block selection", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        // Get description
        String description = etDescription.getText().toString().trim();
        if (TextUtils.isEmpty(description) && selectedCategory.getDefaultDescription() != null) {
            description = selectedCategory.getDefaultDescription();
        }

        long date = selectedDate.getTimeInMillis();

        // Create expense with batch and supplier info
        ExpenseEntity expense = new ExpenseEntity(
                farmId,
                blockId,
                isProcurement ? selectedBatchId : null,
                isProcurement ? selectedSupplierId : null,
                selectedCategory,
                amount,
                date,
                description
        );

        // Set denormalized fields for quick display
        if (isProcurement) {
            expense.setBatchNumber(selectedBatchNumber);
            expense.setSupplierName(selectedSupplierName);
        }

        // Save using repository
        expenseRepository.insertExpense(expense, new BaseRepository.InsertCallback() {
            @Override
            public void onSuccess(long id) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Expense added successfully!", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
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
        if (batchRepository != null) {
            batchRepository.shutdown();
        }
        if (supplierRepository != null) {
            supplierRepository.shutdown();
        }
        executor.shutdown();
    }
}