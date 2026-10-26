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
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CertificationListFragment extends Fragment {

    private RecyclerView recyclerView;
    private CertificationAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private CertificationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long farmId = 1;
    private List<CertificationEntity> certifications = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_certification_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();

        // ✅ Get farm ID on background thread
        executor.execute(() -> {
            try {
                farmId = AppDatabase.getInstance(requireContext()).farmDao().getFirstFarm().getId();
                repository = new CertificationRepository(requireContext(), farmId);

                // ✅ Switch to main thread to observe LiveData
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadCertifications());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error loading certifications", Toast.LENGTH_SHORT).show();
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
        adapter = new CertificationAdapter(certifications, certification -> {
            // ✅ Navigate to CertificationDetailActivity
            Intent intent = new Intent(requireContext(), CertificationDetailActivity.class);
            intent.putExtra(CertificationDetailActivity.EXTRA_CERTIFICATION_ID, certification.getCertificationId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    // ✅ This runs on the main thread
    private void loadCertifications() {
        if (repository == null) return;

        repository.getAllCertifications().observe(getViewLifecycleOwner(), new Observer<List<CertificationEntity>>() {
            @Override
            public void onChanged(List<CertificationEntity> certs) {
                certifications.clear();
                if (certs != null) {
                    certifications.addAll(certs);
                }

                showLoading(false);

                if (certifications.isEmpty()) {
                    showEmpty(true);
                } else {
                    showEmpty(false);
                    adapter.updateCertifications(certifications);
                }
            }
        });
    }

    /**
     * Called by the Activity's FAB to show the add certification dialog
     */
    public void showAddCertificationDialog() {
        if (getActivity() == null) return;

        AddCertificationDialog dialog = AddCertificationDialog.newInstance(farmId);
        dialog.setOnCertificationAddedListener(() -> {
            loadCertifications();
            Toast.makeText(requireContext(), "Certification added!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "AddCertificationDialog");
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
            tvEmptyTitle.setText("No Certifications");
            tvEmptySubtitle.setText("Tap + to add your first certification");
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