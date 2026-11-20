package com.afrinuts.farmos.ui.quality;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
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
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
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

public class RcnEditDialog extends com.afrinuts.farmos.ui.farm.FarmDialogFragment {

    private static final String TAG = "RcnEditDialog";
    private static final String ARG_FARM_ID = "farm_id";
    private static final String ARG_BATCH_ID = "batch_id";

    private long farmId;
    private long batchId;
    private QualityBatchEntity existing;
    private QualityBatchRepository repo;
    private SupplierRepository supplierRepo;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private MaterialAutoCompleteTextView etSupplier;
    private TextInputEditText etInspectionDate;
    private TextInputEditText etInspectedBy;

    // ✅ Path B: two raw KOR measurements + computed display
    private TextInputEditText etKorSampleGrams;
    private TextInputEditText etKorKernelGrams;
    private TextInputEditText etKorComputed;
    private TextView tvKorEquivalents;

    private TextInputEditText etMoisture;
    private TextInputEditText etNutCount;
    private TextInputEditText etMouldy;
    private TextInputEditText etImmature;
    private TextInputEditText etInsectDamaged;
    private TextInputEditText etShrivelled;
    private TextInputEditText etForeignMatter;
    private MaterialAutoCompleteTextView etDryingMethod;
    private TextInputEditText etDefectiveTotal;
    private TextInputEditText etComputedGrade;
    private TextInputEditText etComputedScore;
    private TextInputEditText etBatchNumberDisplay;
    private TextInputEditText etWeightKgDisplay;
    private MaterialAutoCompleteTextView etSourceTypeDisplay;
    private MaterialAutoCompleteTextView etBatchTypeDisplay;
    private TextInputEditText etNotes;

    private final Calendar selectedDate = Calendar.getInstance();

    public interface OnUpdatedListener { void onUpdated(); }
    private OnUpdatedListener listener;

    public static RcnEditDialog newInstance(long farmId, long batchId) {
        RcnEditDialog d = new RcnEditDialog();
        Bundle b = new Bundle();
        b.putLong(ARG_FARM_ID, farmId);
        b.putLong(ARG_BATCH_ID, batchId);
        d.setArguments(b);
        return d;
    }

    public void setOnUpdatedListener(OnUpdatedListener l) { this.listener = l; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            farmId = getArguments().getLong(ARG_FARM_ID);
            batchId = getArguments().getLong(ARG_BATCH_ID);
        }
        repo = new QualityBatchRepository(requireContext(), farmId);
        supplierRepo = new SupplierRepository(requireContext(), farmId);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_rcn_intake, null);
        initViews(v);
        loadSuppliersThenPopulate();

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
        etInspectionDate = v.findViewById(R.id.etInspectionDate);
        etInspectedBy = v.findViewById(R.id.etInspectedBy);
        etBatchNumberDisplay  = v.findViewById(R.id.etBatchNumberDisplay);
        etWeightKgDisplay     = v.findViewById(R.id.etWeightKgDisplay);
        etSourceTypeDisplay   = v.findViewById(R.id.etSourceTypeDisplay);
        etBatchTypeDisplay    = v.findViewById(R.id.etBatchTypeDisplay);

        // ✅ Path B KOR fields
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

        // Read-only fields
        for (TextInputEditText f : new TextInputEditText[]{
                etKorComputed, etDefectiveTotal, etComputedGrade, etComputedScore,
                etBatchNumberDisplay, etWeightKgDisplay}) {
            f.setEnabled(false);
            f.setFocusable(false);
        }

        String[] methods = {"Sun-dried", "Mechanical", "Unknown"};
        etDryingMethod.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, methods));

        etInspectionDate.setOnClickListener(x -> showDatePicker());
        etInspectionDate.setFocusable(false);
        etInspectionDate.setClickable(true);

        android.text.TextWatcher w = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { recompute(); }
            public void afterTextChanged(android.text.Editable s) {}
        };
        for (TextInputEditText f : new TextInputEditText[]{
                etKorSampleGrams, etKorKernelGrams,
                etMoisture, etNutCount, etMouldy, etImmature,
                etInsectDamaged, etShrivelled, etForeignMatter}) {
            f.addTextChangedListener(w);
        }

        ((Button) v.findViewById(R.id.btnCancel)).setOnClickListener(x -> dismiss());
        ((Button) v.findViewById(R.id.btnSave)).setText("Update Intake");
        ((Button) v.findViewById(R.id.btnSave)).setOnClickListener(x -> save());
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    selectedDate.set(y, m, d);
                    updateDateLabel();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        etInspectionDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void loadSuppliersThenPopulate() {
        executor.execute(() -> {
            if (existing == null) {
                existing = repo.getBatchByIdSync(batchId);
                if (existing != null) {
                    long dateToUse = existing.getInspectionDate() > 0
                            ? existing.getInspectionDate()
                            : existing.getProcessingDate();
                    selectedDate.setTimeInMillis(dateToUse);
                }
            }
            List<SupplierEntity> suppliers = supplierRepo.getSuppliersSync();
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                List<String> names = new ArrayList<>();
                for (SupplierEntity s : suppliers) {
                    names.add(s.getName() + " (" + s.getType().getDisplayName() + ")");
                }
                etSupplier.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, names));
                etSupplier.setOnItemClickListener((p, view, pos, id) -> {
                    existing.setSupplierId(suppliers.get(pos).getId());
                    existing.setSupplierName(suppliers.get(pos).getName());
                });

                if (existing != null) {
                    if (existing.getSupplierId() != null) {
                        for (int i = 0; i < suppliers.size(); i++) {
                            if (suppliers.get(i).getId() == existing.getSupplierId()) {
                                etSupplier.setText(names.get(i), false);
                                break;
                            }
                        }
                    }
                    populateFields();
                }
            });
        });
    }

    private void populateFields() {
        // --- Read-only batch summary ---
        etBatchNumberDisplay.setText(existing.getBatchNumber());
        etWeightKgDisplay.setText(String.format(Locale.getDefault(), "%.0f",
                existing.getWeightKg()));
        if (existing.getSourceType() != null) {
            etSourceTypeDisplay.setText(existing.getSourceType().getDisplayName(), false);
        }
        if (existing.getInspectedBy() != null) {
            etInspectedBy.setText(existing.getInspectedBy());
        }
        String bn = existing.getBatchNumber();
        if (bn != null) {
            if (bn.contains("-ORG-")) etBatchTypeDisplay.setText("Organic", false);
            else if (bn.contains("-CONV-")) etBatchTypeDisplay.setText("Conventional", false);
        }

        // --- Editable inspection ---
        updateDateLabel();

        // --- ✅ Path B: prefill the two raw KOR measurements ---
        if (existing.getKorSampleGrams() > 0) {
            etKorSampleGrams.setText(String.format(Locale.getDefault(),
                    "%.0f", existing.getKorSampleGrams()));
        }
        if (existing.getKorKernelGrams() > 0) {
            etKorKernelGrams.setText(String.format(Locale.getDefault(),
                    "%.0f", existing.getKorKernelGrams()));
        }
        // If the batch has a computed korGPerKg but no raw measurements (legacy
        // pre-Path-B batch), leave the two inputs empty — the user can re-enter
        // the raw measurements to repair the audit trail, or save to keep the
        // computed KOR unchanged (handled in save()).

        // --- Editable quality metrics ---
        etMoisture.setText(String.valueOf(existing.getMoistureContent()));
        etNutCount.setText(String.valueOf(existing.getNutCountPerKg()));
        etMouldy.setText(String.valueOf(existing.getMouldyRate()));
        etImmature.setText(String.valueOf(existing.getImmatureRate()));
        etInsectDamaged.setText(String.valueOf(existing.getInsectDamagedRate()));
        etShrivelled.setText(String.valueOf(existing.getShrivelledRate()));
        etForeignMatter.setText(String.valueOf(existing.getForeignMatter()));
        etDryingMethod.setText(existing.getDryingMethod() != null
                ? existing.getDryingMethod() : "Sun-dried", false);

        etNotes.setText(existing.getNotes());

        recompute();
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

    private void recompute() {
        updateKorDisplay();
        double defectSum = d(etMouldy) + d(etImmature) + d(etInsectDamaged) + d(etShrivelled);
        etDefectiveTotal.setText(String.format(Locale.getDefault(), "%.2f%%", defectSum));

        QualityBatchEntity probe = buildEntity();
        QualityGradeCalculator.Grade g = QualityGradeCalculator.grade(probe);
        etComputedGrade.setText(QualityGradeCalculator.toEntityGrade(g).getDisplayName());
        etComputedScore.setText(String.format(Locale.getDefault(), "%.0f%%",
                QualityGradeCalculator.rcnScore(probe)));
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

    private QualityBatchEntity buildEntity() {
        QualityBatchEntity b = new QualityBatchEntity(
                farmId,
                existing.getBlockId(),
                existing.getSupplierId(),
                existing.getSourceType(),
                existing.getBatchNumber(),
                existing.getProcessingDate(),
                d(etMoisture), i(etNutCount),
                d(etMouldy) + d(etImmature) + d(etInsectDamaged) + d(etShrivelled),
                0, existing.getWeightKg(),
                QualityBatchEntity.QualityGrade.STANDARD,
                etNotes.getText() == null ? "" : etNotes.getText().toString().trim()
        );
        b.setId(existing.getId());
        b.setProductType("RCN");

        // ✅ Inspection metadata
        b.setInspectionDate(selectedDate.getTimeInMillis());
        b.setInspectedBy(etInspectedBy.getText() == null
                ? null : etInspectedBy.getText().toString().trim());

        double sampleGrams = d(etKorSampleGrams);
        double kernelGrams = d(etKorKernelGrams);
        b.setKorSampleGrams(sampleGrams);
        b.setKorKernelGrams(kernelGrams);
        double computed = computeKorGPerKg(sampleGrams, kernelGrams);
        b.setKorGPerKg(computed > 0 ? computed : existing.getKorGPerKg());

        b.setMouldyRate(d(etMouldy));
        b.setImmatureRate(d(etImmature));
        b.setInsectDamagedRate(d(etInsectDamaged));
        b.setShrivelledRate(d(etShrivelled));
        b.setForeignMatter(d(etForeignMatter));
        b.setDryingMethod(etDryingMethod.getText().toString());
        b.setSupplierName(existing.getSupplierName());
        b.setNoChildLabor(existing.isNoChildLabor());
        b.setFarmOsMonitored(existing.isFarmOsMonitored());
        b.setFirebreakProtection(existing.isFirebreakProtection());
        b.setSustainableHarvesting(existing.isSustainableHarvesting());
        b.setVerifiedAt(existing.getVerifiedAt());
        b.setVerificationCode(existing.getVerificationCode());
        b.setCreatedAt(existing.getCreatedAt());
        return b;
    }

    /**
     * SOP §4.3 guard + save dispatch.
     *
     * The revenue lookup is a Room query and must not run on the main thread —
     * save() is invoked from the click handler, so the query goes through the
     * dialog's existing executor and posts back to main. The rest of save() is
     * unchanged from before; it just lives in continueSave() now.
     */
    private void save() {
        if (existing == null) {
            Toast.makeText(getContext(), "Batch not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        final long batchId = existing.getId();

        executor.execute(() -> {
            List<RevenueEntity> existingRevenues =
                    repo.getRevenuesByBatchIdSync(batchId);

            boolean hasRevenue = existingRevenues != null && !existingRevenues.isEmpty();

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (hasRevenue) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Batch already sold")
                            .setMessage("This batch has a sale recorded against it. " +
                                    "Changing its quality metrics would make the recorded grade " +
                                    "differ from the actual grade.\n\n" +
                                    "The sale must be reversed before quality can be edited (SOP §4.3).")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                continueSave();
            });
        });
    }

    /** Runs on the main thread, after the SOP §4.3 revenue check has passed. */
    private void continueSave() {
        if (existing == null) {
            Toast.makeText(getContext(), "Batch not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        double sample = d(etKorSampleGrams);
        double kernel = d(etKorKernelGrams);

        // ✅ Path B validation — same rules as intake
        // If the user is re-editing a legacy batch without entering raw
        // measurements, allow the save only if a KOR already exists.
        boolean legacyNoRaw = (sample == 0 && kernel == 0)
                && existing.getKorGPerKg() > 0;

        if (!legacyNoRaw) {
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
        }

        QualityBatchEntity updated = buildEntity();

        if (!QualityGradeCalculator.defectsConsistent(updated)) {
            Toast.makeText(getContext(),
                    "Defect sum does not reconcile — hold for re-inspection.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        repo.updateBatch(updated, new QualityBatchRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean r) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "RCN intake updated", Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onUpdated();
                    dismiss();
                });
            }
            @Override public void onError(String e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error: " + e, Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repo != null) repo.shutdown();
        if (supplierRepo != null) supplierRepo.shutdown();
        executor.shutdown();
    }
}