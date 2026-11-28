package com.afrinuts.farmos.ui.integrity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.IntegrityFindingEntity;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierCommitmentEntity;
import com.afrinuts.farmos.data.local.entity.SupplierInspectionEntity;
import com.afrinuts.farmos.data.local.entity.SyncEntity;
import com.afrinuts.farmos.data.local.entity.UserEntity;
import com.afrinuts.farmos.data.sync.SyncManager;
import com.afrinuts.farmos.ui.BaseActivity;
import com.afrinuts.farmos.utils.AccessMap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Phone evidence and human-resolution screen for one finding. */
public class IntegrityFindingDetailActivity extends BaseActivity {
    public static final String EXTRA_FINDING_ID = "integrity_finding_id";
    private static final int CREAM = 0xFFF6F1E7, WHITE = Color.WHITE, INK = 0xFF1C1B1A;
    private static final int MUTED = 0xFF59544C, GREEN = 0xFF0E6B3C, RED = 0xFFB3402A;
    private static final int PALE_RED = 0xFFF8E4DF, PALE_GREEN = 0xFFE3F0E7, PALE_GOLD = 0xFFF5ECD3, GOLD = 0xFF916313;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private long findingId;
    private IntegrityFindingEntity finding;
    private long farmId;
    private final List<LinkedRecord> linkedRecords = new ArrayList<>();
    private LinearLayout root;
    private EditText notes;
    private TextView resolveButton, rejectButton, underReviewButton;
    private boolean notesAllowed;
    private boolean creatorBlocked;

    @Override protected boolean checkRoleAccess() {
        return AccessMap.canOpenScreen("IntegrityFindingDetailActivity", sessionManager.getUserRole());
    }
    @Override protected String getAccessDeniedMessage() { return "You cannot open finding details"; }

    @Override protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!sessionManager.isLoggedIn() || !checkRoleAccess()) return;
        findingId = getIntent().getLongExtra(EXTRA_FINDING_ID, 0L);
        db = AppDatabase.getInstance(this);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(CREAM);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22), dp(22), dp(22), dp(32)); scroll.addView(root);
        setContentView(scroll);
        io.execute(() -> {
            IntegrityFindingEntity loaded = db.integrityFindingDao().getById(findingId);
            if (loaded == null) { runOnUiThread(this::finish); return; }
            List<LinkedRecord> records = loadLinkedRecords(loaded);
            boolean isCreator = currentUserCreatedSource(loaded);
            runOnUiThread(() -> { finding = loaded; farmId = loaded.getFarmId(); creatorBlocked = isCreator; linkedRecords.clear(); linkedRecords.addAll(records); render(); });
        });
    }

    private void render() {
        root.removeAllViews();
        android.widget.FrameLayout header = new android.widget.FrameLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(54)));
        ImageButton back = new ImageButton(this); back.setImageResource(R.drawable.ic_finance_back_chevron);
        back.setBackground(shape(WHITE, 999, 0xFFE9E1CF));
        header.addView(back, new android.widget.FrameLayout.LayoutParams(dp(44), dp(44), Gravity.START | Gravity.CENTER_VERTICAL));
        back.setOnClickListener(v -> finish());
        TextView title = text("Finding detail", 17, INK, true); title.setGravity(Gravity.CENTER);
        header.addView(title, new android.widget.FrameLayout.LayoutParams(-1, -1)); root.addView(header);

        addHero(); addExpectedActual(); addRecords();
        TextView noAuto = text("No automatic correction. A person resolves this finding with notes — the record is never rewritten by the system.", 17, MUTED, false);
        noAuto.setPadding(0, dp(2), 0, dp(8)); root.addView(noAuto);
        if (creatorBlocked) root.addView(text("You recorded the underlying record. Another authorised person must resolve this finding.", 15, RED, true));

        root.addView(text("Resolution notes — required", 17, INK, true));
        notes = new EditText(this); notes.setTextSize(16); notes.setTextColor(INK); notes.setGravity(Gravity.TOP | Gravity.START);
        notes.setHint("Enter resolution notes before continuing"); notes.setHintTextColor(0xFF8E887F);
        notes.setMinLines(3); notes.setPadding(dp(16), dp(14), dp(16), dp(14)); notes.setBackground(shape(CREAM, 18, 0xFFE9E1CF));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(128)); np.topMargin = dp(8); root.addView(notes, np);
        root.addView(text("Notes are required before a finding can be resolved or rejected.", 15, MUTED, false));

        LinearLayout actions = horizontal(); LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(58)); ap.topMargin = dp(14); root.addView(actions, ap);
        underReviewButton = button("Mark under review", PALE_GOLD, INK);
        resolveButton = button("Resolve", GREEN, WHITE);
        actions.addView(underReviewButton, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -1, 1f); rp.leftMargin = dp(10); actions.addView(resolveButton, rp);
        rejectButton = text("Reject finding", 15, RED, true); rejectButton.setGravity(Gravity.CENTER); root.addView(rejectButton);
        notes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) { updateActionAvailability(); }
            @Override public void afterTextChanged(Editable e) {}
        });
        underReviewButton.setOnClickListener(v -> transition("UNDER_REVIEW", notes.getText().toString().trim()));
        resolveButton.setOnClickListener(v -> transition("RESOLVED", notes.getText().toString().trim()));
        rejectButton.setOnClickListener(v -> transition("REJECTED", notes.getText().toString().trim()));
        updateActionAvailability();
        addHistory();
    }

    private void addHero() {
        LinearLayout hero = card(); LinearLayout top = horizontal();
        boolean critical = "Critical".equalsIgnoreCase(finding.getSeverity());
        TextView icon = text("⚠", 32, critical ? RED : GOLD, true); icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(critical ? PALE_RED : PALE_GOLD, 999, Color.TRANSPARENT));
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(78), dp(78)); top.addView(icon, ip);
        LinearLayout name = new LinearLayout(this); name.setOrientation(LinearLayout.VERTICAL); name.setPadding(dp(14), 0, 0, 0);
        name.addView(text(finding.getRuleCode() + " — " + finding.getDescription(), 21, INK, true));
        name.addView(text("Detected " + date(finding.getDetectedAt()) + " · Rule " + finding.getRuleCode() + " · Severity " + finding.getSeverity(), 15, MUTED, false));
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1f)); hero.addView(top);
        LinearLayout pills = horizontal(); LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(34)); pp.topMargin = dp(16); hero.addView(pills, pp);
        pills.addView(pill(finding.getSeverity().toUpperCase(Locale.ROOT), critical ? PALE_RED : PALE_GOLD, critical ? RED : GOLD));
        TextView state = pill(finding.getStatus().replace('_', ' '), statusBg(finding.getStatus()), statusColor(finding.getStatus()));
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(-2, -1); stp.leftMargin = dp(8); pills.addView(state, stp);
        addSectionCard(hero);
    }

    private void addExpectedActual() {
        LinearLayout c = card(); c.addView(text("Expected vs actual", 21, INK, true));
        row(c, "Expected", safe(finding.getExpectedValue()));
        row(c, "Actual", safe(finding.getActualValue()));
        String difference = finding.getExpectedNumeric() != null && finding.getActualNumeric() != null
                ? format(Math.abs(finding.getExpectedNumeric() - finding.getActualNumeric()), finding.getExpectedValue()) : "—";
        row(c, "Difference", difference);
        row(c, "Status", finding.getStatus().replace('_', ' '));
        row(c, "Detected", dateTime(finding.getDetectedAt()));
        addSectionCard(c);
    }

    private void addRecords() {
        LinearLayout c = card(); c.addView(text("Records behind this finding", 21, INK, true));
        for (LinkedRecord item : linkedRecords) recordRow(c, item.label, item.value, item.openable ? () -> openRecord(item) : null);
        if (linkedRecords.isEmpty()) recordRow(c, finding.getEntityType(), finding.getEntityId(), null);
        c.addView(text("Tap a record to open it.", 15, MUTED, false)); addSectionCard(c);
    }

    private List<LinkedRecord> loadLinkedRecords(IntegrityFindingEntity f) {
        List<LinkedRecord> out = new ArrayList<>();
        if ("master_batch".equals(f.getEntityType())) {
            MasterBatchTraceabilityEntity master = db.masterTraceabilityDao().getByMasterBatchNumber(f.getEntityId());
            if (master != null) {
                out.add(new LinkedRecord("Master batch", master.getMasterBatchNumber() + " · " + format(master.getTotalWeightKg(), "kg"), "master", master.getId()));
                for (BatchTraceabilityEntity child : db.batchTraceabilityDao().getChildrenOfMaster(String.valueOf(master.getId()))) {
                    QualityBatchEntity batch = db.qualityBatchDao().getBatchByNumber(child.getBatchNumber());
                    out.add(new LinkedRecord("Child batch", child.getBatchNumber() + " · " + format(child.getWeightKg(), "kg"), batch == null ? null : "batch", batch == null ? 0 : batch.getId()));
                }
            }
        } else if ("quality_batch".equals(f.getEntityType())) {
            QualityBatchEntity batch = db.qualityBatchDao().getBatchById(parseId(f.getEntityId()));
            if (batch == null) batch = db.qualityBatchDao().getBatchByNumber(f.getEntityId());
            if (batch != null) out.add(new LinkedRecord("Batch", batch.getBatchNumber() + " · " + format(batch.getWeightKg(), "kg"), "batch", batch.getId()));
        } else if ("supplier".equals(f.getEntityType())) {
            long id = parseId(f.getEntityId());
            com.afrinuts.farmos.data.local.entity.SupplierEntity supplier = db.supplierDao().getSupplierById(id);
            if (supplier != null) out.add(new LinkedRecord("Supplier", supplier.getName(), "supplier", id));
        } else if ("block".equals(f.getEntityType())) {
            out.add(new LinkedRecord("Block", f.getEntityId(), "block", parseId(f.getEntityId())));
        } else if ("supplier_inspection".equals(f.getEntityType())) {
            SupplierInspectionEntity inspection = db.supplierInspectionDao().getById(parseId(f.getEntityId()));
            out.add(new LinkedRecord("Supplier inspection", f.getEntityId(), null, 0));
        } else {
            out.add(new LinkedRecord(f.getEntityType(), f.getEntityId(), null, 0));
        }
        return out;
    }

    private void openRecord(LinkedRecord item) {
        if ("master".equals(item.type)) openMaster(item.id);
        else if ("batch".equals(item.type)) openBatch(item.id);
        else if ("supplier".equals(item.type)) startActivity(new Intent(this,
                com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.class).putExtra(com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.EXTRA_SUPPLIER_ID, item.id));
        else if ("block".equals(item.type)) startActivity(new Intent(this, com.afrinuts.farmos.ui.blocks.BlockDetailActivity.class)
                .putExtra(com.afrinuts.farmos.ui.blocks.BlockDetailActivity.EXTRA_BLOCK_ID, item.id));
    }

    private void addHistory() {
        LinearLayout c = card(); c.addView(text("History — append-only", 21, INK, true));
        try {
            JsonArray arr = JsonParser.parseString(finding.getHistoryJson() == null ? "[]" : finding.getHistoryJson()).getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject e = arr.get(i).getAsJsonObject();
                String status = e.has("status") ? e.get("status").getAsString() : "Event";
                String actor = e.has("actor") ? e.get("actor").getAsString() : "—";
                long at = e.has("at") ? e.get("at").getAsLong() : 0;
                row(c, displayStatus(status), dateTime(at) + " · " + actor);
            }
        } catch (Exception ignored) { row(c, "Detected", dateTime(finding.getDetectedAt()) + " · Scan run"); }
        if (!containsStatus("UNDER_REVIEW")) row(c, "Under review", "Not yet");
        if (!containsStatus("RESOLVED") && !containsStatus("REJECTED")) row(c, "Resolved", "Not yet");
        c.addView(text("Resolving never deletes the fact the finding happened — the history stays on the record.", 16, MUTED, false)); addSectionCard(c);
    }

    private void updateActionAvailability() {
        if (resolveButton == null || finding == null) return;
        boolean canResolve = AccessMap.canResolveIntegrityFinding(sessionManager.getUserRole()) && !creatorBlocked;
        if (sessionManager.getUserRole() == UserEntity.UserRole.COMPLIANCE_OFFICER && !isComplianceFinding()) canResolve = false;
        if ("Critical".equalsIgnoreCase(finding.getSeverity()) && sessionManager.getUserRole() == UserEntity.UserRole.COMPLIANCE_OFFICER) canResolve = false;
        notesAllowed = !notes.getText().toString().trim().isEmpty();
        boolean open = "OPEN".equals(finding.getStatus()) || "UNDER_REVIEW".equals(finding.getStatus());
        resolveButton.setEnabled(canResolve && open && notesAllowed); resolveButton.setAlpha(resolveButton.isEnabled() ? 1f : .45f);
        rejectButton.setEnabled(canResolve && open && notesAllowed); rejectButton.setAlpha(rejectButton.isEnabled() ? 1f : .45f);
        underReviewButton.setEnabled(canResolve && "OPEN".equals(finding.getStatus())); underReviewButton.setAlpha(underReviewButton.isEnabled() ? 1f : .45f);
        if (!canResolve) rejectButton.setVisibility(View.GONE);
    }

    private void transition(String status, String note) {
        if ("RESOLVED".equals(status) || "REJECTED".equals(status)) {
            if (note.trim().isEmpty()) { Toast.makeText(this, "Resolution notes are required", Toast.LENGTH_SHORT).show(); return; }
            if (!canResolveNow()) { Toast.makeText(this, "You cannot resolve this finding", Toast.LENGTH_SHORT).show(); return; }
        }
        io.execute(() -> {
            long now = System.currentTimeMillis();
            JsonArray arr;
            try { arr = JsonParser.parseString(finding.getHistoryJson() == null ? "[]" : finding.getHistoryJson()).getAsJsonArray(); }
            catch (Exception e) { arr = new JsonArray(); }
            JsonObject entry = new JsonObject(); entry.addProperty("status", status); entry.addProperty("actor", safe(sessionManager.getUserName()));
            entry.addProperty("at", now); entry.addProperty("notes", note); arr.add(entry);
            finding.setHistoryJson(new Gson().toJson(arr)); finding.setStatus(status); finding.setUpdatedAt(now); finding.setSynced(false);
            if ("RESOLVED".equals(status) || "REJECTED".equals(status)) { finding.setResolvedAt(now); finding.setResolvedBy(safe(sessionManager.getUserName())); finding.setResolutionNotes(note); }
            db.integrityFindingDao().update(finding);
            SyncManager sync = SyncManager.getInstance(getApplicationContext()); sync.setSyncCenterContext(farmId, null);
            sync.queueForSync("integrity_finding", finding.getId(), SyncEntity.SyncAction.UPDATE); sync.triggerAutoSync();
            runOnUiThread(() -> { Toast.makeText(this, "Finding marked " + displayStatus(status).toLowerCase(Locale.ROOT), Toast.LENGTH_SHORT).show(); finish(); });
        });
    }

    private boolean canResolveNow() {
        UserEntity.UserRole role = sessionManager.getUserRole();
        if (!AccessMap.canResolveIntegrityFinding(role) || creatorBlocked) return false;
        if (role == UserEntity.UserRole.COMPLIANCE_OFFICER && (!isComplianceFinding() || "Critical".equalsIgnoreCase(finding.getSeverity()))) return false;
        return true;
    }
    private boolean isComplianceFinding() { String c = finding.getRuleCode(); return c != null && (c.equals("INT-03") || c.equals("INT-05") || c.equals("INT-13") || c.equals("INT-14") || c.equals("INT-15") || c.equals("INT-22") || c.equals("INT-23")); }
    private boolean containsStatus(String status) { try { JsonArray a = JsonParser.parseString(finding.getHistoryJson()).getAsJsonArray(); for (int i=0;i<a.size();i++) if (status.equals(a.get(i).getAsJsonObject().get("status").getAsString())) return true; } catch (Exception ignored) {} return false; }
    private boolean currentUserCreatedSource(IntegrityFindingEntity f) {
        String entity = f.getEntityType(); long id = parseId(f.getEntityId());
        String createdBy = null;
        if ("supplier_inspection".equals(entity)) {
            SupplierInspectionEntity record = db.supplierInspectionDao().getById(id);
            if (record != null) createdBy = record.getCreatedBy();
        } else if ("supplier_commitments".equals(entity)) {
            SupplierCommitmentEntity record = db.supplierCommitmentDao().getById(id);
            if (record != null) createdBy = record.getCreatedBy();
        }
        if (createdBy == null) return false;
        return createdBy.equalsIgnoreCase(String.valueOf(sessionManager.getUserName()))
                || createdBy.equalsIgnoreCase(String.valueOf(sessionManager.getUserUid()));
    }
    private String displayStatus(String s) { return s.replace('_', ' ').toLowerCase(Locale.ROOT).replaceFirst("^.", s.substring(0,1).toUpperCase(Locale.ROOT)); }

    private void recordRow(LinearLayout c, String label, String value, Runnable click) {
        LinearLayout r = horizontal(); TextView left = text(label, 15, MUTED, true), right = text(value, 16, INK, true); right.setGravity(Gravity.RIGHT);
        r.addView(left, new LinearLayout.LayoutParams(dp(105), -2)); r.addView(right, new LinearLayout.LayoutParams(0, -2, 1f));
        if (click != null) { r.setClickable(true); r.setFocusable(true); r.setOnClickListener(v -> click.run()); }
        c.addView(r); addDivider(c);
    }
    private void row(LinearLayout c, String label, String value) { recordRow(c, label, value, null); }
    private void openMaster(long id) { startActivity(new Intent(this, com.afrinuts.farmos.ui.traceability.MasterTraceabilityDetailActivity.class).putExtra(com.afrinuts.farmos.ui.traceability.MasterTraceabilityDetailActivity.EXTRA_MASTER_ID, id)); }
    private void openBatch(long id) { startActivity(new Intent(this, com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity.class).putExtra(com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity.EXTRA_BATCH_ID, id)); }
    private String format(double n, String context) { String unit = context != null && context.toLowerCase(Locale.ROOT).contains("fcfa") ? "FCFA" : "kg"; return String.format(Locale.getDefault(), "%,.0f %s", n, unit); }
    private long parseId(String id) { try { return Long.parseLong(id); } catch (Exception e) { return -1; } }
    private String safe(String s) { return s == null || s.trim().isEmpty() ? "—" : s; }
    private String date(long t) { return t <= 0 ? "—" : new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(t)); }
    private String dateTime(long t) { return t <= 0 ? "—" : new SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault()).format(new Date(t)); }
    private int statusBg(String s) { if ("RESOLVED".equals(s)) return PALE_GREEN; if ("UNDER_REVIEW".equals(s)) return PALE_GOLD; if ("REJECTED".equals(s)) return 0xFFE9E6E0; return PALE_RED; }
    private int statusColor(String s) { if ("RESOLVED".equals(s)) return 0xFF0A5530; if ("UNDER_REVIEW".equals(s)) return GOLD; if ("REJECTED".equals(s)) return MUTED; return RED; }
    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String s, int sp, int color, boolean bold) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if (bold) v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); v.setPadding(0, dp(6), 0, dp(6)); return v; }
    private LinearLayout horizontal() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout card() { LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18), dp(16), dp(18), dp(16)); c.setBackground(shape(WHITE, 24, 0xFFEFE6D7)); return c; }
    private void addSectionCard(LinearLayout c) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(12); root.addView(c, p); }
    private TextView pill(String s, int bg, int fg) { TextView t = text(s, 13, fg, true); t.setGravity(Gravity.CENTER); t.setPadding(dp(12), 0, dp(12), 0); t.setBackground(shape(bg, 999, Color.TRANSPARENT)); return t; }
    private TextView button(String s, int bg, int fg) { TextView v = text(s, 16, fg, true); v.setGravity(Gravity.CENTER); v.setBackground(shape(bg, 999, Color.TRANSPARENT)); return v; }
    private GradientDrawable shape(int color, int radius, int stroke) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (stroke != Color.TRANSPARENT) d.setStroke(dp(1), stroke); return d; }
    private void addDivider(LinearLayout c) { View d = new View(this); d.setBackgroundColor(0xFFE9E1CF); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1)); p.topMargin = dp(8); p.bottomMargin = dp(7); c.addView(d, p); }
    private static final class LinkedRecord {
        final String label, value, type; final long id; final boolean openable;
        LinkedRecord(String label, String value, String type, long id) { this.label = label; this.value = value; this.type = type; this.id = id; this.openable = type != null && id > 0; }
    }
    @Override protected void onDestroy() { super.onDestroy(); io.shutdown(); }
}
