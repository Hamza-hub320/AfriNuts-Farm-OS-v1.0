package com.afrinuts.farmos.ui.traceability;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.repository.MasterTraceabilityRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.afrinuts.farmos.ui.traceability.MasterTraceabilityDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MasterTraceabilityFragment extends Fragment {

    private RecyclerView recyclerView;
    private MasterTraceabilityAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvTotalMasters;
    private TextView tvTotalMasterWeight;
    private FloatingActionButton fabAdd;

    private AppDatabase database;
    private MasterTraceabilityRepository repository;
    private FarmEntity currentFarm;
    private long farmId = -1;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<MasterBatchTraceabilityEntity> allMasters = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_master_traceability, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupFab();
        loadFarmData();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvTotalMasters = view.findViewById(R.id.tvTotalMasters);
        tvTotalMasterWeight = view.findViewById(R.id.tvTotalMasterWeight);
        fabAdd = view.findViewById(R.id.fabAdd);
    }

    private void setupFab() {
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                // Show dialog to create master traceability
                showCreateMasterDialog();
            });
        }
    }

    private void showCreateMasterDialog() {
        // Check if there are batch traceabilities to create master from
        Toast.makeText(getContext(), "Create Master Traceability from FAB", Toast.LENGTH_SHORT).show();
        // You can open the CreateMasterTraceabilityDialog here
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MasterTraceabilityAdapter(allMasters, new MasterTraceabilityAdapter.OnMasterClickListener() {
            @Override
            public void onMasterClick(MasterBatchTraceabilityEntity master) {
                Intent intent = new Intent(getActivity(), MasterTraceabilityDetailActivity.class);
                intent.putExtra(MasterTraceabilityDetailActivity.EXTRA_MASTER_ID, master.getId());
                startActivity(intent);
            }

            @Override
            public void onMasterLongClick(MasterBatchTraceabilityEntity master) {
                // Show options menu
                showMasterOptions(master);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void showMasterOptions(MasterBatchTraceabilityEntity master) {
        String[] options = {"View Details", "Share", "Delete"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Master: " + master.getMasterBatchNumber())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent intent = new Intent(getActivity(), MasterTraceabilityDetailActivity.class);
                            intent.putExtra(MasterTraceabilityDetailActivity.EXTRA_MASTER_ID, master.getId());
                            startActivity(intent);
                            break;
                        case 1:
                            shareMaster(master);
                            break;
                        case 2:
                            confirmDeleteMaster(master);
                            break;
                    }
                })
                .show();
    }

    private void shareMaster(MasterBatchTraceabilityEntity master) {
        StringBuilder shareText = new StringBuilder();
        shareText.append("🌿 AfriNuts Master Traceability\n");
        shareText.append("=================================\n");
        shareText.append("Master Batch: ").append(master.getMasterBatchNumber()).append("\n");
        shareText.append("Shipment: ").append(master.getShipmentName()).append("\n");
        shareText.append("Buyer: ").append(master.getBuyer()).append("\n");
        shareText.append("Total Weight: ").append(master.getFormattedWeight()).append("\n");
        shareText.append("Total Batches: ").append(master.getTotalBatches()).append("\n");
        shareText.append("🔗 Trace URL: ").append(master.getTraceabilityUrl());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Master Traceability"));
    }

    private void confirmDeleteMaster(MasterBatchTraceabilityEntity master) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Master Traceability")
                .setMessage("Are you sure you want to delete " + master.getMasterBatchNumber() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteMasterTraceability(master, new MasterTraceabilityRepository.RepositoryCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            Toast.makeText(getContext(), "Master deleted", Toast.LENGTH_SHORT).show();
                            loadMasterTraceabilities();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadFarmData() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                farmId = currentFarm.getId();
                repository = new MasterTraceabilityRepository(requireContext(), farmId);
                loadMasterTraceabilities();
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                });
            }
        });
    }

    private void loadMasterTraceabilities() {
        executor.execute(() -> {
            List<MasterBatchTraceabilityEntity> masters = repository.getAllMastersSync();

            requireActivity().runOnUiThread(() -> {
                allMasters.clear();
                if (masters != null) {
                    allMasters.addAll(masters);
                }

                showLoading(false);

                if (allMasters.isEmpty()) {
                    showEmpty(true);
                } else {
                    showEmpty(false);
                    updateSummaryStats();
                    adapter.updateMasters(allMasters);
                }
            });
        });
    }

    private void updateSummaryStats() {
        int total = allMasters.size();
        double totalWeight = 0;
        for (MasterBatchTraceabilityEntity master : allMasters) {
            totalWeight += master.getTotalWeightKg();
        }

        tvTotalMasters.setText(String.valueOf(total));
        tvTotalMasterWeight.setText(String.format(Locale.getDefault(), "%,.0f kg", totalWeight));
    }

    private void showLoading(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }
        });
    }

    private void showEmpty(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (emptyView != null) {
                emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentFarm != null) {
            loadMasterTraceabilities();
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