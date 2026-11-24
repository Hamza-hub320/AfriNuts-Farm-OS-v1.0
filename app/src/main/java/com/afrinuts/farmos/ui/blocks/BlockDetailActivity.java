package com.afrinuts.farmos.ui.blocks;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.blocks.RecordHarvestDialog;
import com.afrinuts.farmos.ui.blocks.StatusUpdateDialog;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockDetailActivity extends BaseActivity {

    public static final String EXTRA_BLOCK_ID = "block_id";
    private static final String TAG = "BlockDetailActivity";

    private long blockId;
    private BlockEntity block;
    private BlockRepository blockRepository;
    private PhotoRepository photoRepository;
    private long farmId = 1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private TextView blockNameText;
    private TextView statusBadge;
    private TextView farmInfoText;

    // Timeline views
    private TextView clearedDateText;
    private TextView plowedDateText;
    private TextView plantedDateText;
    private LinearLayout clearedSection;
    private LinearLayout plowedSection;
    private LinearLayout plantedSection;

    // Planting details
    private View plantingDetailsSection;
    private TextInputEditText etSurvivalRate;
    private TextInputEditText etReplacementCount;
    private TextView aliveTreesText;
    private TextView deadTreesText;
    private ProgressBar survivalProgress;

    // ✅ Harvest UI Elements
    private TextView tvTotalHarvest;
    private TextView tvLastHarvestDate;
    private TextView tvLastHarvestQuantity;
    private TextView tvHarvestCount;
    private TextView tvYieldPerHectare;
    private View harvestSection;
    private MaterialButton btnRecordHarvest;
    private MaterialButton btnRecordHarvestBottom;
    private LinearLayout harvestHistoryContainer;

    // Notes
    private TextInputEditText etNotes;

    // Photo gallery
    private LinearLayout btnPhotoGallery;
    private TextView tvPhotoCount;

    // Edit mode
    private boolean isEditMode = false;
    private MaterialButton btnEdit;
    private MaterialButton btnSave;
    private Button btnDelete;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected boolean checkRoleAccess() {
        return com.afrinuts.farmos.utils.AccessMap.canOpenScreen(
                getClass().getSimpleName(), sessionManager.getUserRole());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_detail);

        blockId = getIntent().getLongExtra(EXTRA_BLOCK_ID, -1);
        if (blockId == -1) {
            Toast.makeText(this, "Block not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();

        blockRepository = new BlockRepository(this, farmId);
        photoRepository = new PhotoRepository(this, farmId);

        loadBlockData();
    }

    private void initViews() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        blockNameText = findViewById(R.id.blockNameText);
        statusBadge = findViewById(R.id.statusBadge);
        farmInfoText = findViewById(R.id.farmInfoText);

        clearedSection = findViewById(R.id.clearedSection);
        plowedSection = findViewById(R.id.plowedSection);
        plantedSection = findViewById(R.id.plantedSection);

        clearedDateText = findViewById(R.id.clearedDateText);
        plowedDateText = findViewById(R.id.plowedDateText);
        plantedDateText = findViewById(R.id.plantedDateText);

        plantingDetailsSection = findViewById(R.id.plantingDetailsSection);
        etSurvivalRate = findViewById(R.id.etSurvivalRate);
        etReplacementCount = findViewById(R.id.etReplacementCount);
        aliveTreesText = findViewById(R.id.aliveTreesText);
        deadTreesText = findViewById(R.id.deadTreesText);
        survivalProgress = findViewById(R.id.survivalProgress);

        // ✅ Harvest UI
        tvTotalHarvest = findViewById(R.id.tvTotalHarvest);
        tvLastHarvestDate = findViewById(R.id.tvLastHarvestDate);
        tvLastHarvestQuantity = findViewById(R.id.tvLastHarvestQuantity);
        tvHarvestCount = findViewById(R.id.tvHarvestCount);
        tvYieldPerHectare = findViewById(R.id.tvYieldPerHectare);
        harvestSection = findViewById(R.id.harvestSection);
        btnRecordHarvest = findViewById(R.id.btnRecordHarvest);
        btnRecordHarvestBottom = findViewById(R.id.btnRecordHarvestBottom);
        harvestHistoryContainer = findViewById(R.id.harvestHistoryContainer);

        etNotes = findViewById(R.id.etNotes);

        btnPhotoGallery = findViewById(R.id.btnPhotoGallery);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v -> enableEditMode(true));
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnRecordHarvest.setOnClickListener(v -> showRecordHarvestDialog());
        btnRecordHarvestBottom.setOnClickListener(v -> showRecordHarvestDialog());

        btnPhotoGallery.setOnClickListener(v -> {
            Intent intent = new Intent(this, PhotoGalleryActivity.class);
            intent.putExtra("block_id", blockId);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.apply(this,
                findViewById(R.id.farmDetailTitle), findViewById(R.id.farmDetailBack),
                findViewById(R.id.farmDetailMore), "Block details" );
        findViewById(R.id.farmDetailEdit).setOnClickListener(v -> enableEditMode(true));
        findViewById(R.id.farmDetailMore).setOnClickListener(v -> showBlockMoreMenu(v));
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.addHero(this, R.id.blockNameText, R.id.farmInfoText, R.id.statusBadge);
        hideBlockHeroAvatar();
        com.afrinuts.farmos.ui.farm.FarmDetailChrome.addStats(this,
                new String[]{"Alive trees", "Survival rate", "Total harvest"},
                new int[]{R.id.aliveTreesText, R.id.etSurvivalRate, R.id.tvTotalHarvest });
        orderBlockStats();
        restyleBlockSectionHeadings(findViewById(android.R.id.content));
    }

    private void hideBlockHeroAvatar() {
        View hero = findViewById(android.R.id.content).findViewWithTag("farm-detail-hero");
        if (!(hero instanceof ViewGroup) || ((ViewGroup) hero).getChildCount() == 0) return;
        View top = ((ViewGroup) hero).getChildAt(0);
        if (top instanceof ViewGroup && ((ViewGroup) top).getChildCount() > 0) {
            ((ViewGroup) top).getChildAt(0).setVisibility(View.GONE);
        }
    }

    private void orderBlockStats() {
        View row = findViewById(android.R.id.content).findViewWithTag("farm-detail-stats");
        if (!(row instanceof ViewGroup)) return;
        ViewGroup stats = (ViewGroup) row;
        for (int i = 0; i < stats.getChildCount(); i++) {
            View card = stats.getChildAt(i);
            if (!(card instanceof ViewGroup)) continue;
            ViewGroup values = (ViewGroup) card;
            TextView unit = null;
            TextView caption = null;
            for (int j = 0; j < values.getChildCount(); j++) {
                View child = values.getChildAt(j);
                if (child instanceof TextView) {
                    TextView text = (TextView) child;
                    if (text.getTag() != null && text.getTag().toString().startsWith("farm-detail-stat-unit:")) unit = text;
                    else if (text.getTag() == null && text.getText() != null && text.getText().length() > 0) caption = text;
                }
            }
            if (unit != null && (unit.getText() == null || unit.getText().length() == 0)) values.removeView(unit);
            if (caption != null) {
                values.removeView(caption);
                values.addView(caption, 0);
            }
        }
    }

    private void showBlockMoreMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(this, anchor);
        menu.getMenu().add("Delete block").setOnMenuItemClickListener(item -> {
            confirmDelete();
            return true;
        });
        menu.show();
    }

    private void restyleBlockSectionHeadings(View view) {
        if (view instanceof androidx.cardview.widget.CardView && "farm-detail-hero".equals(view.getTag())) return;
        if (view instanceof androidx.cardview.widget.CardView) {
            TextView heading = findFirstText((ViewGroup) view);
            if (heading != null) {
                heading.setTextSize(16);
                heading.setTextColor(0xFF1C1B1A);
                heading.setTypeface(android.graphics.Typeface.create(
                        androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_extrabold),
                        android.graphics.Typeface.NORMAL));
            }
        }
        if (view instanceof android.widget.FrameLayout) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null && params.width > 0 && params.width <= dp(48)) view.setVisibility(View.GONE);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) restyleBlockSectionHeadings(group.getChildAt(i));
        }
    }

    private TextView findFirstText(View view) {
        if (view instanceof TextView && !(view instanceof Button)) {
            CharSequence value = ((TextView) view).getText();
            if (value != null && value.length() > 0) return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView text = findFirstText(group.getChildAt(i));
                if (text != null) return text;
            }
        }
        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

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

    private void loadBlockData() {
        blockRepository.getBlockById(blockId).observe(this, new Observer<BlockEntity>() {
            @Override
            public void onChanged(BlockEntity loadedBlock) {
                if (loadedBlock != null) {
                    block = loadedBlock;

                    findViewById(R.id.farmDetailEdit).setVisibility(
                            sessionManager.isInvestor() ? View.GONE : View.VISIBLE);

                    long correctFarmId = block.getFarmId();
                    if (correctFarmId != farmId) {
                        farmId = correctFarmId;
                        blockRepository = new BlockRepository(BlockDetailActivity.this, farmId);
                        photoRepository = new PhotoRepository(BlockDetailActivity.this, farmId);
                    }

                    displayBlockData();
                    loadFarmName(correctFarmId);
                    loadPhotoCount();
                    setFieldsEditable(false);

                    if (sessionManager.isInvestor()) {
                        btnEdit.setVisibility(View.GONE);
                        btnDelete.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(BlockDetailActivity.this,
                            "Block not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void displayBlockData() {
        if (block == null) return;

        blockNameText.setText(block.getBlockName());

        statusBadge.setText(block.getStatus().getDisplayName());
        int statusColor;
        switch (block.getStatus()) {
            case NOT_CLEARED:
                statusColor = getColor(android.R.color.holo_red_dark);
                break;
            case CLEARED:
                statusColor = getColor(android.R.color.holo_orange_dark);
                break;
            case PLOWED:
                statusColor = getColor(android.R.color.holo_blue_dark);
                break;
            case PLANTED:
                statusColor = getColor(android.R.color.holo_green_dark);
                break;
            case HARVESTED:
                statusColor = getColor(android.R.color.holo_purple);
                break;
            default:
                statusColor = getColor(R.color.primary);
        }
        statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));

        farmInfoText.setText(String.format(Locale.getDefault(), "Farm | Size: %.1f ha",
                block.getHectareSize()));

        updateTimelineVisibility();

        if (block.isPlanted()) {
            plantingDetailsSection.setVisibility(View.VISIBLE);

            double survivalRate = block.getSurvivalRate();
            int replacementCount = block.getReplacementCount();
            int aliveTrees = block.getAliveTrees();
            int deadTrees = block.getDeadTrees();

            etSurvivalRate.setText(new java.text.DecimalFormat("0.#").format(survivalRate) + "%");
            etReplacementCount.setText(String.valueOf(replacementCount));

            aliveTreesText.setText(String.format(Locale.getDefault(), "%,d", aliveTrees));
            deadTreesText.setText(String.format(Locale.getDefault(), "%,d", deadTrees));

            survivalProgress.setProgress((int) survivalRate);
        } else {
            plantingDetailsSection.setVisibility(View.GONE);
        }

        // ✅ Display Harvest Data
        displayHarvestData();

        if (block.getNotes() != null && !block.getNotes().isEmpty()) {
            etNotes.setText(block.getNotes());
        }
    }

    // ✅ Display Harvest Data
    private void displayHarvestData() {
        if (block == null) return;

        // Show harvest section if block has been harvested
        if (block.getHarvestCount() > 0) {
            harvestSection.setVisibility(View.VISIBLE);

            tvTotalHarvest.setText(block.getFormattedTotalHarvest());
            tvLastHarvestDate.setText(block.getFormattedLastHarvestDate());
            tvLastHarvestQuantity.setText(block.getLastHarvestQuantity() > 0 ?
                    String.format(Locale.getDefault(), "%,.0f kg", block.getLastHarvestQuantity()) : "N/A");
            tvHarvestCount.setText(block.getHarvestCount() + " harvest" + (block.getHarvestCount() > 1 ? "s" : ""));
            tvYieldPerHectare.setText(String.format(Locale.getDefault(), "%,.0f kg/ha", block.getYieldPerHectare()));

            // Show button to record new harvest
            btnRecordHarvest.setVisibility(View.VISIBLE);
            btnRecordHarvestBottom.setVisibility(View.VISIBLE);
            btnRecordHarvest.setText("Record Harvest");

            // Load harvest history
            loadHarvestHistory();

        } else if (block.isPlanted()) {
            // Block is planted but not yet harvested
            harvestSection.setVisibility(View.VISIBLE);
            tvTotalHarvest.setText("Not harvested yet");
            tvLastHarvestDate.setText("—");
            tvLastHarvestQuantity.setText("—");
            tvHarvestCount.setText("0 harvests");
            tvYieldPerHectare.setText("—");

            btnRecordHarvest.setVisibility(View.VISIBLE);
            btnRecordHarvestBottom.setVisibility(View.VISIBLE);
            btnRecordHarvest.setText("Record Harvest");

        } else {
            harvestSection.setVisibility(View.GONE);
            btnRecordHarvestBottom.setVisibility(View.GONE);
        }
    }
    private void loadHarvestHistory() {
        if (block == null || harvestHistoryContainer == null) return;

        harvestHistoryContainer.removeAllViews();

        List<BlockEntity.HarvestRecord> records = block.getHarvestRecords();
        if (records.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No harvest records yet");
            emptyText.setTextColor(getColor(R.color.text_dark));
            emptyText.setPadding(16, 16, 16, 16);
            harvestHistoryContainer.addView(emptyText);
            return;
        }

        // Show up to 5 most recent records (or all)
        int startIndex = Math.max(0, records.size() - 5);
        for (int i = startIndex; i < records.size(); i++) {
            final int index = i;
            BlockEntity.HarvestRecord record = records.get(i);
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_harvest_record, harvestHistoryContainer, false);

            TextView tvDate = itemView.findViewById(R.id.tvHarvestDate);
            TextView tvQuantity = itemView.findViewById(R.id.tvHarvestQuantity);
            TextView tvGrade = itemView.findViewById(R.id.tvHarvestGrade);
            TextView tvNotes = itemView.findViewById(R.id.tvHarvestNotes);
            View editButton = itemView.findViewById(R.id.btnEditRecord);

            tvDate.setText(record.getFormattedDate());
            tvQuantity.setText(String.format(Locale.getDefault(), "%,.0f kg", record.getQuantityKg()));
            tvGrade.setText(record.getGrade());

            if (record.getNotes() != null && !record.getNotes().isEmpty()) {
                tvNotes.setText(record.getNotes());
                tvNotes.setVisibility(View.VISIBLE);
            } else {
                tvNotes.setVisibility(View.GONE);
            }

            editButton.setOnClickListener(v -> showEditHarvestDialog(index));

            harvestHistoryContainer.addView(itemView);
        }
    }

    private void showEditHarvestDialog(int index) {
        if (block == null) return;

        EditHarvestDialog dialog = EditHarvestDialog.newInstance(block.getId(), index);
        dialog.setOnHarvestUpdatedListener(updatedBlock -> {
            this.block = updatedBlock;
            displayBlockData();
            Toast.makeText(this, "Harvest record updated", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "EditHarvestDialog");
    }

    private void showRecordHarvestDialog() {
        RecordHarvestDialog dialog = RecordHarvestDialog.newInstance(block.getId());
        dialog.setOnHarvestRecordedListener(updatedBlock -> {
            this.block = updatedBlock;
            displayBlockData();

            // ✅ Debug: verify data was saved
            blockRepository.debugBlockData(block.getId());

            Toast.makeText(this, "Harvest recorded successfully!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getSupportFragmentManager(), "RecordHarvestDialog");
    }

    private void updateTimelineVisibility() {
        if (block == null) return;

        if (block.getClearedDate() != null && block.getClearedDate() > 0) {
            clearedSection.setVisibility(View.VISIBLE);
            clearedDateText.setText(formatDate(block.getClearedDate()));
        } else {
            clearedSection.setVisibility(block.getStatus().ordinal() >=
                    BlockEntity.BlockStatus.CLEARED.ordinal() ? View.VISIBLE : View.GONE);
            clearedDateText.setText("Not recorded");
        }

        if (block.getPlowedDate() != null && block.getPlowedDate() > 0) {
            plowedSection.setVisibility(View.VISIBLE);
            plowedDateText.setText(formatDate(block.getPlowedDate()));
        } else {
            plowedSection.setVisibility(block.getStatus().ordinal() >=
                    BlockEntity.BlockStatus.PLOWED.ordinal() ? View.VISIBLE : View.GONE);
            plowedDateText.setText("Not recorded");
        }

        if (block.getPlantedDate() != null && block.getPlantedDate() > 0) {
            plantedSection.setVisibility(View.VISIBLE);
            plantedDateText.setText(formatDate(block.getPlantedDate()));
        } else {
            plantedSection.setVisibility(block.getStatus().ordinal() >=
                    BlockEntity.BlockStatus.PLANTED.ordinal() ? View.VISIBLE : View.GONE);
            plantedDateText.setText("Not recorded");
        }
        View clearedDivider = findViewById(R.id.timelineDividerCleared);
        View plowedDivider = findViewById(R.id.timelineDividerPlowed);
        clearedDivider.setVisibility(clearedSection.getVisibility() == View.VISIBLE
                && plowedSection.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        plowedDivider.setVisibility(plowedSection.getVisibility() == View.VISIBLE
                && plantedSection.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Not recorded";
        return dateFormat.format(timestamp);
    }

    private void showStatusUpdateDialog() {
        if (block == null) return;

        StatusUpdateDialog dialog = StatusUpdateDialog.newInstance(block);
        dialog.setOnStatusUpdatedListener(updatedBlock -> {
            this.block = updatedBlock;

            if (blockRepository != null) {
                // Use CompletableFuture pattern with proper error handling
                blockRepository.updateBlock(updatedBlock)
                        .thenRun(() -> {
                            runOnUiThread(() -> {
                                displayBlockData();
                                Toast.makeText(BlockDetailActivity.this,
                                        "Status updated successfully", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .exceptionally(throwable -> {
                            runOnUiThread(() ->
                                    Toast.makeText(BlockDetailActivity.this,
                                            "Error updating status: " + throwable.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                            return null;
                        });
            }
        });

        dialog.show(getSupportFragmentManager(), "StatusUpdateDialog");
    }

    private void enableEditMode(boolean enable) {
        if (!sessionManager.canEditData()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "You don't have permission to edit", Snackbar.LENGTH_SHORT).show();
            return;
        }

        isEditMode = enable;

        btnEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        findViewById(R.id.farmDetailEdit).setVisibility(
                enable || sessionManager.isInvestor() ? View.GONE : View.VISIBLE);

        statusBadge.setClickable(enable);
        statusBadge.setFocusable(enable);
        if (enable) {
            statusBadge.setOnClickListener(v -> showStatusUpdateDialog());
            statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getColor(R.color.accent)));
        } else {
            statusBadge.setOnClickListener(null);
            resetStatusBadgeColor();
        }

        setFieldsEditable(enable);

        if (enable) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Edit mode enabled. Click status badge to update block progress.",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void resetStatusBadgeColor() {
        if (block == null) return;

        int statusColor;
        switch (block.getStatus()) {
            case NOT_CLEARED:
                statusColor = getColor(android.R.color.holo_red_dark);
                break;
            case CLEARED:
                statusColor = getColor(android.R.color.holo_orange_dark);
                break;
            case PLOWED:
                statusColor = getColor(android.R.color.holo_blue_dark);
                break;
            case PLANTED:
                statusColor = getColor(android.R.color.holo_green_dark);
                break;
            default:
                statusColor = getColor(R.color.primary);
        }
        statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));
    }

    private void setFieldsEditable(boolean editable) {
        if (block == null) return;

        if (editable) {
            etSurvivalRate.setText(etSurvivalRate.getText().toString().replace("%", "").trim());
        }

        etNotes.setEnabled(editable);
        etNotes.setTextSize(14);
        etNotes.setTypeface(android.graphics.Typeface.create(
                androidx.core.content.res.ResourcesCompat.getFont(this, R.font.manrope_semibold),
                android.graphics.Typeface.NORMAL));
        etNotes.setTextColor(0xFF3D3A35);
        etNotes.setBackground(editable ? getDrawable(R.drawable.bg_rounded_description) : null);
        if (!editable) etNotes.setPadding(0, 0, 0, 0);

        if (block.isPlanted()) {
            plantingDetailsSection.setVisibility(View.VISIBLE);

            etSurvivalRate.setEnabled(editable);
            etReplacementCount.setEnabled(editable);

            if (editable) {
                etSurvivalRate.setBackgroundResource(R.drawable.edit_mode_background);
                etReplacementCount.setBackgroundResource(R.drawable.edit_mode_background);
            } else {
                etSurvivalRate.setBackgroundResource(0);
                etReplacementCount.setBackgroundResource(0);
            }
        } else {
            plantingDetailsSection.setVisibility(View.GONE);
        }
    }

    private void loadFarmName(long id) {
        executor.execute(() -> {
            FarmEntity farm = AppDatabase.getInstance(getApplicationContext()).farmDao().getFarmById(id);
            if (farm == null || isFinishing()) return;
            runOnUiThread(() -> {
                if (block != null && block.getFarmId() == id) {
                    farmInfoText.setText(String.format(Locale.getDefault(), "Farm: %s | Size: %.1f ha",
                            farm.getName(), block.getHectareSize()));
                }
            });
        });
    }

    private void saveChanges() {
        if (block == null || blockRepository == null) return;

        String notes = etNotes.getText().toString().trim();
        block.setNotes(notes.isEmpty() ? null : notes);

        if (block.isPlanted()) {
            String survivalRateStr = etSurvivalRate.getText().toString().replace("%", "").trim();
            String replacementCountStr = etReplacementCount.getText().toString().trim();

            if (TextUtils.isEmpty(survivalRateStr)) {
                etSurvivalRate.setError("Survival rate is required");
                return;
            }

            double survivalRate;
            try {
                survivalRate = Double.parseDouble(survivalRateStr);
                if (survivalRate < 0 || survivalRate > 100) {
                    etSurvivalRate.setError("Must be between 0-100");
                    return;
                }
            } catch (NumberFormatException e) {
                etSurvivalRate.setError("Invalid number");
                return;
            }

            int replacementCount;
            try {
                replacementCount = TextUtils.isEmpty(replacementCountStr) ?
                        0 : Integer.parseInt(replacementCountStr);
                if (replacementCount < 0) {
                    etReplacementCount.setError("Cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                etReplacementCount.setError("Invalid number");
                return;
            }

            block.setSurvivalRate(survivalRate);
            block.setReplacementCount(replacementCount);
        }

        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();
        btnSave.setEnabled(false);

        // Use CompletableFuture pattern with proper error handling
        blockRepository.updateBlock(block)
                .thenRun(() -> {
                    runOnUiThread(() -> {
                        Toast.makeText(BlockDetailActivity.this,
                                "Changes saved successfully!", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        enableEditMode(false);
                        displayBlockData();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() -> {
                        Toast.makeText(BlockDetailActivity.this,
                                "Error saving changes: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    });
                    return null;
                });
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "Only owners can delete blocks", Snackbar.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete block")
                .setMessage("Are you sure you want to delete " + block.getBlockName() +
                        "? This action cannot be undone.")
                .setPositiveButton("Delete block", (dialog, which) -> deleteBlock())
                .setNegativeButton("Cancel", null)
                .show().getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getColor(R.color.accent_ink));
    }

    private void deleteBlock() {
        Toast.makeText(this, "Deleting block...", Toast.LENGTH_SHORT).show();

        // Use CompletableFuture pattern with proper error handling
        blockRepository.deleteBlock(block)
                .thenRun(() -> {
                    runOnUiThread(() -> {
                        Toast.makeText(BlockDetailActivity.this, "Block deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                })
                .exceptionally(throwable -> {
                    runOnUiThread(() ->
                            Toast.makeText(BlockDetailActivity.this,
                                    "Error deleting block: " + throwable.getMessage(), Toast.LENGTH_LONG).show()
                    );
                    return null;
                });
    }

    private void loadPhotoCount() {
        if (photoRepository == null) return;

        // Use CompletableFuture pattern with the new async method
        photoRepository.getPhotoCountForBlockAsync(blockId)  // ← Note the Async suffix
                .thenAccept(count -> {
                    runOnUiThread(() -> {
                        tvPhotoCount.setText(count + " " + (count == 1 ? "photo" : "photos"));
                    });
                })
                .exceptionally(throwable -> {
                    Log.e(TAG, "Error loading photo count: " + throwable.getMessage());
                    runOnUiThread(() -> {
                        tvPhotoCount.setText("0 photos");
                    });
                    return null;
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (blockRepository != null) {
            blockRepository.shutdown();
        }
        if (photoRepository != null) {
            photoRepository.shutdown();
        }
        executor.shutdown();
    }
}
