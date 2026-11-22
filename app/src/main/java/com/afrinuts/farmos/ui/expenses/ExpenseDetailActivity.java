package com.afrinuts.farmos.ui.expenses;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.finance.FinanceDetailChrome;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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

public class ExpenseDetailActivity extends BaseActivity {

    public static final String EXTRA_EXPENSE_ID = "expense_id";
    private static final String TAG = "ExpenseDetailActivity";

    private long expenseId;
    private ExpenseEntity expense;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private List<QualityBatchEntity> allBatches;
    private List<SupplierEntity> allSuppliers;
    private AppDatabase database;
    private ExpenseRepository expenseRepository;
    private QualityBatchRepository batchRepository;
    private SupplierRepository supplierRepository;

    // Edit mode fields
    private CardView layoutEditFields;
    private TextInputEditText etAmountEdit;
    private TextInputEditText etDateEdit;
    private TextInputEditText etDescriptionEdit;
    private TextInputLayout layoutBlockSelectorEdit;
    private MaterialAutoCompleteTextView tvBlockSelectorEdit;

    private PhotoRepository photoRepository;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private LinearLayout btnPhotoGallery;
    private TextView tvPhotoCount;
    private ImageView tvCategoryIcon;
    private TextView tvCategoryName;
    private TextView tvBlockInfo;
    private TextInputEditText etAmount;
    private TextInputEditText etDate;
    private TextInputEditText etDescription;
    private TextInputLayout layoutBlockSelector;
    private TextView tvBlockSelector;

    // ✅ Procurement UI Elements
    private LinearLayout layoutProcurementSection;
    private TextInputLayout layoutSupplierSelector;
    private MaterialAutoCompleteTextView etSupplier;
    private TextInputLayout layoutBatchSelector;
    private MaterialAutoCompleteTextView etBatch;
    private TextView tvProcurementInfo;

    private MaterialButton btnEdit;
    private MaterialButton btnSave;
    private Button btnDelete;
    private FloatingActionButton fabEdit;
    private MaterialButton footerPhotos;
    private ImageButton headerEdit;

    private boolean isEditMode = false;
    private Calendar selectedDate = Calendar.getInstance();

    // ✅ Procurement tracking fields
    private Long selectedSupplierId = null;
    private Long selectedBatchId = null;
    private String selectedSupplierName = null;
    private String selectedBatchNumber = null;

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkRoleAccess()) {
            Toast.makeText(this, "You don't have permission to view expenses",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_expense_detail);

        expenseId = getIntent().getLongExtra(EXTRA_EXPENSE_ID, -1);
        if (expenseId == -1) {
            Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);

        initViews();
        setupToolbar();
        loadData();
        applyRoleBasedRestrictions();

        btnPhotoGallery = findViewById(R.id.btnPhotoGallery);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        btnPhotoGallery.setOnClickListener(v -> {
            Intent intent = new Intent(this, PhotoGalleryActivity.class);
            intent.putExtra("expense_id", expenseId);
            startActivity(intent);
        });
    }

    private void applyRoleBasedRestrictions() {
        if (sessionManager.isInvestor()) {
            finish();
            return;
        }
    }

    private void initViews() {
        // Category display
        tvCategoryIcon = findViewById(R.id.tvCategoryIcon);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvBlockInfo = findViewById(R.id.tvBlockInfo);
        tvProcurementInfo = findViewById(R.id.tvProcurementInfo);  // ✅ Add this

        layoutEditFields = findViewById(R.id.layoutEditFields);
        etAmountEdit = findViewById(R.id.etAmountEdit);
        etDateEdit = findViewById(R.id.etDateEdit);
        etDescriptionEdit = findViewById(R.id.etDescriptionEdit);
        layoutBlockSelectorEdit = findViewById(R.id.layoutBlockSelectorEdit);
        tvBlockSelectorEdit = findViewById(R.id.tvBlockSelectorEdit);

        // Editable fields
        etAmount = findViewById(R.id.etAmount);
        etDate = findViewById(R.id.etDate);
        etDescription = findViewById(R.id.etDescription);

        // Block selector
        layoutBlockSelector = findViewById(R.id.layoutBlockSelector);
        tvBlockSelector = findViewById(R.id.tvBlockSelector);

        // ✅ Procurement UI Elements
        layoutProcurementSection = findViewById(R.id.layoutProcurementSection);
        layoutSupplierSelector = findViewById(R.id.layoutSupplierSelector);
        etSupplier = findViewById(R.id.etSupplier);
        layoutBatchSelector = findViewById(R.id.layoutBatchSelector);
        etBatch = findViewById(R.id.etBatch);

        // Buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        fabEdit = findViewById(R.id.fabEdit);

        // Setup date picker
        setupDatePicker();

        // Set click listeners
        btnEdit.setOnClickListener(v -> enableEditMode(!isEditMode));
        fabEdit.setOnClickListener(v -> enableEditMode(true));
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());

        // Block selector click
        tvBlockSelector.setOnClickListener(v -> showBlockSelectorDialog());

        // ✅ Supplier selection listener
        etSupplier.setOnItemClickListener((parent, view, position, id) -> {
            SupplierEntity selectedSupplier = allSuppliers.get(position);
            selectedSupplierId = selectedSupplier.getId();
            selectedSupplierName = selectedSupplier.getName();
            loadBatchesForSupplier(selectedSupplierId);
        });

        // ✅ Batch selection listener
        etBatch.setOnItemClickListener((parent, view, position, id) -> {
            QualityBatchEntity selectedBatch = allBatches.get(position);
            selectedBatchId = selectedBatch.getId();
            selectedBatchNumber = selectedBatch.getBatchNumber();
        });

        // Initially not editable
        setFieldsEditable(false);
    }

    private void setupToolbar() {
        installDetailFooter();
        if (fabEdit != null) fabEdit.setVisibility(View.GONE);
        headerEdit = findViewById(R.id.farmDetailEdit);
        FinanceDetailChrome.apply(this, findViewById(R.id.farmDetailTitle),
                findViewById(R.id.farmDetailBack), findViewById(R.id.farmDetailEdit),
                findViewById(R.id.farmDetailMore), "Expense details");
    }

    private void installDetailFooter() {
        LinearLayout root = (LinearLayout) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        ViewGroup sourceRow = (ViewGroup) btnEdit.getParent();
        sourceRow.removeView(btnEdit);
        sourceRow.removeView(btnSave);
        sourceRow.setVisibility(View.GONE);
        btnDelete.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);

        footerPhotos = new MaterialButton(this);
        footerPhotos.setText("Photos");
        styleFooterButton(footerPhotos, false);
        footerPhotos.setOnClickListener(v -> btnPhotoGallery.performClick());
        styleFooterButton(btnEdit, true);
        styleFooterButton(btnSave, true);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(dp(20), dp(8), dp(20), dp(12));
        footer.setBackgroundColor(0xFFF6F1E7);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(52), 1f);
        left.setMargins(0, 0, dp(12), 0);
        footer.addView(footerPhotos, left);
        footer.addView(btnEdit, new LinearLayout.LayoutParams(0, dp(52), 1f));
        footer.addView(btnSave, new LinearLayout.LayoutParams(0, dp(52), 1f));
        root.addView(footer);
    }

    private void styleFooterButton(MaterialButton button, boolean primary) {
        button.setTextSize(15);
        button.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
        button.setTextColor(primary ? Color.WHITE : 0xFF1C1B1A);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary ? 0xFF0E6B3C : 0xFFF3EAD8));
        button.setCornerRadius(dp(999));
        button.setMinHeight(dp(52));
        button.setInsetTop(0);
        button.setInsetBottom(0);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

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

    private void setEditModeVisibility(boolean visible) {
        View layoutEditFields = findViewById(R.id.layoutEditFields);
        if (layoutEditFields != null) {
            layoutEditFields.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        int editVisibility = visible ? View.VISIBLE : View.GONE;
        if (etAmountEdit != null) etAmountEdit.setVisibility(editVisibility);
        if (etDateEdit != null) etDateEdit.setVisibility(editVisibility);
        if (etDescriptionEdit != null) etDescriptionEdit.setVisibility(editVisibility);
        if (etSupplier != null) etSupplier.setVisibility(editVisibility);
        if (etBatch != null) etBatch.setVisibility(editVisibility);
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

    private void loadPhotoCount() {
        if (expenseId == -1 || photoRepository == null) return;

        photoRepository.getPhotoCountForExpenseAsync(expenseId)
                .thenAccept(count -> {
                    runOnUiThread(() -> {
                        tvPhotoCount.setText(count + " " + (count == 1 ? "photo" : "photos"));
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error loading photo count: " + throwable.getMessage());
                    runOnUiThread(() -> tvPhotoCount.setText("0 photos"));
                    return null;
                });
    }

    private void loadData() {
        executor.execute(() -> {
            expense = database.expenseDao().getExpenseById(expenseId);

            if (expense == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            FarmEntity farm = database.farmDao().getFirstFarm();
            List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farm.getId());

            farmId = farm.getId();
            expenseRepository = new ExpenseRepository(ExpenseDetailActivity.this, farmId);
            batchRepository = new QualityBatchRepository(ExpenseDetailActivity.this, farmId);
            supplierRepository = new SupplierRepository(ExpenseDetailActivity.this, farmId);

            // ✅ Load all suppliers and batches for dropdowns
            allSuppliers = supplierRepository.getSuppliersSync();
            allBatches = batchRepository.getAllBatchesSync();

            final ExpenseEntity finalExpense = expense;
            final FarmEntity finalFarm = farm;
            final List<BlockEntity> finalBlocks = blocks;

            runOnUiThread(() -> {
                expense = finalExpense;
                currentFarm = finalFarm;
                allBlocks = finalBlocks;
                displayExpenseData();
                loadPhotoCount();
                setupProcurementDropdowns();
            });
        });
    }

    private void setupProcurementDropdowns() {
        // Setup supplier dropdown
        List<String> supplierNames = new ArrayList<>();
        for (SupplierEntity supplier : allSuppliers) {
            supplierNames.add(supplier.getName() + " (" + supplier.getType().getDisplayName() + ")");
        }
        ArrayAdapter<String> supplierAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                supplierNames
        );
        etSupplier.setAdapter(supplierAdapter);

        // Setup batch dropdown
        List<String> batchDisplayNames = new ArrayList<>();
        for (QualityBatchEntity batch : allBatches) {
            batchDisplayNames.add(batch.getBatchNumber() + " - " +
                    batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)");
        }
        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                batchDisplayNames
        );
        etBatch.setAdapter(batchAdapter);
    }

    // ✅ Load batches for a specific supplier
    private void loadBatchesForSupplier(long supplierId) {
        executor.execute(() -> {
            List<QualityBatchEntity> supplierBatches =
                    batchRepository.getBatchesBySupplierIdSync(supplierId);

            runOnUiThread(() -> {
                List<String> batchDisplayNames = new ArrayList<>();
                for (QualityBatchEntity batch : supplierBatches) {
                    batchDisplayNames.add(batch.getBatchNumber() + " - " +
                            batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)");
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        batchDisplayNames
                );
                etBatch.setAdapter(adapter);
            });
        });
    }

    private void displayExpenseData() {
        if (expense == null) return;

        // Set category icon and name
        int iconRes = getIconForCategory(expense.getCategory());
        tvCategoryIcon.setImageResource(iconRes);
        tvCategoryName.setText(expense.getCategory().getDisplayName());

        // ✅ Determine and display the correct source info
        if (expense.isProcurementRelated()) {
            // Procurement expense - show batch and supplier info
            String sourceInfo = "";
            if (expense.getBatchNumber() != null && !expense.getBatchNumber().trim().isEmpty()) sourceInfo = "Batch " + expense.getBatchNumber().trim();
            if (expense.getSupplierName() != null && !expense.getSupplierName().trim().isEmpty()) {
                if (!sourceInfo.isEmpty()) sourceInfo += " · ";
                sourceInfo += "Supplier " + expense.getSupplierName().trim();
            }
            if (sourceInfo.isEmpty()) {
                sourceInfo = "Procurement Expense";
            }
            tvBlockInfo.setText(sourceInfo);
            tvBlockInfo.setVisibility(View.VISIBLE);

            // Hide block selector in non-edit mode
            if (!isEditMode) {
                layoutBlockSelector.setVisibility(View.GONE);
            }

            // Show procurement info
            if (tvProcurementInfo != null) {
                tvProcurementInfo.setVisibility(View.VISIBLE);
                String info = "";
                if (expense.getBatchNumber() != null) {
                    info += "Batch: " + expense.getBatchNumber();
                    selectedBatchNumber = expense.getBatchNumber();
                    if (allBatches != null) {
                        for (QualityBatchEntity batch : allBatches) {
                            if (batch.getBatchNumber().equals(expense.getBatchNumber())) {
                                selectedBatchId = batch.getId();
                                break;
                            }
                        }
                    }
                }
                if (expense.getSupplierName() != null) {
                    if (!info.isEmpty()) info += " | ";
                    info += "Supplier: " + expense.getSupplierName();
                    selectedSupplierName = expense.getSupplierName();
                    if (allSuppliers != null) {
                        for (SupplierEntity supplier : allSuppliers) {
                            if (supplier.getName().equals(expense.getSupplierName())) {
                                selectedSupplierId = supplier.getId();
                                break;
                            }
                        }
                    }
                }
                tvProcurementInfo.setText(info);
            }
        } else {
            // Farm expense - show block info
            if (expense.isFarmWide()) {
                tvBlockInfo.setText("Farm-Wide Expense");
                tvBlockSelector.setText("Farm-Wide");
            } else {
                String blockName = getBlockName(expense.getBlockId());
                tvBlockInfo.setText("Block " + blockName);
                tvBlockSelector.setText("Block " + blockName);
            }
            tvBlockInfo.setVisibility(View.VISIBLE);

            // Hide procurement info
            if (tvProcurementInfo != null) {
                tvProcurementInfo.setVisibility(View.GONE);
            }
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

        String sourceLabel = tvBlockInfo.getText() == null ? "" : tvBlockInfo.getText().toString().replace(" Expense", "");
        String sourceText = sourceLabel;
        String formattedDate = expense.getDate() > 0
                ? new java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(expense.getDate()) : "Date not recorded";
        if (!formattedDate.isEmpty()) sourceText += " · " + formattedDate;
        tvBlockInfo.setText(sourceText);
        ((TextView) findViewById(R.id.tvFinanceAmountDisplay)).setText(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount()));
        ((TextView) findViewById(R.id.tvFinanceDateDisplay)).setText(formattedDate);
        ((TextView) findViewById(R.id.tvFinanceSourceDisplay)).setText(sourceLabel);
        FinanceDetailChrome.addHero(this, R.id.tvCategoryName, R.id.tvBlockInfo, View.NO_ID);
        FinanceDetailChrome.addStats(this,
                new String[]{"Amount", "Date", "Source"},
                new int[]{R.id.tvFinanceAmountDisplay, R.id.tvFinanceDateDisplay, R.id.tvFinanceSourceDisplay});
    }

    private int getIconForCategory(ExpenseEntity.ExpenseCategory category) {
        switch (category) {
            case LAND_CLEARING:
                return R.drawable.ic_tractor;
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
            case PROCUREMENT:
                return R.drawable.ic_shopping_cart;
            case TRANSPORT:
                return R.drawable.ic_local_shipping;
            case QUALITY_TESTING:
                return R.drawable.ic_science;
            case STORAGE:
                return R.drawable.ic_warehouse;
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
        if (!sessionManager.canEditData()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "You don't have permission to edit", Snackbar.LENGTH_SHORT).show();
            return;
        }

        isEditMode = enable;

        btnEdit.setText(enable ? "Cancel" : "Edit");
        btnEdit.setVisibility(View.VISIBLE);
        btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        fabEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        footerPhotos.setVisibility(enable ? View.GONE : View.VISIBLE);
        if (headerEdit != null) headerEdit.setVisibility(enable ? View.GONE : View.VISIBLE);

        setFieldsEditable(enable);

        // ✅ Show/hide procurement section based on expense type
        if (enable) {
            if (expense != null && expense.isProcurementRelated()) {
                layoutProcurementSection.setVisibility(View.VISIBLE);
                // Pre-populate dropdowns with current values
                if (selectedSupplierName != null) {
                    etSupplier.setText(selectedSupplierName, false);
                }
                if (selectedBatchNumber != null) {
                    etBatch.setText(selectedBatchNumber, false);
                }
            } else {
                layoutProcurementSection.setVisibility(View.GONE);
            }

            // Show block selector only for farm expenses
            if (expense != null && !expense.isProcurementRelated()) {
                layoutBlockSelector.setVisibility(View.VISIBLE);
            } else {
                layoutBlockSelector.setVisibility(View.GONE);
            }
        } else {
            layoutProcurementSection.setVisibility(View.GONE);
            // Block selector visibility in view mode handled by displayExpenseData
        }

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

        // ✅ Only show block selector for non-procurement expenses
        if (expense != null && !expense.isProcurementRelated()) {
            tvBlockSelector.setEnabled(editable);
            if (!editable) {
                // In view mode, show block info but not editable
                if (expense.isFarmWide()) {
                    tvBlockSelector.setText("Farm-Wide");
                } else {
                    String blockName = getBlockName(expense.getBlockId());
                    tvBlockSelector.setText("Block " + blockName);
                }
            }
        }

        // ✅ Enable procurement fields only for procurement expenses
        if (expense != null && expense.isProcurementRelated()) {
            etSupplier.setEnabled(editable);
            etBatch.setEnabled(editable);
        }
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
                        expense.setBlockId(null);
                        tvBlockSelector.setText("Farm-Wide");
                    } else {
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

        // ✅ Update procurement fields if procurement category
        if (expense.isProcurementRelated()) {
            // Get supplier from dropdown
            String supplierStr = etSupplier.getText().toString().trim();
            if (!TextUtils.isEmpty(supplierStr) && allSuppliers != null) {
                for (SupplierEntity supplier : allSuppliers) {
                    String displayName = supplier.getName() + " (" + supplier.getType().getDisplayName() + ")";
                    if (displayName.equals(supplierStr)) {
                        selectedSupplierId = supplier.getId();
                        selectedSupplierName = supplier.getName();
                        break;
                    }
                }
            }

            // Get batch from dropdown
            String batchStr = etBatch.getText().toString().trim();
            if (!TextUtils.isEmpty(batchStr) && allBatches != null) {
                for (QualityBatchEntity batch : allBatches) {
                    String displayName = batch.getBatchNumber() + " - " +
                            batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)";
                    if (displayName.equals(batchStr)) {
                        selectedBatchId = batch.getId();
                        selectedBatchNumber = batch.getBatchNumber();
                        break;
                    }
                }
            }

            // Update expense with procurement fields
            expense.setBatchId(selectedBatchId);
            expense.setSupplierId(selectedSupplierId);
            expense.setBatchNumber(selectedBatchNumber);
            expense.setSupplierName(selectedSupplierName);
        }

        // Update expense object
        expense.setAmount(amount);
        expense.setDate(selectedDate.getTimeInMillis());
        expense.setDescription(etDescription.getText().toString().trim());
        expense.setUpdatedAt(System.currentTimeMillis());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        expenseRepository.updateExpense(expense, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(ExpenseDetailActivity.this,
                            Boolean.TRUE.equals(result)
                                    ? "Changes saved successfully!"
                                    : "Saved on this device — will sync when you're back online",
                            Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ExpenseDetailActivity.this,
                            Boolean.TRUE.equals(result)
                                    ? "Expense deleted"
                                    : "Expense deleted on this device — will sync when you're back online",
                            Toast.LENGTH_SHORT).show();
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
        if (photoRepository != null) {
            photoRepository.shutdown();
        }
        if (batchRepository != null) {
            batchRepository.shutdown();
        }
        if (supplierRepository != null) {
            supplierRepository.shutdown();
        }
        executor.shutdown();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
