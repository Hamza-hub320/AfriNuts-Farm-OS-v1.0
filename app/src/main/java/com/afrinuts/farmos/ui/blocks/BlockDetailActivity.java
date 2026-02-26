package com.afrinuts.farmos.ui.blocks;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BlockDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BLOCK_ID = "block_id";

    private long blockId;
    private BlockEntity block;
    private AppDatabase database;

    // UI Elements
    private TextView blockNameText;
    private TextView statusBadge;
    private TextView farmInfoText;

    // Timeline views
    private TextView clearedDateText;
    private TextView plowedDateText;
    private TextView plantedDateText;
    // Timeline views - these are LinearLayouts (correct)
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

    // Edit mode
    private boolean isEditMode = false;
    private MaterialButton btnEdit;
    private MaterialButton btnSave;
    private Button btnDelete;
    private FloatingActionButton fabEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block_detail);

        // Get block ID from intent
        blockId = getIntent().getLongExtra(EXTRA_BLOCK_ID, -1);
        if (blockId == -1) {
            Toast.makeText(this, "Block not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load block data
        loadBlockData();
    }

    private void initViews() {
        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Header
        blockNameText = findViewById(R.id.blockNameText);
        statusBadge = findViewById(R.id.statusBadge);
        farmInfoText = findViewById(R.id.farmInfoText);

        // Timeline sections - these are LinearLayouts inside CardViews
        clearedSection = findViewById(R.id.clearedSection);
        plowedSection = findViewById(R.id.plowedSection);
        plantedSection = findViewById(R.id.plantedSection);

        clearedDateText = findViewById(R.id.clearedDateText);
        plowedDateText = findViewById(R.id.plowedDateText);
        plantedDateText = findViewById(R.id.plantedDateText);

        // Planting details
        plantingDetailsSection = findViewById(R.id.plantingDetailsSection);
        etSurvivalRate = findViewById(R.id.etSurvivalRate);
        etReplacementCount = findViewById(R.id.etReplacementCount);
        aliveTreesText = findViewById(R.id.aliveTreesText);
        deadTreesText = findViewById(R.id.deadTreesText);
        survivalProgress = findViewById(R.id.survivalProgress);

        // Notes
        etNotes = findViewById(R.id.etNotes);

        // Buttons
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        fabEdit = findViewById(R.id.fabEdit);

        // Set click listeners
        btnEdit.setOnClickListener(v -> enableEditMode(true));
        fabEdit.setOnClickListener(v -> enableEditMode(true));
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());

        // Make fields read-only initially
        // setFieldsEditable(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Block Details");
        }
    }

    private void loadBlockData() {
        new Thread(() -> {
            block = database.blockDao().getBlockById(blockId);

            runOnUiThread(() -> {
                if (block != null) {
                    displayBlockData();
                    setFieldsEditable(false);  // Now block is not null!
                } else {
                    Toast.makeText(this, "Block not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }).start();
    }

    private void displayBlockData() {
        // Header
        blockNameText.setText(block.getBlockName());

        // Status badge
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

        // Farm info
        farmInfoText.setText(String.format("Farm: AfriNuts Odienné | Size: %.1f ha", block.getHectareSize()));

        // Timeline - show relevant sections based on status
        if (block.getClearedDate() != null && block.getClearedDate() > 0) {
            clearedSection.setVisibility(View.VISIBLE);
            clearedDateText.setText(formatDate(block.getClearedDate()));
        } else {
            clearedSection.setVisibility(block.getStatus().ordinal() >= BlockEntity.BlockStatus.CLEARED.ordinal() ?
                    View.VISIBLE : View.GONE);
            clearedDateText.setText("Not recorded");
        }

        if (block.getPlowedDate() != null && block.getPlowedDate() > 0) {
            plowedSection.setVisibility(View.VISIBLE);
            plowedDateText.setText(formatDate(block.getPlowedDate()));
        } else {
            plowedSection.setVisibility(block.getStatus().ordinal() >= BlockEntity.BlockStatus.PLOWED.ordinal() ?
                    View.VISIBLE : View.GONE);
            plowedDateText.setText("Not recorded");
        }

        if (block.getPlantedDate() != null && block.getPlantedDate() > 0) {
            plantedSection.setVisibility(View.VISIBLE);
            plantedDateText.setText(formatDate(block.getPlantedDate()));
        } else {
            plantedSection.setVisibility(block.getStatus().ordinal() >= BlockEntity.BlockStatus.PLANTED.ordinal() ?
                    View.VISIBLE : View.GONE);
            plantedDateText.setText("Not recorded");
        }

        // Planting details - only visible if planted
        if (block.isPlanted()) {
            plantingDetailsSection.setVisibility(View.VISIBLE);

            double survivalRate = block.getSurvivalRate();
            int replacementCount = block.getReplacementCount();
            int aliveTrees = block.getAliveTrees();
            int deadTrees = block.getDeadTrees();

            etSurvivalRate.setText(String.valueOf(survivalRate));
            etReplacementCount.setText(String.valueOf(replacementCount));

            aliveTreesText.setText(String.format("🌱 Alive: %d trees", aliveTrees));
            deadTreesText.setText(String.format("💀 Dead: %d trees", deadTrees));

            survivalProgress.setProgress((int) survivalRate);
        } else {
            plantingDetailsSection.setVisibility(View.GONE);
        }

        // Notes
        if (block.getNotes() != null && !block.getNotes().isEmpty()) {
            etNotes.setText(block.getNotes());
        }
    }

    private String formatDate(long timestamp) {
        if (timestamp <= 0) return "Not recorded";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(timestamp);
    }

    private void enableEditMode(boolean enable) {
        isEditMode = enable;

        // Toggle visibility of edit/save buttons
        btnEdit.setVisibility(enable ? View.GONE : View.VISIBLE);
        btnSave.setVisibility(enable ? View.VISIBLE : View.GONE);
        fabEdit.setVisibility(enable ? View.GONE : View.VISIBLE);

        // Make fields editable
        setFieldsEditable(enable);

        if (enable) {
            // Show message
            Snackbar.make(findViewById(android.R.id.content),
                    "Edit mode enabled. Make your changes and save.",
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void setFieldsEditable(boolean editable) {

        if (block == null) {
            return; // Block not loaded yet
        }

        // Notes can always be edited in edit mode
        etNotes.setEnabled(editable);

        // Planting details can only be edited if block is planted
        if (block.isPlanted()) {
            etSurvivalRate.setEnabled(editable);
            etReplacementCount.setEnabled(editable);
        }

        // Status change will be handled via a dialog (more complex)
        // We'll implement that next
    }

    private void saveChanges() {
        // Collect updated values
        String notes = etNotes.getText().toString().trim();
        String survivalRateStr = etSurvivalRate.getText().toString().trim();
        String replacementCountStr = etReplacementCount.getText().toString().trim();

        // Validate if planted
        if (block.isPlanted()) {
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

            // Update block
            block.setSurvivalRate(survivalRate);
            block.setReplacementCount(replacementCount);
        }

        // Update notes
        block.setNotes(notes.isEmpty() ? null : notes);

        // Save to database
        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            database.blockDao().update(block);

            runOnUiThread(() -> {
                Toast.makeText(this, "Changes saved successfully!", Toast.LENGTH_SHORT).show();
                enableEditMode(false);
                displayBlockData(); // Refresh display
            });
        }).start();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Block")
                .setMessage("Are you sure you want to delete " + block.getBlockName() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteBlock())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBlock() {
        Toast.makeText(this, "Deleting block...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            database.blockDao().delete(block);

            runOnUiThread(() -> {
                Toast.makeText(this, "Block deleted", Toast.LENGTH_SHORT).show();
                finish(); // Go back to blocks list
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}