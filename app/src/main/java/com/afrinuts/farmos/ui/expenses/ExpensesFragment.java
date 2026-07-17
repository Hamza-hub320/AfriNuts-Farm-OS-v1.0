package com.afrinuts.farmos.ui.expenses;

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
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.ExpenseRepository;
import com.afrinuts.farmos.data.repository.QualityBatchRepository;
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

public class ExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView totalExpensesText;
    private ChipGroup filterChipGroup;
    private Chip chipAll;
    private Chip chipFarmWide;
    private Chip chipByBlock;
    private Chip chipProcurement;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private ExpenseRepository expenseRepository;
    private QualityBatchRepository batchRepository;
    private SupplierRepository supplierRepository;
    private List<ExpenseWithBlockName> allExpenses = new ArrayList<>();
    private List<ExpenseWithBlockName> filteredExpenses = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();
    private Map<Long, String> batchNumberMap = new HashMap<>();
    private Map<Long, String> supplierNameMap = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String currentFilter = "ALL"; // ALL, FARM_WIDE, BLOCK_SPECIFIC, PROCUREMENT

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        loadFarmData();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyLayout = view.findViewById(R.id.emptyView);
        totalExpensesText = view.findViewById(R.id.totalExpensesText);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        chipAll = view.findViewById(R.id.chipAll);
        chipFarmWide = view.findViewById(R.id.chipFarmWide);
        chipByBlock = view.findViewById(R.id.chipByBlock);
        chipProcurement = view.findViewById(R.id.chipProcurement);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        setupFilters();
    }

    private void setupFilters() {
        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "ALL";
                    applyFilter();
                }
            });
        }

        if (chipFarmWide != null) {
            chipFarmWide.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "FARM_WIDE";
                    applyFilter();
                }
            });
        }

        if (chipByBlock != null) {
            chipByBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "BLOCK_SPECIFIC";
                    applyFilter();
                }
            });
        }

        if (chipProcurement != null) {
            chipProcurement.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    currentFilter = "PROCUREMENT";
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
                long farmId = currentFarm.getId();
                expenseRepository = new ExpenseRepository(requireContext(), farmId);
                batchRepository = new QualityBatchRepository(requireContext(), farmId);
                supplierRepository = new SupplierRepository(requireContext(), farmId);

                // Load blocks for name mapping
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farmId);
                Map<Long, String> blockMap = new HashMap<>();
                for (BlockEntity block : blocks) {
                    blockMap.put(block.getId(), block.getBlockName());
                }
                blockNameMap.clear();
                blockNameMap.putAll(blockMap);

                // Load batches for name mapping
                List<QualityBatchEntity> batches = batchRepository.getAllBatchesSync();
                Map<Long, String> batchMap = new HashMap<>();
                for (QualityBatchEntity batch : batches) {
                    batchMap.put(batch.getId(), batch.getBatchNumber());
                }
                batchNumberMap.clear();
                batchNumberMap.putAll(batchMap);

                // Load suppliers for name mapping
                List<SupplierEntity> suppliers = supplierRepository.getSuppliersSync();
                Map<Long, String> supplierMap = new HashMap<>();
                for (SupplierEntity supplier : suppliers) {
                    supplierMap.put(supplier.getId(), supplier.getName());
                }
                supplierNameMap.clear();
                supplierNameMap.putAll(supplierMap);

                requireActivity().runOnUiThread(() -> {
                    expenseRepository.getAllExpenses().observe(getViewLifecycleOwner(), new Observer<List<ExpenseEntity>>() {
                        @Override
                        public void onChanged(List<ExpenseEntity> expenses) {
                            processExpenses(expenses);
                        }
                    });
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                    totalExpensesText.setText("Total: 0 XAF");
                });
            }
        });
    }

    private void processExpenses(List<ExpenseEntity> expenseList) {
        allExpenses.clear();
        for (ExpenseEntity expense : expenseList) {
            String blockName = null;
            String batchNumber = null;
            String supplierName = null;

            if (expense.getBlockId() != null) {
                blockName = blockNameMap.get(expense.getBlockId());
            }

            // Enrich with batch and supplier info
            if (expense.getBatchId() != null) {
                batchNumber = batchNumberMap.get(expense.getBatchId());
                if (batchNumber != null) {
                    expense.setBatchNumber(batchNumber);
                }
            }

            if (expense.getSupplierId() != null) {
                supplierName = supplierNameMap.get(expense.getSupplierId());
                if (supplierName != null) {
                    expense.setSupplierName(supplierName);
                }
            }

            allExpenses.add(new ExpenseWithBlockName(expense, blockName));
        }

        showLoading(false);

        if (allExpenses.isEmpty()) {
            showEmpty(true);
            totalExpensesText.setText("Total: 0 XAF");
        } else {
            showEmpty(false);
            applyFilter();
        }
    }

    private void applyFilter() {
        filteredExpenses.clear();

        for (ExpenseWithBlockName item : allExpenses) {
            ExpenseEntity expense = item.getExpense();
            boolean isProcurement = expense.isProcurementRelated();

            switch (currentFilter) {
                case "ALL":
                    filteredExpenses.add(item);
                    break;
                case "FARM_WIDE":
                    if (item.isFarmWide() && !isProcurement) {
                        filteredExpenses.add(item);
                    }
                    break;
                case "BLOCK_SPECIFIC":
                    if (!item.isFarmWide() && !isProcurement) {
                        filteredExpenses.add(item);
                    }
                    break;
                case "PROCUREMENT":
                    if (isProcurement) {
                        filteredExpenses.add(item);
                    }
                    break;
            }
        }

        adapter = new ExpenseAdapter(filteredExpenses, expense -> {
            Intent intent = new Intent(getContext(), ExpenseDetailActivity.class);
            intent.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, expense.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        for (ExpenseWithBlockName item : filteredExpenses) {
            total += item.getExpense().getAmount();
        }
        totalExpensesText.setText(String.format(Locale.getDefault(),
                "Total: %,d XAF", (int) total));
    }

    public void showAddExpenseDialog() {
        if (currentFarm != null) {
            AddExpenseDialog dialog = AddExpenseDialog.newInstance(currentFarm.getId());
            dialog.setOnExpenseAddedListener(() -> {
                loadFarmData();
            });
            dialog.show(getParentFragmentManager(), "AddExpenseDialog");
        } else {
            // ✅ FIXED: LENGTH_SHORT (not LENGTH_SHOW)
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
        if (expenseRepository != null) {
            expenseRepository.shutdown();
        }
        if (batchRepository != null) {
            batchRepository.shutdown();
        }
        if (supplierRepository != null) {
            supplierRepository.shutdown();
        }
        executor.shutdown();
    }
}