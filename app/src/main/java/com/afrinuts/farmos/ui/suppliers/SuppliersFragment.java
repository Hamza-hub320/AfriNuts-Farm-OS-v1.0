package com.afrinuts.farmos.ui.suppliers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SuppliersFragment extends Fragment {

    private RecyclerView recyclerView;
    private SupplierAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvTotalSuppliers;
    private TextView tvActiveSuppliers;
    private TextView tvCertifiedSuppliers;
    private TextView tvAvgRating;
    private ChipGroup filterChipGroup;

    private SupplierRepository repository;
    private AppDatabase database;
    private FarmEntity currentFarm;

    // 3.3: search state
    private TextInputEditText etSearch;
    private String searchQuery = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Three stages: Room → chip filter → search filter
    private final List<SupplierEntity> allSuppliers = new ArrayList<>();
    private final List<SupplierEntity> chipFilteredSuppliers = new ArrayList<>();
    private final List<SupplierEntity> filteredSuppliers = new ArrayList<>();

    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suppliers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = AppDatabase.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupFilters();
        setupFab();
        loadFarmAndData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvTotalSuppliers = view.findViewById(R.id.tvTotalSuppliers);
        tvActiveSuppliers = view.findViewById(R.id.tvActiveSuppliers);
        tvCertifiedSuppliers = view.findViewById(R.id.tvCertifiedSuppliers);
        tvAvgRating = view.findViewById(R.id.tvAvgRating);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        // 3.3: search box. Filter runs on the in-memory Room list —
        // not a server query. Never becomes one.
        etSearch = view.findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void afterTextChanged(android.text.Editable s) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                    applyCombinedFilter();
                }
            });
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SupplierAdapter(filteredSuppliers, supplier -> {
            Intent intent = new Intent(getActivity(), SupplierDetailActivity.class);
            intent.putExtra(SupplierDetailActivity.EXTRA_SUPPLIER_ID, supplier.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        Chip chipAll = getView().findViewById(R.id.chipAll);
        Chip chipActive = getView().findViewById(R.id.chipActive);
        Chip chipCertified = getView().findViewById(R.id.chipCertified);
        Chip chipCooperative = getView().findViewById(R.id.chipCooperative);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { currentFilter = "ALL"; applyCombinedFilter(); }
        });
        chipActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { currentFilter = "ACTIVE"; applyCombinedFilter(); }
        });
        chipCertified.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { currentFilter = "CERTIFIED"; applyCombinedFilter(); }
        });
        chipCooperative.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { currentFilter = "COOPERATIVE"; applyCombinedFilter(); }
        });
    }

    private void setupFab() {
        FloatingActionButton fab = getView().findViewById(R.id.fabAdd);
        if (fab != null) {
            fab.setImageResource(R.drawable.ic_add);
            fab.setOnClickListener(v -> showAddSupplierDialog());
        }
    }

    public void showAddSupplierDialog() {
        if (currentFarm == null) {
            Toast.makeText(getContext(), "Farm not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        AddSupplierDialog dialog = AddSupplierDialog.newInstance(currentFarm.getId());
        dialog.setOnSupplierAddedListener(() -> {
            loadFarmAndData();
            Toast.makeText(getContext(), "Supplier added!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getChildFragmentManager(), "AddSupplierDialog");
    }

    private void loadFarmAndData() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        repository = new SupplierRepository(getContext(), currentFarm.getId());
                        loadSuppliers();
                    });
                }
            } else {
                showLoading(false);
                showEmpty(true);
            }
        });
    }

    private void loadSuppliers() {
        if (repository == null) return;

        repository.getAllSuppliers().observe(getViewLifecycleOwner(), new Observer<List<SupplierEntity>>() {
            @Override
            public void onChanged(List<SupplierEntity> suppliers) {
                allSuppliers.clear();
                if (suppliers != null) {
                    allSuppliers.addAll(suppliers);
                }

                showLoading(false);

                if (allSuppliers.isEmpty()) {
                    showEmpty(true);
                } else {
                    showEmpty(false);
                    updateSummaryStats();
                    applyCombinedFilter();
                }
            }
        });
    }

    /**
     * 3.3: chip filter first, then search narrows. Clearing the search
     * restores the chip-filtered set — it does not disable the chips.
     * Fully offline.
     */
    private void applyCombinedFilter() {
        // Stage 1 — chip filter
        chipFilteredSuppliers.clear();
        for (SupplierEntity s : allSuppliers) {
            if (matchesCurrentChipFilter(s)) {
                chipFilteredSuppliers.add(s);
            }
        }

        // Stage 2 — search narrows the chip-filtered set
        filteredSuppliers.clear();
        if (searchQuery.isEmpty()) {
            filteredSuppliers.addAll(chipFilteredSuppliers);
        } else {
            for (SupplierEntity s : chipFilteredSuppliers) {
                if (matchesSearch(s, searchQuery)) {
                    filteredSuppliers.add(s);
                }
            }
        }

        adapter.updateSuppliers(filteredSuppliers);
        recyclerView.setVisibility(View.VISIBLE);

        // Empty state reflects the *filtered* result, not just the raw list.
        // If a search returns zero matches while allSuppliers is non-empty,
        // we want the empty view, not a blank list with no explanation.
        if (filteredSuppliers.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
        }
    }

    /** The existing chip predicate, extracted so the composition is explicit. */
    private boolean matchesCurrentChipFilter(SupplierEntity s) {
        switch (currentFilter) {
            case "ALL":
                return true;
            case "ACTIVE":
                return s.isActive();
            case "CERTIFIED":
                return s.isCertified();
            case "COOPERATIVE":
                return s.getType() == SupplierEntity.SupplierType.COOPERATIVE;
            default:
                return true;
        }
    }

    /**
     * 3.3: case-insensitive contains on name + location.
     * Null-safe: null name / location just doesn't match, no NPE.
     */
    private boolean matchesSearch(SupplierEntity s, String query) {
        if (query == null || query.isEmpty()) return true;
        String name = s.getName() == null ? "" : s.getName().toLowerCase(Locale.ROOT);
        String location = s.getLocation() == null ? "" : s.getLocation().toLowerCase(Locale.ROOT);
        return name.contains(query) || location.contains(query);
    }

    private void updateSummaryStats() {
        int total = allSuppliers.size();
        int active = 0;
        int certified = 0;
        double totalRating = 0;

        for (SupplierEntity supplier : allSuppliers) {
            if (supplier.isActive()) active++;
            if (supplier.isCertified()) certified++;
            totalRating += supplier.getRating();
        }

        double avgRating = total > 0 ? totalRating / total : 0;

        tvTotalSuppliers.setText(String.valueOf(total));
        tvActiveSuppliers.setText(String.valueOf(active));
        tvCertifiedSuppliers.setText(String.valueOf(certified));
        tvAvgRating.setText(String.format(Locale.getDefault(), "%.1f ★", avgRating));
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
    public void onDestroy() {
        super.onDestroy();
        if (repository != null) {
            repository.shutdown();
        }
        executor.shutdown();
    }
}