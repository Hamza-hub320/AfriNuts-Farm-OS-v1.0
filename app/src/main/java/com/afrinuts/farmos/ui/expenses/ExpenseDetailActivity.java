package com.afrinuts.farmos.ui.expenses;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
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

public class ExpenseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXPENSE_ID = "expense_id";

    private long expenseId;
    private ExpenseEntity expense;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;

    // UI Elements
    private TextView tvCategoryIcon;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Load data
        loadData();
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
            getSupportActionBar().setTitle("Expense Details");
        }
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
        new Thread(() -> {
            // Get expense
            expense = database.expenseDao().getExpenseById(expenseId);

            if (expense == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            // Get farm and blocks
            currentFarm = database.farmDao().getFirstFarm();
            allBlocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());

            runOnUiThread(() -> displayExpenseData());
        }).start();
    }

    private void displayExpenseData() {
        // Set category icon and name
        tvCategoryIcon.setText(expense.getCategory().getIcon());
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

    private String getBlockName(Long blockId) {
        if (blockId == null) return null;
        for (BlockEntity block : allBlocks) {
            if (block.getId() == blockId) {
                return block.getBlockName();
            }
        }
        return null;
    }

    private void enableEditMode(boolean enable) {
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

        // Save to database
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            database.expenseDao().update(expense);

            runOnUiThread(() -> {
                Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();
                enableEditMode(false);
                displayExpenseData(); // Refresh display
            });
        }).start();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteExpense())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteExpense() {
        Toast.makeText(this, "Deleting expense...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            database.expenseDao().delete(expense);

            runOnUiThread(() -> {
                Toast.makeText(this, "Expense deleted", Toast.LENGTH_SHORT).show();
                finish(); // Go back to expenses list
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}