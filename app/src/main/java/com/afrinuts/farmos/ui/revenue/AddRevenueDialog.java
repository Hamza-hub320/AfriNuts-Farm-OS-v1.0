package com.afrinuts.farmos.ui.revenue;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
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

public class AddRevenueDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;

    // UI Elements
    private TextInputEditText etQuantity;
    private TextInputEditText etPricePerKg;
    private TextInputEditText etHarvestDate;
    private MaterialButtonToggleGroup toggleRevenueType;
    private Button btnPerBlock;
    private Button btnProcessingCenter;
    private TextInputLayout layoutBlockSelector;
    private MaterialAutoCompleteTextView etBlock;
    private MaterialAutoCompleteTextView etQuality;
    private TextInputEditText etBuyer;
    private TextInputEditText etNotes;
    private TextView tvTotalAmount;

    private Calendar selectedDate = Calendar.getInstance();
    private boolean isPerBlock = true; // Default to per block

    public interface OnRevenueAddedListener {
        void onRevenueAdded();
    }

    private OnRevenueAddedListener listener;

    public static AddRevenueDialog newInstance(long farmId) {
        AddRevenueDialog dialog = new AddRevenueDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnRevenueAddedListener(OnRevenueAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
        }

        database = AppDatabase.getInstance(requireContext());

        // Load data in background
        new Thread(() -> {
            currentFarm = database.farmDao().getFarmById(farmId);
            allBlocks = database.blockDao().getBlocksByFarmId(farmId);
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_revenue, null);

        initViews(view);
        setupQualityDropdown();
        setupDatePicker();
        setupRevenueTypeToggle();
        loadBlocksForDropdown();
        setupTotalCalculation();

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
        etQuantity = view.findViewById(R.id.etQuantity);
        etPricePerKg = view.findViewById(R.id.etPricePerKg);
        etHarvestDate = view.findViewById(R.id.etHarvestDate);
        toggleRevenueType = view.findViewById(R.id.toggleRevenueType);
        btnPerBlock = view.findViewById(R.id.btnPerBlock);
        btnProcessingCenter = view.findViewById(R.id.btnProcessingCenter);
        layoutBlockSelector = view.findViewById(R.id.layoutBlockSelector);
        etBlock = view.findViewById(R.id.etBlock);
        etQuality = view.findViewById(R.id.etQuality);
        etBuyer = view.findViewById(R.id.etBuyer);
        etNotes = view.findViewById(R.id.etNotes);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);

        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveRevenue());

        // Set default date to today
        updateDateLabel();

        // Default to per block selected
        toggleRevenueType.check(R.id.btnPerBlock);
        layoutBlockSelector.setVisibility(View.VISIBLE);
    }

    private void setupQualityDropdown() {
        RevenueEntity.QualityGrade[] grades = RevenueEntity.QualityGrade.values();
        String[] gradeNames = new String[grades.length];

        for (int i = 0; i < grades.length; i++) {
            gradeNames[i] = grades[i].getIcon() + " " + grades[i].getDisplayName() +
                    " - " + grades[i].getDescription();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                gradeNames
        );

        etQuality.setAdapter(adapter);
        etQuality.setText(gradeNames[0], false); // Default to Premium
    }

    private void setupDatePicker() {
        etHarvestDate.setOnClickListener(v -> showDatePicker());
        etHarvestDate.setOnFocusChangeListener((v, hasFocus) -> {
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
        etHarvestDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupRevenueTypeToggle() {
        toggleRevenueType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnPerBlock) {
                    isPerBlock = true;
                    layoutBlockSelector.setVisibility(View.VISIBLE);
                } else {
                    isPerBlock = false;
                    layoutBlockSelector.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadBlocksForDropdown() {
        new Thread(() -> {
            if (allBlocks == null) {
                allBlocks = database.blockDao().getBlocksByFarmId(farmId);
            }

            List<String> blockNames = new ArrayList<>();
            for (BlockEntity block : allBlocks) {
                blockNames.add(block.getBlockName() + " - " + block.getStatus().getDisplayName());
            }

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        blockNames
                );
                etBlock.setAdapter(adapter);
            });
        }).start();
    }

    private void setupTotalCalculation() {
        View.OnKeyListener listener = (v, keyCode, event) -> {
            calculateTotal();
            return false;
        };

        etQuantity.setOnKeyListener(listener);
        etPricePerKg.setOnKeyListener(listener);

        // Also calculate when focus loses
        etQuantity.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });

        etPricePerKg.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) calculateTotal();
        });
    }

    private void calculateTotal() {
        String quantityStr = etQuantity.getText().toString().trim();
        String priceStr = etPricePerKg.getText().toString().trim();

        if (TextUtils.isEmpty(quantityStr) || TextUtils.isEmpty(priceStr)) {
            tvTotalAmount.setText("0 XAF");
            return;
        }

        try {
            double quantity = Double.parseDouble(quantityStr);
            double price = Double.parseDouble(priceStr);
            double total = quantity * price;

            tvTotalAmount.setText(String.format(Locale.getDefault(),
                    "%,.0f XAF", total));
        } catch (NumberFormatException e) {
            tvTotalAmount.setText("0 XAF");
        }
    }

    private void saveRevenue() {
        // Validate quantity
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

        // Validate price
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

        // Validate block if per block
        Long blockId = null;
        if (isPerBlock) {
            String blockStr = etBlock.getText().toString().trim();
            if (TextUtils.isEmpty(blockStr)) {
                etBlock.setError("Please select a block");
                return;
            }

            // Find block ID from name
            String blockName = blockStr.split(" - ")[0];
            for (BlockEntity block : allBlocks) {
                if (block.getBlockName().equals(blockName)) {
                    blockId = block.getId();
                    break;
                }
            }

            if (blockId == null) {
                Toast.makeText(getContext(), "Invalid block selection", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate quality
        String qualityStr = etQuality.getText().toString().trim();
        if (TextUtils.isEmpty(qualityStr)) {
            etQuality.setError("Quality grade is required");
            return;
        }

        // Parse quality
        RevenueEntity.QualityGrade selectedQuality = null;
        for (RevenueEntity.QualityGrade grade : RevenueEntity.QualityGrade.values()) {
            if (qualityStr.contains(grade.getDisplayName())) {
                selectedQuality = grade;
                break;
            }
        }

        if (selectedQuality == null) {
            Toast.makeText(getContext(), "Invalid quality grade", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get buyer and notes
        String buyer = etBuyer.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        long harvestDate = selectedDate.getTimeInMillis();

        // Create and save revenue
        RevenueEntity revenue = new RevenueEntity(
                farmId,
                blockId,
                harvestDate,
                quantity,
                price,
                buyer.isEmpty() ? null : buyer,
                selectedQuality,
                notes.isEmpty() ? null : notes
        );

        Toast.makeText(getContext(), "Saving revenue...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            long id = database.revenueDao().insert(revenue);

            requireActivity().runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(getContext(),
                            "Revenue added successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onRevenueAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(),
                            "Error adding revenue", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}