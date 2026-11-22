package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.AuditEntity;
import com.afrinuts.farmos.data.local.entity.InspectionEntity;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;
import com.afrinuts.farmos.data.repository.AuditRepository;
import com.afrinuts.farmos.data.repository.InspectionRepository;
import com.afrinuts.farmos.data.repository.VerificationRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Combined, newest-first list for audits, compliance inspections, and verifications. */
public class ChecksFragment extends Fragment {
    private static final int ALL = 0, AUDITS = 1, INSPECTIONS = 2, VERIFICATIONS = 3;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<AuditEntity> audits = new ArrayList<>();
    private final List<InspectionEntity> inspections = new ArrayList<>();
    private final List<VerificationEntity> verifications = new ArrayList<>();
    private final List<Entry> visibleEntries = new ArrayList<>();
    private CheckAdapter adapter;
    private ChipGroup filters;
    private long farmId = 1;
    private int selectedFilter = ALL;
    private AuditRepository auditRepository;
    private InspectionRepository inspectionRepository;
    private VerificationRepository verificationRepository;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(8));
        filters = new ChipGroup(requireContext());
        filters.setSingleSelection(true);
        filters.setSelectionRequired(true);
        filters.setChipSpacingHorizontal(dp(8));
        filters.setChipSpacingVertical(dp(6));
        root.addView(filters, new LinearLayout.LayoutParams(-1, -2));
        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CheckAdapter(visibleEntries);
        list.setAdapter(adapter);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1f);
        lp.topMargin = dp(8);
        root.addView(list, lp);
        addFilterChip("All", ALL);
        addFilterChip("Audits", AUDITS);
        addFilterChip("Inspections", INSPECTIONS);
        addFilterChip("Verifications", VERIFICATIONS);
        return root;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        executor.execute(() -> {
            try {
                farmId = AppDatabase.getInstance(requireContext()).farmDao().getFirstFarm().getId();
                auditRepository = new AuditRepository(requireContext(), farmId);
                inspectionRepository = new InspectionRepository(requireContext(), farmId);
                verificationRepository = new VerificationRepository(requireContext(), farmId);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> observeLists());
            } catch (Exception e) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error loading compliance checks", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void observeLists() {
        auditRepository.getAllAudits().observe(getViewLifecycleOwner(), data -> {
            audits.clear(); if (data != null) audits.addAll(data); refreshRows();
        });
        inspectionRepository.getAllInspections().observe(getViewLifecycleOwner(), data -> {
            inspections.clear(); if (data != null) inspections.addAll(data); refreshRows();
        });
        verificationRepository.getAllVerifications().observe(getViewLifecycleOwner(), data -> {
            verifications.clear(); if (data != null) verifications.addAll(data); refreshRows();
        });
    }

    private void addFilterChip(String label, int filter) {
        Chip chip = new Chip(requireContext(), null, R.style.Widget_AfriNuts_Chip);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setTag(filter);
        chip.setOnClickListener(v -> { selectedFilter = filter; refreshRows(); });
        filters.addView(chip);
        if (filter == ALL) chip.setChecked(true);
    }

    private void refreshRows() {
        if (adapter == null || !isAdded()) return;
        int auditCount = audits.size(), inspectionCount = inspections.size(), verificationCount = verifications.size();
        updateChip(ALL, "All " + (auditCount + inspectionCount + verificationCount));
        updateChip(AUDITS, "Audits " + auditCount);
        updateChip(INSPECTIONS, "Inspections " + inspectionCount);
        updateChip(VERIFICATIONS, "Verifications " + verificationCount);
        visibleEntries.clear();
        if (selectedFilter == ALL || selectedFilter == AUDITS) {
            for (AuditEntity item : audits) visibleEntries.add(Entry.audit(item));
        }
        if (selectedFilter == ALL || selectedFilter == INSPECTIONS) {
            for (InspectionEntity item : inspections) visibleEntries.add(Entry.inspection(item));
        }
        if (selectedFilter == ALL || selectedFilter == VERIFICATIONS) {
            for (VerificationEntity item : verifications) visibleEntries.add(Entry.verification(item));
        }
        Collections.sort(visibleEntries, Comparator.comparingLong((Entry e) -> e.date).reversed());
        adapter.notifyDataSetChanged();
    }

    private void updateChip(int filter, String text) {
        for (int i = 0; i < filters.getChildCount(); i++) {
            View child = filters.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).getTag() instanceof Integer
                    && ((Integer) ((Chip) child).getTag()) == filter) ((Chip) child).setText(text);
        }
    }

    public void showAddAuditDialog() {
        AddAuditDialog dialog = AddAuditDialog.newInstance(farmId);
        dialog.setOnAuditAddedListener(() -> Toast.makeText(requireContext(), "Audit added!", Toast.LENGTH_SHORT).show());
        dialog.show(getParentFragmentManager(), "AddAuditDialog");
    }
    public void showAddInspectionDialog() {
        AddInspectionDialog dialog = AddInspectionDialog.newInstance(farmId);
        dialog.setOnInspectionAddedListener(() -> Toast.makeText(requireContext(), "Compliance inspection added!", Toast.LENGTH_SHORT).show());
        dialog.show(getParentFragmentManager(), "AddInspectionDialog");
    }
    public void showAddVerificationDialog() {
        AddVerificationDialog dialog = AddVerificationDialog.newInstance(farmId);
        dialog.setOnVerificationAddedListener(() -> Toast.makeText(requireContext(), "Verification added!", Toast.LENGTH_SHORT).show());
        dialog.show(getParentFragmentManager(), "AddVerificationDialog");
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onDestroy() {
        super.onDestroy();
        if (auditRepository != null) auditRepository.shutdown();
        if (inspectionRepository != null) inspectionRepository.shutdown();
        if (verificationRepository != null) verificationRepository.shutdown();
        executor.shutdown();
    }

    private static class Entry {
        int type; long id; String title, subtitle; long date; Object model;
        static Entry audit(AuditEntity a) {
            Entry e = new Entry(); e.type = AUDITS; e.id = 0; e.date = a.getAuditDate(); e.model = a;
            e.title = "Audit · " + a.getAuditId();
            e.subtitle = join(safeAuditType(a), a.getAuditorBody(), a.getScope(), safeAuditStatus(a)); return e;
        }
        static Entry inspection(InspectionEntity i) {
            Entry e = new Entry(); e.type = INSPECTIONS; e.id = 0; e.date = i.getInspectionDate(); e.model = i;
            e.title = "Compliance inspection · " + i.getInspectionId();
            e.subtitle = join(i.getCertificationName(), safeInspectionCategory(i), safeInspectionStatus(i)); return e;
        }
        static Entry verification(VerificationEntity v) {
            Entry e = new Entry(); e.type = VERIFICATIONS; e.id = 0; e.date = v.getVerificationDate(); e.model = v;
            e.title = "Verification · " + v.getVerificationId();
            e.subtitle = join(safeVerificationType(v), v.getVerifiedBy(), safeVerificationStatus(v)); return e;
        }
        static String join(String... values) {
            StringBuilder b = new StringBuilder();
            for (String value : values) if (value != null && !value.trim().isEmpty()) {
                if (b.length() > 0) b.append(" · "); b.append(value.trim());
            }
            return b.length() == 0 ? "—" : b.toString();
        }
        static String safeAuditType(AuditEntity a) { try { return a.getAuditTypeDisplay(); } catch (Exception e) { return "Audit"; } }
        static String safeInspectionCategory(InspectionEntity i) { try { return i.getRequirementCategoryDisplay(); } catch (Exception e) { return "Compliance inspection"; } }
        static String safeVerificationType(VerificationEntity v) { try { return v.getVerificationTypeDisplay(); } catch (Exception e) { return "Verification"; } }
        static String safeAuditStatus(AuditEntity a) { try { return a.getStatusDisplay(); } catch (Exception e) { return "Pending"; } }
        static String safeInspectionStatus(InspectionEntity i) { try { return i.getStatusDisplay(); } catch (Exception e) { return "Pending"; } }
        static String safeVerificationStatus(VerificationEntity v) { try { return v.getStatusDisplay(); } catch (Exception e) { return "Pending"; } }
    }

    private class CheckAdapter extends RecyclerView.Adapter<CheckAdapter.Holder> {
        private final List<Entry> entries;
        CheckAdapter(List<Entry> entries) { this.entries = entries; }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout card = new LinearLayout(parent.getContext()); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(ContextCompat.getColor(parent.getContext(), R.color.surface)); bg.setCornerRadius(dp(16));
            card.setBackground(bg); RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
            params.bottomMargin = dp(8); card.setLayoutParams(params);
            TextView title = new TextView(parent.getContext()); title.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_primary));
            title.setTextSize(15); title.setTypeface(null, android.graphics.Typeface.BOLD); card.addView(title);
            TextView subtitle = new TextView(parent.getContext()); subtitle.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_secondary));
            subtitle.setTextSize(12); LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2); subParams.topMargin = dp(4); card.addView(subtitle, subParams);
            TextView date = new TextView(parent.getContext()); date.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_tertiary));
            date.setTextSize(11); date.setGravity(Gravity.END); LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2); dateParams.topMargin = dp(8); card.addView(date, dateParams);
            return new Holder(card, title, subtitle, date);
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Entry e = entries.get(position); holder.title.setText(e.title); holder.subtitle.setText(e.subtitle);
            holder.date.setText(e.date > 0 ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(e.date)) : "Date not set");
            holder.itemView.setOnClickListener(v -> {
                Intent intent;
                if (e.type == AUDITS) { intent = new Intent(requireContext(), AuditDetailActivity.class); intent.putExtra(AuditDetailActivity.EXTRA_AUDIT_ID, ((AuditEntity)e.model).getAuditId()); }
                else if (e.type == INSPECTIONS) { intent = new Intent(requireContext(), InspectionDetailActivity.class); intent.putExtra(InspectionDetailActivity.EXTRA_INSPECTION_ID, ((InspectionEntity)e.model).getInspectionId()); }
                else { intent = new Intent(requireContext(), VerificationDetailActivity.class); intent.putExtra(VerificationDetailActivity.EXTRA_VERIFICATION_ID, ((VerificationEntity)e.model).getVerificationId()); }
                startActivity(intent);
            });
        }
        @Override public int getItemCount() { return entries.size(); }
        class Holder extends RecyclerView.ViewHolder {
            TextView title, subtitle, date;
            Holder(@NonNull View item, TextView title, TextView subtitle, TextView date) { super(item); this.title=title; this.subtitle=subtitle; this.date=date; }
        }
    }
}
