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
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.data.repository.SupplierRepository;
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

    public interface OnRevenueSelectedListener { void onRevenueSelected(long revenueId); }
    private OnRevenueSelectedListener selectionListener;
    private boolean tabletLayout;
    private long selectedRevenueId = -1;

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnRevenueSelectedListener) selectionListener = (OnRevenueSelectedListener) context;
    }

    private RecyclerView recyclerView;
    private RevenueAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView totalRevenueValue;
    private TextView totalQuantityValue;  // Changed from totalHarvestValue
    private ChipGroup filterChipGroup;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private RevenueRepository revenueRepository;
    private QualityBatchRepository batchRepository;
    private SupplierRepository supplierRepository;
    private List<RevenueEntity> allRevenues = new ArrayList<>();
    private List<RevenueEntity> filteredRevenues = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private Map<Long, String> batchNumberMap = new HashMap<>();
    private Map<Long, String> supplierNameMap = new HashMap<>();
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
        totalQuantityValue = view.findViewById(R.id.totalQuantityValue);  // Updated ID
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        tabletLayout = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        if (tabletLayout) {
            view.setBackgroundResource(R.drawable.bg_finance_tablet_panel);
            view.setClipToOutline(true);
            view.setPadding(dp(10), dp(10), dp(10), dp(10));
            View summary = view.findViewById(R.id.revenueSummaryRow);
            if (summary != null) summary.setVisibility(View.GONE);
            View chipScroller = (View) filterChipGroup.getParent();
            if (chipScroller != null) chipScroller.setVisibility(View.GONE);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (tabletLayout) {
            final android.graphics.Paint dividerPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            dividerPaint.setColor(0xFFE9E1CF);
            dividerPaint.setStrokeWidth(Math.max(1, dp(1)));
            recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override public void onDraw(@NonNull android.graphics.Canvas canvas,
                                             @NonNull RecyclerView parent,
                                             @NonNull RecyclerView.State state) {
                    int left = parent.getPaddingLeft() + dp(8);
                    int right = parent.getWidth() - parent.getPaddingRight() - dp(8);
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        View child = parent.getChildAt(i);
                        if (parent.getChildAdapterPosition(child) < state.getItemCount() - 1) {
                            float y = child.getBottom() + dp(2);
                            canvas.drawLine(left, y, right, y, dividerPaint);
                        }
                    }
                }
            });
        }
        setupFilters();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onResume() {
        super.onResume();
        if (!tabletLayout || allRevenues.isEmpty()) return;
        RevenueEntity selected = null;
        for (RevenueEntity item : filteredRevenues) {
            if (item.getId() == selectedRevenueId) { selected = item; break; }
        }
        selectRevenue(selected == null ? (filteredRevenues.isEmpty() ? allRevenues.get(0) : filteredRevenues.get(0)) : selected);
    }

    private void setupFilters() {
        Chip chipAll = filterChipGroup.findViewById(R.id.chipAll);
        Chip chipPremium = filterChipGroup.findViewById(R.id.chipPremium);
        Chip chipStandard = filterChipGroup.findViewById(R.id.chipStandard);
        Chip chipProcessing = filterChipGroup.findViewById(R.id.chipProcessing);

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
                revenueRepository = new RevenueRepository(requireContext(), currentFarm.getId());
                batchRepository = new QualityBatchRepository(requireContext(), currentFarm.getId());
                supplierRepository = new SupplierRepository(requireContext(), currentFarm.getId());

                // Load block names
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                for (BlockEntity block : blocks) {
                    blockNameMap.put(block.getId(), block.getBlockName());
                }

                // Load batch numbers
                List<QualityBatchEntity> batches = batchRepository.getAllBatchesSync();
                for (QualityBatchEntity batch : batches) {
                    batchNumberMap.put(batch.getId(), batch.getBatchNumber());
                }
                for (com.afrinuts.farmos.data.local.entity.SupplierEntity supplier : supplierRepository.getSuppliersSync())
                    supplierNameMap.put(supplier.getId(), supplier.getName());

                requireActivity().runOnUiThread(() -> {
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
        allRevenues.addAll(revenues);

        // Enrich with batch numbers if missing
        for (RevenueEntity revenue : allRevenues) {
            if (revenue.getBatchId() != null &&
                    (revenue.getBatchNumber() == null || revenue.getBatchNumber().isEmpty())) {
                String batchNumber = batchNumberMap.get(revenue.getBatchId());
                if (batchNumber != null) {
                    revenue.setBatchNumber(batchNumber);
                }
            }
            if (revenue.getSupplierId() != null &&
                    (revenue.getSupplierName() == null || revenue.getSupplierName().trim().isEmpty())) {
                String supplierName = supplierNameMap.get(revenue.getSupplierId());
                if (supplierName != null) revenue.setSupplierName(supplierName);
            }
        }

        showLoading(false);

        if (allRevenues.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            applyFilter();
            updateSummary();
            if (tabletLayout) selectRevenue(filteredRevenues.isEmpty() ? allRevenues.get(0) : filteredRevenues.get(0));
        }
    }

    private void applyFilter() {
        filteredRevenues.clear();

        for (RevenueEntity revenue : allRevenues) {
            switch (currentFilter) {
                case "ALL":
                    filteredRevenues.add(revenue);
                    break;
                case "PREMIUM":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.PREMIUM) {
                        filteredRevenues.add(revenue);
                    }
                    break;
                case "STANDARD":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.STANDARD) {
                        filteredRevenues.add(revenue);
                    }
                    break;
                case "PROCESSING":
                    if (revenue.getQuality() == RevenueEntity.QualityGrade.PROCESSING) {
                        filteredRevenues.add(revenue);
                    }
                    break;
            }
        }

        // ✅ FIXED: Pass filteredRevenues directly and handle click in adapter
        adapter = new RevenueAdapter();
        adapter.setOnRevenueClickListener(new RevenueAdapter.OnRevenueClickListener() {
            @Override
            public void onRevenueClick(RevenueEntity revenue) {
                if (tabletLayout && selectionListener != null) {
                    selectRevenue(revenue);
                } else {
                    Intent intent = new Intent(getContext(), RevenueDetailActivity.class);
                    intent.putExtra(RevenueDetailActivity.EXTRA_REVENUE_ID, revenue.getId());
                    startActivity(intent);
                }
            }

            @Override
            public void onRevenueLongClick(RevenueEntity revenue) {
                // Handle long click if needed
            }
        });
        adapter.setRevenues(filteredRevenues);
        adapter.setSelectedRevenueId(selectedRevenueId);
        recyclerView.setAdapter(adapter);

        updateSummary();
    }

    private void selectRevenue(RevenueEntity revenue) {
        if (revenue == null) return;
        selectedRevenueId = revenue.getId();
        if (adapter != null) adapter.setSelectedRevenueId(selectedRevenueId);
        if (selectionListener != null) selectionListener.onRevenueSelected(selectedRevenueId);
    }

    private void updateSummary() {
        double totalRevenue = 0;
        double totalKg = 0;

        for (RevenueEntity revenue : filteredRevenues) {
            totalRevenue += revenue.getTotalAmount();
            totalKg += revenue.getQuantityKg();
        }

        totalRevenueValue.setText(String.format(Locale.getDefault(),
                "%,.0f", totalRevenue));
        totalQuantityValue.setText(String.format(Locale.getDefault(),
                "%,.1f kg", totalKg));
    }

    public void showAddRevenueDialog() {
        if (currentFarm != null) {
            AddRevenueDialog dialog = AddRevenueDialog.newInstance(currentFarm.getId());
            dialog.setOnRevenueAddedListener(() -> {
                // Refresh data after adding revenue
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
        if (batchRepository != null) {
            batchRepository.shutdown();
        }
        if (supplierRepository != null) supplierRepository.shutdown();
        executor.shutdown();
    }
}
