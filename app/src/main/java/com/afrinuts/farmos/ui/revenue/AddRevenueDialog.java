package com.afrinuts.farmos.ui.revenue;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddRevenueDialog extends DialogFragment {

    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_EDIT_MODE = "edit_mode";
    private static final String ARG_REVENUE_ID = "revenue_id";

    private long farmId;
    private boolean isEditMode = false;
    private long revenueId = -1;
    private RevenueEntity existingRevenue = null;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
    private RevenueEntity.QualityGrade selectedQuality = RevenueEntity.QualityGrade.PREMIUM; // Default

    public interface OnRevenueAddedListener {
        void onRevenueAdded();
    }

    private OnRevenueAddedListener listener;

    public static AddRevenueDialog newInstance(long farmId) {
        AddRevenueDialog dialog = new AddRevenueDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        args.putBoolean(ARG_EDIT_MODE, false);
        dialog.setArguments(args);
        return dialog;
    }

    public static AddRevenueDialog newEditInstance(long farmId, long revenueId) {
        AddRevenueDialog dialog = new AddRevenueDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_FARM_ID, farmId);
        args.putBoolean(ARG_EDIT_MODE, true);
        args.putLong(ARG_REVENUE_ID, revenueId);
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
            isEditMode = getArguments().getBoolean(ARG_EDIT_MODE, false);
            if (isEditMode) {
                revenueId = getArguments().getLong(ARG_REVENUE_ID, -1);
            }
        }

        database = AppDatabase.getInstance(requireContext());

        // Load data in background
        executor.execute(() -> {
            currentFarm = database.farmDao().getFarmById(farmId);
            allBlocks = database.blockDao().getBlocksByFarmId(farmId);
            if (isEditMode && revenueId != -1) {
                existingRevenue = database.revenueDao().getRevenueById(revenueId);
                if (existingRevenue != null) {
                    selectedDate.setTimeInMillis(existingRevenue.getHarvestDate());
                    selectedQuality = existingRevenue.getQuality();
                }
            }
        });
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

        if (isEditMode) {
            btnSave.setText("Update Revenue");
            btnSave.setOnClickListener(v -> updateRevenue());
        } else {
            btnSave.setOnClickListener(v -> insertRevenue());
        }

        // Set default date to today
        updateDateLabel();

        if (isEditMode && existingRevenue != null) {
            // Populate fields with existing revenue data
            etQuantity.setText(String.valueOf(existingRevenue.getQuantityKg()));
            etPricePerKg.setText(String.valueOf(existingRevenue.getPricePerKg()));

            // Set revenue type
            if (existingRevenue.getBlockId() != null) {
                isPerBlock = true;
                toggleRevenueType.check(R.id.btnPerBlock);
                layoutBlockSelector.setVisibility(View.VISIBLE);
            } else {
                isPerBlock = false;
                toggleRevenueType.check(R.id.btnProcessingCenter);
                layoutBlockSelector.setVisibility(View.GONE);
            }

            // Quality will be set in setupQualityDropdown after adapter is ready
            if (existingRevenue.getBuyer() != null) {
                etBuyer.setText(existingRevenue.getBuyer());
            }
            if (existingRevenue.getNotes() != null) {
                etNotes.setText(existingRevenue.getNotes());
            }
            calculateTotal();
        } else {
            // Default to per block selected
            toggleRevenueType.check(R.id.btnPerBlock);
            layoutBlockSelector.setVisibility(View.VISIBLE);
        }
    }

    // Custom adapter for quality grade dropdown with icons
    private class QualityGradeAdapter extends ArrayAdapter<RevenueEntity.QualityGrade> {
        private List<RevenueEntity.QualityGrade> grades;

        public QualityGradeAdapter(Context context, List<RevenueEntity.QualityGrade> grades) {
            super(context, 0, grades);
            this.grades = grades;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, R.layout.item_quality_dropdown);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createViewFromResource(position, convertView, parent, R.layout.item_quality_dropdown);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(resource, parent, false);
            }

            RevenueEntity.QualityGrade grade = getItem(position);

            ImageView iconView = view.findViewById(R.id.qualityIcon);
            TextView titleView = view.findViewById(R.id.qualityTitle);
            TextView descriptionView = view.findViewById(R.id.qualityDescription);

            if (grade != null) {
                // Set icon based on quality grade
                int iconRes = getIconForQuality(grade);
                iconView.setImageResource(iconRes);
                iconView.setColorFilter(getContext().getColor(R.color.primary));

                titleView.setText(grade.getDisplayName() + " Grade");
                descriptionView.setText(grade.getDescription());
            }

            return view;
        }

        private int getIconForQuality(RevenueEntity.QualityGrade grade) {
            switch (grade) {
                case PREMIUM:
                    return R.drawable.ic_star;
                case STANDARD:
                    return R.drawable.ic_check_circle;
                case PROCESSING:
                    return R.drawable.ic_factory;
                default:
                    return R.drawable.ic_star;
            }
        }
    }

    private void setupQualityDropdown() {
        RevenueEntity.QualityGrade[] grades = RevenueEntity.QualityGrade.values();
        List<RevenueEntity.QualityGrade> gradeList = new ArrayList<>();
        for (RevenueEntity.QualityGrade grade : grades) {
            gradeList.add(grade);
        }

        QualityGradeAdapter adapter = new QualityGradeAdapter(requireContext(), gradeList);
        etQuality.setAdapter(adapter);

        // Set selection listener
        etQuality.setOnItemClickListener((parent, view, position, id) -> {
            selectedQuality = gradeList.get(position);
        });

        // Set default or existing selection
        if (isEditMode && existingRevenue != null) {
            selectedQuality = existingRevenue.getQuality();
            etQuality.setText(selectedQuality.getDisplayName() + " Grade", false);
        } else {
            etQuality.setText(gradeList.get(0).getDisplayName() + " Grade", false);
        }
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
        executor.execute(() -> {
            if (allBlocks == null) {
                allBlocks = database.blockDao().getBlocksByFarmId(farmId);
            }

            // Sort blocks naturally (A1, A2, etc.)
            List<BlockEntity> sortedBlocks = new ArrayList<>(allBlocks);
            sortedBlocks.sort((b1, b2) -> {
                String num1 = b1.getBlockName().substring(1);
                String num2 = b2.getBlockName().substring(1);
                return Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
            });

            // Get blocks that already have revenue
            List<RevenueEntity> existingRevenues = database.revenueDao().getRevenuesByFarmId(farmId);
            java.util.HashSet<Long> blocksWithRevenue = new java.util.HashSet<>();
            for (RevenueEntity revenue : existingRevenues) {
                if (revenue.getBlockId() != null) {
                    blocksWithRevenue.add(revenue.getBlockId());
                }
            }

            List<String> blockNames = new ArrayList<>();
            for (BlockEntity block : sortedBlocks) {
                String displayName = block.getBlockName() + " - " + block.getStatus().getDisplayName();
                blockNames.add(displayName);
            }

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        blockNames
                );
                etBlock.setAdapter(adapter);

                // Suggest next available block
                String nextBlock = getNextAvailableBlockForRevenue(sortedBlocks, blocksWithRevenue);
                if (nextBlock != null && !isEditMode) {
                    etBlock.setText(nextBlock, false);
                } else if (isEditMode && existingRevenue != null && existingRevenue.getBlockId() != null) {
                    // Set the block for edit mode
                    for (BlockEntity block : allBlocks) {
                        if (block.getId() == existingRevenue.getBlockId()) {
                            etBlock.setText(block.getBlockName() + " - " + block.getStatus().getDisplayName(), false);
                            break;
                        }
                    }
                }
            });
        });
    }

    private String getNextAvailableBlockForRevenue(List<BlockEntity> blocks, java.util.HashSet<Long> blocksWithRevenue) {
        if (blocks == null || blocks.isEmpty()) return null;

        for (BlockEntity block : blocks) {
            if (!blocksWithRevenue.contains(block.getId())) {
                return block.getBlockName() + " - " + block.getStatus().getDisplayName();
            }
        }
        return null;
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

    private void insertRevenue() {
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
        if (selectedQuality == null) {
            Toast.makeText(getContext(), "Please select a quality grade", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get buyer and notes
        String buyer = etBuyer.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        long harvestDate = selectedDate.getTimeInMillis();

        // Check for duplicate block revenue (only in add mode)
        if (isPerBlock && blockId != null) {
            Long finalBlockId = blockId;
            executor.execute(() -> {
                List<RevenueEntity> existingRevenues = database.revenueDao()
                        .getRevenuesByBlockId(finalBlockId);

                if (!existingRevenues.isEmpty()) {
                    requireActivity().runOnUiThread(() -> {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Duplicate Revenue")
                                .setMessage("This block already has revenue recorded. Are you sure you want to add another entry?")
                                .setPositiveButton("Yes, Add", (dialog, which) -> {
                                    proceedWithInsert(quantity, price, finalBlockId, buyer, notes, harvestDate);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    proceedWithInsert(quantity, price, finalBlockId, buyer, notes, harvestDate);
                });
            });
        } else {
            proceedWithInsert(quantity, price, blockId, buyer, notes, harvestDate);
        }
    }

    private void proceedWithInsert(double quantity, double price, Long blockId,
                                   String buyer, String notes, long harvestDate) {

        // Create final copies for lambda
        final long finalFarmId = farmId;
        final Long finalBlockId = blockId;
        final double finalQuantity = quantity;
        final double finalPrice = price;
        final String finalBuyer = buyer.isEmpty() ? null : buyer;
        final RevenueEntity.QualityGrade finalQuality = selectedQuality;
        final String finalNotes = notes.isEmpty() ? null : notes;
        final long finalHarvestDate = harvestDate;

        executor.execute(() -> {
            RevenueEntity revenue = new RevenueEntity(
                    finalFarmId,
                    finalBlockId,
                    finalHarvestDate,
                    finalQuantity,
                    finalPrice,
                    finalBuyer,
                    finalQuality,
                    finalNotes
            );

            long id = database.revenueDao().insert(revenue);

            requireActivity().runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(getContext(), "Revenue added successfully!", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onRevenueAdded();
                    }
                    dismiss();
                } else {
                    Toast.makeText(getContext(), "Error adding revenue", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateRevenue() {
        if (existingRevenue == null) {
            Toast.makeText(getContext(), "No revenue to update", Toast.LENGTH_SHORT).show();
            return;
        }

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
        if (selectedQuality == null) {
            Toast.makeText(getContext(), "Please select a quality grade", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get buyer and notes
        String buyer = etBuyer.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        long harvestDate = selectedDate.getTimeInMillis();

        // Create final copies for lambda
        final RevenueEntity finalExistingRevenue = existingRevenue;
        final Long finalBlockId = blockId;
        final double finalQuantity = quantity;
        final double finalPrice = price;
        final String finalBuyer = buyer.isEmpty() ? null : buyer;
        final RevenueEntity.QualityGrade finalQuality = selectedQuality;
        final String finalNotes = notes.isEmpty() ? null : notes;
        final long finalHarvestDate = harvestDate;

        executor.execute(() -> {
            finalExistingRevenue.setQuantityKg(finalQuantity);
            finalExistingRevenue.setPricePerKg(finalPrice);
            finalExistingRevenue.setHarvestDate(finalHarvestDate);
            finalExistingRevenue.setBlockId(finalBlockId);
            finalExistingRevenue.setQuality(finalQuality);
            finalExistingRevenue.setBuyer(finalBuyer);
            finalExistingRevenue.setNotes(finalNotes);
            finalExistingRevenue.setUpdatedAt(System.currentTimeMillis());

            database.revenueDao().update(finalExistingRevenue);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Revenue updated successfully!", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onRevenueAdded();
                }
                dismiss();
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}