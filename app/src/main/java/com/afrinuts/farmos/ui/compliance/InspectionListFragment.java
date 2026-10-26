package com.afrinuts.farmos.ui.compliance;

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
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.repository.InspectionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InspectionListFragment extends Fragment {

    private RecyclerView recyclerView;
    private InspectionAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private InspectionRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long farmId = 1;
    private List<InspectionEntity> inspections = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inspection_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();

        executor.execute(() -> {
            try {
                farmId = AppDatabase.getInstance(requireContext()).farmDao().getFirstFarm().getId();
                repository = new InspectionRepository(requireContext(), farmId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadInspections());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error loading inspections", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InspectionAdapter(inspections, inspection -> {
            Intent intent = new Intent(requireContext(), InspectionDetailActivity.class);
            intent.putExtra(InspectionDetailActivity.EXTRA_INSPECTION_ID, inspection.getInspectionId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadInspections() {
        if (repository == null) return;

        repository.getAllInspections().observe(getViewLifecycleOwner(), inspectionList -> {
            inspections.clear();
            if (inspectionList != null) {
                inspections.addAll(inspectionList);
            }

            showLoading(false);

            if (inspections.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                adapter.updateInspections(inspections);
            }
        });
    }

    public void showAddInspectionDialog() {
        if (getActivity() == null) return;

        AddInspectionDialog dialog = AddInspectionDialog.newInstance(farmId);
        dialog.setOnInspectionAddedListener(() -> {
            loadInspections();
            Toast.makeText(requireContext(), "Inspection added!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "AddInspectionDialog");
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

        if (show) {
            tvEmptyTitle.setText("No Inspections");
            tvEmptySubtitle.setText("Add your first inspection record");
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