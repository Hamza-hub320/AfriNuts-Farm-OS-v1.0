package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.afrinuts.farmos.data.repository.CertificationRepository;
import com.afrinuts.farmos.data.repository.DocumentRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.data.sync.CertificationDueCheckWorker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DueExpiringFragment extends Fragment {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    public static final String ARG_SELECTED_RECORD_TYPE = "selected_record_type";
    public static final String ARG_SELECTED_RECORD_ID = "selected_record_id";

    public interface OnDueSelectedListener {
        void onDueSelected(String recordType, String recordId);
    }

    private ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private long farmId;
    private AppDatabase database;
    private CertificationRepository certificationRepository;
    private DocumentRepository documentRepository;
    private AuditRepository auditRepository;
    private InspectionRepository inspectionRepository;

    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private DueExpiringAdapter adapter;

    private List<CertificationEntity> certifications = new ArrayList<>();
    private List<DocumentEntity> documents = new ArrayList<>();
    private List<AuditEntity> audits = new ArrayList<>();
    private List<InspectionEntity> inspections = new ArrayList<>();
    private boolean certificationsLoaded;
    private boolean documentsLoaded;
    private boolean auditsLoaded;
    private boolean inspectionsLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_due_expiring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.findViewById(R.id.progressBarDueExpiring);
        emptyView = view.findViewById(R.id.emptyDueExpiring);
        recyclerView = view.findViewById(R.id.recyclerDueExpiring);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (isTablet()) {
            GradientDrawable pane = new GradientDrawable();
            pane.setColor(Color.WHITE);
            pane.setCornerRadius(dp(24));
            view.setBackground(pane);
            view.setClipToOutline(true);
            int inset = dp(16);
            recyclerView.setPadding(inset, dp(12), inset, dp(16));
        }
        adapter = new DueExpiringAdapter(requireContext(), row -> {
            if (isTablet()) {
                adapter.setSelectedDue(row.recordType, row.recordId);
                notifyDueSelected(row.recordType, row.recordId);
            } else {
                openRecord(row);
            }
        });
        Bundle args = getArguments();
        if (args != null) {
            adapter.setSelectedDue(args.getString(ARG_SELECTED_RECORD_TYPE),
                    args.getString(ARG_SELECTED_RECORD_ID));
        }
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btnCheckNow).setOnClickListener(v -> {
            if (farmId <= 0) return;
            CertificationDueCheckWorker.enqueueNow(requireContext(), farmId);
            Toast.makeText(requireContext(), "Checking due dates…", Toast.LENGTH_SHORT).show();
        });

        executor = Executors.newSingleThreadExecutor();
        certifications = new ArrayList<>();
        documents = new ArrayList<>();
        audits = new ArrayList<>();
        inspections = new ArrayList<>();
        certificationsLoaded = false;
        documentsLoaded = false;
        auditsLoaded = false;
        inspectionsLoaded = false;
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        android.content.Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> initializeRepositories(appContext));
    }

    private void initializeRepositories(android.content.Context context) {
        try {
            database = AppDatabase.getInstance(context);
            com.afrinuts.farmos.data.local.entity.FarmEntity farm = database.farmDao().getFirstFarm();
            if (farm == null) {
                mainHandler.post(() -> showLoadError("No farm is available."));
                return;
            }
            farmId = farm.getId();
            certificationRepository = new CertificationRepository(context, farmId);
            documentRepository = new DocumentRepository(context, farmId);
            auditRepository = new AuditRepository(context, farmId);
            inspectionRepository = new InspectionRepository(context, farmId);

            mainHandler.post(() -> {
                if (!isAdded() || getView() == null) return;
                observeRepositories();
            });
        } catch (Exception error) {
            mainHandler.post(() -> showLoadError("Could not load compliance due dates."));
        }
    }

    private void observeRepositories() {
        LiveData<List<CertificationEntity>> certificationData = certificationRepository.getAllCertifications();
        certificationData.observe(getViewLifecycleOwner(), rows -> {
            certifications = rows == null ? new ArrayList<>() : rows;
            certificationsLoaded = true;
            renderIfLoaded();
        });
        documentRepository.getAllDocuments().observe(getViewLifecycleOwner(), rows -> {
            documents = rows == null ? new ArrayList<>() : rows;
            documentsLoaded = true;
            renderIfLoaded();
        });
        auditRepository.getAllAudits().observe(getViewLifecycleOwner(), rows -> {
            audits = rows == null ? new ArrayList<>() : rows;
            auditsLoaded = true;
            renderIfLoaded();
        });
        inspectionRepository.getAllInspections().observe(getViewLifecycleOwner(), rows -> {
            inspections = rows == null ? new ArrayList<>() : rows;
            inspectionsLoaded = true;
            renderIfLoaded();
        });
    }

    private void renderIfLoaded() {
        if (!certificationsLoaded || !documentsLoaded || !auditsLoaded || !inspectionsLoaded) return;
        progressBar.setVisibility(View.GONE);
        List<DueExpiringAdapter.Row> rows = DueAlertRules.buildRows(
                certifications, documents, audits, inspections, System.currentTimeMillis());

        adapter.submitRows(rows);
        if (isTablet()) {
            DueExpiringAdapter.Row selected = adapter.findItem(
                    adapter.getSelectedRecordType(), adapter.getSelectedRecordId());
            if (selected == null) selected = adapter.firstItem();
            if (selected == null) {
                adapter.setSelectedDue(null, null);
                notifyDueSelected(null, null);
            } else {
                adapter.setSelectedDue(selected.recordType, selected.recordId);
                notifyDueSelected(selected.recordType, selected.recordId);
            }
        }
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void openRecord(DueExpiringAdapter.Row row) {
        if (!hasText(row.recordId) || !isAdded()) return;
        Intent intent;
        switch (row.recordType) {
            case "document":
                intent = new Intent(requireContext(), DocumentDetailActivity.class);
                intent.putExtra(DocumentDetailActivity.EXTRA_DOCUMENT_ID, row.recordId);
                break;
            case "audit":
                intent = new Intent(requireContext(), AuditDetailActivity.class);
                intent.putExtra(AuditDetailActivity.EXTRA_AUDIT_ID, row.recordId);
                break;
            case "inspection":
                intent = new Intent(requireContext(), InspectionDetailActivity.class);
                intent.putExtra(InspectionDetailActivity.EXTRA_INSPECTION_ID, row.recordId);
                break;
            default:
                intent = new Intent(requireContext(), CertificationDetailActivity.class);
                intent.putExtra(CertificationDetailActivity.EXTRA_CERTIFICATION_ID, row.recordId);
                break;
        }
        startActivity(intent);
    }

    private void notifyDueSelected(String recordType, String recordId) {
        if (!isTablet() || getActivity() == null) return;
        if (getActivity() instanceof OnDueSelectedListener) {
            ((OnDueSelectedListener) getActivity()).onDueSelected(recordType, recordId);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isTablet() {
        return getResources().getConfiguration().screenWidthDp >= 600;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showLoadError(String message) {
        if (!isAdded() || getView() == null) return;
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        if (certificationRepository != null) certificationRepository.shutdown();
        if (documentRepository != null) documentRepository.shutdown();
        if (auditRepository != null) auditRepository.shutdown();
        if (inspectionRepository != null) inspectionRepository.shutdown();
        if (executor != null) executor.shutdown();
        executor = null;
        certificationRepository = null;
        documentRepository = null;
        auditRepository = null;
        inspectionRepository = null;
        adapter = null;
        super.onDestroyView();
    }

}
