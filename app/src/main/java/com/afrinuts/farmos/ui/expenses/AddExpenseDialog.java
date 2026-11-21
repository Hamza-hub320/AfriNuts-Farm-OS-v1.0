package com.afrinuts.farmos.ui.expenses;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

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
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddExpenseDialog extends FarmDialogFragment {

    private static final String TAG = "AddExpenseDialog";
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

    // Track already expensed batch IDs
    private Set<Long> expensedBatchIds = new HashSet<>();

    // UI Elements
    private TextInputEditText etAmount;
    private MaterialAutoCompleteTextView etCategory;
    private TextInputEditText etDate;
    private Button btnCancel;
    private Button btnSave;

    // Expense Scope
    private View layoutExpenseScope;
    private MaterialButtonToggleGroup toggleExpenseType;
    private Button btnFarmWide;
    private Button btnBlockSpecific;
    private TextInputLayout layoutBlockSelector;
    private MaterialAutoCompleteTextView etBlock;

    private TextInputEditText etDescription;

    // Procurement UI Elements
    private View layoutProcurementSection;
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

        // Load expensed batch IDs in background
        executor.execute(() -> {
            loadExpensedBatchIds();
            allBatches = batchRepository.getAllBatchesSync();
            allSuppliers = supplierRepository.getSuppliersSync();
        });
    }

    // Load all batch IDs that have already been expensed
    private void loadExpensedBatchIds() {
        AppDatabase database = AppDatabase.getInstance(requireContext());
        List<ExpenseEntity> allExpenses = database.expenseDao().getExpensesByFarmId(farmId);
        expensedBatchIds.clear();
        for (ExpenseEntity expense : allExpenses) {
            if (expense.getBatchId() != null) {
                expensedBatchIds.add(expense.getBatchId());
            }
        }
        Log.d(TAG, "Loaded " + expensedBatchIds.size() + " expensed batch IDs");
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

        // Expense Scope
        layoutExpenseScope = view.findViewById(R.id.layoutExpenseScope);
        toggleExpenseType = view.findViewById(R.id.toggleExpenseType);
        btnFarmWide = view.findViewById(R.id.btnFarmWide);
        btnBlockSpecific = view.findViewById(R.id.btnBlockSpecific);
        layoutBlockSelector = view.findViewById(R.id.layoutBlockSelector);
        etBlock = view.findViewById(R.id.etBlock);

        etDescription = view.findViewById(R.id.etDescription);

        // Procurement UI Elements
        layoutProcurementSection = view.findViewById(R.id.layoutProcurementSection);
        layoutSupplierSelector = view.findViewById(R.id.layoutSupplierSelector);
        etSupplier = view.findViewById(R.id.etSupplier);
        layoutBatchSelector = view.findViewById(R.id.layoutBatchSelector);
        etBatch = view.findViewById(R.id.etBatch);

        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveExpense());

        // Set default date to today
        updateDateLabel();

        // Default to farm-wide selected
        toggleExpenseType.check(R.id.btnFarmWide);
        layoutBlockSelector.setVisibility(View.GONE);
        toggleExpenseType.post(this::roundScopeButtons);

        // Hide procurement section initially
        layoutProcurementSection.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        // The shared sheet formatter may use the heading as the primary label;
        // this dialog's agreed action label is Save.
        if (btnSave != null) btnSave.setText("Save");
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
                toggleExpenseType.post(this::roundScopeButtons);
            }
        });
    }

    private void roundScopeButtons() {
        if (toggleExpenseType == null) return;
        float radius = 16f * getResources().getDisplayMetrics().density;
        for (int i = 0; i < toggleExpenseType.getChildCount(); i++) {
            View child = toggleExpenseType.getChildAt(i);
            if (child instanceof com.google.android.material.button.MaterialButton) {
                com.google.android.material.button.MaterialButton button =
                        (com.google.android.material.button.MaterialButton) child;
                button.setShapeAppearanceModel(button.getShapeAppearanceModel().toBuilder()
                        .setAllCornerSizes(radius).build());
                button.setInsetTop(0);
                button.setInsetBottom(0);
            }
        }
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

            // Check if category is procurement-related
            boolean isProcurement = selectedCategory == ExpenseEntity.ExpenseCategory.PROCUREMENT ||
                    selectedCategory == ExpenseEntity.ExpenseCategory.TRANSPORT ||
                    selectedCategory == ExpenseEntity.ExpenseCategory.QUALITY_TESTING ||
                    selectedCategory == ExpenseEntity.ExpenseCategory.STORAGE;

            if (isProcurement) {
                // Hide Expense Scope for procurement
                layoutExpenseScope.setVisibility(View.GONE);

                // Show Procurement Section
                layoutProcurementSection.setVisibility(View.VISIBLE);
                layoutSupplierSelector.setVisibility(View.VISIBLE);
                layoutBatchSelector.setVisibility(View.VISIBLE);

                // Load suppliers in background
                loadSuppliersForDropdown();
            } else {
                // Show Expense Scope for farm expenses
                layoutExpenseScope.setVisibility(View.VISIBLE);

                // Hide Procurement Section
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

    // Load suppliers for dropdown - runs on background thread
    private void loadSuppliersForDropdown() {
        executor.execute(() -> {
            if (allSuppliers == null) {
                allSuppliers = supplierRepository.getSuppliersSync();
            }

            // Build supplier list with available batch counts
            List<String> supplierNames = new ArrayList<>();
            final List<SupplierEntity> supplierList = new ArrayList<>(allSuppliers);

            for (SupplierEntity supplier : supplierList) {
                // Get available batches for this supplier (not expensed)
                List<QualityBatchEntity> supplierBatches =
                        batchRepository.getBatchesBySupplierIdSync(supplier.getId());
                int availableCount = 0;
                for (QualityBatchEntity batch : supplierBatches) {
                    if (!expensedBatchIds.contains(batch.getId())) {
                        availableCount++;
                    }
                }
                String status = availableCount > 0 ? " (" + availableCount + " batches)" : " (No batches)";
                supplierNames.add(supplier.getName() + " (" + supplier.getType().getDisplayName() + ")" + status);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            supplierNames
                    );
                    etSupplier.setAdapter(adapter);

                    etSupplier.setOnItemClickListener((parent, view, position, id) -> {
                        SupplierEntity selectedSupplier = supplierList.get(position);
                        selectedSupplierId = selectedSupplier.getId();
                        selectedSupplierName = selectedSupplier.getName();

                        // Load available batches for this supplier (on background thread)
                        loadAvailableBatchesForSupplier(selectedSupplierId);
                    });
                });
            }
        });
    }

    // Load only available (not expensed) batches for a supplier - runs on background thread
    private void loadAvailableBatchesForSupplier(long supplierId) {
        executor.execute(() -> {
            List<QualityBatchEntity> supplierBatches =
                    batchRepository.getBatchesBySupplierIdSync(supplierId);

            // Filter out already expensed batches
            List<QualityBatchEntity> availableBatches = new ArrayList<>();
            for (QualityBatchEntity batch : supplierBatches) {
                if (!expensedBatchIds.contains(batch.getId())) {
                    availableBatches.add(batch);
                }
            }

            List<String> batchDisplayNames = new ArrayList<>();
            final List<QualityBatchEntity> finalAvailableBatches = new ArrayList<>(availableBatches);

            for (QualityBatchEntity batch : availableBatches) {
                batchDisplayNames.add(batch.getBatchNumber() + " - " +
                        batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)");
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (finalAvailableBatches.isEmpty()) {
                        // Show message when no batches available
                        etBatch.setEnabled(false);
                        etBatch.setText("No available batches for this supplier", false);
                        Toast.makeText(getContext(),
                                "All batches from this supplier have been expensed",
                                Toast.LENGTH_LONG).show();
                    } else {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                batchDisplayNames
                        );
                        etBatch.setAdapter(adapter);
                        etBatch.setEnabled(true);

                        etBatch.setOnItemClickListener((parent, view, position, id) -> {
                            QualityBatchEntity selectedBatch = finalAvailableBatches.get(position);
                            selectedBatchId = selectedBatch.getId();
                            selectedBatchNumber = selectedBatch.getBatchNumber();
                            Log.d(TAG, "Selected batch: " + selectedBatchNumber);
                        });

                        // Auto-select first available batch
                        if (!finalAvailableBatches.isEmpty()) {
                            QualityBatchEntity firstBatch = finalAvailableBatches.get(0);
                            selectedBatchId = firstBatch.getId();
                            selectedBatchNumber = firstBatch.getBatchNumber();
                            etBatch.setText(batchDisplayNames.get(0), false);
                            Log.d(TAG, "Auto-selected batch: " + firstBatch.getBatchNumber());
                        }
                    }
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

        // Check if procurement category
        boolean isProcurement = selectedCategory == ExpenseEntity.ExpenseCategory.PROCUREMENT ||
                selectedCategory == ExpenseEntity.ExpenseCategory.TRANSPORT ||
                selectedCategory == ExpenseEntity.ExpenseCategory.QUALITY_TESTING ||
                selectedCategory == ExpenseEntity.ExpenseCategory.STORAGE;

        Long blockId = null;

        if (isProcurement) {
            // Validate procurement fields
            if (selectedSupplierId == null) {
                Toast.makeText(getContext(), "Please select a supplier", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedBatchId == null) {
                Toast.makeText(getContext(), "Please select a batch", Toast.LENGTH_SHORT).show();
                return;
            }

            // Double-check that the batch hasn't been expensed already
            if (expensedBatchIds.contains(selectedBatchId)) {
                Toast.makeText(getContext(),
                        "This batch has already been expensed! Please select a different batch.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            // Validate block if block-specific (farm expenses only)
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
