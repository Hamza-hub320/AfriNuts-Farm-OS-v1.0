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

    public interface OnExpenseSelectedListener { void onExpenseSelected(long expenseId); }
    private OnExpenseSelectedListener selectionListener;
    private boolean tabletLayout;
    private long selectedExpenseId = -1;

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnExpenseSelectedListener) selectionListener = (OnExpenseSelectedListener) context;
    }

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView totalExpensesText;
    private TextView expensesRecordedText;
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
        expensesRecordedText = view.findViewById(R.id.expensesRecordedText);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        chipAll = view.findViewById(R.id.chipAll);
        chipFarmWide = view.findViewById(R.id.chipFarmWide);
        chipByBlock = view.findViewById(R.id.chipByBlock);
        chipProcurement = view.findViewById(R.id.chipProcurement);

        tabletLayout = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        if (tabletLayout) {
            view.setBackgroundResource(R.drawable.bg_finance_tablet_panel);
            view.setClipToOutline(true);
            view.setPadding(dp(10), dp(10), dp(10), dp(10));
            View summary = view.findViewById(R.id.expensesSummaryRow);
            if (summary != null) summary.setVisibility(View.GONE);
            View chipScroller = (View) filterChipGroup.getParent();
            if (chipScroller != null) chipScroller.setVisibility(View.GONE);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (tabletLayout) {
            recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override public void onDraw(@NonNull android.graphics.Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                    paint.setColor(0xFFE9E1CF);
                    float density = getResources().getDisplayMetrics().density;
                    for (int i = 0; i < parent.getChildCount(); i++) {
                        View child = parent.getChildAt(i);
                        int position = parent.getChildAdapterPosition(child);
                        if (position >= 0 && position < parent.getAdapter().getItemCount() - 1) {
                            canvas.drawRect(child.getLeft() + 8 * density, child.getBottom() - density,
                                    child.getRight() - 8 * density, child.getBottom(), paint);
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
        if (!tabletLayout || filteredExpenses.isEmpty()) return;
        ExpenseEntity selected = null;
        for (ExpenseWithBlockName item : filteredExpenses) {
            if (item.getExpense().getId() == selectedExpenseId) { selected = item.getExpense(); break; }
        }
        selectExpense(selected == null ? filteredExpenses.get(0).getExpense() : selected);
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
                    totalExpensesText.setText("0");
                    expensesRecordedText.setText("0");
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

            // Keep the list audit-ready: a referenced block or procurement source must resolve to a name.
            if (expense.isProcurementRelated()
                    && (isBlank(batchNumber) && isBlank(supplierName))) continue;
            if (!expense.isProcurementRelated() && expense.getBlockId() != null && isBlank(blockName)) continue;

            allExpenses.add(new ExpenseWithBlockName(expense, blockName));
        }

        showLoading(false);

        if (allExpenses.isEmpty()) {
            showEmpty(true);
            totalExpensesText.setText("0");
            expensesRecordedText.setText("0");
        } else {
            showEmpty(false);
            applyFilter();
            if (tabletLayout && !filteredExpenses.isEmpty()) selectExpense(filteredExpenses.get(0).getExpense());
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
            if (tabletLayout && selectionListener != null) selectExpense(expense);
            else {
                Intent intent = new Intent(getContext(), ExpenseDetailActivity.class);
                intent.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, expense.getId());
                startActivity(intent);
            }
        });
        adapter.setSelectedExpenseId(selectedExpenseId);

        recyclerView.setAdapter(adapter);
        updateTotal();
    }

    private void selectExpense(ExpenseEntity expense) {
        if (expense == null) return;
        selectedExpenseId = expense.getId();
        if (adapter != null) adapter.setSelectedExpenseId(selectedExpenseId);
        if (selectionListener != null) selectionListener.onExpenseSelected(selectedExpenseId);
    }

    private void updateTotal() {
        double total = 0;
        for (ExpenseWithBlockName item : filteredExpenses) {
            total += item.getExpense().getAmount();
        }
        totalExpensesText.setText(String.format(Locale.getDefault(), "%,d", (int) total));
        expensesRecordedText.setText(String.valueOf(filteredExpenses.size()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
