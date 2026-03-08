package com.afrinuts.farmos.ui.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.MainActivity;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpensesListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyLayout;
    private TextView totalExpensesText;
    private ChipGroup filterChipGroup;
    private Chip chipAll;
    private Chip chipFarmWide;
    private Chip chipByBlock;

    private AppDatabase database;
    private FarmEntity currentFarm;
    private List<ExpenseWithBlockName> allExpenses = new ArrayList<>();
    private List<ExpenseWithBlockName> filteredExpenses = new ArrayList<>();
    private Map<Long, String> blockNameMap = new HashMap<>();

    private String currentFilter = "ALL"; // ALL, FARM_WIDE, BLOCK_SPECIFIC

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_list);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Load data
        loadFarmAndBlocks();

        // Setup filter listeners
        setupFilters();

        // Setup FAB to add new expense
        FloatingActionButton fabAdd = findViewById(R.id.fabAddExpense);
        fabAdd.setOnClickListener(v -> {
            if (currentFarm != null) {
                AddExpenseDialog dialog = AddExpenseDialog.newInstance(currentFarm.getId());
                dialog.setOnExpenseAddedListener(() -> {
                    loadExpenses(); // Refresh list
                });
                dialog.show(getSupportFragmentManager(), "AddExpenseDialog");
            }
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyLayout = findViewById(R.id.emptyView);
        totalExpensesText = findViewById(R.id.totalExpensesText);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        chipAll = findViewById(R.id.chipAll);
        chipFarmWide = findViewById(R.id.chipFarmWide);
        chipByBlock = findViewById(R.id.chipByBlock);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupFab() {
        FloatingActionButton fabAdd = findViewById(R.id.fabAddExpense);
        fabAdd.setOnClickListener(v -> {
            if (currentFarm != null) {
                AddExpenseDialog dialog = AddExpenseDialog.newInstance(currentFarm.getId());
                dialog.setOnExpenseAddedListener(() -> {
                    loadExpenses(); // Refresh list
                });
                dialog.show(getSupportFragmentManager(), "AddExpenseDialog");
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_home);
            getSupportActionBar().setTitle("Farm Expenses");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            // Navigate to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_sort_date) {
            sortExpensesByDate();
            return true;
        } else if (itemId == R.id.action_sort_amount) {
            sortExpensesByAmount();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupFilters() {
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        chipFarmWide.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "FARM_WIDE";
                applyFilter();
            }
        });

        chipByBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "BLOCK_SPECIFIC";
                applyFilter();
            }
        });
    }

    private void loadFarmAndBlocks() {
        new Thread(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                // Load all blocks for name mapping
                List<BlockEntity> blocks = database.blockDao().getBlocksByFarmId(currentFarm.getId());
                for (BlockEntity block : blocks) {
                    blockNameMap.put(block.getId(), block.getBlockName());
                }

                // Load expenses
                loadExpenses();
            }
        }).start();
    }

    private void loadExpenses() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
        });

        new Thread(() -> {
            if (currentFarm != null) {
                List<ExpenseEntity> expenses = database.expenseDao().getExpensesByFarmId(currentFarm.getId());

                allExpenses.clear();
                for (ExpenseEntity expense : expenses) {
                    String blockName = null;
                    if (expense.getBlockId() != null) {
                        blockName = blockNameMap.get(expense.getBlockId());
                    }
                    allExpenses.add(new ExpenseWithBlockName(expense, blockName));
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    if (allExpenses.isEmpty()) {
                        emptyLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        totalExpensesText.setText("Total: 0 XAF");
                    } else {
                        emptyLayout.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        // Apply current filter
                        applyFilter();

                        // Update total
                        updateTotal();
                    }
                });
            }
        }).start();
    }

    private void applyFilter() {
        filteredExpenses.clear();

        for (ExpenseWithBlockName item : allExpenses) {
            switch (currentFilter) {
                case "ALL":
                    filteredExpenses.add(item);
                    break;
                case "FARM_WIDE":
                    if (item.isFarmWide()) {
                        filteredExpenses.add(item);
                    }
                    break;
                case "BLOCK_SPECIFIC":
                    if (!item.isFarmWide()) {
                        filteredExpenses.add(item);
                    }
                    break;
            }
        }

        // Update adapter
        adapter = new ExpenseAdapter(filteredExpenses, expense -> {
            // TODO: Open ExpenseDetailActivity
            // For now, show toast
            android.widget.Toast.makeText(this,
                    "Edit expense coming soon",
                    android.widget.Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(adapter);

        // Update total for filtered view
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_expenses, menu);
        return true;
    }

    private void sortExpensesByDate() {
        filteredExpenses.sort((e1, e2) ->
                Long.compare(e2.getExpense().getDate(), e1.getExpense().getDate()));
        adapter.notifyDataSetChanged();
    }

    private void sortExpensesByAmount() {
        filteredExpenses.sort((e1, e2) ->
                Double.compare(e2.getExpense().getAmount(), e1.getExpense().getAmount()));
        adapter.notifyDataSetChanged();
    }
}