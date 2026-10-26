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
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.VerificationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VerificationListFragment extends Fragment {

    private RecyclerView recyclerView;
    private VerificationAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private VerificationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long farmId = 1;
    private List<VerificationEntity> verifications = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verification_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();

        executor.execute(() -> {
            try {
                farmId = AppDatabase.getInstance(requireContext()).farmDao().getFirstFarm().getId();
                repository = new VerificationRepository(requireContext(), farmId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadVerifications());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error loading verifications", Toast.LENGTH_SHORT).show();
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
        adapter = new VerificationAdapter(verifications, verification -> {
            Intent intent = new Intent(requireContext(), VerificationDetailActivity.class);
            intent.putExtra(VerificationDetailActivity.EXTRA_VERIFICATION_ID, verification.getVerificationId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadVerifications() {
        if (repository == null) return;

        repository.getAllVerifications().observe(getViewLifecycleOwner(), verificationList -> {
            verifications.clear();
            if (verificationList != null) {
                verifications.addAll(verificationList);
            }

            showLoading(false);

            if (verifications.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                adapter.updateVerifications(verifications);
            }
        });
    }

    public void showAddVerificationDialog() {
        if (getActivity() == null) return;

        AddVerificationDialog dialog = AddVerificationDialog.newInstance(farmId);
        dialog.setOnVerificationAddedListener(() -> {
            loadVerifications();
            Toast.makeText(requireContext(), "Verification added!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "AddVerificationDialog");
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
            tvEmptyTitle.setText("No Verifications");
            tvEmptySubtitle.setText("Add your first verification record");
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