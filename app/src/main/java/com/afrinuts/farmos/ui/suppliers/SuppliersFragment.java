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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SuppliersFragment extends Fragment {

    private RecyclerView recyclerView;
    private SupplierAdapter adapter;  // ✅ Now this will work
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
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<SupplierEntity> allSuppliers = new ArrayList<>();
    private List<SupplierEntity> filteredSuppliers = new ArrayList<>();

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
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SupplierAdapter(filteredSuppliers, supplier -> {
            // Open SupplierDetailActivity
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
            if (isChecked) {
                currentFilter = "ALL";
                applyFilter();
            }
        });

        chipActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ACTIVE";
                applyFilter();
            }
        });

        chipCertified.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "CERTIFIED";
                applyFilter();
            }
        });

        chipCooperative.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "COOPERATIVE";
                applyFilter();
            }
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
                    applyFilter();
                }
            }
        });
    }

    private void applyFilter() {
        filteredSuppliers.clear();

        for (SupplierEntity supplier : allSuppliers) {
            switch (currentFilter) {
                case "ALL":
                    filteredSuppliers.add(supplier);
                    break;
                case "ACTIVE":
                    if (supplier.isActive()) {
                        filteredSuppliers.add(supplier);
                    }
                    break;
                case "CERTIFIED":
                    if (supplier.isCertified()) {
                        filteredSuppliers.add(supplier);
                    }
                    break;
                case "COOPERATIVE":
                    if (supplier.getType() == SupplierEntity.SupplierType.COOPERATIVE) {
                        filteredSuppliers.add(supplier);
                    }
                    break;
            }
        }

        adapter.updateSuppliers(filteredSuppliers);
        recyclerView.setVisibility(View.VISIBLE);
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