package com.afrinuts.farmos.ui.revenue;

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
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.finance.FinanceDetailChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

public class RevenueDetailActivity extends BaseActivity {

    public static final String EXTRA_REVENUE_ID = "revenue_id";
    private static final String TAG = "RevenueDetailActivity";

    private long revenueId;
    private RevenueEntity revenue;
    private FarmEntity currentFarm;
    private List<QualityBatchEntity> allBatches;
    private List<SupplierEntity> allSuppliers;
    private QualityBatchEntity batch;
    private SupplierEntity supplier;
    private AppDatabase database;
    private RevenueRepository revenueRepository;
    private QualityBatchRepository batchRepository;
    private SupplierRepository supplierRepository;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements - Display
    private ImageView tvQualityIcon;
    private TextView tvQualityGrade;
    private TextView tvSource;
    private TextView tvQuantity;
    private TextView tvPricePerKg;
    private TextView tvTotalAmount;
    private TextView tvSaleDate;
    private TextView tvBuyer;
    private TextView tvNotes;
    private LinearLayout notesLayout;

    // UI Elements - Edit
    private TextInputEditText etQuantity;
    private TextInputEditText etPricePerKg;
    private TextInputEditText etSaleDate;
    private TextInputEditText etBuyer;
    private TextInputEditText etNotes;
    private MaterialAutoCompleteTextView etQuality;

    // Procurement UI Elements - Display
    private LinearLayout layoutProcurementInfo;
    private TextView tvBatchNumber;
    private TextView tvSupplierName;
    private TextView tvProcurementInfo;
    private MaterialButton btnViewBatch;
    private MaterialButton btnViewSupplier;

    // Procurement UI Elements - Edit
    private LinearLayout layoutProcurementSection;
    private TextInputLayout layoutSupplierSelector;
    private MaterialAutoCompleteTextView etSupplier;
    private TextInputLayout layoutBatchSelector;
    private MaterialAutoCompleteTextView etBatch;

    // Buttons
    private Button btnDelete;
    private MaterialButton btnEdit;
    private MaterialButton btnSave;
    private MaterialButton footerViewBatch;
    private ImageButton headerEdit;

    private boolean isEditMode = false;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    private Long selectedSupplierId = null;
    private Long selectedBatchId = null;
    private String selectedSupplierName = null;
    private String selectedBatchNumber = null;
    private RevenueEntity.QualityGrade selectedQuality = RevenueEntity.QualityGrade.PREMIUM;

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue_detail);

        if (!checkRoleAccess()) {
            Toast.makeText(this, "You don't have permission to view revenue details",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        revenueId = getIntent().getLongExtra(EXTRA_REVENUE_ID, -1);
        if (revenueId == -1) {
            Toast.makeText(this, "Revenue not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = AppDatabase.getInstance(this);
        revenueRepository = new RevenueRepository(this, farmId);
        batchRepository = new QualityBatchRepository(this, farmId);
        supplierRepository = new SupplierRepository(this, farmId);

        initViews();
        setupToolbar();
        setupRoleBasedUI();
        FinanceDetailChrome.apply(this, findViewById(R.id.farmDetailTitle),
                findViewById(R.id.farmDetailBack), findViewById(R.id.farmDetailEdit),
                findViewById(R.id.farmDetailMore), "Revenue details");
        headerEdit = findViewById(R.id.farmDetailEdit);
        loadData();
    }

    private void initViews() {
        // Display views — the layout may still use R.id.tvHarvestDate /
        // R.id.etHarvestDate; the field names in code are now sale-date
        // semantic, but the view IDs are unchanged to avoid a layout diff.
        tvQualityIcon = findViewById(R.id.tvQualityIcon);
        tvQualityGrade = findViewById(R.id.tvQualityGrade);
        tvSource = findViewById(R.id.tvSource);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvPricePerKg = findViewById(R.id.tvPricePerKg);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvSaleDate = findViewById(R.id.tvHarvestDate);
        tvBuyer = findViewById(R.id.tvBuyer);
        tvNotes = findViewById(R.id.tvNotes);
        notesLayout = findViewById(R.id.notesLayout);

        // Edit-mode fields
        etQuantity = findViewById(R.id.etQuantity);
        etPricePerKg = findViewById(R.id.etPricePerKg);
        etSaleDate = findViewById(R.id.etHarvestDate);
        etBuyer = findViewById(R.id.etBuyer);
        etNotes = findViewById(R.id.etNotes);
        etQuality = findViewById(R.id.etQuality);

        // Procurement display
        layoutProcurementInfo = findViewById(R.id.layoutProcurementInfo);
        tvBatchNumber = findViewById(R.id.tvBatchNumber);
        tvSupplierName = findViewById(R.id.tvSupplierName);
        tvProcurementInfo = findViewById(R.id.tvProcurementInfo);
        btnViewBatch = findViewById(R.id.btnViewBatch);
        btnViewSupplier = findViewById(R.id.btnViewSupplier);

        // Procurement edit
        layoutProcurementSection = findViewById(R.id.layoutProcurementSection);
        layoutSupplierSelector = findViewById(R.id.layoutSupplierSelector);
        etSupplier = findViewById(R.id.etSupplier);
        layoutBatchSelector = findViewById(R.id.layoutBatchSelector);
        etBatch = findViewById(R.id.etBatch);

        // Buttons
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);

        btnDelete.setOnClickListener(v -> confirmDelete());

        if (btnEdit != null) btnEdit.setOnClickListener(v -> enableEditMode(!isEditMode));

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveChanges());
        }

        if (btnViewBatch != null) {
            btnViewBatch.setOnClickListener(v -> viewBatch());
        }

        if (btnViewSupplier != null) {
            btnViewSupplier.setOnClickListener(v -> viewSupplier());
        }

        if (etSupplier != null) {
            etSupplier.setOnItemClickListener((parent, view, position, id) -> {
                if (allSuppliers != null && position < allSuppliers.size()) {
                    SupplierEntity selectedSupplier = allSuppliers.get(position);
                    selectedSupplierId = selectedSupplier.getId();
                    selectedSupplierName = selectedSupplier.getName();
                    loadBatchesForSupplier(selectedSupplierId);
                }
            });
        }

        if (etBatch != null) {
            etBatch.setOnItemClickListener((parent, view, position, id) -> {
                if (allBatches != null && position < allBatches.size()) {
                    QualityBatchEntity selectedBatch = allBatches.get(position);
                    selectedBatchId = selectedBatch.getId();
                    selectedBatchNumber = selectedBatch.getBatchNumber();
                }
            });
        }

        setupDatePicker();

        setFieldsEditable(false);
        setEditModeVisibility(false);
    }

    private void setupToolbar() {
        installDetailFooter();
        headerEdit = findViewById(R.id.farmDetailEdit);
    }

    private void installDetailFooter() {
        ScrollView scroll = findViewById(R.id.revenueDetailScroll);
        LinearLayout content = scroll != null && scroll.getChildAt(0) instanceof LinearLayout
                ? (LinearLayout) scroll.getChildAt(0) : null;
        if (content == null) return;
        ViewGroup sourceRow = (ViewGroup) btnEdit.getParent();
        sourceRow.removeView(btnEdit);
        sourceRow.removeView(btnSave);
        sourceRow.setVisibility(View.GONE);
        btnDelete.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);

        footerViewBatch = new MaterialButton(this);
        footerViewBatch.setText("View batch");
        footerViewBatch.setTextSize(16);
        footerViewBatch.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
        footerViewBatch.setTextColor(Color.WHITE);
        footerViewBatch.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF0E6B3C));
        footerViewBatch.setCornerRadius(dp(999));
        footerViewBatch.setMinHeight(dp(56));
        footerViewBatch.setInsetTop(0);
        footerViewBatch.setInsetBottom(0);
        footerViewBatch.setElevation(0f);
        footerViewBatch.setStateListAnimator(null);
        footerViewBatch.setOnClickListener(v -> btnViewBatch.performClick());

        styleFooterButton(btnEdit, false);
        styleFooterButton(btnSave, true);
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(dp(20), dp(8), dp(20), dp(12));
        footer.setBackgroundColor(0xFFF6F1E7);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(56), 1f);
        left.setMargins(0, 0, dp(12), 0);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(56), 1f);
        footer.addView(btnEdit, left);
        footer.addView(btnSave, new LinearLayout.LayoutParams(0, dp(56), 1f));
        footer.addView(footerViewBatch, right);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(8);
        footerParams.bottomMargin = dp(12);
        content.addView(footer, footerParams);
    }

    private void styleFooterButton(MaterialButton button, boolean primary) {
        button.setIcon(null);
        button.setTextSize(16);
        button.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold), android.graphics.Typeface.NORMAL));
        button.setTextColor(primary ? Color.WHITE : 0xFF1C1B1A);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary ? 0xFF0E6B3C : 0xFFF3EAD8));
        button.setCornerRadius(dp(999));
        button.setMinHeight(dp(56));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setElevation(0f);
        button.setStateListAnimator(null);
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

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    private void setupRoleBasedUI() {
        if (sessionManager.canEditData()) {
            if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
        } else {
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
        }

        if (sessionManager.isOwner()) {
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        } else {
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        }
    }

    private void setupDatePicker() {
        if (etSaleDate != null) {
            etSaleDate.setOnClickListener(v -> showDatePicker());
            etSaleDate.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    showDatePicker();
                }
            });
        }
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
        if (etSaleDate != null) {
            etSaleDate.setText(dateFormat.format(selectedDate.getTime()));
        }
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Loading revenue with ID: " + revenueId);

                if (database == null) {
                    Log.e(TAG, "Database is null");
                    runOnUiThread(() -> {
                        Toast.makeText(RevenueDetailActivity.this,
                                "Database error", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                revenue = database.revenueDao().getRevenueById(revenueId);

                if (revenue == null) {
                    Log.e(TAG, "Revenue not found in database with ID: " + revenueId);
                    runOnUiThread(() -> {
                        Toast.makeText(RevenueDetailActivity.this,
                                "Revenue not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                Log.d(TAG, "Revenue loaded: " + revenue.getId() +
                        ", farmId: " + revenue.getFarmId() +
                        ", quality: " + revenue.getQuality());

                farmId = revenue.getFarmId();
                revenueRepository = new RevenueRepository(RevenueDetailActivity.this, farmId);
                batchRepository = new QualityBatchRepository(RevenueDetailActivity.this, farmId);
                supplierRepository = new SupplierRepository(RevenueDetailActivity.this, farmId);

                currentFarm = database.farmDao().getFirstFarm();

                allSuppliers = supplierRepository.getSuppliersSync();
                allBatches = batchRepository.getAllBatchesSync();

                if (revenue.getBatchId() != null && revenue.getBatchId() > 0) {
                    batch = database.qualityBatchDao().getBatchById(revenue.getBatchId());
                    if (batch != null) {
                        Log.d(TAG, "Loaded batch: " + batch.getBatchNumber());
                        selectedBatchId = batch.getId();
                        selectedBatchNumber = batch.getBatchNumber();
                    }
                }

                if (revenue.getSupplierId() != null && revenue.getSupplierId() > 0) {
                    supplier = database.supplierDao().getSupplierById(revenue.getSupplierId());
                    if (supplier != null) {
                        Log.d(TAG, "Loaded supplier: " + supplier.getName());
                        selectedSupplierId = supplier.getId();
                        selectedSupplierName = supplier.getName();
                    }
                }

                if (revenue.getQuality() != null) {
                    selectedQuality = revenue.getQuality();
                }

                runOnUiThread(() -> {
                    try {
                        setupQualityDropdown();
                        setupProcurementDropdowns();
                        displayRevenueData();
                        setEditModeVisibility(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error displaying revenue data", e);
                        Toast.makeText(RevenueDetailActivity.this,
                                "Error displaying revenue: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Critical error in loadData", e);
                runOnUiThread(() -> {
                    Toast.makeText(RevenueDetailActivity.this,
                            "Error loading revenue: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void setupQualityDropdown() {
        RevenueEntity.QualityGrade[] grades = RevenueEntity.QualityGrade.values();
        List<String> gradeDisplayNames = new ArrayList<>();
        for (RevenueEntity.QualityGrade grade : grades) {
            gradeDisplayNames.add(grade.getDisplayName() + " Grade");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                gradeDisplayNames
        );
        etQuality.setAdapter(adapter);

        etQuality.setOnItemClickListener((parent, view, position, id) -> {
            selectedQuality = grades[position];
        });
    }

    private void setupProcurementDropdowns() {
        if (allSuppliers != null && etSupplier != null) {
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
        }

        if (allBatches != null && etBatch != null) {
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
    }

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

    private void displayRevenueData() {
        if (revenue == null) return;

        // Quality Grade
        switch (revenue.getQuality()) {
            case PREMIUM:
                tvQualityIcon.setImageResource(R.drawable.ic_star);
                tvQualityIcon.setColorFilter(getColor(R.color.accent_ink));
                break;
            case STANDARD:
                tvQualityIcon.setImageResource(R.drawable.ic_check_circle);
                tvQualityIcon.setColorFilter(getColor(R.color.primary));
                break;
            case PROCESSING:
                tvQualityIcon.setImageResource(R.drawable.ic_construction);
                tvQualityIcon.setColorFilter(getColor(R.color.navy));
                break;
        }

        tvQualityGrade.setText(revenue.getQuality().getDisplayName() + " Grade");
        selectedQuality = revenue.getQuality();

        // The hero subtitle combines the existing buyer, supplier and date fields.
        if (tvSource != null) {
            String buyer = revenue.getBuyer() == null || revenue.getBuyer().trim().isEmpty() ? "Buyer not recorded" : revenue.getBuyer().trim();
            String supplierName = revenue.getSupplierName() == null || revenue.getSupplierName().trim().isEmpty() ? "Supplier not recorded" : revenue.getSupplierName().trim();
            tvSource.setText(buyer + " · " + supplierName + " · " + dateFormat.format(revenue.getCreatedAt()));
        }

        // Financial details
        tvQuantity.setText(String.format(Locale.getDefault(), "%.1f kg", revenue.getQuantityKg()));
        tvPricePerKg.setText(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getPricePerKg()));
        tvTotalAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getTotalAmount()));
        tvSaleDate.setText(dateFormat.format(revenue.getCreatedAt()));

        // Edit fields
        etQuantity.setText(String.valueOf(revenue.getQuantityKg()));
        etPricePerKg.setText(String.valueOf(revenue.getPricePerKg()));
        selectedDate.setTimeInMillis(revenue.getCreatedAt());
        updateDateLabel();
        etQuality.setText(revenue.getQuality().getDisplayName() + " Grade", false);

        // Buyer
        if (revenue.getBuyer() != null && !revenue.getBuyer().isEmpty()) {
            tvBuyer.setText(revenue.getBuyer());
            etBuyer.setText(revenue.getBuyer());
        } else {
            tvBuyer.setText("No buyer recorded");
            etBuyer.setText("");
        }

        // Notes
        if (revenue.getNotes() != null && !revenue.getNotes().isEmpty()) {
            tvNotes.setText(revenue.getNotes());
            etNotes.setText(revenue.getNotes());
            notesLayout.setVisibility(View.VISIBLE);
        } else {
            tvNotes.setText("No notes recorded.");
            notesLayout.setVisibility(View.VISIBLE);
            etNotes.setText("");
        }

        // Procurement info
        if (revenue.isProcurementRelated()) {
            layoutProcurementInfo.setVisibility(View.VISIBLE);

            if (revenue.getBatchNumber() != null && !revenue.getBatchNumber().isEmpty()) {
                tvBatchNumber.setText("#" + revenue.getBatchNumber());
                tvBatchNumber.setVisibility(View.VISIBLE);
                btnViewBatch.setVisibility(View.VISIBLE);
                etBatch.setText(revenue.getBatchNumber(), false);
            } else {
                tvBatchNumber.setVisibility(View.GONE);
                btnViewBatch.setVisibility(View.GONE);
            }

            if (revenue.getSupplierName() != null && !revenue.getSupplierName().isEmpty()) {
                tvSupplierName.setText(revenue.getSupplierName());
                tvSupplierName.setVisibility(View.VISIBLE);
                btnViewSupplier.setVisibility(View.VISIBLE);
                etSupplier.setText(revenue.getSupplierName(), false);
            } else {
                tvSupplierName.setVisibility(View.GONE);
                btnViewSupplier.setVisibility(View.GONE);
            }

            tvProcurementInfo.setText(revenue.getProcurementInfo());
        } else {
            layoutProcurementInfo.setVisibility(View.GONE);
        }

        if (revenue.getBatchNumber() != null && !revenue.getBatchNumber().trim().isEmpty()) {
            tvBatchNumber.setText(revenue.getBatchNumber());
        } else {
            tvBatchNumber.setText("Processing center sale");
        }
        FinanceDetailChrome.addHero(this, R.id.tvBatchNumber, R.id.tvSource, R.id.tvQualityGrade);
        FinanceDetailChrome.addStats(this,
                new String[]{"Quantity", "Price / kg", "Total (XAF)"},
                new int[]{R.id.tvQuantity, R.id.tvPricePerKg, R.id.tvTotalAmount});
        FinanceDetailChrome.splitRevenueSections(this);
        FinanceDetailChrome.bindRevenueBatch(this, batch);
        int supplierBatchCount = 0;
        if (supplier != null && allBatches != null) {
            for (QualityBatchEntity linkedBatch : allBatches) {
                if (linkedBatch.getSupplierId() != null && linkedBatch.getSupplierId() == supplier.getId()) {
                    supplierBatchCount++;
                }
            }
        }
        FinanceDetailChrome.bindRevenueSupplier(this, supplier, supplierBatchCount);
        footerViewBatch.setVisibility(revenue.getBatchId() == null ? View.GONE : View.VISIBLE);
        if (btnViewBatch != null) btnViewBatch.setVisibility(View.GONE);
        FinanceDetailChrome.apply(this, findViewById(R.id.farmDetailTitle),
                findViewById(R.id.farmDetailBack), findViewById(R.id.farmDetailEdit),
                findViewById(R.id.farmDetailMore), "Revenue details");
        moveFooterToContentEnd();
        headerEdit = findViewById(R.id.farmDetailEdit);
    }

    private void moveFooterToContentEnd() {
        if (footerViewBatch == null || !(footerViewBatch.getParent() instanceof ViewGroup)) return;
        View footer = (View) footerViewBatch.getParent();
        ViewGroup oldParent = (ViewGroup) footer.getParent();
        if (oldParent == null) return;
        oldParent.removeView(footer);
        ScrollView scroll = findViewById(R.id.revenueDetailScroll);
        if (scroll == null || scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
        ((LinearLayout) scroll.getChildAt(0)).addView(footer);
    }

    private void setFieldsEditable(boolean editable) {
        if (etQuantity != null) etQuantity.setEnabled(editable);
        if (etPricePerKg != null) etPricePerKg.setEnabled(editable);
        if (etSaleDate != null) etSaleDate.setEnabled(editable);
        if (etBuyer != null) etBuyer.setEnabled(editable);
        if (etNotes != null) etNotes.setEnabled(editable);
        if (etQuality != null) etQuality.setEnabled(editable);

        if (revenue != null && revenue.isProcurementRelated()) {
            if (etSupplier != null) etSupplier.setEnabled(editable);
            if (etBatch != null) etBatch.setEnabled(editable);
        }
    }

    private void setEditModeVisibility(boolean visible) {
        int displayVisibility = visible ? View.GONE : View.VISIBLE;
        tvQuantity.setVisibility(displayVisibility);
        tvPricePerKg.setVisibility(displayVisibility);
        tvTotalAmount.setVisibility(displayVisibility);
        tvSaleDate.setVisibility(displayVisibility);
        tvBuyer.setVisibility(displayVisibility);

        CardView layoutEditFields = findViewById(R.id.layoutEditFields);
        if (layoutEditFields != null) {
            layoutEditFields.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        int editVisibility = visible ? View.VISIBLE : View.GONE;
        if (etQuantity != null) etQuantity.setVisibility(editVisibility);
        if (etPricePerKg != null) etPricePerKg.setVisibility(editVisibility);
        if (etSaleDate != null) etSaleDate.setVisibility(editVisibility);
        if (etBuyer != null) etBuyer.setVisibility(editVisibility);
        if (etNotes != null) etNotes.setVisibility(editVisibility);
        if (etQuality != null) etQuality.setVisibility(editVisibility);

        if (revenue != null && revenue.isProcurementRelated()) {
            if (layoutProcurementSection != null) {
                layoutProcurementSection.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            if (etSupplier != null) etSupplier.setVisibility(editVisibility);
            if (etBatch != null) etBatch.setVisibility(editVisibility);
        } else {
            if (layoutProcurementSection != null) {
                layoutProcurementSection.setVisibility(View.GONE);
            }
        }

        if (visible) {
            if (notesLayout != null) notesLayout.setVisibility(View.VISIBLE);
        } else if (notesLayout != null) {
            notesLayout.setVisibility(View.VISIBLE);
        }
        if (footerViewBatch != null) footerViewBatch.setVisibility(visible || revenue == null || revenue.getBatchId() == null ? View.GONE : View.VISIBLE);
        if (headerEdit != null) headerEdit.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void enableEditMode(boolean enable) {
        if (!sessionManager.canEditData()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "You don't have permission to edit", Snackbar.LENGTH_SHORT).show();
            return;
        }

        isEditMode = enable;

        if (btnEdit != null) {
            btnEdit.setText(enable ? "Cancel" : "Edit");
            btnEdit.setVisibility(View.VISIBLE);
        }
        if (btnSave != null) {
            btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        }

        setFieldsEditable(enable);
        setEditModeVisibility(enable);

        if (enable) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Edit mode enabled. Make your changes and save.",
                    Snackbar.LENGTH_LONG).show();
        } else {
            displayRevenueData();
        }
    }

    private void viewBatch() {
        if (batch == null) {
            Toast.makeText(this, "Batch not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity.class);
        intent.putExtra(com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity.EXTRA_BATCH_ID, batch.getId());
        startActivity(intent);
    }

    private void viewSupplier() {
        if (supplier == null) {
            Toast.makeText(this, "Supplier not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.class);
        intent.putExtra(com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.EXTRA_SUPPLIER_ID, supplier.getId());
        startActivity(intent);
    }

    private void saveChanges() {
        if (revenue == null || revenueRepository == null) return;

        String quantityStr = etQuantity.getText().toString().trim();
        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Quantity is required");
            return;
        }

        double quantity;
        try {
            quantity = Double.parseDouble(quantityStr);
            if (quantity <= 0) {
                etQuantity.setError("Quantity must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            etQuantity.setError("Invalid quantity");
            return;
        }

        String priceStr = etPricePerKg.getText().toString().trim();
        if (TextUtils.isEmpty(priceStr)) {
            etPricePerKg.setError("Price is required");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price <= 0) {
                etPricePerKg.setError("Price must be positive");
                return;
            }
        } catch (NumberFormatException e) {
            etPricePerKg.setError("Invalid price");
            return;
        }

        if (revenue.isProcurementRelated()) {
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
        }

        revenue.setQuantityKg(quantity);
        revenue.setPricePerKg(price);
        revenue.setCreatedAt(selectedDate.getTimeInMillis());
        revenue.setQuality(selectedQuality);
        revenue.setBuyer(etBuyer.getText().toString().trim());
        revenue.setNotes(etNotes.getText().toString().trim());

        if (revenue.isProcurementRelated()) {
            revenue.setBatchId(selectedBatchId);
            revenue.setSupplierId(selectedSupplierId);
            revenue.setBatchNumber(selectedBatchNumber);
            revenue.setSupplierName(selectedSupplierName);
        }

        revenue.setUpdatedAt(System.currentTimeMillis());

        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
        }

        revenueRepository.updateRevenue(revenue, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(RevenueDetailActivity.this,
                            "Changes saved successfully!", Toast.LENGTH_SHORT).show();
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                    }
                    enableEditMode(false);
                    loadData();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(RevenueDetailActivity.this,
                            "Error saving changes: " + error, Toast.LENGTH_LONG).show();
                    if (btnSave != null) {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save");
                    }
                });
            }
        });
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Only owners can delete revenue records", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Revenue")
                .setMessage("Are you sure you want to delete this revenue record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRevenue())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRevenue() {
        Toast.makeText(this, "Deleting revenue...", Toast.LENGTH_SHORT).show();

        revenueRepository.deleteRevenue(revenue, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                runOnUiThread(() -> {
                    Toast.makeText(RevenueDetailActivity.this,
                            "Revenue deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(RevenueDetailActivity.this,
                                "Error deleting revenue: " + error, Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (revenueRepository != null) {
            revenueRepository.shutdown();
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
