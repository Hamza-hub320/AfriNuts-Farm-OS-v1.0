package com.afrinuts.farmos.ui.expenses;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseDetailActivity extends BaseActivity {

    public static final String EXTRA_EXPENSE_ID = "expense_id";
    private static final String TAG = "ExpenseDetailActivity";

    private long expenseId;
    private ExpenseEntity expense;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;
    private ExpenseRepository expenseRepository;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private ImageView tvCategoryIcon;
    private TextView tvCategoryName;
    private TextView tvBlockInfo;
    private TextInputEditText etAmount;
    private TextInputEditText etDate;
    private TextInputEditText etDescription;
    private TextInputLayout layoutBlockSelector;
    private TextView tvBlockSelector;

    private MaterialButton btnEdit;
    private MaterialButton btnSave;
    private Button btnDelete;
    private FloatingActionButton fabEdit;

    private boolean isEditMode = false;
    private Calendar selectedDate = Calendar.getInstance();

    @Override
    protected boolean checkRoleAccess() {
        // Only Owners and Managers can access expense details
        return !sessionManager.isInvestor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user has permission
        if (!checkRoleAccess()) {
            Toast.makeText(this, "You don't have permission to view expenses",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_expense_detail);

        // Get expense ID from intent
        expenseId = getIntent().getLongExtra(EXTRA_EXPENSE_ID, -1);
        if (expenseId == -1) {
            Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load data using repository (after we get farmId)
        loadData();

        // Apply role-based UI restrictions
        applyRoleBasedRestrictions();
    }

    private void applyRoleBasedRestrictions() {
        if (sessionManager.isInvestor()) {
            finish();
            return;
        }
    }

    private void initViews() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Category display
        tvCategoryIcon = findViewById(R.id.tvCategoryIcon);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvBlockInfo = findViewById(R.id.tvBlockInfo);

        // Editable fields
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        etDescription = findViewById(R.id.etDescription);

        // Block selector (for editing)
        layoutBlockSelector = findViewById(R.id.layoutBlockSelector);
        tvBlockSelector = findViewById(R.id.tvBlockSelector);

        // Buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        fabEdit = findViewById(R.id.fabEdit);

        // Setup date picker
        setupDatePicker();

        // Set click listeners
        btnEdit.setOnClickListener(v -> enableEditMode(true));
        fabEdit.setOnClickListener(v -> enableEditMode(true));
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());

        // Block selector click
        tvBlockSelector.setOnClickListener(v -> showBlockSelectorDialog());

        // Initially not editable
        setFieldsEditable(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Expense Details");
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
                this,
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

    private void loadData() {
        executor.execute(() -> {
            // First get the expense to know the farmId
            expense = database.expenseDao().getExpenseById(expenseId);

            if (expense == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            // Get farm and blocks
            FarmEntity farm = database.farmDao().getFirstFarm();
            List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());

            // Initialize repository with farmId
            farmId = farm.getId();
            expenseRepository = new ExpenseRepository(ExpenseDetailActivity.this, farmId);

            final ExpenseEntity finalExpense = expense;
            final FarmEntity finalFarm = farm;
            final List<BlockEntity> finalBlocks = blocks;

            runOnUiThread(() -> {
                expense = finalExpense;
                currentFarm = finalFarm;
                allBlocks = finalBlocks;
                displayExpenseData();
            });
        });
    }

    private void displayExpenseData() {
        if (expense == null) return;

        // Set category icon and name
        int iconRes = getIconForCategory(expense.getCategory());
        tvCategoryIcon.setImageResource(iconRes);
        tvCategoryName.setText(expense.getCategory().getDisplayName());

        // Set block info
        if (expense.isFarmWide()) {
            tvBlockInfo.setText("Farm-Wide Expense");
            tvBlockSelector.setText("Farm-Wide");
        } else {
            String blockName = getBlockName(expense.getBlockId());
            tvBlockInfo.setText("Block " + blockName);
            tvBlockSelector.setText("Block " + blockName);
        }

        // Set amount
        etAmount.setText(String.format(Locale.getDefault(), "%.0f", expense.getAmount()));

        // Set date
        selectedDate.setTimeInMillis(expense.getDate());
        updateDateLabel();

        // Set description
        if (expense.getDescription() != null) {
            etDescription.setText(expense.getDescription());
        }
    }

    private int getIconForCategory(ExpenseEntity.ExpenseCategory category) {
        switch (category) {
            case LAND_CLEARING:
                return R.drawable.ic_clear;
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
            case PROCESSING_CENTER:
                return R.drawable.ic_factory;
            case FERTILIZER:
                return R.drawable.ic_fertilizer;
            case IRRIGATION:
                return R.drawable.ic_water_drop;
            case EQUIPMENT:
                return R.drawable.ic_construction;
            case MAINTENANCE:
                return R.drawable.ic_maintenance;
            case OTHER:
            default:
                return R.drawable.ic_attach_money;
        }
    }

    private String getBlockName(Long blockId) {
        if (blockId == null || allBlocks == null) return null;
        for (BlockEntity block : allBlocks) {
            if (block.getId() == blockId) {
                return block.getBlockName();
            }
        }
        return null;
    }

    private void enableEditMode(boolean enable) {
        // Only Owners and Managers can edit
        if (!sessionManager.canEditData()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "You don't have permission to edit", Snackbar.LENGTH_SHORT).show();
            return;
        }

        isEditMode = enable;

        // Toggle button visibility
        btnEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        fabEdit.setVisibility(enable ? View.GONE : View.VISIBLE);

        // Make fields editable
        setFieldsEditable(enable);

        if (enable) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Edit mode enabled. Make your changes and save.",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void setFieldsEditable(boolean editable) {
        etAmount.setEnabled(editable);
        etDate.setEnabled(editable);
        etDescription.setEnabled(editable);

        // Block selector only editable in edit mode
        layoutBlockSelector.setVisibility(editable ? View.VISIBLE : View.GONE);
        tvBlockSelector.setEnabled(editable);
    }

    private void showBlockSelectorDialog() {
        if (!sessionManager.canEditData()) {
            Toast.makeText(this, "You don't have permission to change block",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] blockOptions = new String[allBlocks.size() + 1];
        blockOptions[0] = "Farm-Wide";

        for (int i = 0; i < allBlocks.size(); i++) {
            blockOptions[i + 1] = "Block " + allBlocks.get(i).getBlockName();
        }

        int currentSelection = 0;
        if (!expense.isFarmWide()) {
            String currentBlockName = getBlockName(expense.getBlockId());
            for (int i = 0; i < allBlocks.size(); i++) {
                if (("Block " + allBlocks.get(i).getBlockName()).equals(currentBlockName)) {
                    currentSelection = i + 1;
                    break;
                }
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Block")
                .setSingleChoiceItems(blockOptions, currentSelection, (dialog, which) -> {
                    if (which == 0) {
                        // Farm-wide
                        expense.setBlockId(null);
                        tvBlockSelector.setText("Farm-Wide");
                    } else {
                        // Specific block
                        BlockEntity selectedBlock = allBlocks.get(which - 1);
                        expense.setBlockId(selectedBlock.getId());
                        tvBlockSelector.setText("Block " + selectedBlock.getBlockName());
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void saveChanges() {
        if (expense == null || expenseRepository == null) return;

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

        // Update expense object
        expense.setAmount(amount);
        expense.setDate(selectedDate.getTimeInMillis());
        expense.setDescription(etDescription.getText().toString().trim());
        expense.setUpdatedAt(System.currentTimeMillis());

        // Show loading state
        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // Save using repository
        expenseRepository.updateExpense(expense, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(ExpenseDetailActivity.this,
                            "Changes saved successfully!", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    enableEditMode(false);
                    displayExpenseData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ExpenseDetailActivity.this,
                            "Error saving changes: " + error, Toast.LENGTH_LONG).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                });
            }
        });
    }

    private void confirmDelete() {
        // Only Owners can delete
        if (!sessionManager.isOwner()) {
            Toast.makeText(this, "Only owners can delete expenses",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteExpense())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteExpense() {
        Toast.makeText(this, "Deleting expense...", Toast.LENGTH_SHORT).show();

        expenseRepository.deleteExpense(expense, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(ExpenseDetailActivity.this, "Expense deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(ExpenseDetailActivity.this,
                                "Error deleting expense: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expenseRepository != null) {
            expenseRepository.shutdown();
        }
        executor.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}