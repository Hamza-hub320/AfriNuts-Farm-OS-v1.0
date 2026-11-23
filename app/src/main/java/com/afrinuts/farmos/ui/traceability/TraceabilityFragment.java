package com.afrinuts.farmos.ui.traceability;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.repository.BatchTraceabilityRepository;
import com.afrinuts.farmos.utils.BulkQRGenerator;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TraceabilityFragment extends Fragment {

    private RecyclerView recyclerView;
    private TraceabilityAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvTotalBatches;
    private TextView tvExportedBatches;
    private TextView tvTotalWeight;
    private ChipGroup filterChipGroup;
    private FloatingActionButton fabAdd;

    private AppDatabase database;
    private BatchTraceabilityRepository repository;
    private FarmEntity currentFarm;
    private long farmId = -1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<BatchTraceabilityEntity> allTraceabilities = new ArrayList<>();
    private List<BatchTraceabilityEntity> filteredTraceabilities = new ArrayList<>();

    private String currentFilter = "ALL"; // ALL, EXPORTED, PROCESSING

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_traceability, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupFilters();
        setupFab();
        loadFarmData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvTotalBatches = view.findViewById(R.id.tvTotalBatches);
        tvExportedBatches = view.findViewById(R.id.tvExportedBatches);
        tvTotalWeight = view.findViewById(R.id.tvTotalWeight);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        fabAdd = view.findViewById(R.id.fabAdd);
    }

    private void setupFab() {
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> showTraceabilityOptions());
        }
    }

    public void showTraceabilityOptions() {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_traceability_options, null);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();

        view.findViewById(R.id.btnCloseOptions).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnCancelOptions).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnOptionScan).setOnClickListener(v -> {
            dialog.dismiss();
            scanQRCode();
        });
        view.findViewById(R.id.btnOptionBulk).setOnClickListener(v -> {
            dialog.dismiss();
            showBulkQRDialog();
        });
        view.findViewById(R.id.btnOptionBatches).setOnClickListener(v -> {
            dialog.dismiss();
            showBulkQRDialog(true);
        });
        dialog.setOnShowListener(ignored -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
                dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });
        dialog.show();
    }

    private void scanQRCode() {
        Intent intent = new Intent(getActivity(), QRScannerActivity.class);
        startActivity(intent);
    }

    private void showBulkQRDialog() {
        showBulkQRDialog(false);
    }

    private void showBulkQRDialog(boolean scrollToBatchList) {
        if (allTraceabilities.isEmpty()) {
            Toast.makeText(getContext(), "No traceability records found", Toast.LENGTH_SHORT).show();
            return;
        }

        BulkQRDialog dialog = BulkQRDialog.newInstance(scrollToBatchList);
        dialog.setOnBulkQRGeneratedListener(pdfFile -> {
            BulkQRGenerator generator = new BulkQRGenerator(requireContext());
            generator.sharePDF(pdfFile);
            Toast.makeText(getContext(), "PDF generated: " + pdfFile.getName(), Toast.LENGTH_SHORT).show();
        });
        dialog.show(getChildFragmentManager(), "BulkQRDialog");
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TraceabilityAdapter(filteredTraceabilities, traceability -> {
            Intent intent = new Intent(getActivity(), TraceabilityDetailActivity.class);
            intent.putExtra(TraceabilityDetailActivity.EXTRA_TRACEABILITY_ID, traceability.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        Chip chipAll = filterChipGroup.findViewById(R.id.chipAll);
        Chip chipExported = filterChipGroup.findViewById(R.id.chipExported);
        Chip chipProcessing = filterChipGroup.findViewById(R.id.chipProcessing);

        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "ALL";
                    applyFilter();
                }
            });
        }

        if (chipExported != null) {
            chipExported.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "EXPORTED";
                    applyFilter();
                }
            });
        }

        if (chipProcessing != null) {
            chipProcessing.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "PROCESSING";
                    applyFilter();
                }
            });
        }
    }

    private void loadFarmData() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                farmId = currentFarm.getId();
                repository = new BatchTraceabilityRepository(requireContext(), farmId);
                loadTraceabilities();
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                });
            }
        });
    }

    private void loadTraceabilities() {
        executor.execute(() -> {
            List<BatchTraceabilityEntity> traceabilities = repository.getAllTraceabilitiesSync();

            requireActivity().runOnUiThread(() -> {
                allTraceabilities.clear();
                if (traceabilities != null) {
                    allTraceabilities.addAll(traceabilities);
                }

                showLoading(false);

                if (allTraceabilities.isEmpty()) {
                    showEmpty(true);
                } else {
                    showEmpty(false);
                    updateSummaryStats();
                    applyFilter();
                }
            });
        });
    }

    private void applyFilter() {
        filteredTraceabilities.clear();

        for (BatchTraceabilityEntity traceability : allTraceabilities) {
            switch (currentFilter) {
                case "ALL":
                    filteredTraceabilities.add(traceability);
                    break;
                case "EXPORTED":
                    if (traceability.isExported()) {
                        filteredTraceabilities.add(traceability);
                    }
                    break;
                case "PROCESSING":
                    if (!traceability.isExported()) {
                        filteredTraceabilities.add(traceability);
                    }
                    break;
            }
        }

        adapter.updateTraceabilities(filteredTraceabilities);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void updateSummaryStats() {
        int total = allTraceabilities.size();
        int exported = 0;
        double totalWeight = 0;

        for (BatchTraceabilityEntity traceability : allTraceabilities) {
            if (traceability.isExported()) exported++;
            totalWeight += traceability.getWeightKg();
        }

        tvTotalBatches.setText(String.valueOf(total));
        tvExportedBatches.setText(String.valueOf(exported));
        tvTotalWeight.setText(String.format(Locale.getDefault(), "%,.0f", totalWeight));
    }

    private void showLoading(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
        });
    }

    private void showEmpty(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (emptyView != null) {
                emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentFarm != null) {
            loadTraceabilities();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
        executor.shutdown();
    }
}
