package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
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
import com.afrinuts.farmos.utils.DateFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DueExpiringFragment extends Fragment {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

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
        adapter = new DueExpiringAdapter(requireContext(), this::openRecord);
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
        List<DueExpiringAdapter.Row> overdue = new ArrayList<>();
        List<DueExpiringAdapter.Row> dueSoon = new ArrayList<>();
        long now = System.currentTimeMillis();
        Map<String, CertificationEntity> certificationsById = new HashMap<>();
        for (CertificationEntity certification : certifications) {
            certificationsById.put(certification.getCertificationId(), certification);
            addCertificationRows(certification, now, overdue, dueSoon);
        }
        addDocumentRows(certificationsById, now, overdue, dueSoon);
        addAuditFindingRows(now, overdue);
        addInspectionActionRows(now, overdue);

        Comparator<DueExpiringAdapter.Row> byDate = Comparator.comparingLong(row -> row.sortDate);
        Collections.sort(overdue, byDate);
        Collections.sort(dueSoon, byDate);
        List<DueExpiringAdapter.Row> rows = new ArrayList<>();
        rows.add(DueExpiringAdapter.Row.section("Overdue"));
        if (overdue.isEmpty()) rows.add(DueExpiringAdapter.Row.empty("No overdue items."));
        else rows.addAll(overdue);
        rows.add(DueExpiringAdapter.Row.section("Due soon"));
        if (dueSoon.isEmpty()) rows.add(DueExpiringAdapter.Row.empty("No due-soon items."));
        else rows.addAll(dueSoon);

        adapter.submitRows(rows);
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void addCertificationRows(CertificationEntity certification, long now,
                                      List<DueExpiringAdapter.Row> overdue,
                                      List<DueExpiringAdapter.Row> dueSoon) {
        long targetDate = certification.getTargetDate();
        String status = certification.getEffectiveStatus().name();
        if (targetDate > 0 && "IN_PROGRESS".equals(status)) {
            addDueRow(overdue, dueSoon, certification.getCertificationName(),
                    "Programme target · " + certification.getEffectiveStatusDisplay(), targetDate,
                    certification.getCertificationId(), "certification", now, true);
        } else if (targetDate > now && "NOT_STARTED".equals(status)) {
            addDueRow(overdue, dueSoon, certification.getCertificationName(),
                    "Programme target · " + certification.getEffectiveStatusDisplay(), targetDate,
                    certification.getCertificationId(), "certification", now, false);
        }

        if (certification.getStatus() == CertificationEntity.CertificationStatus.CERTIFIED
                && certification.getExpiryDate() > 0) {
            addDueRow(overdue, dueSoon, certification.getCertificationName(),
                    "Certification expiry · " + certification.getEffectiveStatusDisplay(), certification.getExpiryDate(),
                    certification.getCertificationId(), "certification", now, true);
        }
    }

    private void addDocumentRows(Map<String, CertificationEntity> certificationsById, long now,
                                 List<DueExpiringAdapter.Row> overdue,
                                 List<DueExpiringAdapter.Row> dueSoon) {
        for (DocumentEntity document : documents) {
            if (document.getExpiryDate() <= 0) continue;
            CertificationEntity parent = certificationsById.get(document.getCertificationId());
            String parentStatus = parent == null ? "" : " · Programme " + parent.getEffectiveStatusDisplay();
            String type = document.getDocumentType() == DocumentEntity.DocumentType.CERTIFICATE
                    ? "Certificate expiry" : "Document expiry";
            addDueRow(overdue, dueSoon, document.getDocumentName(),
                    type + parentStatus, document.getExpiryDate(), document.getDocumentId(),
                    "document", now, true);
        }
    }

    private void addAuditFindingRows(long now, List<DueExpiringAdapter.Row> overdue) {
        for (AuditEntity audit : audits) {
            for (AuditEntity.Finding finding : audit.getFindings()) {
                if (finding.dueDate <= 0 || finding.dueDate >= now || isClosed(finding.status)) continue;
                String title = hasText(finding.finding) ? finding.finding : "Audit finding";
                String subtitle = audit.getAuditTypeDisplay() + " · " + audit.getAuditId();
                overdue.add(createRow(title, subtitle, finding.dueDate, audit.getAuditId(), "audit", now, true));
            }
        }
    }

    private void addInspectionActionRows(long now, List<DueExpiringAdapter.Row> overdue) {
        for (InspectionEntity inspection : inspections) {
            for (InspectionEntity.CorrectiveAction action : inspection.getCorrectiveActions()) {
                if (action.dueDate <= 0 || action.dueDate >= now || isClosed(action.status)) continue;
                String title = hasText(action.description) ? action.description : "Inspection corrective action";
                String subtitle = inspection.getCertificationName() + " · " + inspection.getInspectionId();
                overdue.add(createRow(title, subtitle, action.dueDate, inspection.getInspectionId(), "inspection", now, true));
            }
        }
    }

    private void addDueRow(List<DueExpiringAdapter.Row> overdue, List<DueExpiringAdapter.Row> dueSoon,
                           String title, String subtitle, long date, String recordId, String recordType,
                           long now, boolean includeOverdue) {
        if (date <= 0) return;
        if (date < now) {
            if (includeOverdue) overdue.add(createRow(title, subtitle, date, recordId, recordType, now, true));
        } else {
            dueSoon.add(createRow(title, subtitle, date, recordId, recordType, now, false));
        }
    }

    private DueExpiringAdapter.Row createRow(String title, String subtitle, long date,
                                              String recordId, String recordType, long now,
                                              boolean isOverdue) {
        long days = isOverdue
                ? Math.max(1, (long) Math.ceil((now - date) / (double) DAY_MS))
                : Math.max(1, (long) Math.ceil((date - now) / (double) DAY_MS));
        String daysText = isOverdue ? days + (days == 1 ? " day overdue" : " days overdue")
                : days + (days == 1 ? " day remaining" : " days remaining");
        return DueExpiringAdapter.Row.item(
                hasText(title) ? title : "Compliance item", subtitle,
                DateFormatter.formatDateOrDash(date), daysText, recordId, recordType, date);
    }

    private boolean isClosed(String status) {
        return status != null && "CLOSED".equalsIgnoreCase(status.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
