package com.afrinuts.farmos.ui.traceability;

import static android.content.ContentValues.TAG;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.ui.farm.FarmDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddRevenueFromTraceabilityDialog extends FarmDialogFragment {

    private static final String ARG_BATCH_ID = "batch_id";

    private long batchId;
    private QualityBatchEntity batch;
    private RevenueRepository revenueRepository;
    private QualityBatchRepository qualityBatchRepository;
    private AppDatabase database;

    private TextInputEditText etQuantity;
    private TextInputEditText etPricePerKg;
    private TextInputEditText etBuyer;
    private TextInputEditText etHarvestDate;
    private TextInputEditText etNotes;

    // ✅ Declare btnSave as a class field
    private Button btnSave;
    private Button btnCancel;

    private Calendar selectedDate = Calendar.getInstance();

    public interface OnRevenueAddedListener {
        void onRevenueAdded(RevenueEntity revenue);
    }

    private OnRevenueAddedListener listener;

    public static AddRevenueFromTraceabilityDialog newInstance(long batchId) {
        AddRevenueFromTraceabilityDialog dialog = new AddRevenueFromTraceabilityDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_BATCH_ID, batchId);
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
            batchId = getArguments().getLong(ARG_BATCH_ID);
        }

        database = AppDatabase.getInstance(requireContext());
        qualityBatchRepository = new QualityBatchRepository(requireContext(), 1);
        revenueRepository = new RevenueRepository(requireContext(), 1);

        // Load batch data
        new Thread(() -> {
            batch = qualityBatchRepository.getBatchByIdSync(batchId);
        }).start();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_revenue_from_traceability, null);

        initViews(view);
        setupDatePicker();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveRevenue());
        return builder.create();
    }

    private void initViews(View view) {
        etQuantity = view.findViewById(R.id.etQuantity);
        etPricePerKg = view.findViewById(R.id.etPricePerKg);
        etBuyer = view.findViewById(R.id.etBuyer);
        etHarvestDate = view.findViewById(R.id.etHarvestDate);
        etNotes = view.findViewById(R.id.etNotes);

        // ✅ Initialize buttons from the view
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);

        // Pre-fill quantity from batch
        if (batch != null) {
            etQuantity.setText(String.valueOf(batch.getWeightKg()));
            // Disable quantity editing since it should match the batch
            etQuantity.setEnabled(false);
        }

        // Set default date
        updateDateLabel();
    }

    private void setupDatePicker() {
        etHarvestDate.setOnClickListener(v -> showDatePicker());
        etHarvestDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePicker();
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

    // ✅ Add this method to update traceability with revenue data
    private void updateTraceabilityWithRevenue(RevenueEntity revenue) {
        new Thread(() -> {
            try {
                // Get the traceability record for this batch
                BatchTraceabilityRepository traceRepo = new BatchTraceabilityRepository(requireContext(), 1);
                BatchTraceabilityEntity traceability = traceRepo.getByQualityBatchId(batchId);

                if (traceability != null) {
                    traceability.setRevenueId(revenue.getId());
                    traceability.setBuyer(revenue.getBuyer());
                    traceability.setPricePerKg(revenue.getPricePerKg());
                    traceability.setTotalAmount(revenue.getTotalAmount());
                    traceability.setUpdatedAt(System.currentTimeMillis());

                    traceRepo.updateTraceability(traceability, null);
                    Log.d(TAG, "✅ Updated traceability with revenue data");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating traceability with revenue", e);
            }
        }).start();
    }

    private void saveRevenue() {
        // Validate inputs
        String quantityStr = etQuantity.getText().toString().trim();
        if (TextUtils.isEmpty(quantityStr)) {
            etQuantity.setError("Quantity is required");
            return;
        }

        String priceStr = etPricePerKg.getText().toString().trim();
        if (TextUtils.isEmpty(priceStr)) {
            etPricePerKg.setError("Price is required");
            return;
        }

        String buyer = etBuyer.getText().toString().trim();
        if (TextUtils.isEmpty(buyer)) {
            etBuyer.setError("Buyer is required for invoice");
            return;
        }

        try {
            double quantity = Double.parseDouble(quantityStr);
            double price = Double.parseDouble(priceStr);

            if (quantity <= 0) {
                etQuantity.setError("Quantity must be positive");
                return;
            }

            if (price <= 0) {
                etPricePerKg.setError("Price must be positive");
                return;
            }

            // Show loading
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            // Create revenue — A1: batchId/supplierId are constructor args now,
            // saleDate lands in createdAt via the canonical constructor.
            Long supplierIdForRevenue = (batch != null) ? batch.getSupplierId() : null;
            String batchNumberForRevenue = (batch != null) ? batch.getBatchNumber() : null;

            RevenueEntity revenue = new RevenueEntity(
                    1,                                          // farmId
                    batchId,                                    // batchId
                    supplierIdForRevenue,                       // supplierId
                    selectedDate.getTimeInMillis(),             // saleDate → createdAt
                    quantity,
                    price,
                    buyer,
                    RevenueEntity.QualityGrade.PREMIUM,         // Default to premium
                    etNotes.getText().toString().trim()
            );

            if (batchNumberForRevenue != null) {
                revenue.setBatchNumber(batchNumberForRevenue);
            }

            revenueRepository.insertRevenue(revenue, new RevenueRepository.InsertCallback() {
                @Override
                public void onSuccess(long id) {
                    revenue.setId(id);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            btnSave.setEnabled(true);
                            btnSave.setText("Save");
                            Toast.makeText(getContext(), "Revenue added! You can now generate the Commercial Invoice.", Toast.LENGTH_LONG).show();

                            // Update traceability with revenue data
                            updateTraceabilityWithRevenue(revenue);

                            if (listener != null) {
                                listener.onRevenueAdded(revenue);
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
                            btnSave.setText("Save");
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
}
