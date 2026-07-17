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
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.BaseRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddRevenueDialog extends DialogFragment {

    private static final String TAG = "AddRevenueDialog";
    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_EDIT_MODE = "edit_mode";
    private static final String ARG_REVENUE_ID = "revenue_id";

    private long farmId;
    private String farmIdString;
    private boolean isEditMode = false;
    private long revenueId = -1;
    private RevenueEntity existingRevenue = null;
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
    private MaterialAutoCompleteTextView etQuality;
    private TextInputEditText etBuyer;
    private TextInputEditText etNotes;
    private TextView tvTotalAmount;

    // Procurement UI Elements
    private TextInputLayout layoutSupplierSelector;
    private MaterialAutoCompleteTextView etSupplier;
    private TextInputLayout layoutBatchSelector;
    private MaterialAutoCompleteTextView etBatch;

    private Calendar selectedDate = Calendar.getInstance();
    private RevenueEntity.QualityGrade selectedQuality = RevenueEntity.QualityGrade.PREMIUM;

    // Procurement tracking fields
    private Long selectedSupplierId = null;
    private Long selectedBatchId = null;
    private String selectedSupplierName = null;
    private String selectedBatchNumber = null;
    private QualityBatchEntity selectedBatch = null;

    // ✅ Track already sold batch IDs
    private Set<Long> soldBatchIds = new HashSet<>();

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
            allBatches = batchRepository.getAllBatchesSync();
            allSuppliers = supplierRepository.getSuppliersSync();

            // ✅ Load already sold batch IDs
            loadSoldBatchIds();

            if (isEditMode && revenueId != -1) {
                existingRevenue = database.revenueDao().getRevenueById(revenueId);
                if (existingRevenue != null) {
                    selectedDate.setTimeInMillis(existingRevenue.getHarvestDate());
                    selectedQuality = existingRevenue.getQuality();

                    // Pre-fill procurement fields if available
                    if (existingRevenue.getSupplierId() != null) {
                        selectedSupplierId = existingRevenue.getSupplierId();
                        selectedSupplierName = existingRevenue.getSupplierName();
                    }
                    if (existingRevenue.getBatchId() != null) {
                        selectedBatchId = existingRevenue.getBatchId();
                        selectedBatchNumber = existingRevenue.getBatchNumber();
                        // Find the batch object
                        for (QualityBatchEntity batch : allBatches) {
                            if (batch.getId() == selectedBatchId) {
                                selectedBatch = batch;
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    // ✅ Load all batch IDs that have already been sold (have revenue records)
    private void loadSoldBatchIds() {
        List<RevenueEntity> allRevenues = database.revenueDao().getRevenuesByFarmId(farmId);
        soldBatchIds.clear();
        for (RevenueEntity revenue : allRevenues) {
            if (revenue.getBatchId() != null) {
                soldBatchIds.add(revenue.getBatchId());
            }
        }
        Log.d(TAG, "Loaded " + soldBatchIds.size() + " sold batch IDs");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_revenue, null);

        initViews(view);
        setupQualityDropdown();
        setupDatePicker();
        setupTotalCalculation();
        loadSuppliersForDropdown();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add Processing Revenue")
                .setView(view)
                .setCancelable(false);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            // Using custom buttons
        });

        return dialog;
    }

    private void initViews(View view) {
        etQuantity = view.findViewById(R.id.etQuantity);
        etPricePerKg = view.findViewById(R.id.etPricePerKg);
        etHarvestDate = view.findViewById(R.id.etHarvestDate);
        etQuality = view.findViewById(R.id.etQuality);
        etBuyer = view.findViewById(R.id.etBuyer);
        etNotes = view.findViewById(R.id.etNotes);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);

        // Procurement UI Elements
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

            if (existingRevenue.getBuyer() != null) {
                etBuyer.setText(existingRevenue.getBuyer());
            }
            if (existingRevenue.getNotes() != null) {
                etNotes.setText(existingRevenue.getNotes());
            }

            calculateTotal();
        }
    }

    // Quality Grade Adapter
    private class QualityGradeAdapter extends ArrayAdapter<RevenueEntity.QualityGrade> {
        private List<RevenueEntity.QualityGrade> grades;

        public QualityGradeAdapter(Context context, List<RevenueEntity.QualityGrade> grades) {
            super(context, 0, grades);
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

    // Load suppliers for procurement
    private void loadSuppliersForDropdown() {
        executor.execute(() -> {
            if (allSuppliers == null) {
                allSuppliers = supplierRepository.getSuppliersSync();
            }

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

                    // Pre-fill if editing
                    if (isEditMode && selectedSupplierName != null) {
                        for (int i = 0; i < allSuppliers.size(); i++) {
                            if (allSuppliers.get(i).getId() == selectedSupplierId) {
                                etSupplier.setText(supplierNames.get(i), false);
                                loadBatchesForSupplier(selectedSupplierId);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    // Load batches for a specific supplier - ✅ FILTER OUT SOLD BATCHES
    private void loadBatchesForSupplier(long supplierId) {
        executor.execute(() -> {
            List<QualityBatchEntity> supplierBatches =
                    batchRepository.getBatchesBySupplierIdSync(supplierId);

            // ✅ Filter out already sold batches (unless we're editing this specific revenue)
            List<QualityBatchEntity> availableBatches = new ArrayList<>();
            for (QualityBatchEntity batch : supplierBatches) {
                boolean isSold = soldBatchIds.contains(batch.getId());

                // If editing, allow the current batch even if it's sold
                if (isEditMode && existingRevenue != null &&
                        existingRevenue.getBatchId() != null &&
                        existingRevenue.getBatchId() == batch.getId()) {
                    availableBatches.add(batch);
                    Log.d(TAG, "In edit mode - allowing current batch: " + batch.getBatchNumber());
                } else if (!isSold) {
                    availableBatches.add(batch);
                } else {
                    Log.d(TAG, "Batch " + batch.getBatchNumber() + " is already sold, excluding from list");
                }
            }

            List<String> batchDisplayNames = new ArrayList<>();
            for (QualityBatchEntity batch : availableBatches) {
                String status = soldBatchIds.contains(batch.getId()) ? " (SOLD)" : "";
                batchDisplayNames.add(batch.getBatchNumber() + " - " +
                        batch.getFormattedDate() + " (" + batch.getWeightKg() + "kg)" + status);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (availableBatches.isEmpty()) {
                        // ✅ Show message when no batches available
                        etBatch.setEnabled(false);
                        etBatch.setText("No available batches for this supplier", false);

                        // Show a warning toast
                        Toast.makeText(getContext(),
                                "All batches from this supplier have been sold",
                                Toast.LENGTH_LONG).show();
                    } else {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                batchDisplayNames
                        );
                        etBatch.setAdapter(adapter);
                        etBatch.setEnabled(true);

                        etBatch.setOnItemClickListener((parent, view, position, id) -> {
                            QualityBatchEntity selectedBatch = availableBatches.get(position);
                            selectedBatchId = selectedBatch.getId();
                            selectedBatchNumber = selectedBatch.getBatchNumber();
                            selectedBatch = selectedBatch;

                            Log.d(TAG, "Selected batch: " + selectedBatchNumber + " (ID: " + selectedBatchId + ")");
                        });

                        // Pre-fill if editing
                        if (isEditMode && selectedBatchNumber != null) {
                            for (int i = 0; i < availableBatches.size(); i++) {
                                if (availableBatches.get(i).getId() == selectedBatchId) {
                                    etBatch.setText(batchDisplayNames.get(i), false);
                                    break;
                                }
                            }
                        } else if (!availableBatches.isEmpty()) {
                            // ✅ Auto-select the first available batch
                            QualityBatchEntity firstBatch = availableBatches.get(0);
                            selectedBatchId = firstBatch.getId();
                            selectedBatchNumber = firstBatch.getBatchNumber();
                            selectedBatch = firstBatch;
                            etBatch.setText(batchDisplayNames.get(0), false);
                            Log.d(TAG, "Auto-selected first available batch: " + firstBatch.getBatchNumber());
                        }
                    }
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

        // Validate procurement fields
        if (selectedSupplierId == null) {
            Toast.makeText(getContext(), "Please select a supplier", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBatchId == null) {
            Toast.makeText(getContext(), "Please select a batch", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Check if this batch is already sold (double-check)
        if (soldBatchIds.contains(selectedBatchId)) {
            Toast.makeText(getContext(),
                    "This batch has already been sold! Please select a different batch.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedQuality == null) {
            Toast.makeText(getContext(), "Please select a quality grade", Toast.LENGTH_SHORT).show();
            return;
        }

        String buyer = etBuyer.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        long harvestDate = selectedDate.getTimeInMillis();
        int selectedYear = selectedDate.get(Calendar.YEAR);

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // ✅ Create revenue with procurement data (NO BLOCK ID)
        RevenueEntity revenue = new RevenueEntity(
                farmId,
                null,  // No block ID for processing center revenue
                selectedBatchId,
                selectedSupplierId,
                harvestDate,
                quantity,
                price,
                buyer.isEmpty() ? null : buyer,
                selectedQuality,
                notes.isEmpty() ? null : notes
        );

        // Set denormalized fields for quick display
        revenue.setBatchNumber(selectedBatchNumber);
        revenue.setSupplierName(selectedSupplierName);

        // Set harvest cycle info
        revenue.setHarvestYear(selectedYear);
        revenue.setHarvestSeason(0);
        revenue.setHarvestCycleId("PROCESSING_CENTER_" + selectedYear + "_" + System.currentTimeMillis());

        revenueRepository.insertRevenue(revenue, new BaseRepository.InsertCallback() {
            @Override
            public void onSuccess(long id) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Revenue added successfully! Batch: " + selectedBatchNumber,
                                Toast.LENGTH_LONG).show();
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

        if (selectedSupplierId == null) {
            Toast.makeText(getContext(), "Please select a supplier", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBatchId == null) {
            Toast.makeText(getContext(), "Please select a batch", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ For editing, allow the current batch even if it's sold
        // But don't allow changing to a different sold batch
        if (existingRevenue.getBatchId() != selectedBatchId &&
                soldBatchIds.contains(selectedBatchId)) {
            Toast.makeText(getContext(),
                    "This batch has already been sold to another customer!",
                    Toast.LENGTH_LONG).show();
            return;
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
        existingRevenue.setBlockId(null);  // ✅ Ensure no block ID
        existingRevenue.setBatchId(selectedBatchId);
        existingRevenue.setSupplierId(selectedSupplierId);
        existingRevenue.setBatchNumber(selectedBatchNumber);
        existingRevenue.setSupplierName(selectedSupplierName);
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