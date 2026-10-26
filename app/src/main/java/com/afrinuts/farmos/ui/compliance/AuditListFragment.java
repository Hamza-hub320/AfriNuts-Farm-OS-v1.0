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
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuditListFragment extends Fragment {

    private RecyclerView recyclerView;
    private AuditAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private AuditRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long farmId = 1;
    private List<AuditEntity> audits = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audit_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();

        executor.execute(() -> {
            try {
                farmId = AppDatabase.getInstance(requireContext()).farmDao().getFirstFarm().getId();
                repository = new AuditRepository(requireContext(), farmId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadAudits());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error loading audits", Toast.LENGTH_SHORT).show();
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
        adapter = new AuditAdapter(audits, audit -> {
            // ✅ Navigate to AuditDetailActivity
            Intent intent = new Intent(requireContext(), AuditDetailActivity.class);
            intent.putExtra(AuditDetailActivity.EXTRA_AUDIT_ID, audit.getAuditId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadAudits() {
        if (repository == null) return;

        repository.getAllAudits().observe(getViewLifecycleOwner(), auditList -> {
            audits.clear();
            if (auditList != null) {
                audits.addAll(auditList);
            }

            showLoading(false);

            if (audits.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                adapter.updateAudits(audits);
            }
        });
    }

    public void showAddAuditDialog() {
        if (getActivity() == null) return;

        AddAuditDialog dialog = AddAuditDialog.newInstance(farmId);
        dialog.setOnAuditAddedListener(() -> {
            loadAudits();
            Toast.makeText(requireContext(), "Audit added!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "AddAuditDialog");
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
            tvEmptyTitle.setText("No Audits");
            tvEmptySubtitle.setText("Add your first audit record");
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