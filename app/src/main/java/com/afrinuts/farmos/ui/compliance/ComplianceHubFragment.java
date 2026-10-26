package com.afrinuts.farmos.ui.compliance;

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
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComplianceHubFragment extends Fragment {

    private TextView tvTotalCertifications;
    private TextView tvActiveCertifications;
    private TextView tvExpiringCertifications;
    private TextView tvExpiredCertifications;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout emptyView;

    private CertificationAdapter adapter;
    private CertificationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long farmId = 1;
    private List<CertificationEntity> expiringCertifications = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compliance_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupFab(view);

        // Get farm ID
        executor.execute(() -> {
            farmId = AppDatabase.getInstance(requireContext()).farmDao().getFirstFarm().getId();
            repository = new CertificationRepository(requireContext(), farmId);
            loadDashboardData();
        });
    }

    private void initViews(View view) {
        tvTotalCertifications = view.findViewById(R.id.tvTotalCertifications);
        tvActiveCertifications = view.findViewById(R.id.tvActiveCertifications);
        tvExpiringCertifications = view.findViewById(R.id.tvExpiringCertifications);
        tvExpiredCertifications = view.findViewById(R.id.tvExpiredCertifications);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        // Set click listeners on stat cards
        CardView cardTotal = view.findViewById(R.id.cardTotal);
        CardView cardActive = view.findViewById(R.id.cardActive);
        CardView cardExpiring = view.findViewById(R.id.cardExpiring);
        CardView cardExpired = view.findViewById(R.id.cardExpired);

        cardTotal.setOnClickListener(v -> navigateToCertifications());
        cardActive.setOnClickListener(v -> navigateToCertifications());
        cardExpiring.setOnClickListener(v -> navigateToCertifications());
        cardExpired.setOnClickListener(v -> navigateToCertifications());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CertificationAdapter(expiringCertifications, certification -> {
            openCertificationDetail(certification);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFab(View view) {
        MaterialButton btnAddCertification = view.findViewById(R.id.btnAddCertification);
        btnAddCertification.setOnClickListener(v -> showAddCertificationDialog());
    }

    private void loadDashboardData() {
        repository.getAllCertifications().observe(getViewLifecycleOwner(), certifications -> {
            if (certifications == null) return;

            showLoading(false);

            int total = certifications.size();
            int active = 0;
            int expired = 0;
            int expiring = 0;

            expiringCertifications.clear();

            for (CertificationEntity cert : certifications) {
                if (cert.isActive()) {
                    active++;
                }
                if (cert.isExpired()) {
                    expired++;
                }
                if (cert.isExpiringSoon()) {
                    expiring++;
                    expiringCertifications.add(cert);
                }
            }

            tvTotalCertifications.setText(String.valueOf(total));
            tvActiveCertifications.setText(String.valueOf(active));
            tvExpiringCertifications.setText(String.valueOf(expiring));
            tvExpiredCertifications.setText(String.valueOf(expired));

            if (expiringCertifications.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.updateCertifications(expiringCertifications);
            }
        });
    }

    private void navigateToCertifications() {
        if (getActivity() != null) {
            // Check if the activity is ComplianceHubActivity
            if (getActivity() instanceof ComplianceHubActivity) {
                Toast.makeText(getContext(), "View all certifications", Toast.LENGTH_SHORT).show();
            } else {
                // For other activities, use the fragment container
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(android.R.id.content, new CertificationListFragment())
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    private void openCertificationDetail(CertificationEntity certification) {
        // Open Certification Detail
        android.content.Intent intent = new android.content.Intent(requireContext(), CertificationDetailActivity.class);
        intent.putExtra(CertificationDetailActivity.EXTRA_CERTIFICATION_ID, certification.getCertificationId());
        startActivity(intent);
    }

    private void showAddCertificationDialog() {
        AddCertificationDialog dialog = AddCertificationDialog.newInstance(farmId);
        dialog.setOnCertificationAddedListener(() -> {
            loadDashboardData();
            Toast.makeText(requireContext(), "Certification added!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "AddCertificationDialog");
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE);
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