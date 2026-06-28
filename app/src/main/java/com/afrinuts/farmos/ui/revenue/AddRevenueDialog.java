package com.afrinuts.farmos.ui.revenue;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
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

    private static final String TAG = "AddRevenueDialog";  // ✅ Add TAG constant
    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_EDIT_MODE = "edit_mode";
    private static final String ARG_REVENUE_ID = "revenue_id";
    private static final int MAX_HARVESTS_PER_YEAR = 2;

    private long farmId;
    private String farmIdString;
    private boolean isEditMode = false;
    private long revenueId = -1;
    private RevenueEntity existingRevenue = null;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocks;
    private List<QualityBatchEntity> allBatches;
    private List<SupplierEntity> allSuppliers;
    private AppDatabase database;
    private RevenueRepository revenueRepository;
    private QualityBatchRepository batchRepository;
    private SupplierRepository supplierRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI Elements
    private Button btnCancel;
    private Button btnSave;
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

    // ✅ Procurement UI Elements
    private View layoutProcurementSection;
    private TextInputLayout layoutSupplierSelector;
    private MaterialAutoCompleteTextView etSupplier;
    private TextInputLayout layoutBatchSelector;
    private MaterialAutoCompleteTextView etBatch;

    private Calendar selectedDate = Calendar.getInstance();
    private boolean isPerBlock = true;
    private RevenueEntity.QualityGrade selectedQuality = RevenueEntity.QualityGrade.PREMIUM;

    // ✅ Procurement tracking fields
    private Long selectedSupplierId = null;
    private Long selectedBatchId = null;
    private String selectedSupplierName = null;
    private String selectedBatchNumber = null;

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
            farmIdString = String.valueOf(farmId);
            isEditMode = getArguments().getBoolean(ARG_EDIT_MODE, false);
            if (isEditMode) {
                revenueId = getArguments().getLong(ARG_REVENUE_ID, -1);
            }
        }

        database = AppDatabase.getInstance(requireContext());
        revenueRepository = new RevenueRepository(requireContext(), farmId);
        batchRepository = new QualityBatchRepository(requireContext(), farmId);
        supplierRepository = new SupplierRepository(requireContext(), farmId);

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
        loadSuppliersForDropdown();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            // Using custom buttons, so no need to handle dialog buttons
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

        // ✅ Procurement UI Elements
        layoutProcurementSection = view.findViewById(R.id.layoutProcurementSection);
        layoutSupplierSelector = view.findViewById(R.id.layoutSupplierSelector);
        etSupplier = view.findViewById(R.id.etSupplier);
        layoutBatchSelector = view.findViewById(R.id.layoutBatchSelector);
        etBatch = view.findViewById(R.id.etBatch);

        btnCancel = view.findViewById(R.id.btnCancel);
        btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());

        if (isEditMode) {
            btnSave.setText("Update Revenue");
            btnSave.setOnClickListener(v -> updateRevenue());
        } else {
            btnSave.setText("Add Revenue");
            btnSave.setOnClickListener(v -> insertRevenue());
        }

        updateDateLabel();

        if (isEditMode && existingRevenue != null) {
            etQuantity.setText(String.valueOf(existingRevenue.getQuantityKg()));
            etPricePerKg.setText(String.valueOf(existingRevenue.getPricePerKg()));

            if (existingRevenue.getBlockId() != null) {
                isPerBlock = true;
                toggleRevenueType.check(R.id.btnPerBlock);
                layoutBlockSelector.setVisibility(View.VISIBLE);
            } else {
                isPerBlock = false;
                toggleRevenueType.check(R.id.btnProcessingCenter);
                layoutBlockSelector.setVisibility(View.GONE);
            }

            if (existingRevenue.getBuyer() != null) {
                etBuyer.setText(existingRevenue.getBuyer());
            }
            if (existingRevenue.getNotes() != null) {
                etNotes.setText(existingRevenue.getNotes());
            }
            calculateTotal();
        } else {
            toggleRevenueType.check(R.id.btnPerBlock);
            layoutBlockSelector.setVisibility(View.VISIBLE);
            layoutProcurementSection.setVisibility(View.GONE);
        }
    }

    // ✅ Fixed QualityGradeAdapter
    private class QualityGradeAdapter extends ArrayAdapter<RevenueEntity.QualityGrade> {
        private List<RevenueEntity.QualityGrade> grades;

        public QualityGradeAdapter(Context context, List<RevenueEntity.QualityGrade> grades) {
            super(context, 0, grades);  // ✅ Use 0 as layout resource, and pass grades list
            this.grades = grades;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createViewFromResource(position, convertView, parent);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createViewFromResource(position, convertView, parent);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_quality_dropdown, parent, false);
            }

            RevenueEntity.QualityGrade grade = getItem(position);

            ImageView iconView = view.findViewById(R.id.qualityIcon);
            TextView titleView = view.findViewById(R.id.qualityTitle);
            TextView descriptionView = view.findViewById(R.id.qualityDescription);

            if (grade != null) {
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

        etQuality.setOnItemClickListener((parent, view, position, id) -> {
            selectedQuality = gradeList.get(position);
        });

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
                    // ✅ Hide procurement section for block revenue
                    layoutProcurementSection.setVisibility(View.GONE);
                } else {
                    isPerBlock = false;
                    layoutBlockSelector.setVisibility(View.GONE);
                    // ✅ Show procurement section for processing center revenue
                    layoutProcurementSection.setVisibility(View.VISIBLE);
                    loadSuppliersForDropdown();
                }
            }
        });
    }

    private void loadBlocksForDropdown() {
        executor.execute(() -> {
            if (allBlocks == null) {
                allBlocks = database.blockDao().getBlocksByFarmId(farmId);
            }

            List<BlockEntity> sortedBlocks = new ArrayList<>(allBlocks);
            sortedBlocks.sort((b1, b2) -> {
                String num1 = b1.getBlockName().substring(1);
                String num2 = b2.getBlockName().substring(1);
                return Integer.compare(Integer.parseInt(num1), Integer.parseInt(num2));
            });

            int selectedYear = selectedDate.get(Calendar.YEAR);
            List<RevenueEntity> existingRevenues = database.revenueDao().getRevenuesByFarmId(farmId);

            java.util.HashSet<Long> blocksHarvestedThisYear = new java.util.HashSet<>();
            java.util.HashMap<Long, Integer> blockHarvestCountThisYear = new java.util.HashMap<>();

            for (RevenueEntity revenue : existingRevenues) {
                if (revenue.getBlockId() != null && revenue.getHarvestYear() == selectedYear) {
                    blocksHarvestedThisYear.add(revenue.getBlockId());
                    int count = blockHarvestCountThisYear.getOrDefault(revenue.getBlockId(), 0);
                    blockHarvestCountThisYear.put(revenue.getBlockId(), count + 1);
                }
            }

            List<String> availableBlockNames = new ArrayList<>();
            List<BlockEntity> availableBlocks = new ArrayList<>();

            for (BlockEntity block : sortedBlocks) {
                if (!block.isPlanted()) {
                    continue;
                }

                int harvestsThisYear = blockHarvestCountThisYear.getOrDefault(block.getId(), 0);
                if (harvestsThisYear < MAX_HARVESTS_PER_YEAR) {
                    String statusInfo = "";
                    if (harvestsThisYear > 0) {
                        statusInfo = " (Harvest #" + (harvestsThisYear + 1) + " this year)";
                    }
                    availableBlockNames.add(block.getBlockName() + " - " + block.getStatus().getDisplayName() + statusInfo);
                    availableBlocks.add(block);
                }
            }

            final List<BlockEntity> finalAvailableBlocks = availableBlocks;

            requireActivity().runOnUiThread(() -> {
                if (availableBlockNames.isEmpty()) {
                    etBlock.setEnabled(false);
                    boolean hasAnyPlanted = sortedBlocks.stream().anyMatch(BlockEntity::isPlanted);
                    if (hasAnyPlanted) {
                        etBlock.setText("All blocks have reached maximum harvests for " + selectedYear, false);
                    } else {
                        etBlock.setText("No blocks are ready for harvest yet", false);
                    }
                } else {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            availableBlockNames
                    );
                    etBlock.setAdapter(adapter);
                    etBlock.setEnabled(true);

                    if (!isEditMode && !finalAvailableBlocks.isEmpty()) {
                        BlockEntity suggestedBlock = finalAvailableBlocks.get(0);
                        int harvestsSoFar = blockHarvestCountThisYear.getOrDefault(suggestedBlock.getId(), 0);
                        String suggestedText = suggestedBlock.getBlockName() + " - " + suggestedBlock.getStatus().getDisplayName();
                        if (harvestsSoFar > 0) {
                            suggestedText += " (Harvest #" + (harvestsSoFar + 1) + ")";
                        }
                        etBlock.setText(suggestedText, false);
                    }
                }
            });
        });
    }

    // ✅ Load suppliers for procurement
    private void loadSuppliersForDropdown() {
        executor.execute(() -> {
            allSuppliers = supplierRepository.getSuppliersSync();

            List<String> supplierNames = new ArrayList<>();
            for (SupplierEntity supplier : allSuppliers) {
                supplierNames.add(supplier.getName() + " (" + supplier.getType().getDisplayName() + ")");
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            supplierNames
                    );
                    etSupplier.setAdapter(adapter);

                    etSupplier.setOnItemClickListener((parent, view, position, id) -> {
                        SupplierEntity selectedSupplier = allSuppliers.get(position);
                        selectedSupplierId = selectedSupplier.getId();
                        selectedSupplierName = selectedSupplier.getName();
                        loadBatchesForSupplier(selectedSupplierId);
                    });
                });
            }
        });
    }

    // ✅ Load batches for a specific supplier
    private void loadBatchesForSupplier(long supplierId) {
        executor.execute(() -> {
            List<QualityBatchEntity> supplierBatches =
                    batchRepository.getBatchesBySupplierIdSync(supplierId);

            List<String> batchDisplayNames = new ArrayList<>();
            for (QualityBatchEntity batch : supplierBatches) {
                batchDisplayNames.add(batch.getBatchNumber() + " - " +
                        batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)");
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            batchDisplayNames
                    );
                    etBatch.setAdapter(adapter);

                    etBatch.setOnItemClickListener((parent, view, position, id) -> {
                        QualityBatchEntity selectedBatch = supplierBatches.get(position);
                        selectedBatchId = selectedBatch.getId();
                        selectedBatchNumber = selectedBatch.getBatchNumber();
                    });
                });
            }
        });
    }

    private void setupTotalCalculation() {
        View.OnKeyListener listener = (v, keyCode, event) -> {
            calculateTotal();
            return false;
        };

        etQuantity.setOnKeyListener(listener);
        etPricePerKg.setOnKeyListener(listener);

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

        String buyer = etBuyer.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        long harvestDate = selectedDate.getTimeInMillis();
        int selectedYear = selectedDate.get(Calendar.YEAR);

        Long blockId = null;
        Long batchId = null;
        Long supplierId = null;
        String batchNumber = null;
        String supplierName = null;

        if (isPerBlock) {
            // ✅ Validate block for per-block revenue
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

            final Long finalBlockId = blockId;
            final String finalBlockName = blockName;
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            executor.execute(() -> {
                try {
                    List<RevenueEntity> blockHarvestsThisYear = database.revenueDao()
                            .getRevenuesByBlockIdAndYear(finalBlockId, selectedYear);
                    int harvestCountThisYear = blockHarvestsThisYear.size();

                    if (harvestCountThisYear >= MAX_HARVESTS_PER_YEAR) {
                        requireActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Add Revenue");
                            Toast.makeText(getContext(),
                                    "Block " + finalBlockName + " has already been harvested " + harvestCountThisYear +
                                            " times in " + selectedYear + ". Maximum is " + MAX_HARVESTS_PER_YEAR + " per year.",
                                    Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    int seasonNumber = harvestCountThisYear + 1;
                    String cycleId = RevenueEntity.generateHarvestCycleId(finalBlockId, selectedYear);

                    RevenueEntity revenue = new RevenueEntity(
                            farmId,
                            finalBlockId,
                            harvestDate,
                            quantity,
                            price,
                            buyer.isEmpty() ? null : buyer,
                            selectedQuality,
                            notes.isEmpty() ? null : notes
                    );

                    revenue.setHarvestYear(selectedYear);
                    revenue.setHarvestSeason(seasonNumber);
                    revenue.setHarvestCycleId(cycleId);

                    revenueRepository.insertRevenue(revenue, new BaseRepository.InsertCallback() {
                        @Override
                        public void onSuccess(long id) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Revenue added successfully!", Toast.LENGTH_SHORT).show();
                                    if (listener != null) {
                                        listener.onRevenueAdded();
                                    }
                                    dismiss();
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    btnSave.setEnabled(true);
                                    btnSave.setText("Add Revenue");
                                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error checking block harvest history", e);
                    requireActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Add Revenue");
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });

        } else {
            // ✅ Processing Center revenue with procurement support
            // Validate procurement fields
            if (selectedSupplierId == null) {
                Toast.makeText(getContext(), "Please select a supplier", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedBatchId == null) {
                Toast.makeText(getContext(), "Please select a batch", Toast.LENGTH_SHORT).show();
                return;
            }

            supplierId = selectedSupplierId;
            batchId = selectedBatchId;
            batchNumber = selectedBatchNumber;
            supplierName = selectedSupplierName;

            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            RevenueEntity revenue = new RevenueEntity(
                    farmId,
                    null, // No block for processing center
                    batchId,
                    supplierId,
                    harvestDate,
                    quantity,
                    price,
                    buyer.isEmpty() ? null : buyer,
                    selectedQuality,
                    notes.isEmpty() ? null : notes
            );

            // Set denormalized fields for quick display
            revenue.setBatchNumber(batchNumber);
            revenue.setSupplierName(supplierName);

            revenue.setHarvestYear(selectedYear);
            revenue.setHarvestSeason(0);
            revenue.setHarvestCycleId("PROCESSING_CENTER_YEAR_" + selectedYear);

            revenueRepository.insertRevenue(revenue, new BaseRepository.InsertCallback() {
                @Override
                public void onSuccess(long id) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Revenue added successfully!", Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onRevenueAdded();
                            }
                            dismiss();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Add Revenue");
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });
        }
    }

    private void updateRevenue() {
        if (existingRevenue == null) {
            Toast.makeText(getContext(), "No revenue to update", Toast.LENGTH_SHORT).show();
            return;
        }

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

        Long blockId = null;
        Long batchId = null;
        Long supplierId = null;
        String batchNumber = null;
        String supplierName = null;

        if (isPerBlock) {
            // Per-block revenue - no procurement
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

            if (existingRevenue.getBlockId() != null && (blockId == null || existingRevenue.getBlockId() != blockId)) {
                Toast.makeText(getContext(), "Cannot change block after harvest is recorded.", Toast.LENGTH_LONG).show();
                return;
            }

            if (blockId == null) {
                Toast.makeText(getContext(), "Invalid block selection", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // Processing center revenue - use procurement
            if (selectedSupplierId == null) {
                Toast.makeText(getContext(), "Please select a supplier", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedBatchId == null) {
                Toast.makeText(getContext(), "Please select a batch", Toast.LENGTH_SHORT).show();
                return;
            }

            supplierId = selectedSupplierId;
            batchId = selectedBatchId;
            batchNumber = selectedBatchNumber;
            supplierName = selectedSupplierName;
        }

        if (selectedQuality == null) {
            Toast.makeText(getContext(), "Please select a quality grade", Toast.LENGTH_SHORT).show();
            return;
        }

        String buyer = etBuyer.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        long harvestDate = selectedDate.getTimeInMillis();

        btnSave.setEnabled(false);
        btnSave.setText("Updating...");

        existingRevenue.setQuantityKg(quantity);
        existingRevenue.setPricePerKg(price);
        existingRevenue.setHarvestDate(harvestDate);
        existingRevenue.setBlockId(blockId);
        existingRevenue.setBatchId(batchId);
        existingRevenue.setSupplierId(supplierId);
        existingRevenue.setBatchNumber(batchNumber);
        existingRevenue.setSupplierName(supplierName);
        existingRevenue.setQuality(selectedQuality);
        existingRevenue.setBuyer(buyer.isEmpty() ? null : buyer);
        existingRevenue.setNotes(notes.isEmpty() ? null : notes);
        existingRevenue.setUpdatedAt(System.currentTimeMillis());

        revenueRepository.updateRevenue(existingRevenue, new BaseRepository.RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Revenue updated successfully!", Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onRevenueAdded();
                        }
                        dismiss();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Update Revenue");
                        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroy() {
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