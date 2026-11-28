package com.afrinuts.farmos.data.integrity;

import android.content.Context;
import android.util.Log;

import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.*;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.utils.QualityGradeCalculator;
import com.google.gson.Gson;

import java.util.*;

/** Runs the offline rule catalogue against Room; source records are never modified. */
public final class IntegrityScanner {
    private static final String TAG = "IntegrityScanner";
    private static final Gson GSON = new Gson();
    private final AppDatabase db;
    private final SyncManager syncManager;

    public IntegrityScanner(Context context) {
        db = AppDatabase.getInstance(context);
        syncManager = SyncManager.getInstance(context);
    }

    public IntegrityScanRunEntity scan(long farmId, String trigger) {
        syncManager.setSyncCenterContext(farmId, null);
        IntegrityScanRunEntity run = new IntegrityScanRunEntity();
        run.setFarmId(farmId);
        run.setStartedAt(System.currentTimeMillis());
        run.setTriggeredBy("scheduled".equalsIgnoreCase(trigger) ? "scheduled" : "manual");
        run.setStatus("RUNNING");
        run.setPendingSyncCount(db.syncDao().getPendingCount());
        long scanId = db.integrityScanRunDao().insert(run);
        run.setId(scanId);
        try {
            runChecks(scanId, farmId, run);
            run.setStatus("COMPLETED");
        } catch (Exception e) {
            Log.e(TAG, "Scan failed", e);
            run.setStatus("FAILED");
        }
        run.setCompletedAt(System.currentTimeMillis());
        run.setIssuesDetected(countForRun(farmId, scanId));
        run.setRulesEvaluated(25); // INT-00 plus INT-01 through INT-24.
        db.integrityScanRunDao().update(run);
        return run;
    }

    private void runChecks(long scanId, long farmId, IntegrityScanRunEntity run) {
        long now = System.currentTimeMillis();
        List<BlockEntity> blocks = db.blockDao().getBlocksByFarmId(farmId);
        List<ExpenseEntity> expenses = db.expenseDao().getExpensesByFarmId(farmId);
        List<RevenueEntity> revenues = db.revenueDao().getRevenuesByFarmId(farmId);
        List<TaskEntity> tasks = db.taskDao().getTasksByFarmId(farmId);
        List<UserEntity> users = new ArrayList<>();
        for (UserEntity x : db.userDao().getAllUsers()) if (x.getFarmId() == farmId) users.add(x);
        List<PhotoEntity> photos = db.photoDao().getPhotosByFarmId(farmId);
        List<QualityBatchEntity> batches = db.qualityBatchDao().getBatchesByFarmId(farmId);
        List<SupplierEntity> suppliers = db.supplierDao().getSuppliersByFarmId(farmId);
        List<BatchTraceabilityEntity> traces = db.batchTraceabilityDao().getByFarmId(farmId);
        List<MasterBatchTraceabilityEntity> masters = new ArrayList<>();
        for (MasterBatchTraceabilityEntity x : db.masterTraceabilityDao().getAll()) if (x.getFarmId() == farmId) masters.add(x);
        List<CertificationEntity> programmes = db.certificationDao().getCertificationsByFarmId(farmId);
        List<AuditEntity> audits = db.auditDao().getAuditsByFarmId(farmId);
        List<InspectionEntity> inspections = db.inspectionDao().getInspectionsByFarmId(farmId);
        List<VerificationEntity> verifications = db.verificationDao().getVerificationsByFarmId(farmId);
        List<DocumentEntity> documents = db.documentDao().getDocumentsByFarmId(farmId);
        List<TaskAssignmentHistoryEntity> assignments = db.taskAssignmentDao().getAllAssignmentsForFarm(farmId);
        List<SupplierInspectionEntity> supplierInspections = new ArrayList<>();
        List<SupplierCommitmentEntity> commitments = new ArrayList<>();
        List<SupplierScoreSnapshotEntity> snapshots = new ArrayList<>();
        for (SupplierEntity s : suppliers) {
            supplierInspections.addAll(db.supplierInspectionDao().getBySupplierId(s.getId()));
            commitments.addAll(db.supplierCommitmentDao().getBySupplierId(s.getId()));
            snapshots.addAll(db.supplierScoreSnapshotDao().getHistoryBySupplierId(s.getId()));
        }
        run.setRecordsScanned(blocks.size() + expenses.size() + revenues.size() + tasks.size() + users.size()
                + photos.size() + batches.size() + suppliers.size() + traces.size() + masters.size()
                + programmes.size() + audits.size() + inspections.size() + verifications.size()
                + documents.size() + assignments.size() + supplierInspections.size() + commitments.size() + snapshots.size());

        Map<Long, QualityBatchEntity> batchById = new HashMap<>();
        for (QualityBatchEntity b : batches) batchById.put(b.getId(), b);

        // INT-00: invoice number must exist and be unique per sale.
        Map<String, List<RevenueEntity>> invoiceRows = new HashMap<>();
        for (RevenueEntity r : revenues) {
            String invoice = clean(r.getInvoiceNumber());
            if (invoice.isEmpty()) finding(scanId, farmId, "revenue", r.getId(), "INT-00", "Critical", "Sale has no invoice number.", "Invoice number present", "Missing", null, null);
            else invoiceRows.computeIfAbsent(invoice.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<RevenueEntity>> e : invoiceRows.entrySet()) if (e.getValue().size() > 1)
            for (RevenueEntity r : e.getValue()) finding(scanId, farmId, "revenue", r.getId(), "INT-00", "Critical", "Invoice number is used by more than one sale.", "Unique invoice number", e.getKey(), null, null);

        // INT-01: compare a master to linked children. Unsettled findings stay Warning.
        for (MasterBatchTraceabilityEntity m : masters) {
            List<BatchTraceabilityEntity> children = db.batchTraceabilityDao().getChildrenOfMaster(String.valueOf(m.getId()));
            double sum = 0;
            boolean isSettled = settled("master_traceability", m.getId(), m.isSynced());
            for (BatchTraceabilityEntity c : children) { sum += c.getWeightKg(); isSettled &= settled("batch_traceability", c.getId(), c.isSynced()); }
            if (Math.abs(sum - m.getTotalWeightKg()) > 0.0001 || children.size() != m.getTotalBatches())
                finding(scanId, farmId, "master_batch", m.getMasterBatchNumber(), "INT-01", isSettled ? "Critical" : "Warning",
                        "Master totals do not match its child batches.", m.getTotalWeightKg() + " kg / " + m.getTotalBatches() + " batches",
                        sum + " kg / " + children.size() + " batches", m.getTotalWeightKg(), sum);
        }

        // INT-02: revenue arithmetic is evaluated on the local row.
        for (RevenueEntity r : revenues) {
            double expected = r.getQuantityKg() * r.getPricePerKg();
            if (Math.abs(r.getTotalAmount() - expected) > 1.0)
                finding(scanId, farmId, "revenue", r.getId(), "INT-02", "Critical", "Revenue total differs from quantity × price.", expected + " FCFA", r.getTotalAmount() + " FCFA", expected, r.getTotalAmount());
        }

        // INT-03: supplier certificate expiry, with escalation if a batch was graded afterward.
        for (SupplierEntity s : suppliers) if (s.isCertified() && s.getCertificationExpiryDate() > 0 && s.getCertificationExpiryDate() < now) {
            long latestBatchDate = 0; boolean isSettled = settled("suppliers", s.getId(), s.isSynced());
            for (QualityBatchEntity b : batches) if (Objects.equals(b.getSupplierId(), s.getId())) {
                latestBatchDate = Math.max(latestBatchDate, Math.max(b.getInspectionDate(), b.getProcessingDate()));
                isSettled &= settled("quality_batch", b.getId(), b.isSynced());
            }
            boolean postExpiry = latestBatchDate > s.getCertificationExpiryDate();
            finding(scanId, farmId, "supplier", s.getId(), "INT-03", postExpiry && isSettled ? "Critical" : "Warning",
                    "Supplier certificate has expired.", "Valid through " + s.getCertificationExpiryDate(),
                    postExpiry ? "Expired; a batch was inspected or processed afterward" : "Expired", null, null);
        }

        // INT-04: duplicate contact / identity keys. Cooperatives may share a phone.
        duplicateSuppliers(scanId, farmId, suppliers, "phone", SupplierEntity::getPhone, true);
        duplicateSuppliers(scanId, farmId, suppliers, "email", SupplierEntity::getEmail, false);
        duplicateSuppliers(scanId, farmId, suppliers, "registration number", SupplierEntity::getRegistrationNumber, false);
        duplicateSuppliers(scanId, farmId, suppliers, "farmer ID number", SupplierEntity::getFarmerIdNumber, false);

        // INT-05: supplier inspections only; unknown evidence requirements are not inferred.
        for (SupplierInspectionEntity i : supplierInspections) if (i.getStatus() == SupplierInspectionEntity.InspectionStatus.CLOSED
                && (i.getEvidenceUrlList().isEmpty() || i.getValidatedAt() == null))
            finding(scanId, farmId, "supplier_inspection", i.getId(), "INT-05", "Warning", "Closed supplier inspection lacks evidence or validation.",
                    "Evidence and validated timestamp", "Evidence=" + i.getEvidenceUrlList().size() + ", validated=" + (i.getValidatedAt() != null), null, null);

        // INT-06: procurement expenses need a valid source.
        for (ExpenseEntity e : expenses) if (e.isProcurementRelated()) {
            if (e.getBatchId() == null && e.getSupplierId() == null)
                finding(scanId, farmId, "expense", e.getId(), "INT-06", "Warning", "Procurement expense has no supplier or batch source.", "Source linked", "No source", null, null);
            else if (e.getBatchId() != null && !batchById.containsKey(e.getBatchId()))
                finding(scanId, farmId, "expense", e.getId(), "INT-06", "Warning", "Procurement expense references a missing batch.", "Existing batch", "Missing batch " + e.getBatchId(), null, null);
            if (!clean(e.getBatchNumber()).isEmpty() && e.getBatchId() == null)
                finding(scanId, farmId, "expense", e.getId(), "INT-06", "Warning", "Expense has a batch number without a batch link.", "batchId linked", e.getBatchNumber(), null, null);
        }

        // INT-07: active suppliers with delivery need location and coordinates.
        for (SupplierEntity s : suppliers) if (s.isActive() && hasDelivery(s.getId(), batches) && (!s.hasLocation() || clean(s.getLocation()).isEmpty()))
            finding(scanId, farmId, "supplier", s.getId(), "INT-07", "Warning", "Active supplier with deliveries has incomplete location data.",
                    "Location and coordinates", "hasLocation=" + s.hasLocation() + ", location=" + clean(s.getLocation()), null, null);

        // INT-08: block harvest and lifecycle consistency.
        for (BlockEntity b : blocks) {
            String problem = null;
            if (b.getStatus() == BlockEntity.BlockStatus.HARVESTED && b.getHarvestCount() == 0) problem = "Block is marked harvested but has no harvest records.";
            else if (b.getHarvestCount() > 0 && b.getTotalHarvestQuantity() == 0) problem = "Block has harvest records but zero total harvest quantity.";
            else if (b.getHarvestCount() > 0 && b.getLastHarvestDate() == null) problem = "Block has harvest records but no last harvest date.";
            else if (b.getPlantedDate() != null && b.getClearedDate() != null && b.getPlantedDate() < b.getClearedDate()) problem = "Block was planted before it was cleared.";
            else if (b.getStatus() == BlockEntity.BlockStatus.PLANTED && b.getPlantedDate() == null) problem = "Block is marked planted without a planted date.";
            else if (b.getSurvivalRate() < 0 || b.getSurvivalRate() > 100) problem = "Block survival rate is outside 0–100%.";
            if (problem != null) finding(scanId, farmId, "block", b.getId(), "INT-08", "Critical", problem, "Consistent block lifecycle", b.getStatus().name(), null, null);
        }

        // INT-09: profile regression checks.
        for (UserEntity u : users) if (u.getLastLoginAt() > 0) {
            if (missing(u.getBio()) || missing(u.getCompany()) || missing(u.getTitle()))
                finding(scanId, farmId, "user", u.getLocalId(), "INT-09", "Warning", "Logged-in user profile is missing bio, company or title.", "Profile fields present", "One or more fields missing", null, null);
            if (missing(u.getLastLoginDevice())) finding(scanId, farmId, "user", u.getLocalId(), "INT-09", "Warning", "Logged-in user has no last-login device.", "Device recorded", "Missing", null, null);
        }

        // INT-10: sales must be sourced and must not exceed batch weight or disagree on grade.
        for (RevenueEntity r : revenues) {
            if (r.getBatchId() == null && r.getSupplierId() == null)
                finding(scanId, farmId, "revenue", r.getId(), "INT-10", "Critical", "Sale has no batch or supplier source.", "Batch or supplier linked", "No source", null, null);
            if (r.getBatchId() != null && batchById.containsKey(r.getBatchId())) {
                QualityBatchEntity b = batchById.get(r.getBatchId());
                if (r.getQuality() != null && b.getQualityGrade() != null && !r.getQuality().name().equals(b.getQualityGrade().name()))
                    finding(scanId, farmId, "revenue", r.getId(), "INT-10", "Critical", "Sale grade differs from the linked batch grade.", b.getQualityGrade().name(), r.getQuality().name(), null, null);
            }
        }
        for (QualityBatchEntity b : batches) {
            double sold = 0; for (RevenueEntity r : revenues) if (Objects.equals(r.getBatchId(), b.getId())) sold += r.getQuantityKg();
            if (sold > b.getWeightKg()) finding(scanId, farmId, "quality_batch", b.getId(), "INT-10", "Critical", "Sales exceed the batch weight.", b.getWeightKg() + " kg", sold + " kg", b.getWeightKg(), sold);
        }

        // INT-11: stored grade versus measurement grade and inspection status.
        for (QualityBatchEntity b : batches) if (b.getQualityGrade() != null) {
            QualityGradeCalculator.Grade computed = QualityGradeCalculator.grade(b);
            if (!computed.name().equals(b.getQualityGrade().name()))
                finding(scanId, farmId, "quality_batch", b.getId(), "INT-11", "Critical", "Stored grade differs from the measurements.", computed.name(), b.getQualityGrade().name(), null, null);
            if (b.getQualityGrade() == QualityBatchEntity.QualityGrade.PREMIUM && !b.isInspected())
                finding(scanId, farmId, "quality_batch", b.getId(), "INT-11", "Critical", "Premium grade is stored without inspection evidence.", "Inspected", "Not inspected", null, null);
        }

        scanTail(scanId, farmId, now, blocks, expenses, revenues, tasks, users, photos, batches, suppliers,
                traces, masters, programmes, audits, inspections, verifications, documents, assignments,
                supplierInspections, commitments, snapshots, batchById);
    }

    private void scanTail(long scanId, long farmId, long now, List<BlockEntity> blocks, List<ExpenseEntity> expenses,
            List<RevenueEntity> revenues, List<TaskEntity> tasks, List<UserEntity> users, List<PhotoEntity> photos,
            List<QualityBatchEntity> batches, List<SupplierEntity> suppliers, List<BatchTraceabilityEntity> traces,
            List<MasterBatchTraceabilityEntity> masters, List<CertificationEntity> programmes, List<AuditEntity> audits,
            List<InspectionEntity> inspections, List<VerificationEntity> verifications, List<DocumentEntity> documents,
            List<TaskAssignmentHistoryEntity> assignments, List<SupplierInspectionEntity> supplierInspections,
            List<SupplierCommitmentEntity> commitments, List<SupplierScoreSnapshotEntity> snapshots,
            Map<Long, QualityBatchEntity> batchById) {
        // INT-12 — supplier state, consent and recorded batch volume.
        for (SupplierEntity s : suppliers) {
            List<QualityBatchEntity> own = new ArrayList<>(); double total = 0;
            for (QualityBatchEntity b : batches) if (Objects.equals(b.getSupplierId(), s.getId())) { own.add(b); total += b.getWeightKg(); }
            String status = s.getStatus() == null ? "" : s.getStatus().name();
            boolean activationIssue = ("ACTIVE".equals(status) != s.isActive())
                    || ("ACTIVE".equals(status) && own.isEmpty() && !"Test Field".equalsIgnoreCase(s.getName()))
                    || ("PENDING".equals(status) && !own.isEmpty())
                    || ("SUSPENDED".equals(status) && !own.isEmpty());
            if (activationIssue) finding(scanId, farmId, "supplier", s.getId(), "INT-12", "Warning", "Supplier status, active flag or delivery history is inconsistent.", "Status consistent with activation and deliveries", status + ", active=" + s.isActive() + ", batches=" + own.size(), null, null);
            if (s.isActive() && !s.isDataConsentGiven()) finding(scanId, farmId, "supplier", s.getId(), "INT-12", "Critical", "Active supplier has not given data consent.", "Consent given", "No consent", null, null);
            if (Math.abs(s.getAnnualVolumeKg() - total) > .0001) finding(scanId, farmId, "supplier", s.getId(), "INT-12", "Warning", "Supplier annual volume differs from its recorded batch volume.", total + " kg", s.getAnnualVolumeKg() + " kg", total, s.getAnnualVolumeKg());
        }

        // INT-13 / INT-14 — programme status and certificate evidence.
        Set<String> certificateDocs = new HashSet<>();
        for (DocumentEntity d : documents) if (d.getDocumentType() == DocumentEntity.DocumentType.CERTIFICATE) certificateDocs.add(clean(d.getCertificationId()));
        for (CertificationEntity c : programmes) {
            if (c.getStatus() == CertificationEntity.CertificationStatus.CERTIFIED && c.getExpiryDate() > 0 && c.getExpiryDate() < now)
                finding(scanId, farmId, "programme", c.getCertificationId(), "INT-13", "Warning", "Programme is stored as certified after its expiry date.", "Not expired", "Expired " + c.getExpiryDate(), null, null);
            if (c.getStatus() == CertificationEntity.CertificationStatus.CERTIFIED && !certificateDocs.contains(clean(c.getCertificationId())))
                finding(scanId, farmId, "programme", c.getCertificationId(), "INT-14", "Critical", "Certified programme has no certificate document on record.", "CERTIFICATE document linked", "No certificate document", null, null);
        }

        // INT-15 — supplier inspection closure, corrective action dates, superseding links/forks.
        for (SupplierInspectionEntity i : supplierInspections) if (i.getStatus() == SupplierInspectionEntity.InspectionStatus.CLOSED && i.getClosedDate() == null)
            finding(scanId, farmId, "supplier_inspection", i.getId(), "INT-15", "Warning", "Closed inspection has no closed date.", "closedDate set", "Missing", null, null);
        for (InspectionEntity i : inspections) for (InspectionEntity.CorrectiveAction a : i.getCorrectiveActions())
            if ("OPEN".equalsIgnoreCase(clean(a.status)) && a.dueDate == 0)
                finding(scanId, farmId, "inspection", i.getInspectionId(), "INT-15", "Warning", "Open corrective action has no due date.", "dueDate set", "Missing due date", null, null);
        Set<String> auditIds = new HashSet<>(), inspectionIds = new HashSet<>(), verificationIds = new HashSet<>();
        for (AuditEntity a : audits) auditIds.add(a.getAuditId());
        for (InspectionEntity i : inspections) inspectionIds.add(i.getInspectionId());
        for (VerificationEntity v : verifications) verificationIds.add(v.getVerificationId());
        Map<String, Integer> supersedes = new HashMap<>();
        for (AuditEntity a : audits) if (!missing(a.getSupersedesId())) { supersedes.merge("audit:" + a.getSupersedesId(), 1, Integer::sum); if (!auditIds.contains(a.getSupersedesId())) finding(scanId, farmId, "audit", a.getAuditId(), "INT-15", "Warning", "Audit supersedes a record that does not exist.", "Existing audit", a.getSupersedesId(), null, null); }
        for (InspectionEntity i : inspections) if (!missing(i.getSupersedesId())) { supersedes.merge("inspection:" + i.getSupersedesId(), 1, Integer::sum); if (!inspectionIds.contains(i.getSupersedesId())) finding(scanId, farmId, "inspection", i.getInspectionId(), "INT-15", "Warning", "Inspection supersedes a record that does not exist.", "Existing inspection", i.getSupersedesId(), null, null); }
        for (VerificationEntity v : verifications) if (!missing(v.getSupersedesId())) { supersedes.merge("verification:" + v.getSupersedesId(), 1, Integer::sum); if (!verificationIds.contains(v.getSupersedesId())) finding(scanId, farmId, "verification", v.getVerificationId(), "INT-15", "Warning", "Verification supersedes a record that does not exist.", "Existing verification", v.getSupersedesId(), null, null); }
        for (Map.Entry<String,Integer> e : supersedes.entrySet()) if (e.getValue() > 1) finding(scanId, farmId, e.getKey().split(":")[0], e.getKey().substring(e.getKey().indexOf(':')+1), "INT-15", "Warning", "More than one correction supersedes the same record.", "One superseding record", e.getValue() + " superseding records", null, null);

        // INT-16 — duplicate business keys, grouped by the owning record class.
        duplicateKeys(scanId, farmId, "quality_batch", "INT-16", "batchNumber", mapKeys(batches, QualityBatchEntity::getBatchNumber), "Warning");
        duplicateKeys(scanId, farmId, "quality_batch", "INT-16", "verificationCode", mapKeys(batches, QualityBatchEntity::getVerificationCode), "Critical");
        duplicateKeys(scanId, farmId, "batch_traceability", "INT-16", "batchNumber", mapKeys(traces, BatchTraceabilityEntity::getBatchNumber), "Warning");
        duplicateKeys(scanId, farmId, "batch_traceability", "INT-16", "verificationCode", mapKeys(traces, BatchTraceabilityEntity::getVerificationCode), "Critical");
        duplicateKeys(scanId, farmId, "master_batch", "INT-16", "masterBatchNumber", mapKeys(masters, MasterBatchTraceabilityEntity::getMasterBatchNumber), "Warning");
        duplicateKeys(scanId, farmId, "master_batch", "INT-16", "verificationCode", mapKeys(masters, MasterBatchTraceabilityEntity::getVerificationCode), "Critical");
        duplicateKeys(scanId, farmId, "supplier_commitment", "INT-16", "contractId", mapKeys(commitments, SupplierCommitmentEntity::getContractId), "Warning");

        // INT-17 — FK-less links. JSON links are parsed conservatively; malformed JSON is not inferred.
        Set<String> masterKeys = new HashSet<>(), traceIds = new HashSet<>(), batchNumbers = new HashSet<>(), supplierInspectionIds = new HashSet<>();
        for (MasterBatchTraceabilityEntity m : masters) { masterKeys.add(String.valueOf(m.getId())); masterKeys.add(clean(m.getMasterBatchNumber())); }
        for (BatchTraceabilityEntity t : traces) { traceIds.add(String.valueOf(t.getId())); batchNumbers.add(clean(t.getBatchNumber())); }
        for (SupplierInspectionEntity i : supplierInspections) supplierInspectionIds.add(String.valueOf(i.getId()));
        for (BatchTraceabilityEntity t : traces) if (!missing(t.getMasterBatchId()) && !masterKeys.contains(t.getMasterBatchId()))
            finding(scanId, farmId, "batch_traceability", t.getId(), "INT-17", "Warning", "Traceability record links to a missing master batch.", "Existing master batch", t.getMasterBatchId(), null, null);
        for (SupplierCommitmentEntity c : commitments) for (String linked : parseStringList(c.getLinkedBatchIds())) if (!traceIds.contains(linked) && !batchNumbers.contains(linked))
            finding(scanId, farmId, "supplier_commitment", c.getId(), "INT-17", "Warning", "Commitment links to a missing batch.", "Existing batch", linked, null, null);
        for (SupplierInspectionEntity i : supplierInspections) for (String linked : parseStringList(i.getLinkedBatchIds())) if (!traceIds.contains(linked) && !batchNumbers.contains(linked))
            finding(scanId, farmId, "supplier_inspection", i.getId(), "INT-17", "Warning", "Inspection links to a missing batch.", "Existing batch", linked, null, null);
        for (MasterBatchTraceabilityEntity m : masters) for (String linked : m.getBatchNumbers()) if (!batchNumbers.contains(linked))
            finding(scanId, farmId, "master_batch", m.getMasterBatchNumber(), "INT-17", "Warning", "Master batch list contains a missing child batch.", "Existing child batch", linked, null, null);
        for (PhotoEntity p : photos) if (!missing(p.getInspectionId()) && !supplierInspectionIds.contains(p.getInspectionId()))
            finding(scanId, farmId, "photo", p.getId(), "INT-17", "Warning", "Photo links to a missing supplier inspection.", "Existing inspection", p.getInspectionId(), null, null);

        // INT-18 — task completion, assignment order, and block reference.
        Set<Long> blockIds = new HashSet<>(); for (BlockEntity b : blocks) blockIds.add(b.getId());
        for (TaskEntity t : tasks) {
            boolean done = t.getStatus() == TaskEntity.TaskStatus.COMPLETED;
            if (done && t.getCompletedDate() == null) finding(scanId, farmId, "task", t.getId(), "INT-18", "Warning", "Completed task has no completed date.", "completedDate set", "Missing", null, null);
            if (!done && t.getCompletedDate() != null) finding(scanId, farmId, "task", t.getId(), "INT-18", "Warning", "Uncompleted task has a completed date.", "completedDate empty", String.valueOf(t.getCompletedDate()), null, null);
            if (t.getBlockId() != null && !blockIds.contains(t.getBlockId())) finding(scanId, farmId, "task", t.getId(), "INT-18", "Warning", "Task links to a missing block.", "Existing block", String.valueOf(t.getBlockId()), null, null);
        }
        for (TaskAssignmentHistoryEntity a : assignments) if (a.getUnassignedAt() != null && a.getUnassignedAt() < a.getAssignedAt())
            finding(scanId, farmId, "task_assignment", a.getId(), "INT-18", "Warning", "Assignment was unassigned before it was assigned.", "unassignedAt ≥ assignedAt", a.getUnassignedAt() + " < " + a.getAssignedAt(), null, null);

        // INT-19 — queue contradictions. Queue-only classes cannot expose the missing-queue case.
        checkSyncFlag(scanId, farmId, "suppliers", suppliers, SupplierEntity::getId, SupplierEntity::isSynced);
        checkSyncFlag(scanId, farmId, "quality_batch", batches, QualityBatchEntity::getId, QualityBatchEntity::isSynced);
        checkSyncFlag(scanId, farmId, "batch_traceability", traces, BatchTraceabilityEntity::getId, BatchTraceabilityEntity::isSynced);
        checkSyncFlag(scanId, farmId, "master_traceability", masters, MasterBatchTraceabilityEntity::getId, MasterBatchTraceabilityEntity::isSynced);
        checkStringSyncFlag(scanId, farmId, "certifications", programmes, CertificationEntity::getCertificationId, CertificationEntity::isSynced);
        checkStringSyncFlag(scanId, farmId, "audits", audits, AuditEntity::getAuditId, AuditEntity::isSynced);
        checkStringSyncFlag(scanId, farmId, "inspections", inspections, InspectionEntity::getInspectionId, InspectionEntity::isSynced);
        checkStringSyncFlag(scanId, farmId, "compliance_verifications", verifications, VerificationEntity::getVerificationId, VerificationEntity::isSynced);
        checkStringSyncFlag(scanId, farmId, "documents", documents, DocumentEntity::getDocumentId, DocumentEntity::isSynced);
        checkSyncFlag(scanId, farmId, "supplier_inspections", supplierInspections, SupplierInspectionEntity::getId, SupplierInspectionEntity::isSynced);
        checkSyncFlag(scanId, farmId, "supplier_commitments", commitments, SupplierCommitmentEntity::getId, SupplierCommitmentEntity::isSynced);
        for (SyncEntity q : db.syncDao().getPendingSyncItems()) if (q.getStatus() == SyncEntity.SyncStatus.FAILED && q.getAttemptCount() >= 7)
            finding(scanId, farmId, "sync_queue", q.getId(), "INT-19", "Warning", "Sync row has failed at least seven attempts.", "Fewer than 7 attempts", q.getAttemptCount() + " failed attempts", 6.0, (double) q.getAttemptCount());

        // INT-20 — denormalised strings and supplier average score.
        Map<Long, SupplierEntity> supplierById = new HashMap<>(); for (SupplierEntity s : suppliers) supplierById.put(s.getId(), s);
        for (RevenueEntity r : revenues) if (r.getBatchId() != null && batchById.containsKey(r.getBatchId()) && !same(r.getBatchNumber(), batchById.get(r.getBatchId()).getBatchNumber()))
            finding(scanId, farmId, "revenue", r.getId(), "INT-20", "Warning", "Sale batch number differs from the linked batch.", batchById.get(r.getBatchId()).getBatchNumber(), r.getBatchNumber(), null, null);
        for (ExpenseEntity e : expenses) if (e.getBatchId() != null && batchById.containsKey(e.getBatchId()) && !same(e.getBatchNumber(), batchById.get(e.getBatchId()).getBatchNumber()))
            finding(scanId, farmId, "expense", e.getId(), "INT-20", "Warning", "Expense batch number differs from the linked batch.", batchById.get(e.getBatchId()).getBatchNumber(), e.getBatchNumber(), null, null);
        for (BatchTraceabilityEntity t : traces) if (t.getSupplierId() != null && supplierById.containsKey(t.getSupplierId()) && !same(t.getSupplierName(), supplierById.get(t.getSupplierId()).getName()))
            finding(scanId, farmId, "batch_traceability", t.getId(), "INT-20", "Warning", "Traceability supplier name differs from the linked supplier.", supplierById.get(t.getSupplierId()).getName(), t.getSupplierName(), null, null);
        for (SupplierEntity s : suppliers) {
            double sum=0; int count=0; for (QualityBatchEntity b:batches) if(Objects.equals(b.getSupplierId(),s.getId())){sum+=b.getQualityScore();count++;}
            double avg=count==0?0:sum/count;
            if(count>0 && Math.abs(s.getAverageQualityScore()-avg)>0.01) finding(scanId,farmId,"supplier",s.getId(),"INT-20","Warning","Supplier average quality score differs from its batch average.",avg+"",s.getAverageQualityScore()+"",avg,s.getAverageQualityScore());
        }

        // INT-21 v1: snapshot self-consistency only; no portal version or weights are hardcoded.
        Map<Long, SupplierScoreSnapshotEntity> latest = new HashMap<>();
        for (SupplierScoreSnapshotEntity s : snapshots) {
            SupplierScoreSnapshotEntity old = latest.get(s.getSupplierId());
            if (old == null || s.getCalculatedAt() > old.getCalculatedAt()) latest.put(s.getSupplierId(), s);
        }
        for (SupplierScoreSnapshotEntity s : latest.values()) if (s.getOverall() != null
                && (s.getQuality() == null || s.getDelivery() == null || s.getTraceability() == null))
            finding(scanId, farmId, "supplier_score_snapshot", s.getId(), "INT-21", "Warning", "Overall trust score exists while a required dimension is missing.", "Quality, delivery and traceability present", "At least one required dimension is null", null, null);

        // INT-22 / INT-23 — document and programme target dates.
        Set<String> programmeIds = new HashSet<>(); for (CertificationEntity c : programmes) programmeIds.add(c.getCertificationId());
        for (DocumentEntity d : documents) {
            if (d.getExpiryDate() > 0 && d.getIssueDate() > 0 && d.getExpiryDate() < d.getIssueDate())
                finding(scanId, farmId, "document", d.getDocumentId(), "INT-22", "Warning", "Document expiry date precedes its issue date.", "expiryDate ≥ issueDate", d.getExpiryDate() + " < " + d.getIssueDate(), null, null);
            if (!missing(d.getCertificationId()) && !programmeIds.contains(d.getCertificationId()))
                finding(scanId, farmId, "document", d.getDocumentId(), "INT-22", "Warning", "Document references a missing certification programme.", "Existing programme", d.getCertificationId(), null, null);
        }
        for (CertificationEntity c : programmes) if ("NOT_STARTED".equals(c.getStatus().name()) && c.getTargetDate() > 0 && c.getTargetDate() < now)
            finding(scanId, farmId, "programme", c.getCertificationId(), "INT-23", "Warning", "Certification target date passed while the programme is not started.", "IN_PROGRESS before target", "NOT_STARTED after target", null, null);

        // INT-24 — block dates must be ordered and must not be future-dated without notes.
        for (BlockEntity b : blocks) {
            lifecycleDate(scanId, farmId, b, "clearedDate", b.getClearedDate());
            lifecycleDate(scanId, farmId, b, "plowedDate", b.getPlowedDate());
            lifecycleDate(scanId, farmId, b, "plantedDate", b.getPlantedDate());
            lifecycleDate(scanId, farmId, b, "lastHarvestDate", b.getLastHarvestDate());
            if (b.getClearedDate() != null && b.getPlowedDate() != null && b.getPlowedDate() < b.getClearedDate())
                finding(scanId, farmId, "block", b.getId(), "INT-24", "Warning", "Block was plowed before it was cleared.", "plowedDate ≥ clearedDate", b.getPlowedDate() + " < " + b.getClearedDate(), null, null);
            if (b.getPlowedDate() != null && b.getPlantedDate() != null && b.getPlantedDate() < b.getPlowedDate())
                finding(scanId, farmId, "block", b.getId(), "INT-24", "Warning", "Block was planted before it was plowed.", "plantedDate ≥ plowedDate", b.getPlantedDate() + " < " + b.getPlowedDate(), null, null);
        }
    }

    private void lifecycleDate(long scanId, long farmId, BlockEntity b, String field, Long value) {
        if (value != null && b.getCreatedAt() > 0 && value > b.getCreatedAt() && missing(b.getNotes()))
            finding(scanId, farmId, "block", b.getId(), "INT-24", "Warning", "Block " + field + " is later than its creation time and has no explanation.", "Date ≤ createdAt or notes explain it", String.valueOf(value), null, null);
    }

    private interface IdValue<T> { long id(T value); }
    private interface SyncFlag<T> { boolean synced(T value); }
    private <T> void checkSyncFlag(long scanId, long farmId, String entityType, List<T> rows, IdValue<T> ids, SyncFlag<T> flags) {
        for (T row : rows) {
            long id = ids.id(row); boolean synced = flags.synced(row); List<SyncEntity> queue = db.syncDao().getByEntity(entityType, id);
            boolean any = !queue.isEmpty(), pending = false;
            for (SyncEntity q : queue) pending |= q.getStatus() == SyncEntity.SyncStatus.PENDING || q.getStatus() == SyncEntity.SyncStatus.FAILED;
            if (!synced && !any) finding(scanId, farmId, entityType, id, "INT-19", "Warning", "Unsynced entity has no queue row and will not upload.", "Queue row present", "No queue row", null, null);
            if (synced && pending) finding(scanId, farmId, entityType, id, "INT-19", "Warning", "Entity is marked synced while its queue row is pending or failed.", "No pending or failed row", "Pending/failed queue row", null, null);
        }
    }
    private <T> void checkStringSyncFlag(long scanId, long farmId, String entityType, List<T> rows,
            java.util.function.Function<T, String> ids, SyncFlag<T> flags) {
        for (T row : rows) {
            String id = ids.apply(row); boolean synced = flags.synced(row); List<SyncEntity> queue = db.syncDao().getByStringEntity(entityType, id);
            boolean any = !queue.isEmpty(), pending = false;
            for (SyncEntity q : queue) pending |= q.getStatus() == SyncEntity.SyncStatus.PENDING || q.getStatus() == SyncEntity.SyncStatus.FAILED;
            if (!synced && !any) finding(scanId, farmId, entityType, id, "INT-19", "Warning", "Unsynced entity has no queue row and will not upload.", "Queue row present", "No queue row", null, null);
            if (synced && pending) finding(scanId, farmId, entityType, id, "INT-19", "Warning", "Entity is marked synced while its queue row is pending or failed.", "No pending or failed row", "Pending/failed queue row", null, null);
        }
    }
    private interface KeyValue<T> { String key(T value); }
    private <T> Map<String, List<T>> mapKeys(List<T> rows, KeyValue<T> keyFunction) {
        Map<String, List<T>> grouped = new HashMap<>();
        for (T row : rows) { String key = clean(keyFunction.key(row)); if (!key.isEmpty()) grouped.computeIfAbsent(key.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(row); }
        return grouped;
    }
    private <T> void duplicateKeys(long scanId, long farmId, String entityType, String rule, String label,
            Map<String, List<T>> grouped, String severity) {
        for (Map.Entry<String, List<T>> e : grouped.entrySet()) if (e.getValue().size() > 1)
            for (T row : e.getValue()) {
                Object id = row instanceof QualityBatchEntity ? ((QualityBatchEntity) row).getId()
                        : row instanceof BatchTraceabilityEntity ? ((BatchTraceabilityEntity) row).getId()
                        : row instanceof MasterBatchTraceabilityEntity ? ((MasterBatchTraceabilityEntity) row).getMasterBatchNumber()
                        : ((SupplierCommitmentEntity) row).getId();
                finding(scanId, farmId, entityType, id, rule, severity, "Duplicate " + label + ".", "Unique " + label, e.getKey(), null, null);
            }
    }
    private static List<String> parseStringList(String json) {
        if (missing(json)) return Collections.emptyList();
        try {
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(json);
            List<String> out = new ArrayList<>();
            if (element.isJsonArray()) for (com.google.gson.JsonElement v : element.getAsJsonArray()) out.add(v.isJsonPrimitive() ? v.getAsString() : GSON.toJson(v));
            return out;
        } catch (Exception ignored) { return Collections.emptyList(); }
    }
    private static boolean same(String a, String b) { return clean(a).equalsIgnoreCase(clean(b)); }

    private void finding(long scanId, long farmId, String type, Object entityId, String code, String severity,
            String description, String expected, String actual, Double expectedNumeric, Double actualNumeric) {
        String id = String.valueOf(entityId);
        IntegrityFindingEntity f = db.integrityFindingDao().findActive(code, id, farmId);
        long now = System.currentTimeMillis();
        boolean isNew = f == null;
        if (f == null) {
            f = new IntegrityFindingEntity(); f.setFarmId(farmId); f.setEntityType(type); f.setEntityId(id);
            f.setRuleCode(code); f.setStatus("OPEN"); f.setDetectedAt(now); f.setHistoryJson("[]");
            List<Map<String, Object>> history = new ArrayList<>(); Map<String, Object> event = new LinkedHashMap<>();
            event.put("status", "OPEN"); event.put("actor", "scan"); event.put("at", now); event.put("notes", "Detected by integrity scan"); history.add(event);
            f.setHistoryJson(GSON.toJson(history));
        }
        boolean changed = isNew || !Objects.equals(f.getSeverity(), severity)
                || !Objects.equals(f.getDescription(), description)
                || !Objects.equals(f.getExpectedValue(), expected)
                || !Objects.equals(f.getActualValue(), actual)
                || !Objects.equals(f.getExpectedNumeric(), expectedNumeric)
                || !Objects.equals(f.getActualNumeric(), actualNumeric);
        if (!changed) return;
        f.setScanId(scanId); f.setSeverity(severity); f.setDescription(description); f.setExpectedValue(expected); f.setActualValue(actual);
        f.setExpectedNumeric(expectedNumeric); f.setActualNumeric(actualNumeric); f.setUpdatedAt(now); f.setSynced(false);
        if (isNew) {
            long findingId = db.integrityFindingDao().insert(f);
            syncManager.queueForSync("integrity_finding", findingId, SyncEntity.SyncAction.CREATE);
        } else {
            db.integrityFindingDao().update(f);
            syncManager.queueForSync("integrity_finding", f.getId(), SyncEntity.SyncAction.UPDATE);
        }
    }

    private boolean settled(String type, long id, boolean isSynced) {
        for (SyncEntity q : db.syncDao().getByEntity(type, id))
            if (q.getStatus() == SyncEntity.SyncStatus.PENDING || q.getStatus() == SyncEntity.SyncStatus.FAILED) return false;
        return isSynced;
    }

    private interface SupplierField { String get(SupplierEntity value); }
    private void duplicateSuppliers(long scanId, long farmId, List<SupplierEntity> suppliers, String field, SupplierField getter, boolean phone) {
        Map<String, List<SupplierEntity>> grouped = new HashMap<>();
        for (SupplierEntity s : suppliers) {
            String v = clean(getter.get(s)); if (v.isEmpty()) continue;
            if (phone) { v = v.replaceAll("[^0-9]", ""); if (v.startsWith("225") && v.length() > 10) v = v.substring(3); }
            grouped.computeIfAbsent(v.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(s);
        }
        for (Map.Entry<String, List<SupplierEntity>> e : grouped.entrySet()) if (e.getValue().size() > 1) {
            boolean coops = phone; for (SupplierEntity s : e.getValue()) coops &= s.getType() == SupplierEntity.SupplierType.COOPERATIVE;
            if (coops) continue;
            for (SupplierEntity s : e.getValue()) finding(scanId, farmId, "supplier", s.getId(), "INT-04", "Warning", "Potential duplicate supplier " + field + ".", "Unique " + field, e.getKey(), null, null);
        }
    }
    private static boolean hasDelivery(long id, List<QualityBatchEntity> batches) { for (QualityBatchEntity b : batches) if (Objects.equals(b.getSupplierId(), id)) return true; return false; }
    private int countForRun(long farmId, long scanId) { int n=0; for (IntegrityFindingEntity f:db.integrityFindingDao().listByFarmSync(farmId)) if(f.getScanId()==scanId)n++; return n; }
    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static boolean missing(String s) { return clean(s).isEmpty(); }
}
