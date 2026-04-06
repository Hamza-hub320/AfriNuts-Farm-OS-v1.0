package com.afrinuts.farmos.ui.blocks;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.afrinuts.farmos.data.repository.PhotoRepository;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.ui.photos.PhotoGalleryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    @Override
    protected boolean checkRoleAccess() {
        return !sessionManager.isInvestor();
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

        // Initialize repositories with long farmId
        blockRepository = new BlockRepository(this, farmId);
        photoRepository = new PhotoRepository(this, farmId);

        loadBlockData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

        etNotes = findViewById(R.id.etNotes);

        btnPhotoGallery = findViewById(R.id.btnPhotoGallery);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v -> enableEditMode(true));
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());

        btnPhotoGallery.setOnClickListener(v -> {
            Intent intent = new Intent(this, PhotoGalleryActivity.class);
            intent.putExtra("block_id", blockId);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_backward);
            getSupportActionBar().setTitle("Block Details");
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

                    long correctFarmId = block.getFarmId();
                    if (correctFarmId != farmId) {
                        farmId = correctFarmId;

                        blockRepository = new BlockRepository(BlockDetailActivity.this, farmId);
                        photoRepository = new PhotoRepository(BlockDetailActivity.this, farmId);
                    }

                    displayBlockData();
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
            default:
                statusColor = getColor(R.color.primary);
        }
        statusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));

        farmInfoText.setText(String.format("Farm: AfriNuts Odienné | Size: %.1f ha",
                block.getHectareSize()));

        updateTimelineVisibility();

        if (block.isPlanted()) {
            plantingDetailsSection.setVisibility(View.VISIBLE);

            double survivalRate = block.getSurvivalRate();
            int replacementCount = block.getReplacementCount();
            int aliveTrees = block.getAliveTrees();
            int deadTrees = block.getDeadTrees();

            etSurvivalRate.setText(String.valueOf(survivalRate));
            etReplacementCount.setText(String.valueOf(replacementCount));

            aliveTreesText.setText(String.format("Alive: %d trees", aliveTrees));
            deadTreesText.setText(String.format("Dead: %d trees", deadTrees));

            survivalProgress.setProgress((int) survivalRate);
        } else {
            plantingDetailsSection.setVisibility(View.GONE);
        }

        if (block.getNotes() != null && !block.getNotes().isEmpty()) {
            etNotes.setText(block.getNotes());
        }
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
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Not recorded";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(timestamp);
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

        etNotes.setEnabled(editable);

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

    private void saveChanges() {
        if (block == null || blockRepository == null) return;

        String notes = etNotes.getText().toString().trim();
        block.setNotes(notes.isEmpty() ? null : notes);

        if (block.isPlanted()) {
            String survivalRateStr = etSurvivalRate.getText().toString().trim();
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
                .setTitle("Delete Block")
                .setMessage("Are you sure you want to delete " + block.getBlockName() +
                        "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteBlock())
                .setNegativeButton("Cancel", null)
                .show();
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