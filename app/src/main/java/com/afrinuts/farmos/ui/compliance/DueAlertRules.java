package com.afrinuts.farmos.ui.compliance;

import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared source of truth for the Due tab and the notifications inbox. */
public final class DueAlertRules {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private DueAlertRules() { }

    public static List<DueExpiringAdapter.Row> buildRows(List<CertificationEntity> certifications,
            List<DocumentEntity> documents, List<AuditEntity> audits,
            List<InspectionEntity> inspections, long now) {
        List<DueExpiringAdapter.Row> overdue = new ArrayList<>();
        List<DueExpiringAdapter.Row> dueSoon = new ArrayList<>();
        Map<String, CertificationEntity> byId = new HashMap<>();
        if (certifications != null) for (CertificationEntity cert : certifications) {
            byId.put(cert.getCertificationId(), cert);
            addCertificationRows(cert, now, overdue, dueSoon);
        }
        if (documents != null) addDocumentRows(documents, byId, now, overdue, dueSoon);
        if (audits != null) addAuditFindingRows(audits, now, overdue);
        if (inspections != null) addInspectionActionRows(inspections, now, overdue);
        Comparator<DueExpiringAdapter.Row> byDate = Comparator.comparingLong(row -> row.sortDate);
        Collections.sort(overdue, byDate);
        Collections.sort(dueSoon, byDate);
        List<DueExpiringAdapter.Row> rows = new ArrayList<>();
        rows.add(DueExpiringAdapter.Row.section("Overdue"));
        if (overdue.isEmpty()) rows.add(DueExpiringAdapter.Row.empty("No overdue items.")); else rows.addAll(overdue);
        rows.add(DueExpiringAdapter.Row.section("Due soon"));
        if (dueSoon.isEmpty()) rows.add(DueExpiringAdapter.Row.empty("No due-soon items.")); else rows.addAll(dueSoon);
        return rows;
    }

    private static void addCertificationRows(CertificationEntity cert, long now,
            List<DueExpiringAdapter.Row> overdue, List<DueExpiringAdapter.Row> dueSoon) {
        long target = cert.getTargetDate();
        String status = cert.getEffectiveStatus().name();
        if (target > 0 && "IN_PROGRESS".equals(status)) {
            addDueRow(overdue, dueSoon, cert.getCertificationName(), "Programme target · " + cert.getEffectiveStatusDisplay(),
                    target, cert.getCertificationId(), "certification", now, true, true);
        } else if (target > now && "NOT_STARTED".equals(status)) {
            addDueRow(overdue, dueSoon, cert.getCertificationName(), "Programme target · " + cert.getEffectiveStatusDisplay(),
                    target, cert.getCertificationId(), "certification", now, false, true);
        }
        if (cert.getStatus() == CertificationEntity.CertificationStatus.CERTIFIED && cert.getExpiryDate() > 0) {
            addDueRow(overdue, dueSoon, cert.getCertificationName(), "Certification expiry · " + cert.getEffectiveStatusDisplay(),
                    cert.getExpiryDate(), cert.getCertificationId(), "certification", now, true, false);
        }
    }

    private static void addDocumentRows(List<DocumentEntity> documents, Map<String, CertificationEntity> byId, long now,
            List<DueExpiringAdapter.Row> overdue, List<DueExpiringAdapter.Row> dueSoon) {
        for (DocumentEntity doc : documents) {
            if (doc.getExpiryDate() <= 0) continue;
            CertificationEntity parent = byId.get(doc.getCertificationId());
            String parentStatus = parent == null ? "" : " · Programme " + parent.getEffectiveStatusDisplay();
            String kind = doc.getDocumentType() == DocumentEntity.DocumentType.CERTIFICATE ? "Certificate expiry" : "Document expiry";
            addDueRow(overdue, dueSoon, doc.getDocumentName(), kind + parentStatus, doc.getExpiryDate(),
                    doc.getDocumentId(), "document", now, true, false);
        }
    }

    private static void addAuditFindingRows(List<AuditEntity> audits, long now, List<DueExpiringAdapter.Row> overdue) {
        for (AuditEntity audit : audits) for (AuditEntity.Finding finding : audit.getFindings()) {
            if (finding.dueDate <= 0 || finding.dueDate >= now || isClosed(finding.status)) continue;
            String title = hasText(finding.finding) ? finding.finding : "Audit finding";
            String subtitle = audit.getAuditTypeDisplay() + " · " + audit.getAuditId();
            overdue.add(createRow(title, subtitle, finding.dueDate, audit.getAuditId(), "audit", now, true,
                    "audit_" + audit.getAuditId() + "_finding_" + finding.dueDate + "_" + title.hashCode()));
        }
    }

    private static void addInspectionActionRows(List<InspectionEntity> inspections, long now, List<DueExpiringAdapter.Row> overdue) {
        for (InspectionEntity inspection : inspections) for (InspectionEntity.CorrectiveAction action : inspection.getCorrectiveActions()) {
            if (action.dueDate <= 0 || action.dueDate >= now || isClosed(action.status)) continue;
            String title = hasText(action.description) ? action.description : "Inspection corrective action";
            String subtitle = inspection.getCertificationName() + " · " + inspection.getInspectionId();
            overdue.add(createRow(title, subtitle, action.dueDate, inspection.getInspectionId(), "inspection", now, true,
                    "inspection_" + inspection.getInspectionId() + "_action_" + action.dueDate + "_" + title.hashCode()));
        }
    }

    private static void addDueRow(List<DueExpiringAdapter.Row> overdue, List<DueExpiringAdapter.Row> dueSoon,
            String title, String subtitle, long date, String recordId, String recordType, long now,
            boolean includeOverdue, boolean target) {
        if (date <= 0) return;
        String key;
        if (target) key = "target_" + recordId + "_" + date;
        else {
            int band = band(date, now);
            String type = "document".equals(recordType) ? "doc" : "cert";
            key = type + "_" + recordId + "_expiry_" + date + "_band_" + band;
        }
        if (date < now) {
            if (includeOverdue) overdue.add(createRow(title, subtitle, date, recordId, recordType, now, true, key));
        } else dueSoon.add(createRow(title, subtitle, date, recordId, recordType, now, false, key));
    }

    private static int band(long date, long now) {
        long days = (long) Math.ceil(Math.abs(date - now) / (double) DAY_MS);
        return days <= 7 ? 7 : days <= 30 ? 30 : 90;
    }

    private static DueExpiringAdapter.Row createRow(String title, String subtitle, long date,
            String recordId, String recordType, long now, boolean overdue, String key) {
        long days = overdue ? Math.max(1, (long) Math.ceil((now - date) / (double) DAY_MS))
                : Math.max(1, (long) Math.ceil((date - now) / (double) DAY_MS));
        String dayCount = NumberFormat.getIntegerInstance(Locale.US).format(days);
        String daysText = overdue ? dayCount + (days == 1 ? " day overdue" : " days overdue")
                : "Due in " + dayCount + (days == 1 ? " day" : " days");
        return DueExpiringAdapter.Row.item(hasText(title) ? title : "Compliance item", subtitle,
                new SimpleDateFormat("d MMM yyyy", Locale.US).format(new Date(date)), daysText,
                recordId, recordType, date, overdue, key);
    }

    private static boolean isClosed(String status) { return status != null && "CLOSED".equalsIgnoreCase(status.trim()); }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
