package com.afrinuts.farmos.ui.quality;

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
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class QualityMetricsFragment extends Fragment {

    private static final String TAG = "QualityMetricsFragment";

    private RecyclerView recyclerView;
    private QualityBatchAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView avgMoistureValue;
    private TextView avgDefectiveValue;
    private TextView avgQualityScoreValue;
    private TextView avgKernelValue;
    private TextView avgNutCountValue;
    private ChipGroup filterChipGroup;

    private QualityBatchRepository repository;
    private AppDatabase database;
    private FarmEntity currentFarm;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // Store farmId for use by parent activity
    private volatile long farmId = -1;

    private List<QualityBatchEntity> allBatches = new ArrayList<>();
    private List<QualityBatchEntity> filteredBatches = new ArrayList<>();

    private String currentFilter = "ALL"; // ALL, PREMIUM, STANDARD, REJECT

    // Flag to track if fragment is destroyed
    private boolean isDestroyed = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quality_metrics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize database
        if (getContext() != null) {
            database = AppDatabase.getInstance(getContext());
        }

        initViews(view);
        setupRecyclerView();
        setupFilters();
        loadFarmAndData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        avgMoistureValue = view.findViewById(R.id.avgMoistureValue);
        avgDefectiveValue = view.findViewById(R.id.avgDefectiveValue);
        avgQualityScoreValue = view.findViewById(R.id.avgQualityScoreValue);
        avgKernelValue = view.findViewById(R.id.avgKernelValue);
        avgNutCountValue = view.findViewById(R.id.avgNutCountValue);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QualityBatchAdapter(filteredBatches, batch -> {
            // ✅ Use getActivity() to start intent
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), QualityBatchDetailActivity.class);
                intent.putExtra(QualityBatchDetailActivity.EXTRA_BATCH_ID, batch.getId());
                getActivity().startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        Chip chipAll = getView().findViewById(R.id.chipAll);
        Chip chipPremium = getView().findViewById(R.id.chipPremium);
        Chip chipStandard = getView().findViewById(R.id.chipStandard);
        Chip chipReject = getView().findViewById(R.id.chipReject);

        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "ALL";
                    applyFilter();
                }
            });
        }

        if (chipPremium != null) {
            chipPremium.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "PREMIUM";
                    applyFilter();
                }
            });
        }

        if (chipStandard != null) {
            chipStandard.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "STANDARD";
                    applyFilter();
                }
            });
        }

        if (chipReject != null) {
            chipReject.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "REJECT";
                    applyFilter();
                }
            });
        }
    }

    // add after refreshData()
    private void showRcnIntakeDialog() {
        if (farmId <= 0) {
            Toast.makeText(getContext(), "Loading farm data...", Toast.LENGTH_SHORT).show();
            return;
        }
        RcnIntakeDialog d = RcnIntakeDialog.newInstance(farmId);
        d.setOnBatchCreatedListener(id -> {
            loadBatches();
            Toast.makeText(requireContext(),
                    "RCN batch created — id " + id, Toast.LENGTH_SHORT).show();
        });
        d.show(getParentFragmentManager(), "RcnIntakeDialog");
    }

    public void openRcnIntakeDialog() {
        showRcnIntakeDialog();
    }

    // Method for parent activity to get farmId
    public long getFarmId() {
        return farmId;
    }

    // Method for parent activity to refresh data after adding batch
    public void refreshData() {
        if (repository != null && !isDestroyed) {
            loadBatches();
        } else if (!isDestroyed) {
            loadFarmAndData();
        }
    }

    private void loadFarmAndData() {
        // Check if fragment is destroyed before executing
        if (isDestroyed || getActivity() == null) {
            return;
        }

        showLoading(true);

        executor.execute(() -> {
            // Check again inside the thread
            if (isDestroyed || getActivity() == null) {
                return;
            }

            if (database == null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isDestroyed) {
                            showLoading(false);
                            showEmpty(true);
                            Toast.makeText(getContext(), "Database not initialized", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return;
            }

            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // SET THE FARM ID HERE
                farmId = currentFarm.getId();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isDestroyed) {
                            repository = new QualityBatchRepository(getContext(), currentFarm.getId());
                            loadBatches();
                        }
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isDestroyed) {
                            showLoading(false);
                            showEmpty(true);
                        }
                    });
                }
            }
        });
    }

    private void loadBatches() {
        // Check if fragment is destroyed
        if (isDestroyed || repository == null || getViewLifecycleOwner() == null) {
            return;
        }

        repository.getAllBatches().observe(getViewLifecycleOwner(), batches -> {
            if (isDestroyed) {
                return;
            }

            allBatches.clear();
            if (batches != null) {
                allBatches.addAll(batches);
            }

            showLoading(false);

            if (allBatches.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                updateSummaryStats();
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        filteredBatches.clear();

        for (QualityBatchEntity batch : allBatches) {
            switch (currentFilter) {
                case "ALL":
                    filteredBatches.add(batch);
                    break;
                case "PREMIUM":
                    if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.PREMIUM) {
                        filteredBatches.add(batch);
                    }
                    break;
                case "STANDARD":
                    if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.STANDARD) {
                        filteredBatches.add(batch);
                    }
                    break;
                case "REJECT":
                    if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.REJECT) {
                        filteredBatches.add(batch);
                    }
                    break;
            }
        }

        adapter.updateBatches(filteredBatches);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void updateSummaryStats() {
        if (allBatches.isEmpty()) {
            avgMoistureValue.setText("0.0%");
            avgDefectiveValue.setText("0.0%");
            avgQualityScoreValue.setText("0 / 100");
            avgKernelValue.setText("0.0%");
            avgNutCountValue.setText("0 / kg");
            return;
        }

        double totalMoisture = 0;
        double totalDefective = 0;
        double totalScore = 0;
        double totalKernel = 0;
        int totalNutCount = 0;
        int count = allBatches.size();

        for (QualityBatchEntity batch : allBatches) {
            totalMoisture += batch.getMoistureContent();
            totalDefective += batch.getDefectiveRate();
            totalScore += batch.getQualityScore();
            totalKernel += batch.getKernelRatio();
            totalNutCount += batch.getNutCountPerKg();
        }

        double avgMoisture = totalMoisture / count;
        double avgDefective = totalDefective / count;
        double avgScore = totalScore / count;
        double avgKernel = totalKernel / count;
        double avgNutCount = (double) totalNutCount / count;

        avgMoistureValue.setText(String.format(Locale.getDefault(), "%.1f%%", avgMoisture));
        avgDefectiveValue.setText(String.format(Locale.getDefault(), "%.1f%%", avgDefective));
        avgKernelValue.setText(String.format(Locale.getDefault(), "%.1f%%", avgKernel));
        avgNutCountValue.setText(String.format(Locale.getDefault(), "%.0f / kg", avgNutCount));
        avgQualityScoreValue.setText(String.format(Locale.getDefault(), "%.0f / 100", avgScore));
    }

    private void showLoading(boolean show) {
        if (isDestroyed) {
            return;
        }

        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        if (isDestroyed) {
            return;
        }

        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repository != null && !isDestroyed) {
            loadBatches();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;

        // Shutdown executor properly
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (repository != null) {
            repository.shutdown();
        }
    }
}
