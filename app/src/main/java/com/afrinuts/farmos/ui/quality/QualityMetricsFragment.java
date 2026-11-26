package com.afrinuts.farmos.ui.quality;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

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

    public interface OnBatchSelectedListener {
        void onBatchSelected(long batchId);
    }

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
    private boolean tablet;
    private long selectedBatchId = -1;
    private long pendingSelectedBatchId = -1;
    private OnBatchSelectedListener selectionListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnBatchSelectedListener) {
            selectionListener = (OnBatchSelectedListener) context;
        }
    }

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
        tablet = getResources().getConfiguration().screenWidthDp >= 600;
        if (tablet) {
            View kpiRow = ((ViewGroup) view).getChildAt(0);
            if (kpiRow != null) kpiRow.setVisibility(View.GONE);
            if (filterChipGroup != null && filterChipGroup.getParent() instanceof View) {
                ((View) filterChipGroup.getParent()).setVisibility(View.GONE);
            }
            GradientDrawable listSurface = new GradientDrawable();
            listSurface.setColor(Color.WHITE);
            listSurface.setCornerRadius(getResources().getDisplayMetrics().density * 26f);
            recyclerView.setBackground(listSurface);
            recyclerView.setPadding(dp(8), dp(8), dp(8), dp(20));
            recyclerView.setClipToOutline(true);
        }
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
            if (tablet && selectionListener != null) {
                selectedBatchId = batch.getId();
                adapter.setSelectedBatchId(selectedBatchId);
                selectionListener.onBatchSelected(selectedBatchId);
            } else if (getActivity() != null) {
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

    public void openBatchById(long batchId) {
        pendingSelectedBatchId = batchId;
        if (!allBatches.isEmpty()) applyFilter();
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
                filteredBatches.clear();
                adapter.updateBatches(filteredBatches);
                selectedBatchId = -1;
                if (tablet && selectionListener != null) selectionListener.onBatchSelected(-1);
                updateSummaryStats();
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
        if (tablet) {
            selectedBatchId = filteredBatches.isEmpty() ? -1 : filteredBatches.get(0).getId();
            int selectedPosition = filteredBatches.isEmpty() ? -1 : 0;
            if (pendingSelectedBatchId >= 0) {
                for (int i = 0; i < filteredBatches.size(); i++) {
                    QualityBatchEntity item = filteredBatches.get(i);
                    if (item.getId() == pendingSelectedBatchId) {
                        selectedBatchId = pendingSelectedBatchId;
                        selectedPosition = i;
                        break;
                    }
                }
                pendingSelectedBatchId = -1;
            }
            adapter.setSelectedBatchId(selectedBatchId);
            if (selectedPosition >= 0) recyclerView.scrollToPosition(selectedPosition);
            if (selectionListener != null) selectionListener.onBatchSelected(selectedBatchId);
        }
        recyclerView.setVisibility(View.VISIBLE);
        updateSummaryStats();
    }

    private void updateSummaryStats() {
        if (filteredBatches.isEmpty()) {
            avgMoistureValue.setText("0.0");
            avgDefectiveValue.setText("0.0");
            avgQualityScoreValue.setText("0");
            avgKernelValue.setText("0.0");
            avgNutCountValue.setText("0");
            return;
        }

        double totalMoisture = 0;
        double totalDefective = 0;
        double totalScore = 0;
        double totalKernel = 0;
        int totalNutCount = 0;
        int count = filteredBatches.size();

        for (QualityBatchEntity batch : filteredBatches) {
            totalMoisture += batch.getMoistureContent();
            totalDefective += batch.getDefectiveRate();
            totalScore += batch.getQualityScore();
            double kernelPercent = batch.isRcn()
                    ? batch.getKorAsPercent()
                    : batch.getKernelRatio();
            totalKernel += kernelPercent;
            totalNutCount += batch.getNutCountPerKg();
        }

        double avgMoisture = totalMoisture / count;
        double avgDefective = totalDefective / count;
        double avgScore = totalScore / count;
        double avgKernel = totalKernel / count;
        double avgNutCount = (double) totalNutCount / count;

        avgMoistureValue.setText(String.format(Locale.getDefault(), "%.1f", avgMoisture));
        avgDefectiveValue.setText(String.format(Locale.getDefault(), "%.1f", avgDefective));
        avgKernelValue.setText(String.format(Locale.getDefault(), "%.1f", avgKernel));
        avgNutCountValue.setText(String.format(Locale.getDefault(), "%.0f", avgNutCount));
        avgQualityScoreValue.setText(String.format(Locale.getDefault(), "%.0f", avgScore));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
        selectionListener = null;
    }
}
