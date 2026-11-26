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
import com.afrinuts.farmos.data.local.entity.SupplierScoreSnapshotEntity;
import com.afrinuts.farmos.data.repository.SupplierRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SuppliersFragment extends Fragment {

    public interface SupplierSelectionListener {
        boolean onSupplierSelected(SupplierEntity supplier);
        void onSupplierDeleted(long supplierId);
    }

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
    private SupplierSelectionListener selectionListener;
    private boolean tablet;
    private long selectedSupplierId = -1;
    private long requestedSupplierId = -1;

    @Override public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof SupplierSelectionListener) selectionListener = (SupplierSelectionListener) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_suppliers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (selectionListener == null && requireActivity() instanceof SupplierSelectionListener) {
            selectionListener = (SupplierSelectionListener) requireActivity();
        }

        database = AppDatabase.getInstance(requireContext());
        tablet = requireActivity().findViewById(R.id.detailContainer) != null;
        if (tablet) {
            view.setPadding(view.getPaddingLeft(), 0, view.getPaddingRight(), view.getPaddingBottom());
            view.setBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.background));
        }

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
        if (tablet) {
            View searchCard = view.findViewById(R.id.layoutSearch);
            if (searchCard != null) searchCard.setVisibility(View.GONE);
            View summaryRow = tvTotalSuppliers.getParent() == null ? null : (View) tvTotalSuppliers.getParent().getParent();
            if (summaryRow != null) summaryRow.setVisibility(View.GONE);
            View filterScroller = (View) filterChipGroup.getParent();
            if (filterScroller != null) filterScroller.setVisibility(View.GONE);
            recyclerView.setBackground(makeTabletListCard());
            recyclerView.setClipToOutline(true);
            recyclerView.setPadding(dp(12), dp(12), dp(12), dp(104));
        }

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
            if (tablet) {
                selectedSupplierId = supplier.getId();
                adapter.setSelectedSupplierId(selectedSupplierId);
                showTabletSupplierDetail(supplier);
                return;
            }
            Intent intent = new Intent(getActivity(), SupplierDetailActivity.class);
            intent.putExtra(SupplierDetailActivity.EXTRA_SUPPLIER_ID, supplier.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
        if (tablet) recyclerView.addItemDecoration(new androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(), androidx.recyclerview.widget.DividerItemDecoration.VERTICAL));
    }

    private android.graphics.drawable.GradientDrawable makeTabletListCard() {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(android.graphics.Color.WHITE);
        bg.setCornerRadius(dp(28));
        return bg;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void showTabletSupplierDetail(SupplierEntity supplier) {
        if (selectionListener != null && selectionListener.onSupplierSelected(supplier)) return;
        if (isAdded() && requireActivity().findViewById(R.id.detailContainer) != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.detailContainer, SupplierDetailFragment.newInstance(supplier.getId()), "tablet-supplier-detail")
                    .commit();
        }
    }

    public void showSelectedTabletDetail() {
        if (!tablet || filteredSuppliers.isEmpty() || selectionListener == null) return;
        if (requestedSupplierId >= 0) {
            openSupplierById(requestedSupplierId);
            return;
        }
        SupplierEntity first = filteredSuppliers.get(0);
        selectedSupplierId = first.getId();
        if (adapter != null) adapter.setSelectedSupplierId(selectedSupplierId);
        showTabletSupplierDetail(first);
    }

    public void openSupplierById(long supplierId) {
        if (supplierId < 0) return;
        requestedSupplierId = supplierId;
        if (!tablet || filteredSuppliers.isEmpty()) return;
        for (SupplierEntity supplier : filteredSuppliers) {
            if (supplier.getId() == supplierId) {
                requestedSupplierId = -1;
                selectedSupplierId = supplierId;
                if (adapter != null) adapter.setSelectedSupplierId(supplierId);
                showTabletSupplierDetail(supplier);
                return;
            }
        }
    }

    private void setupFilters() {
        Chip chipAll = getView().findViewById(R.id.chipAll);
        Chip chipActive = getView().findViewById(R.id.chipActive);
        Chip chipAtRisk = getView().findViewById(R.id.chipCertified);

        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { currentFilter = "ALL"; applyCombinedFilter(); }
        });
        chipActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { currentFilter = "ACTIVE"; applyCombinedFilter(); }
        });
        chipAtRisk.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { currentFilter = "AT_RISK"; applyCombinedFilter(); }
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
        if (!isAdded() || getContext() == null || getView() == null) return;
        if (currentFarm == null) {
            Toast.makeText(requireContext(), "Farm is still loading. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        AddSupplierDialog dialog = AddSupplierDialog.newInstance(currentFarm.getId());
        dialog.setOnSupplierAddedListener(() -> {
            loadFarmAndData();
            if (isAdded() && getContext() != null) {
                Toast.makeText(requireContext(), "Supplier added!", Toast.LENGTH_SHORT).show();
            }
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
                    loadTrustEngineData(new ArrayList<>(allSuppliers));
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
        if (tablet && !filteredSuppliers.isEmpty()) {
            if (requestedSupplierId >= 0) {
                openSupplierById(requestedSupplierId);
            }
            if (selectedSupplierId < 0 || filteredSuppliers.stream().noneMatch(s -> s.getId() == selectedSupplierId)) {
                SupplierEntity preferred = filteredSuppliers.get(0);
                for (SupplierEntity candidate : filteredSuppliers) {
                    if (candidate.getName() != null && candidate.getName().startsWith("Coopérative Espoir d’Odienné")) {
                        preferred = candidate;
                        break;
                    }
                }
                selectedSupplierId = preferred.getId();
                showTabletSupplierDetail(preferred);
            }
            adapter.setSelectedSupplierId(selectedSupplierId);
        }
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
                return s.getStatus() == SupplierEntity.SupplierStatus.ACTIVE;
            case "AT_RISK":
                Double score = trustScores.get(s.getId());
                return s.getStatus() == SupplierEntity.SupplierStatus.SUSPENDED
                        || s.getStatus() == SupplierEntity.SupplierStatus.INACTIVE
                        || (score != null && score < 40);
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

    private final Map<Long, Double> trustScores = new HashMap<>();

    private void loadTrustEngineData(List<SupplierEntity> suppliers) {
        executor.execute(() -> {
            Map<Long, Double> scores = new HashMap<>();
            Map<Long, Integer> batches = new HashMap<>();
            double sum = 0;
            int scored = 0;
            for (SupplierEntity supplier : suppliers) {
                SupplierScoreSnapshotEntity snapshot = database.supplierScoreSnapshotDao().getLatestBySupplierId(supplier.getId());
                if (snapshot != null && snapshot.getOverall() != null) {
                    scores.put(supplier.getId(), snapshot.getOverall());
                    sum += snapshot.getOverall();
                    scored++;
                }
                batches.put(supplier.getId(), database.qualityBatchDao().getBatchCountBySupplierId(supplier.getId()));
            }
            final double average = scored == 0 ? Double.NaN : sum / scored;
            requireActivity().runOnUiThread(() -> {
                trustScores.clear();
                trustScores.putAll(scores);
                adapter.setTrustData(scores, batches);
                tvTotalSuppliers.setText(String.valueOf(allSuppliers.size()));
                tvActiveSuppliers.setText(String.valueOf(allSuppliers.stream()
                        .filter(supplier -> supplier.getStatus() == SupplierEntity.SupplierStatus.ACTIVE)
                        .count()));
                tvCertifiedSuppliers.setText("0");
                tvAvgRating.setText(Double.isNaN(average) ? "— / 100" : String.format(Locale.getDefault(), "%.0f / 100", average));
                applyCombinedFilter();
            });
        });
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
