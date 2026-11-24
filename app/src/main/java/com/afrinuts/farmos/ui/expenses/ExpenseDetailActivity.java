package com.afrinuts.farmos.ui.expenses;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.PhotoEntity;
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
import com.afrinuts.farmos.ui.photos.PhotoDetailActivity;
import com.bumptech.glide.Glide;
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
    private HorizontalScrollView photoThumbnailScroller;
    private LinearLayout photoThumbnailStrip;
    private LinearLayout expenseDetailContent;
    private LinearLayout expenseDetailsBody;
    private LinearLayout expenseReadout;
    private CardView procurementReadoutCard;
    private TextView readoutAmount, readoutDate, readoutCategory, readoutBlock, readoutNotes;
    private TextView procurementSupplier, procurementBatch;
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
    private ImageButton headerEdit;

    private boolean isEditMode = false;
    private Calendar selectedDate = Calendar.getInstance();

    // ✅ Procurement tracking fields
    private Long selectedSupplierId = null;
    private Long selectedBatchId = null;
    private String selectedSupplierName = null;
    private String selectedBatchNumber = null;
    private String procurementBatchNumber = null;

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
    }

    private void applyRoleBasedRestrictions() {
        if (sessionManager.isInvestor()) {
            finish();
            return;
        }
    }

    private void initViews() {
        expenseDetailContent = findViewById(R.id.expenseDetailContent);
        expenseDetailsBody = findViewById(R.id.expenseDetailsBody);
        // Category display
        tvCategoryIcon = findViewById(R.id.tvCategoryIcon);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvBlockInfo = findViewById(R.id.tvBlockInfo);
        btnPhotoGallery = findViewById(R.id.btnPhotoGallery);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);
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
        if (headerEdit != null) headerEdit.setVisibility(View.GONE);
        buildExpenseReadouts();
        configurePhotosCard();
    }

    private void installDetailFooter() {
        ViewGroup sourceRow = (ViewGroup) btnEdit.getParent();
        sourceRow.removeView(btnEdit);
        sourceRow.removeView(btnSave);
        sourceRow.removeView(btnDelete);
        sourceRow.setVisibility(View.GONE);
        btnDelete.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);
        btnEdit.setText("Edit");
        btnEdit.setIcon(null);
        btnSave.setIcon(null);
        btnDelete.setText("Delete");
        ((MaterialButton) btnDelete).setIcon(null);
        styleFooterButton(btnEdit, false);
        styleFooterButton(btnSave, true);
        styleFooterButton((MaterialButton) btnDelete, false);
        btnDelete.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF9E4DE));
        btnDelete.setTextColor(0xFFC13D25);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(0, dp(4), 0, dp(28));
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(16);
        expenseDetailContent.addView(footer, footerParams);
        footer.addView(btnEdit, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(52));
        saveParams.topMargin = dp(12);
        footer.addView(btnSave, saveParams);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-1, dp(52));
        deleteParams.topMargin = dp(12);
        footer.addView(btnDelete, deleteParams);
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
        button.setElevation(0);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void buildExpenseReadouts() {
        if (expenseDetailsBody == null || expenseReadout != null) return;
        expenseReadout = new LinearLayout(this);
        expenseReadout.setOrientation(LinearLayout.VERTICAL);
        expenseDetailsBody.addView(expenseReadout, 1);
        readoutAmount = addReadoutRow(expenseReadout, "Amount", true);
        readoutDate = addReadoutRow(expenseReadout, "Date", true);
        readoutCategory = addReadoutRow(expenseReadout, "Category", true);
        readoutBlock = addReadoutRow(expenseReadout, "Block", false);
        addReadoutLabel(expenseReadout, "Notes");
        readoutNotes = new TextView(this);
        readoutNotes.setTextColor(0xFF3F3B35);
        readoutNotes.setTextSize(14);
        readoutNotes.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_medium));
        readoutNotes.setLineSpacing(dp(3), 1f);
        expenseReadout.addView(readoutNotes);
        setExpenseInputVisibility(false);

        procurementReadoutCard = createSectionCard("Procurement Details");
        LinearLayout procurementBody = (LinearLayout) procurementReadoutCard.getChildAt(0);
        procurementSupplier = addReadoutRow(procurementBody, "Supplier", true);
        procurementBatch = addReadoutRow(procurementBody, "Batch", false);
        procurementReadoutCard.setVisibility(View.GONE);
        int photoIndex = expenseDetailContent.indexOfChild(findViewById(R.id.expensePhotosCard));
        expenseDetailContent.addView(procurementReadoutCard, Math.max(0, photoIndex + 1));
        if (layoutProcurementSection.getParent() instanceof ViewGroup) {
            ((ViewGroup) layoutProcurementSection.getParent()).removeView(layoutProcurementSection);
            expenseDetailsBody.addView(layoutProcurementSection);
        }
        if (layoutEditFields != null) layoutEditFields.setVisibility(View.GONE);
    }

    private CardView createSectionCard(String title) {
        CardView card = new CardView(this);
        card.setCardBackgroundColor(Color.WHITE);
        card.setRadius(dp(24));
        card.setCardElevation(0);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(18), dp(20), dp(18));
        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextColor(0xFF1C1B1A);
        heading.setTextSize(16);
        heading.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold));
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.bottomMargin = dp(8);
        body.addView(heading, headingParams);
        card.addView(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);
        return card;
    }

    private TextView addReadoutRow(LinearLayout parent, String label, boolean dividerAfter) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));
        TextView key = new TextView(this);
        key.setText(label);
        key.setTextColor(0xFF59544C);
        key.setTextSize(13);
        key.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_bold));
        row.addView(key, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView value = new TextView(this);
        value.setGravity(android.view.Gravity.END);
        value.setTextColor(0xFF1C1B1A);
        value.setTextSize(14.5f);
        value.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_bold));
        value.setMaxWidth(dp(210));
        value.setEllipsize(android.text.TextUtils.TruncateAt.END);
        value.setMaxLines(2);
        row.addView(value);
        parent.addView(row);
        if (dividerAfter) {
            View divider = new View(this);
            divider.setBackgroundColor(0xFFE9E1CF);
            parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        return value;
    }

    private void addReadoutLabel(LinearLayout parent, String label) {
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(0xFF59544C);
        title.setTextSize(13);
        title.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_bold));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(13);
        params.bottomMargin = dp(6);
        parent.addView(title, params);
    }

    private void setExpenseInputVisibility(boolean editing) {
        if (expenseReadout != null) expenseReadout.setVisibility(editing ? View.GONE : View.VISIBLE);
        View[] forms = { (View) etAmount.getParent(), (View) etDate.getParent(),
                (View) etDescription.getParent() };
        for (View form : forms) if (form != null) form.setVisibility(editing ? View.VISIBLE : View.GONE);
    }

    private void updateExpenseReadouts() {
        if (expense == null || readoutAmount == null) return;
        readoutAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount()));
        readoutDate.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(expense.getDate()));
        readoutCategory.setText(expense.getCategory() == null ? "—" : expense.getCategory().getDisplayName());
        String block = expense.isFarmWide() ? "Farm-Wide" : getBlockName(expense.getBlockId());
        readoutBlock.setText(TextUtils.isEmpty(block) ? "—" : (block.startsWith("Block ") ? block : "Block " + block));
        if (expense.isFarmWide()) readoutBlock.setText("Farm-Wide");
        ((View) readoutBlock.getParent()).setVisibility(expense.isProcurementRelated() ? View.GONE : View.VISIBLE);
        readoutNotes.setText(TextUtils.isEmpty(expense.getDescription()) ? "No notes" : expense.getDescription());
        expenseReadout.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        procurementReadoutCard.setVisibility(expense.isProcurementRelated() && !isEditMode ? View.VISIBLE : View.GONE);
        procurementSupplier.setText(TextUtils.isEmpty(expense.getSupplierName()) ? "—" : expense.getSupplierName());
        procurementBatch.setText(TextUtils.isEmpty(procurementBatchNumber) ? "—" : procurementBatchNumber);
    }

    private void configurePhotosCard() {
        if (btnPhotoGallery == null) return;
        if (tvPhotoCount.getParent() instanceof ViewGroup) {
            ((ViewGroup) tvPhotoCount.getParent()).removeView(tvPhotoCount);
        }
        btnPhotoGallery.removeAllViews();
        btnPhotoGallery.setOrientation(LinearLayout.VERTICAL);
        btnPhotoGallery.setPadding(dp(20), dp(18), dp(20), dp(18));
        btnPhotoGallery.setBackgroundColor(Color.WHITE);
        btnPhotoGallery.setClickable(false);
        btnPhotoGallery.setFocusable(false);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Photos");
        title.setTextColor(0xFF1C1B1A);
        title.setTextSize(16);
        title.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        tvPhotoCount.setTextColor(0xFF59544C);
        tvPhotoCount.setTextSize(13);
        tvPhotoCount.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_semibold));
        header.addView(tvPhotoCount);
        btnPhotoGallery.addView(header);
        photoThumbnailScroller = new HorizontalScrollView(this);
        photoThumbnailScroller.setHorizontalScrollBarEnabled(false);
        photoThumbnailScroller.setVisibility(View.GONE);
        photoThumbnailStrip = new LinearLayout(this);
        photoThumbnailStrip.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams stripParams = new LinearLayout.LayoutParams(-1, -2);
        stripParams.topMargin = dp(14);
        btnPhotoGallery.addView(photoThumbnailScroller, stripParams);
        photoThumbnailScroller.addView(photoThumbnailStrip);
        MaterialButton gallery = new MaterialButton(this);
        gallery.setText("View gallery");
        gallery.setIconResource(R.drawable.ic_photo);
        gallery.setIconTint(android.content.res.ColorStateList.valueOf(0xFF0A5530));
        gallery.setTextColor(0xFF0A5530);
        gallery.setTextSize(14);
        gallery.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_bold));
        gallery.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F0E7));
        gallery.setCornerRadius(dp(999));
        gallery.setInsetTop(0); gallery.setInsetBottom(0); gallery.setElevation(0);
        LinearLayout.LayoutParams galleryParams = new LinearLayout.LayoutParams(-2, dp(44));
        galleryParams.gravity = android.view.Gravity.END;
        galleryParams.topMargin = dp(14);
        btnPhotoGallery.addView(gallery, galleryParams);
        gallery.setOnClickListener(v -> openExpenseGallery());
    }

    private void openExpenseGallery() {
        Intent intent = new Intent(this, PhotoGalleryActivity.class);
        intent.putExtra("expense_id", expenseId);
        startActivity(intent);
    }

    private void displayExpensePhotos(List<PhotoEntity> photos) {
        if (photoThumbnailStrip == null) return;
        photoThumbnailStrip.removeAllViews();
        int count = photos == null ? 0 : photos.size();
        tvPhotoCount.setText(count + " " + (count == 1 ? "photo" : "photos"));
        photoThumbnailScroller.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        if (count == 0) return;
        int size = dp(88);
        for (PhotoEntity photo : photos) {
            ImageView thumbnail = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(dp(10));
            thumbnail.setLayoutParams(params);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFF3EAD8); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), 0xFFE9E1CF);
            thumbnail.setBackground(bg); thumbnail.setClipToOutline(true);
            String path = TextUtils.isEmpty(photo.getThumbnailPath()) ? photo.getFilePath() : photo.getThumbnailPath();
            Glide.with(this).load(path).placeholder(R.drawable.ic_photo_placeholder)
                    .error(R.drawable.ic_photo_placeholder).centerCrop().into(thumbnail);
            thumbnail.setOnClickListener(v -> {
                Intent intent = new Intent(this, PhotoDetailActivity.class);
                intent.putExtra(PhotoDetailActivity.EXTRA_PHOTO_ID, photo.getId());
                startActivity(intent);
            });
            photoThumbnailStrip.addView(thumbnail);
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

    private void setEditModeVisibility(boolean visible) {
        if (layoutEditFields != null) layoutEditFields.setVisibility(View.GONE);
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

        photoRepository.getPhotosForExpense(expenseId).observe(this, this::displayExpensePhotos);
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
            photoRepository = new PhotoRepository(ExpenseDetailActivity.this, farmId);
            batchRepository = new QualityBatchRepository(ExpenseDetailActivity.this, farmId);
            supplierRepository = new SupplierRepository(ExpenseDetailActivity.this, farmId);

            // ✅ Load all suppliers and batches for dropdowns
            allSuppliers = supplierRepository.getSuppliersSync();
            allBatches = batchRepository.getAllBatchesSync();
            if (expense.getBatchId() != null) {
                QualityBatchEntity linkedBatch = batchRepository.getBatchByIdSync(expense.getBatchId());
                if (linkedBatch != null) {
                    procurementBatchNumber = linkedBatch.getBatchNumber();
                    selectedBatchId = linkedBatch.getId();
                    selectedBatchNumber = linkedBatch.getBatchNumber();
                }
            }

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
                tvProcurementInfo.setVisibility(View.GONE);
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
            if (tvProcurementInfo != null) tvProcurementInfo.setVisibility(View.GONE);
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
        String location = currentFarm == null ? "" : currentFarm.getLocation();
        if (!TextUtils.isEmpty(location)) sourceText += " · " + location;
        tvBlockInfo.setText(sourceText);
        ((TextView) findViewById(R.id.tvFinanceAmountDisplay)).setText(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount()));
        ((TextView) findViewById(R.id.tvFinanceDateDisplay)).setText(formattedDate);
        ((TextView) findViewById(R.id.tvFinanceSourceDisplay)).setText(sourceLabel);
        FinanceDetailChrome.addHero(this, R.id.tvCategoryName, R.id.tvBlockInfo, View.NO_ID);
        FinanceDetailChrome.addStats(this,
                new String[]{"Amount", "Date", "Source"},
                new int[]{R.id.tvFinanceAmountDisplay, R.id.tvFinanceDateDisplay, R.id.tvFinanceSourceDisplay});
        decorateExpenseHero(iconRes);
        updateExpenseReadouts();
        moveExpenseSectionsIntoWireframeOrder();
    }

    private void decorateExpenseHero(int iconRes) {
        LinearLayout content = findViewById(R.id.expenseDetailContent);
        View heroView = content.findViewWithTag("finance-detail-hero");
        if (!(heroView instanceof CardView)) return;
        LinearLayout body = (LinearLayout) ((CardView) heroView).getChildAt(0);
        TextView name = body.findViewWithTag("finance-detail-name");
        TextView subtitle = body.findViewWithTag("finance-detail-subtitle");
        if (name == null || subtitle == null || body.findViewWithTag("expense-hero-icon") != null) return;
        // Procurement sources include both a batch number and supplier; let the
        // subtitle wrap so neither part is clipped inside the hero card.
        subtitle.setMaxLines(3);
        subtitle.setEllipsize(null);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        body.removeView(name);
        body.removeView(subtitle);
        FrameLayout icon = new FrameLayout(this);
        icon.setTag("expense-hero-icon");
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xFFE3F0E7);
        icon.setBackground(bg);
        ImageView image = new ImageView(this);
        image.setImageResource(iconRes);
        image.setColorFilter(0xFF0A5530);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(dp(26), dp(26), android.view.Gravity.CENTER);
        icon.addView(image, imageParams);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        iconParams.rightMargin = dp(14);
        row.addView(icon, iconParams);
        LinearLayout textStack = new LinearLayout(this);
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.addView(name);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(3);
        textStack.addView(subtitle, subtitleParams);
        row.addView(textStack, new LinearLayout.LayoutParams(0, -2, 1f));
        body.addView(row, 0);
        TextView grade = body.findViewWithTag("finance-detail-grade");
        if (grade != null) grade.setVisibility(View.GONE);
    }

    private void moveExpenseSectionsIntoWireframeOrder() {
        if (expenseDetailContent == null) return;
        View details = findViewById(R.id.expenseDetailsCard);
        View photos = findViewById(R.id.expensePhotosCard);
        if (details != null && photos != null) {
            expenseDetailContent.removeView(details);
            expenseDetailContent.addView(details, Math.min(2, expenseDetailContent.getChildCount()));
        }
        if (procurementReadoutCard != null && photos != null) {
            expenseDetailContent.removeView(procurementReadoutCard);
            int photoIndex = expenseDetailContent.indexOfChild(photos);
            expenseDetailContent.addView(procurementReadoutCard, photoIndex + 1);
        }
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

        btnEdit.setText("Edit");
        btnEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        if (fabEdit != null) fabEdit.setVisibility(View.GONE);
        if (headerEdit != null) headerEdit.setVisibility(View.GONE);

        setFieldsEditable(enable);
        setExpenseInputVisibility(enable);
        setEditModeVisibility(enable);
        updateExpenseReadouts();

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
                    procurementBatchNumber = selectedBatchId == null ? null : selectedBatchNumber;
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

        androidx.appcompat.app.AlertDialog alert = new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense? This action cannot be undone.")
                .setPositiveButton("Delete", (alertDialog, which) -> deleteExpense())
                .setNegativeButton("Cancel", null)
                .create();
        alert.setOnShowListener(ignored -> {
            styleDialogAction(alert.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE), 0xFFF9E4DE, 0xFFC13D25);
            styleDialogAction(alert.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE), 0xFFF3EAD8, 0xFF1C1B1A);
            if (alert.getWindow() != null) {
                GradientDrawable sheet = new GradientDrawable();
                sheet.setColor(Color.WHITE);
                sheet.setCornerRadius(dp(28));
                alert.getWindow().setBackgroundDrawable(sheet);
            }
        });
        alert.show();
    }

    private void styleDialogAction(Button button, int fill, int text) {
        if (button == null) return;
        GradientDrawable background = new GradientDrawable();
        background.setColor(fill);
        background.setCornerRadius(dp(999));
        button.setBackground(background);
        button.setTextColor(text);
        button.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold));
        button.setElevation(0);
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
