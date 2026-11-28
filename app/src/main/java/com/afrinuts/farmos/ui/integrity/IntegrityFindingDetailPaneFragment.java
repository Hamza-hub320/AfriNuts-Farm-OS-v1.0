package com.afrinuts.farmos.ui.integrity;

import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.afrinuts.farmos.utils.AccessMap;
import com.afrinuts.farmos.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet right-pane rendering for the same finding detail and resolution flow as phone. */
public class IntegrityFindingDetailPaneFragment extends Fragment {
    private static final String ARG_ID = "finding_id";
    private static final int WHITE = Color.WHITE, INK = 0xFF1C1B1A, MUTED = 0xFF59544C;
    private static final int GREEN = 0xFF0E6B3C, RED = 0xFFB3402A, GOLD = 0xFF916313;
    private static final int CREAM = 0xFFF6F1E7, PALE_RED = 0xFFF8E4DF, PALE_GREEN = 0xFFE3F0E7, PALE_GOLD = 0xFFF5ECD3;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private SessionManager session;
    private Context appContext;
    private long findingId;
    private IntegrityFindingEntity finding;
    private List<LinkedRecord> records = new ArrayList<>();
    private LinearLayout root, actions;
    private EditText notes;
    private TextView resolve, reject, review;
    private boolean creatorBlocked;

    public static IntegrityFindingDetailPaneFragment newInstance(long id) {
        IntegrityFindingDetailPaneFragment f = new IntegrityFindingDetailPaneFragment();
        Bundle args = new Bundle(); args.putLong(ARG_ID, id); f.setArguments(args); return f;
    }

    @Nullable @Override public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        findingId = requireArguments().getLong(ARG_ID);
        session = new SessionManager(requireContext());
        appContext = requireContext().getApplicationContext();
        db = AppDatabase.getInstance(appContext);
        ScrollView scroll = new ScrollView(requireContext()); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.TRANSPARENT);
        root = new LinearLayout(requireContext()); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24), dp(22), dp(24), dp(24)); scroll.addView(root);
        loadFinding();
        return scroll;
    }

    private void loadFinding() {
        io.execute(() -> {
            IntegrityFindingEntity loaded = db.integrityFindingDao().getById(findingId);
            boolean blocked = loaded != null && currentUserCreatedSource(loaded);
            List<LinkedRecord> loadedRecords = loaded == null ? new ArrayList<>() : loadLinkedRecords(loaded);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (loaded == null) { root.addView(text("Finding is no longer available.", 16, MUTED, false)); return; }
                finding = loaded; creatorBlocked = blocked; records = loadedRecords; render();
            });
        });
    }

    private void render() {
        root.removeAllViews();
        root.addView(hero());
        LinearLayout evidence = sectionCard("Expected vs actual", true);
        addRow(evidence, "Expected", safe(finding.getExpectedValue()));
        addRow(evidence, "Actual", safe(finding.getActualValue()));
        String difference = finding.getExpectedNumeric() != null && finding.getActualNumeric() != null
                ? format(Math.abs(finding.getExpectedNumeric() - finding.getActualNumeric()), finding.getExpectedValue()) : "—";
        addRow(evidence, "Difference", difference); addRow(evidence, "Status", displayStatus(finding.getStatus()));
        addRow(evidence, "Detected", dateTime(finding.getDetectedAt()));

        LinearLayout records = sectionCard("Records behind this finding", true);
        for (LinkedRecord item : this.records) {
            LinearLayout row = row(item.label, item.value);
            if (item.openable) { row.setClickable(true); row.setFocusable(true); row.setOnClickListener(v -> openRecord(item)); }
            records.addView(row); divider(records);
        }
        records.addView(text("Tap a record to open it.", 15, MUTED, false));
        TextView note = text("No automatic correction. A person resolves this finding with notes — the record is never rewritten by the system.", 16, MUTED, false);
        LinearLayout.LayoutParams noteP = new LinearLayout.LayoutParams(-1, -2); noteP.topMargin = dp(14); root.addView(note, noteP);
        if (creatorBlocked) root.addView(text("You recorded the underlying record. Another authorised person must resolve this finding.", 14, RED, true));
        root.addView(text("Resolution notes — required", 16, INK, true));
        notes = new EditText(requireContext()); notes.setTextSize(16); notes.setTextColor(INK); notes.setGravity(Gravity.TOP | Gravity.START);
        notes.setHint("Enter resolution notes before continuing"); notes.setHintTextColor(0xFF8E887F);
        notes.setMinLines(3); notes.setPadding(dp(16), dp(14), dp(16), dp(14)); notes.setBackground(shape(CREAM, 18, 0xFFE9E1CF));
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(118)); np.topMargin = dp(8); root.addView(notes, np);
        root.addView(text("Notes are required before a finding can be resolved or rejected.", 14, MUTED, false));
        actions = horizontal(); LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, dp(54)); ap.topMargin = dp(12); root.addView(actions, ap);
        review = action("Mark under review", PALE_GOLD, INK); resolve = action("Resolve", GREEN, WHITE);
        actions.addView(review, new LinearLayout.LayoutParams(0, -1, 1f)); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -1, 1f); rp.leftMargin = dp(10); actions.addView(resolve, rp);
        reject = text("Reject finding", 14, RED, true); reject.setGravity(Gravity.CENTER); root.addView(reject);
        notes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateActions(); }
            @Override public void afterTextChanged(Editable e) {}
        });
        review.setOnClickListener(v -> transition("UNDER_REVIEW", ""));
        resolve.setOnClickListener(v -> transition("RESOLVED", notes.getText().toString().trim()));
        reject.setOnClickListener(v -> transition("REJECTED", notes.getText().toString().trim()));
        addHistory(); updateActions();
    }

    private View hero() {
        LinearLayout c = new LinearLayout(requireContext()); c.setOrientation(LinearLayout.VERTICAL); LinearLayout line = horizontal();
        boolean critical = "Critical".equalsIgnoreCase(finding.getSeverity());
        TextView icon = text("⚠", 30, critical ? RED : GOLD, true); icon.setGravity(Gravity.CENTER); icon.setBackground(shape(critical ? PALE_RED : PALE_GOLD, 999, Color.TRANSPARENT));
        line.addView(icon, new LinearLayout.LayoutParams(dp(70), dp(70)));
        LinearLayout details = new LinearLayout(requireContext()); details.setOrientation(LinearLayout.VERTICAL); details.setPadding(dp(14), 0, 0, 0);
        details.addView(text(finding.getRuleCode() + " — " + finding.getDescription(), 20, INK, true));
        details.addView(text("Detected " + date(finding.getDetectedAt()) + " · Rule " + finding.getRuleCode() + " · Severity " + finding.getSeverity(), 14, MUTED, false));
        line.addView(details, new LinearLayout.LayoutParams(0, -2, 1f)); c.addView(line);
        LinearLayout pills = horizontal(); LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(32)); pp.topMargin = dp(12); c.addView(pills, pp);
        pills.addView(pill(finding.getSeverity().toUpperCase(Locale.ROOT), critical ? PALE_RED : PALE_GOLD, critical ? RED : GOLD));
        TextView status = pill(finding.getStatus().replace('_', ' '), statusBg(finding.getStatus()), statusColor(finding.getStatus()));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -1); sp.leftMargin = dp(8); pills.addView(status, sp);
        return c;
    }

    private LinearLayout sectionCard(String title, boolean attach) {
        LinearLayout c = new LinearLayout(requireContext()); c.setOrientation(LinearLayout.VERTICAL);
        c.addView(text(title, 20, INK, true));
        if (attach) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(18); root.addView(c, p); }
        return c;
    }

    private List<LinkedRecord> loadLinkedRecords(IntegrityFindingEntity target) {
        List<LinkedRecord> out = new ArrayList<>();
        if ("master_batch".equals(target.getEntityType())) {
            MasterBatchTraceabilityEntity master = db.masterTraceabilityDao().getByMasterBatchNumber(target.getEntityId());
            if (master != null) {
                out.add(new LinkedRecord("Master batch", master.getMasterBatchNumber() + " · " + format(master.getTotalWeightKg(), "kg"), "master", master.getId()));
                for (BatchTraceabilityEntity child : db.batchTraceabilityDao().getChildrenOfMaster(String.valueOf(master.getId()))) {
                    QualityBatchEntity batch = db.qualityBatchDao().getBatchByNumber(child.getBatchNumber());
                    out.add(new LinkedRecord("Child batch", child.getBatchNumber() + " · " + format(child.getWeightKg(), "kg"), batch == null ? null : "batch", batch == null ? 0 : batch.getId()));
                }
            }
        } else if ("quality_batch".equals(target.getEntityType())) {
            QualityBatchEntity batch = db.qualityBatchDao().getBatchById(parseId(target.getEntityId()));
            if (batch == null) batch = db.qualityBatchDao().getBatchByNumber(target.getEntityId());
            if (batch != null) out.add(new LinkedRecord("Batch", batch.getBatchNumber() + " · " + format(batch.getWeightKg(), "kg"), "batch", batch.getId()));
        } else if ("supplier".equals(target.getEntityType())) {
            long id = parseId(target.getEntityId()); com.afrinuts.farmos.data.local.entity.SupplierEntity s = db.supplierDao().getSupplierById(id);
            if (s != null) out.add(new LinkedRecord("Supplier", s.getName(), "supplier", id));
        } else if ("block".equals(target.getEntityType())) out.add(new LinkedRecord("Block", target.getEntityId(), "block", parseId(target.getEntityId())));
        if (out.isEmpty()) out.add(new LinkedRecord(target.getEntityType(), target.getEntityId(), null, 0));
        return out;
    }

    private void openRecord(LinkedRecord r) {
        if ("batch".equals(r.type)) startActivity(new Intent(requireContext(), com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity.class).putExtra(com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity.EXTRA_BATCH_ID, r.id));
        else if ("master".equals(r.type)) startActivity(new Intent(requireContext(), com.afrinuts.farmos.ui.traceability.MasterTraceabilityDetailActivity.class).putExtra(com.afrinuts.farmos.ui.traceability.MasterTraceabilityDetailActivity.EXTRA_MASTER_ID, r.id));
        else if ("supplier".equals(r.type)) startActivity(new Intent(requireContext(), com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.class).putExtra(com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity.EXTRA_SUPPLIER_ID, r.id));
        else if ("block".equals(r.type)) startActivity(new Intent(requireContext(), com.afrinuts.farmos.ui.blocks.BlockDetailActivity.class).putExtra(com.afrinuts.farmos.ui.blocks.BlockDetailActivity.EXTRA_BLOCK_ID, r.id));
    }

    private void addHistory() {
        LinearLayout c = sectionCard("History — append-only", true);
        try {
            JsonArray a = JsonParser.parseString(finding.getHistoryJson() == null ? "[]" : finding.getHistoryJson()).getAsJsonArray();
            for (int i=0; i<a.size(); i++) { JsonObject e = a.get(i).getAsJsonObject(); String s=e.has("status")?e.get("status").getAsString():"Event"; long at=e.has("at")?e.get("at").getAsLong():0; String actor=e.has("actor")?e.get("actor").getAsString():"—"; addRow(c, displayStatus(s), dateTime(at)+" · "+actor); }
        } catch (Exception e) { addRow(c, "Detected", dateTime(finding.getDetectedAt())+" · Scan run"); }
        if (!containsStatus("UNDER_REVIEW")) addRow(c, "Under review", "Not yet");
        if (!containsStatus("RESOLVED") && !containsStatus("REJECTED")) addRow(c, "Resolved", "Not yet");
        c.addView(text("Resolving never deletes the fact the finding happened — the history stays on the record.", 15, MUTED, false));
    }

    private void updateActions() {
        if (finding == null || notes == null) return;
        UserEntity.UserRole role = session.getUserRole();
        boolean can = AccessMap.canResolveIntegrityFinding(role) && !creatorBlocked;
        if (role == UserEntity.UserRole.COMPLIANCE_OFFICER && (!isComplianceFinding() || "Critical".equalsIgnoreCase(finding.getSeverity()))) can = false;
        boolean open = "OPEN".equals(finding.getStatus()) || "UNDER_REVIEW".equals(finding.getStatus());
        boolean hasNotes = !notes.getText().toString().trim().isEmpty();
        resolve.setEnabled(can && open && hasNotes); resolve.setAlpha(resolve.isEnabled()?1f:.45f);
        reject.setEnabled(can && open && hasNotes); reject.setAlpha(reject.isEnabled()?1f:.45f);
        review.setEnabled(can && "OPEN".equals(finding.getStatus())); review.setAlpha(review.isEnabled()?1f:.45f);
        if (!can) reject.setVisibility(View.GONE);
    }

    private void transition(String status, String note) {
        UserEntity.UserRole role = session.getUserRole();
        if ("RESOLVED".equals(status) || "REJECTED".equals(status)) {
            if (note.trim().isEmpty()) { Toast.makeText(requireContext(), "Resolution notes are required", Toast.LENGTH_SHORT).show(); return; }
            if (!AccessMap.canResolveIntegrityFinding(role) || creatorBlocked
                    || (role == UserEntity.UserRole.COMPLIANCE_OFFICER && (!isComplianceFinding() || "Critical".equalsIgnoreCase(finding.getSeverity())))) {
                Toast.makeText(requireContext(), "You cannot resolve this finding", Toast.LENGTH_SHORT).show(); return;
            }
        }
        io.execute(() -> {
            long now=System.currentTimeMillis(); JsonArray a;
            try { a=JsonParser.parseString(finding.getHistoryJson()==null?"[]":finding.getHistoryJson()).getAsJsonArray(); } catch(Exception e){ a=new JsonArray(); }
            JsonObject entry=new JsonObject(); entry.addProperty("status",status); entry.addProperty("actor",safe(session.getUserName())); entry.addProperty("at",now); entry.addProperty("notes",note); a.add(entry);
            finding.setHistoryJson(new Gson().toJson(a)); finding.setStatus(status); finding.setUpdatedAt(now); finding.setSynced(false);
            if ("RESOLVED".equals(status)||"REJECTED".equals(status)) { finding.setResolvedAt(now); finding.setResolvedBy(safe(session.getUserName())); finding.setResolutionNotes(note); }
            db.integrityFindingDao().update(finding); SyncManager sync=SyncManager.getInstance(appContext); sync.setSyncCenterContext(finding.getFarmId(),null); sync.queueForSync("integrity_finding",finding.getId(), SyncEntity.SyncAction.UPDATE); sync.triggerAutoSync();
            if (isAdded()) requireActivity().runOnUiThread(() -> { Toast.makeText(requireContext(), "Finding marked " + displayStatus(status).toLowerCase(Locale.ROOT), Toast.LENGTH_SHORT).show(); loadFinding(); });
        });
    }

    private boolean currentUserCreatedSource(IntegrityFindingEntity f) {
        long id=parseId(f.getEntityId()); String by=null;
        if ("supplier_inspection".equals(f.getEntityType())) { SupplierInspectionEntity r=db.supplierInspectionDao().getById(id); if(r!=null) by=r.getCreatedBy(); }
        else if ("supplier_commitments".equals(f.getEntityType())) { SupplierCommitmentEntity r=db.supplierCommitmentDao().getById(id); if(r!=null) by=r.getCreatedBy(); }
        return by!=null && (by.equalsIgnoreCase(String.valueOf(session.getUserName()))||by.equalsIgnoreCase(String.valueOf(session.getUserUid())));
    }
    private boolean isComplianceFinding() { String c=finding.getRuleCode(); return "INT-03".equals(c)||"INT-05".equals(c)||"INT-13".equals(c)||"INT-14".equals(c)||"INT-15".equals(c)||"INT-22".equals(c)||"INT-23".equals(c); }
    private boolean containsStatus(String status) { try { JsonArray a=JsonParser.parseString(finding.getHistoryJson()).getAsJsonArray(); for(int i=0;i<a.size();i++) if(status.equals(a.get(i).getAsJsonObject().get("status").getAsString())) return true; } catch(Exception ignored){} return false; }
    private void addRow(LinearLayout c,String label,String value){ LinearLayout r=row(label,value); c.addView(r); divider(c); }
    private LinearLayout row(String label,String value){ LinearLayout r=horizontal(); TextView l=text(label,15,MUTED,true), v=text(value,16,INK,true); v.setGravity(Gravity.RIGHT); r.addView(l,new LinearLayout.LayoutParams(dp(150),-2)); r.addView(v,new LinearLayout.LayoutParams(0,-2,1f)); return r; }
    private TextView action(String s,int bg,int fg){ TextView v=text(s,15,fg,true); v.setGravity(Gravity.CENTER); v.setBackground(shape(bg,999,Color.TRANSPARENT)); return v; }
    private TextView pill(String s,int bg,int fg){ TextView v=text(s,13,fg,true); v.setGravity(Gravity.CENTER); v.setPadding(dp(12),0,dp(12),0); v.setBackground(shape(bg,999,Color.TRANSPARENT)); return v; }
    private LinearLayout horizontal(){ LinearLayout r=new LinearLayout(requireContext()); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    private TextView text(String s,int sp,int color,boolean bold){ TextView v=new TextView(requireContext()); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if(bold)v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); v.setPadding(0,dp(5),0,dp(5)); return v; }
    private void divider(LinearLayout c){ View d=new View(requireContext()); d.setBackgroundColor(0xFFE9E1CF); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1)); p.topMargin=dp(7); p.bottomMargin=dp(6); c.addView(d,p); }
    private GradientDrawable shape(int color,int radius,int stroke){ GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=Color.TRANSPARENT)d.setStroke(dp(1),stroke);return d; }
    private int statusBg(String s){if("RESOLVED".equals(s))return PALE_GREEN;if("UNDER_REVIEW".equals(s))return PALE_GOLD;if("REJECTED".equals(s))return 0xFFE9E6E0;return PALE_RED;}
    private int statusColor(String s){if("RESOLVED".equals(s))return 0xFF0A5530;if("UNDER_REVIEW".equals(s))return GOLD;if("REJECTED".equals(s))return MUTED;return RED;}
    private String displayStatus(String s){return s.replace('_',' ').toLowerCase(Locale.ROOT).replaceFirst("^.",s.substring(0,1).toUpperCase(Locale.ROOT));}
    private String safe(String s){return s==null||s.trim().isEmpty()?"—":s;}
    private String date(long t){return t<=0?"—":new SimpleDateFormat("d MMM yyyy",Locale.getDefault()).format(new Date(t));}
    private String dateTime(long t){return t<=0?"—":new SimpleDateFormat("d MMM yyyy · HH:mm",Locale.getDefault()).format(new Date(t));}
    private String format(double n,String context){String unit=context!=null&&context.toLowerCase(Locale.ROOT).contains("fcfa")?"FCFA":"kg";return String.format(Locale.getDefault(),"%,.0f %s",n,unit);}
    private long parseId(String id){try{return Long.parseLong(id);}catch(Exception e){return -1;}}
    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onDestroy(){super.onDestroy();io.shutdown();}
    private static final class LinkedRecord { final String label,value,type; final long id; final boolean openable; LinkedRecord(String l,String v,String t,long i){label=l;value=v;type=t;id=i;openable=t!=null&&i>0;} }
}
