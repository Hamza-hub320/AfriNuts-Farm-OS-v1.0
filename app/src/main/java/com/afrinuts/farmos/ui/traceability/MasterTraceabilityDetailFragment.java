package com.afrinuts.farmos.ui.traceability;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.repository.MasterTraceabilityRepository;
import com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity;
import com.afrinuts.farmos.ui.traceability.TraceabilityDetailActivity;
import com.afrinuts.farmos.utils.QRCodeGenerator;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet right-pane rendering of the existing phone master traceability detail. */
public class MasterTraceabilityDetailFragment extends Fragment {

    private static final String ARG_MASTER_ID = "master_id";

    public static MasterTraceabilityDetailFragment newInstance(long masterId) {
        MasterTraceabilityDetailFragment fragment = new MasterTraceabilityDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASTER_ID, masterId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<BatchTraceabilityEntity> linkedBatches = new ArrayList<>();
    private long masterId = -1;
    private MasterTraceabilityRepository repository;
    private MasterBatchTraceabilityEntity master;
    private View root;
    private TextView tvMasterBatchNumber, tvShipmentName, tvHeroSubtitle, tvBuyer,
            tvDestination, tvExportDate, tvTotalWeight, tvTotalBatches, tvGrades,
            tvCertifications, tvTraceabilityUrl;
    private LinearLayout layoutMasterBatches;
    private MaterialButton btnDelete, btnGenerateQR, btnGenerateQRBottom, btnShare;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.activity_master_traceability_detail, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        masterId = getArguments() == null ? -1 : getArguments().getLong(ARG_MASTER_ID, -1);
        repository = new MasterTraceabilityRepository(requireContext(), 1);
        sessionManager = new SessionManager(requireContext());
        bindViews(view);
        loadData();
    }

    private void bindViews(View view) {
        tvMasterBatchNumber = view.findViewById(R.id.tvMasterBatchNumber);
        tvShipmentName = view.findViewById(R.id.tvShipmentName);
        tvHeroSubtitle = view.findViewById(R.id.tvHeroSubtitle);
        tvBuyer = view.findViewById(R.id.tvBuyer);
        tvDestination = view.findViewById(R.id.tvDestination);
        tvExportDate = view.findViewById(R.id.tvExportDate);
        tvTotalWeight = view.findViewById(R.id.tvTotalWeight);
        tvTotalBatches = view.findViewById(R.id.tvTotalBatches);
        tvGrades = view.findViewById(R.id.tvGrades);
        tvCertifications = view.findViewById(R.id.tvCertifications);
        tvTraceabilityUrl = view.findViewById(R.id.tvTraceabilityUrl);
        layoutMasterBatches = view.findViewById(R.id.layoutMasterBatches);
        btnDelete = view.findViewById(R.id.btnDelete);
        btnGenerateQR = view.findViewById(R.id.btnGenerateQR);
        btnGenerateQRBottom = view.findViewById(R.id.btnGenerateQRBottom);
        btnShare = view.findViewById(R.id.btnShare);

        View back = view.findViewById(R.id.btnHeaderBack);
        if (back != null) back.setVisibility(View.GONE);
        View headerShare = view.findViewById(R.id.btnHeaderShare);
        if (headerShare != null) headerShare.setOnClickListener(v -> shareMasterTraceability());
        btnShare.setOnClickListener(v -> shareMasterTraceability());
        btnDelete.setOnClickListener(v -> confirmDelete());
        btnGenerateQR.setOnClickListener(v -> generateQRCode());
        btnGenerateQRBottom.setOnClickListener(v -> generateQRCode());
        if (!sessionManager.canEditData()) btnDelete.setVisibility(View.GONE);
    }

    private void loadData() {
        if (masterId < 0) return;
        executor.execute(() -> {
            MasterBatchTraceabilityEntity loaded = repository.getMasterByIdSync(masterId);
            if (loaded == null) {
                postToUi(() -> Toast.makeText(requireContext(), "Master traceability not found", Toast.LENGTH_SHORT).show());
                return;
            }

            List<BatchTraceabilityEntity> children = repository.repairMissingChildrenSync(loaded);

            List<BatchTraceabilityEntity> result = new ArrayList<>();
            for (BatchTraceabilityEntity child : children) if (child != null) result.add(child);
            MasterBatchTraceabilityEntity masterResult = loaded;
            List<BatchTraceabilityEntity> batchResult = result;
            postToUi(() -> {
                master = masterResult;
                linkedBatches.clear();
                linkedBatches.addAll(batchResult);
                displayMasterData();
            });
        });
    }

    private void displayMasterData() {
        if (master == null || !isAdded()) return;
        tvMasterBatchNumber.setText("#" + master.getMasterBatchNumber());
        tvShipmentName.setText(master.getShipmentName());
        String buyer = master.getBuyer() != null ? master.getBuyer() : "—";
        tvHeroSubtitle.setText(buyer + " · " + master.getFormattedExportDate());
        tvBuyer.setText(buyer);
        tvDestination.setText(master.getDestination() != null ? master.getDestination() : "—");
        tvExportDate.setText(master.getFormattedExportDate());
        tvTotalWeight.setText(String.format(Locale.getDefault(), "%,.0f", master.getTotalWeightKg()));
        tvTotalBatches.setText(String.valueOf(master.getTotalBatches()));
        String grades = master.getGrades();
        tvGrades.setText(grades != null && !grades.trim().isEmpty() ? grades : "—");
        String certifications = master.getCertifications();
        tvCertifications.setText(certifications != null && !certifications.trim().isEmpty() ? certifications : "—");
        tvTraceabilityUrl.setText(master.getTraceabilityUrl());
        populateMasterBatchRows();
    }

    private void populateMasterBatchRows() {
        layoutMasterBatches.removeAllViews();
        for (int position = 0; position < linkedBatches.size(); position++) {
            BatchTraceabilityEntity batch = linkedBatches.get(position);
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_master_batch, layoutMasterBatches, false);
            TextView batchNumber = row.findViewById(R.id.tvMasterChildBatchNumber);
            TextView grade = row.findViewById(R.id.tvMasterChildGrade);
            TextView weightAndDate = row.findViewById(R.id.tvMasterChildWeightDate);
            View divider = row.findViewById(R.id.masterChildDivider);
            batchNumber.setText("#" + batch.getBatchNumber());
            String qualityGrade = batch.getQualityGrade();
            String gradeLabel = qualityGrade != null && !qualityGrade.trim().isEmpty() ? qualityGrade.trim() : "—";
            grade.setText(gradeLabel.toUpperCase(Locale.ROOT));
            boolean premium = gradeLabel.toLowerCase(Locale.ROOT).contains("premium");
            grade.setBackgroundResource(premium ? R.drawable.bg_revenue_premium_badge : R.drawable.bg_revenue_standard_badge);
            grade.setTextColor(premium ? 0xFF0A5530 : 0xFF80601E);
            weightAndDate.setText(batch.getFormattedWeight() + " · " + batch.getFormattedProcessingDate());
            divider.setVisibility(position == linkedBatches.size() - 1 ? View.GONE : View.VISIBLE);
            row.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), TraceabilityDetailActivity.class);
                intent.putExtra(TraceabilityDetailActivity.EXTRA_TRACEABILITY_ID, batch.getId());
                startActivity(intent);
            });
            layoutMasterBatches.addView(row);
        }
    }

    private void generateQRCode() {
        if (master == null) return;
        Bitmap bitmap = QRCodeGenerator.generateQRCode(master.getQrCodeUrl());
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Master QR Code")
                .setMessage("Scan this QR code to view the master traceability")
                .setPositiveButton("Share", (dialog, which) ->
                        Toast.makeText(requireContext(), "Share QR code", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void shareMasterTraceability() {
        if (master == null) return;
        String shareText = "AfriNuts Master Traceability\n"
                + "=================================\n"
                + "Master Batch: " + master.getMasterBatchNumber() + "\n"
                + "Shipment: " + master.getShipmentName() + "\n"
                + "Buyer: " + master.getBuyer() + "\n"
                + "Destination: " + master.getDestination() + "\n"
                + "Total Weight: " + master.getFormattedWeight() + "\n"
                + "Total Batches: " + master.getTotalBatches() + "\n"
                + "Grades: " + master.getGrades() + "\n"
                + "Export Date: " + master.getFormattedExportDate() + "\n\n"
                + "Trace URL: " + master.getTraceabilityUrl();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Master Traceability"));
    }

    private void confirmDelete() {
        if (!sessionManager.isOwner()) {
            Toast.makeText(requireContext(), "Only owners can delete master traceability records", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Master Traceability")
                .setMessage("Are you sure you want to delete this master traceability record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMasterTraceability())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMasterTraceability() {
        if (master == null) return;
        Toast.makeText(requireContext(), "Deleting...", Toast.LENGTH_SHORT).show();
        repository.deleteMasterTraceability(master, new MasterTraceabilityRepository.RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Master traceability deleted", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof AnalyticsHubActivity) ((AnalyticsHubActivity) getActivity()).onMasterDeleted();
            }
            @Override public void onError(String error) {
                if (isAdded()) Toast.makeText(requireContext(), "Error deleting: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void postToUi(Runnable action) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> { if (isAdded()) action.run(); });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) repository.shutdown();
        executor.shutdown();
    }
}
