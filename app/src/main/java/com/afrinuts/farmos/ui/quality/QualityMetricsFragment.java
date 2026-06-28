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
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Store farmId for use by parent activity
    private long farmId = -1;

    private List<QualityBatchEntity> allBatches = new ArrayList<>();
    private List<QualityBatchEntity> filteredBatches = new ArrayList<>();

    private String currentFilter = "ALL"; // ALL, PREMIUM, STANDARD, PROCESSING

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
        avgKernelValue = view.findViewById(R.id.avgKernelValue);
        avgQualityScoreValue = view.findViewById(R.id.avgQualityScoreValue);
        avgNutCountValue = view.findViewById(R.id.avgNutCountValue);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new QualityBatchAdapter(filteredBatches, batch -> {
            // Navigate to QualityBatchDetailActivity
            Intent intent = new Intent(getActivity(), QualityBatchDetailActivity.class);
            intent.putExtra(QualityBatchDetailActivity.EXTRA_BATCH_ID, batch.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        Chip chipAll = getView().findViewById(R.id.chipAll);
        Chip chipPremium = getView().findViewById(R.id.chipPremium);
        Chip chipStandard = getView().findViewById(R.id.chipStandard);
        Chip chipProcessing = getView().findViewById(R.id.chipProcessing);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        chipPremium.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "PREMIUM";
                applyFilter();
            }
        });

        chipStandard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "STANDARD";
                applyFilter();
            }
        });

        chipProcessing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "PROCESSING";
                applyFilter();
            }
        });
    }

    // Method for parent activity to get farmId
    public long getFarmId() {
        return farmId;
    }

    // Method for parent activity to refresh data after adding batch
    public void refreshData() {
        if (repository != null) {
            loadBatches();
        } else {
            loadFarmAndData();
        }
    }

    private void loadFarmAndData() {
        showLoading(true);

        executor.execute(() -> {
            if (database == null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showEmpty(true);
                        Toast.makeText(getContext(), "Database not initialized", Toast.LENGTH_SHORT).show();
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
                        repository = new QualityBatchRepository(getContext(), currentFarm.getId());
                        loadBatches();
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showEmpty(true);
                    });
                }
            }
        });
    }

    private void loadBatches() {
        if (repository == null) return;

        repository.getAllBatches().observe(getViewLifecycleOwner(), batches -> {
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
                case "PROCESSING":
                    if (batch.getQualityGrade() == QualityBatchEntity.QualityGrade.PROCESSING) {
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
            avgMoistureValue.setText("0%");
            avgDefectiveValue.setText("0%");
            avgQualityScoreValue.setText("0");
            avgKernelValue.setText("0%");
            avgNutCountValue.setText("0");
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
        avgQualityScoreValue.setText(String.valueOf((int) avgScore));
        avgKernelValue.setText(String.format(Locale.getDefault(), "%.0f%%", avgKernel));
        avgNutCountValue.setText(String.format(Locale.getDefault(), "%.0f", avgNutCount));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private void showEmpty(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) {
            loadBatches();
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