package com.afrinuts.farmos.ui.quality;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.afrinuts.farmos.utils.QualityGradeCalculator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RCN quality inspection — Phase One.
 *
 * Attaches quality measurements to an EXISTING batch created under a supplier.
 * The batch is created upstream (batch recording under supplier);
 * this dialog does NOT create batches.
 *
 * Only batches whose RCN measurements are still blank are selectable.
 */
public class RcnIntakeDialog extends DialogFragment {

    private static final String TAG = "RcnIntakeDialog";
    private static final String ARG_FARM_ID = "farm_id";

    private long farmId;
    private QualityBatchRepository qualityBatchRepository;
    private SupplierRepository supplierRepository;
    private AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Supplier → batch selection
    private MaterialAutoCompleteTextView etSupplier;
    private MaterialAutoCompleteTextView etDeliveryBatch;
    private List<SupplierEntity> supplierList = new ArrayList<>();
    private List<QualityBatchEntity> pendingBatches = new ArrayList<>();
    private QualityBatchEntity selectedBatch;

    // Read-only header (populated from selectedBatch)
    private TextInputEditText etBatchNumberDisplay;
    private TextInputEditText etWeightKgDisplay;
    private MaterialAutoCompleteTextView etSourceTypeDisplay;
    private MaterialAutoCompleteTextView etBatchTypeDisplay;

    // Editable RCN fields
    private TextInputEditText etInspectedBy;
    private TextInputEditText etKorSampleGrams;
    private TextInputEditText etKorKernelGrams;
    private TextInputEditText etKorComputed;      // read-only display
    private TextView tvKorEquivalents;            // derived lbs/percent display
    private TextInputEditText etMoisture;
    private TextInputEditText etNutCount;
    private TextInputEditText etMouldy;
    private TextInputEditText etImmature;
    private TextInputEditText etInsectDamaged;
    private TextInputEditText etShrivelled;
    private TextInputEditText etForeignMatter;
    private MaterialAutoCompleteTextView etDryingMethod;

    // Computed / read-only
    private TextInputEditText etDefectiveTotal;
    private TextInputEditText etComputedGrade;
    private TextInputEditText etComputedScore;
    private TextInputEditText etInspectionDate;
    private TextInputEditText etNotes;

    private final Calendar selectedDate = Calendar.getInstance();

    public interface OnBatchCreatedListener { void onBatchCreated(long batchId); }
    private OnBatchCreatedListener listener;
    public void setOnBatchCreatedListener(OnBatchCreatedListener l) { this.listener = l; }

    public static RcnIntakeDialog newInstance(long farmId) {
        RcnIntakeDialog d = new RcnIntakeDialog();
        Bundle b = new Bundle();
        b.putLong(ARG_FARM_ID, farmId);
        d.setArguments(b);
        return d;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) farmId = getArguments().getLong(ARG_FARM_ID);
        database = AppDatabase.getInstance(requireContext());
        qualityBatchRepository = new QualityBatchRepository(requireContext(), farmId);
        supplierRepository = new SupplierRepository(requireContext(), farmId);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_rcn_intake, null);

        initViews(v);
        setupDryingMethodDropdown();
        loadSuppliers();

        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(requireContext())
                .setView(v).setCancelable(false);
        AlertDialog d = b.create();
        d.setOnShowListener(x -> {
            Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
            Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (p != null) p.setVisibility(View.GONE);
            if (n != null) n.setVisibility(View.GONE);
        });
        return d;
    }

    private void initViews(View v) {
        etSupplier = v.findViewById(R.id.etSupplier);
        etDeliveryBatch = v.findViewById(R.id.etDeliveryBatch);

        etBatchNumberDisplay = v.findViewById(R.id.etBatchNumberDisplay);
        etWeightKgDisplay = v.findViewById(R.id.etWeightKgDisplay);
        etSourceTypeDisplay = v.findViewById(R.id.etSourceTypeDisplay);
        etBatchTypeDisplay = v.findViewById(R.id.etBatchTypeDisplay);

        etInspectedBy = v.findViewById(R.id.etInspectedBy);
        etInspectionDate = v.findViewById(R.id.etInspectionDate);
        etInspectionDate.setOnClickListener(x -> showDatePicker());
        etInspectionDate.setFocusable(false);
        etInspectionDate.setClickable(true);
        etKorSampleGrams = v.findViewById(R.id.etKorSampleGrams);
        etKorKernelGrams = v.findViewById(R.id.etKorKernelGrams);
        etKorComputed    = v.findViewById(R.id.etKorComputed);
        tvKorEquivalents = v.findViewById(R.id.tvKorEquivalents);
        etMoisture = v.findViewById(R.id.etMoisture);
        etNutCount = v.findViewById(R.id.etNutCount);
        etMouldy = v.findViewById(R.id.etMouldy);
        etImmature = v.findViewById(R.id.etImmature);
        etInsectDamaged = v.findViewById(R.id.etInsectDamaged);
        etShrivelled = v.findViewById(R.id.etShrivelled);
        etForeignMatter = v.findViewById(R.id.etForeignMatter);
        etDryingMethod = v.findViewById(R.id.etDryingMethod);

        etDefectiveTotal = v.findViewById(R.id.etDefectiveTotal);
        etComputedGrade = v.findViewById(R.id.etComputedGrade);
        etComputedScore = v.findViewById(R.id.etComputedScore);

        etNotes = v.findViewById(R.id.etNotes);

        updateDateLabel();

        // Read-only fields
        for (TextInputEditText f : new TextInputEditText[]{
                etBatchNumberDisplay, etWeightKgDisplay,
                etDefectiveTotal, etComputedGrade, etComputedScore}) {
            f.setEnabled(false);
            f.setFocusable(false);
        }

        // Live recompute
        android.text.TextWatcher w = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { recomputePreview(); }
            public void afterTextChanged(android.text.Editable s) {}
        };
        for (TextInputEditText f : new TextInputEditText[]{
                etBatchNumberDisplay, etWeightKgDisplay,
                etKorComputed, etDefectiveTotal, etComputedGrade, etComputedScore}) {
            f.setEnabled(false);
            f.setFocusable(false);
        }

        Button btnCancel = v.findViewById(R.id.btnCancel);
        Button btnSave = v.findViewById(R.id.btnSave);
        btnCancel.setOnClickListener(x -> dismiss());
        btnSave.setOnClickListener(x -> saveIntake());
    }

    private void showDatePicker() {
        new DatePickerDialog(
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
        ).show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etInspectionDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupDryingMethodDropdown() {
        String[] methods = {"Sun-dried", "Mechanical", "Unknown"};
        etDryingMethod.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, methods));
        etDryingMethod.setText("Sun-dried", false);
    }

    private void loadSuppliers() {
        executor.execute(() -> {
            supplierList = supplierRepository.getSuppliersSync();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                List<String> names = new ArrayList<>();
                for (SupplierEntity s : supplierList) {
                    names.add(s.getName() + " (" + s.getType().getDisplayName() + ")");
                }
                etSupplier.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, names));
                etSupplier.setOnItemClickListener((p, view, pos, id) ->
                        loadPendingBatchesForSupplier(supplierList.get(pos).getId()));
            });
        });
    }

    /**
     * Load the supplier's batches that haven't yet had RCN quality recorded.
     * "Not yet recorded" = productType is RCN/blank AND korPercent/moisture/nutCount all 0.
     */
    private void loadPendingBatchesForSupplier(long supplierId) {
        executor.execute(() -> {
            List<QualityBatchEntity> all =
                    qualityBatchRepository.getBatchesBySupplierIdSync(supplierId);

            // ✅ Log FIRST — before the filter, before any early return
            android.util.Log.d("RcnIntakeDialog",
                    "supplierId=" + supplierId + " rawCount=" + (all == null ? "null" : all.size()));
            if (all != null) {
                for (QualityBatchEntity b : all) {
                    android.util.Log.d("RcnIntakeDialog",
                            "  raw batch=" + b.getBatchNumber()
                                    + " supplierId=" + b.getSupplierId()
                                    + " productType=" + b.getProductType()
                                    + " kor=" + b.getKorPercent()
                                    + " moisture=" + b.getMoistureContent()
                                    + " nutCount=" + b.getNutCountPerKg()
                                    + " pending=" + isRcnPending(b));
                }
            }

            pendingBatches = new ArrayList<>();
            if (all != null) {
                for (QualityBatchEntity b : all) {
                    if (isRcnPending(b)) pendingBatches.add(b);
                }
            }

            android.util.Log.d("RcnIntakeDialog",
                    "  → pending count=" + pendingBatches.size());

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (pendingBatches.isEmpty()) {
                    etDeliveryBatch.setEnabled(false);
                    etDeliveryBatch.setText("No pending RCN batches for this supplier", false);
                    selectedBatch = null;
                    Toast.makeText(getContext(),
                            "No batches to inspect for supplier " + supplierId
                                    + " (raw=" + (all == null ? 0 : all.size()) + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                List<String> names = new ArrayList<>();
                for (QualityBatchEntity b : pendingBatches) {
                    names.add(b.getBatchNumber() + " - " +
                            b.getFormattedDate() + " (" + b.getWeightKg() + "kg)");
                }
                etDeliveryBatch.setEnabled(true);
                etDeliveryBatch.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, names));
                etDeliveryBatch.setText(names.get(0), false);
                etDeliveryBatch.setOnItemClickListener((p, view, pos, id) ->
                        prefillFromBatch(pendingBatches.get(pos)));
                prefillFromBatch(pendingBatches.get(0));
            });
        });
    }

    private boolean isRcnPending(QualityBatchEntity b) {
        boolean hasKernelQuality = b.getKernelQualityScore() > 0
                || b.getAflatoxinPpb() > 0
                || b.getHeavyMetalsPpb() > 0
                || b.getPesticideResiduePpb() > 0;

        if ("KERNEL".equalsIgnoreCase(b.getProductType()) && hasKernelQuality) {
            return false;
        }

        return b.getKorPercent() <= 0;
    }

    private void prefillFromBatch(QualityBatchEntity b) {
        selectedBatch = b;
        etBatchNumberDisplay.setText(b.getBatchNumber());
        etWeightKgDisplay.setText(String.format(Locale.getDefault(), "%.0f", b.getWeightKg()));

        if (b.getSourceType() != null) {
            etSourceTypeDisplay.setText(b.getSourceType().getDisplayName(), false);
        }
        String bn = b.getBatchNumber();
        if (bn != null) {
            if (bn.contains("-ORG-")) etBatchTypeDisplay.setText("Organic", false);
            else if (bn.contains("-CONV-")) etBatchTypeDisplay.setText("Conventional", false);
        }

        // Pre-fill existing values if any (so partial re-edits are safe)
        if (b.getKorSampleGrams() > 0) {
            etKorSampleGrams.setText(String.format(Locale.getDefault(), "%.0f", b.getKorSampleGrams()));
        }
        if (b.getKorKernelGrams() > 0) {
            etKorKernelGrams.setText(String.format(Locale.getDefault(), "%.0f", b.getKorKernelGrams()));
        }
        if (b.getMoistureContent() > 0) etMoisture.setText(String.valueOf(b.getMoistureContent()));
        if (b.getNutCountPerKg() > 0) etNutCount.setText(String.valueOf(b.getNutCountPerKg()));
        if (b.getMouldyRate() > 0) etMouldy.setText(String.valueOf(b.getMouldyRate()));
        if (b.getImmatureRate() > 0) etImmature.setText(String.valueOf(b.getImmatureRate()));
        if (b.getInsectDamagedRate() > 0) etInsectDamaged.setText(String.valueOf(b.getInsectDamagedRate()));
        if (b.getShrivelledRate() > 0) etShrivelled.setText(String.valueOf(b.getShrivelledRate()));
        if (b.getForeignMatter() > 0) etForeignMatter.setText(String.valueOf(b.getForeignMatter()));
        if (b.getDryingMethod() != null) etDryingMethod.setText(b.getDryingMethod(), false);
        if (b.getNotes() != null) etNotes.setText(b.getNotes());

        recomputePreview();
    }

    private double d(TextInputEditText f) {
        try {
            String s = f.getText() == null ? "" : f.getText().toString().trim();
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) { return 0; }
    }
    private int i(TextInputEditText f) {
        try {
            String s = f.getText() == null ? "" : f.getText().toString().trim();
            return s.isEmpty() ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) { return 0; }
    }

    /** Build a preview entity = selectedBatch + current inputs. */
    private QualityBatchEntity buildFromSelection() {
        if (selectedBatch == null) return null;

        long inspectionTs = selectedDate.getTimeInMillis();

        QualityBatchEntity b = new QualityBatchEntity(
                farmId,
                selectedBatch.getBlockId(),
                selectedBatch.getSupplierId(),
                selectedBatch.getSourceType(),
                selectedBatch.getBatchNumber(),
                selectedBatch.getProcessingDate(),
                d(etMoisture),
                i(etNutCount),
                d(etMouldy) + d(etImmature) + d(etInsectDamaged) + d(etShrivelled),
                0,
                selectedBatch.getWeightKg(),
                QualityBatchEntity.QualityGrade.STANDARD,
                etNotes.getText() == null ? "" : etNotes.getText().toString().trim()
        );

        b.setId(selectedBatch.getId());
        b.setProductType("RCN");

// ✅ Inspection metadata
        b.setInspectionDate(inspectionTs);
        b.setInspectedBy(etInspectedBy.getText() == null
                ? null : etInspectedBy.getText().toString().trim());

// KOR + defect rates
        double sampleGrams = d(etKorSampleGrams);
        double kernelGrams = d(etKorKernelGrams);
        b.setKorSampleGrams(sampleGrams);
        b.setKorKernelGrams(kernelGrams);
        b.setKorGPerKg(computeKorGPerKg(sampleGrams, kernelGrams));
        b.setMouldyRate(d(etMouldy));
        b.setImmatureRate(d(etImmature));
        b.setInsectDamagedRate(d(etInsectDamaged));
        b.setShrivelledRate(d(etShrivelled));
        b.setForeignMatter(d(etForeignMatter));
        b.setDryingMethod(etDryingMethod.getText().toString());
        b.setSupplierName(selectedBatch.getSupplierName());
// Preserve sustainability and audit fields
        b.setCreatedAt(selectedBatch.getCreatedAt());
        b.setVerificationCode(selectedBatch.getVerificationCode());
        b.setNoChildLabor(selectedBatch.isNoChildLabor());
        b.setFarmOsMonitored(selectedBatch.isFarmOsMonitored());
        b.setFirebreakProtection(selectedBatch.isFirebreakProtection());
        b.setSustainableHarvesting(selectedBatch.isSustainableHarvesting());
        b.setVerifiedAt(selectedBatch.getVerifiedAt());
        return b;
    }

    private void recomputePreview() {
        updateKorDisplay();
        QualityBatchEntity p = buildFromSelection();
        if (p == null) return;
        double sum = p.getDefectiveRate();
        etDefectiveTotal.setText(String.format(Locale.getDefault(), "%.2f%%", sum));
        QualityGradeCalculator.Grade g = QualityGradeCalculator.grade(p);
        etComputedGrade.setText(QualityGradeCalculator.toEntityGrade(g).getDisplayName());
        etComputedScore.setText(String.format(Locale.getDefault(), "%.0f%%",
                QualityGradeCalculator.rcnScore(p)));
    }

    /** KOR = sound kernel grams ÷ sample grams × 1000. Returns 0 if sample is invalid. */
    private double computeKorGPerKg(double sampleGrams, double kernelGrams) {
        if (sampleGrams <= 0 || kernelGrams <= 0) return 0;
        return (kernelGrams / sampleGrams) * 1000.0;
    }

    /** Update the read-only KOR display and the lbs/percent equivalents. */
    private void updateKorDisplay() {
        double sample = d(etKorSampleGrams);
        double kernel = d(etKorKernelGrams);
        double kor = computeKorGPerKg(sample, kernel);

        if (kor <= 0) {
            etKorComputed.setText("");
            tvKorEquivalents.setVisibility(View.GONE);
            return;
        }

        etKorComputed.setText(String.format(Locale.getDefault(), "%.0f g/kg", kor));
        tvKorEquivalents.setVisibility(View.VISIBLE);
        tvKorEquivalents.setText(String.format(Locale.getDefault(),
                "≈ %.1f lbs / 80 kg bag  ·  %.1f%%",
                kor / 5.67, kor / 10.0));
    }

    private void saveIntake() {
        if (selectedBatch == null) {
            Toast.makeText(getContext(), "Select a batch first", Toast.LENGTH_SHORT).show();
            return;
        }
        String inspector = etInspectedBy.getText() == null
                ? "" : etInspectedBy.getText().toString().trim();
        if (inspector.isEmpty()) {
            etInspectedBy.setError("Inspector required");
            return;
        }
        double sample = d(etKorSampleGrams);
        double kernel = d(etKorKernelGrams);

        if (sample <= 0) {
            etKorSampleGrams.setError("Sample weight required");
            return;
        }
        if (sample < 500 || sample > 2000) {
            etKorSampleGrams.setError("Sample must be 500–2000 g. A cut test uses ~1 kg.");
            return;
        }
        if (kernel <= 0) {
            etKorKernelGrams.setError("Sound kernel weight required");
            return;
        }
        if (kernel >= sample) {
            etKorKernelGrams.setError("Kernel weight cannot exceed sample weight");
            return;
        }

        double kor = computeKorGPerKg(sample, kernel);
        if (kor < 150 || kor > 350) {
            Toast.makeText(getContext(),
                    String.format(Locale.getDefault(),
                            "Computed KOR is %.0f g/kg — outside the expected 150–350 range. " +
                                    "Check your sample and kernel weights.", kor),
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (d(etMoisture) <= 0) {
            etMoisture.setError("Moisture required");
            return;
        }
        if (i(etNutCount) <= 0) {
            etNutCount.setError("Nut count required");
            return;
        }

        QualityBatchEntity updated = buildFromSelection();
        if (updated == null) return;

        if (!QualityGradeCalculator.defectsConsistent(updated)) {
            Toast.makeText(getContext(),
                    "Defect sum does not reconcile — hold for re-inspection.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long batchId = updated.getId();
        qualityBatchRepository.updateBatch(updated,
                new QualityBatchRepository.RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean r) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(),
                                    "RCN quality recorded for " + updated.getBatchNumber(),
                                    Toast.LENGTH_LONG).show();
                            if (listener != null) listener.onBatchCreated(batchId);
                            dismiss();
                        });
                    }

                    @Override
                    public void onError(String e) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(),
                                        "Error: " + e, Toast.LENGTH_LONG).show());
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (qualityBatchRepository != null) qualityBatchRepository.shutdown();
        if (supplierRepository != null) supplierRepository.shutdown();
        executor.shutdown();
    }
}