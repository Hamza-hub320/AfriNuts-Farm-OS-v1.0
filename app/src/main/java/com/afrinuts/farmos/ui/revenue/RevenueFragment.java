package com.afrinuts.farmos.ui.revenue;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevenueFragment extends Fragment {

    private RecyclerView recyclerView;
    private RevenueAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView totalRevenueValue;
    private TextView totalHarvestValue;
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private RevenueRepository revenueRepository;
    private List<RevenueWithBlockName> allRevenues = new ArrayList<>();
    private List<RevenueWithBlockName> filteredRevenues = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String currentFilter = "ALL";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_revenue, container, false);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        loadFarmData();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyLayout = view.findViewById(R.id.emptyView);
        totalRevenueValue = view.findViewById(R.id.totalRevenueValue);
        totalHarvestValue = view.findViewById(R.id.totalHarvestValue);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        setupFilters();
    }

    private void setupFilters() {
        Chip chipAll = filterChipGroup.findViewById(R.id.chipAll);
        Chip chipBlocks = filterChipGroup.findViewById(R.id.chipBlocks);
        Chip chipProcessing = filterChipGroup.findViewById(R.id.chipProcessing);
        Chip chipPremium = filterChipGroup.findViewById(R.id.chipPremium);
        Chip chipStandard = filterChipGroup.findViewById(R.id.chipStandard);

        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "ALL";
                    applyFilter();
                }
            });
        }

        if (chipBlocks != null) {
            chipBlocks.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "BLOCKS";
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
    }

    private void loadFarmData() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                revenueRepository = new RevenueRepository(requireContext(), currentFarm.getId());

                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }

                requireActivity().runOnUiThread(() -> {
                    blockNameMap.clear();
                    blockNameMap.putAll(blockMap);

                    revenueRepository.getAllRevenues().observe(getViewLifecycleOwner(), new Observer<List<RevenueEntity>>() {
                        @Override
                        public void onChanged(List<RevenueEntity> revenues) {
                            processRevenues(revenues);
                        }
                    });
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                });
            }
        });
    }

    private void processRevenues(List<RevenueEntity> revenues) {
        allRevenues.clear();
        for (RevenueEntity revenue : revenues) {
            String blockName = null;
            if (revenue.getBlockId() != null) {
                blockName = blockNameMap.get(revenue.getBlockId());
            }
            allRevenues.add(new RevenueWithBlockName(revenue, blockName));
        }

        showLoading(false);

        if (allRevenues.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            applyFilter();
            updateSummary();
        }
    }

    private void applyFilter() {
        filteredRevenues.clear();

        for (RevenueWithBlockName item : allRevenues) {
            RevenueEntity revenue = item.getRevenue();

            switch (currentFilter) {
                case "ALL":
                    filteredRevenues.add(item);
                    break;
                case "BLOCKS":
                    if (item.isFromBlock()) {
                        filteredRevenues.add(item);
                    }
                    break;
                case "PROCESSING":
                    if (item.isFromProcessingCenter()) {
                        filteredRevenues.add(item);
                    }
                    break;
                case "PREMIUM":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.PREMIUM) {
                        filteredRevenues.add(item);
                    }
                    break;
                case "STANDARD":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.STANDARD) {
                        filteredRevenues.add(item);
                    }
                    break;
            }
        }

        adapter = new RevenueAdapter(filteredRevenues, revenue -> {
            Intent intent = new Intent(getContext(), RevenueDetailActivity.class);
            intent.putExtra(RevenueDetailActivity.EXTRA_REVENUE_ID, revenue.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        updateSummary();
    }

    private void updateSummary() {
        double totalRevenue = 0;
        double totalKg = 0;

        for (RevenueWithBlockName item : filteredRevenues) {
            RevenueEntity revenue = item.getRevenue();
            totalRevenue += revenue.getTotalAmount();
            totalKg += revenue.getQuantityKg();
        }

        totalRevenueValue.setText(String.format(Locale.getDefault(),
                "%,.0f XAF", totalRevenue));
        totalHarvestValue.setText(String.format(Locale.getDefault(),
                "%.1f kg", totalKg));
    }

    public void showAddRevenueDialog() {
        if (currentFarm != null) {
            AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
            dialog.setOnRevenueAddedListener(() -> {
                loadFarmData();
            });
            dialog.show(getParentFragmentManager(), "AddRevenueDialog");
        } else {
            Toast.makeText(getContext(), "Farm not configured", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
        });
    }

    private void showEmpty(boolean show) {
        if (emptyLayout != null) {
            emptyLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (revenueRepository != null) {
            revenueRepository.shutdown();
        }
        executor.shutdown();
    }
}