package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.google.android.material.chip.ChipDrawable;
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
    public static final int TYPE_NONE = 0, TYPE_AUDIT = 1, TYPE_INSPECTION = 2, TYPE_VERIFICATION = 3;

    public interface OnCheckSelectedListener {
        void onCheckSelected(int type, String id);
    }

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
    private boolean auditsLoaded, inspectionsLoaded, verificationsLoaded;
    private boolean initialSelectionApplied;
    private boolean filterSelectionPending;
    private int selectedCheckType = TYPE_NONE;
    private String selectedCheckId;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(8));
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            GradientDrawable paneBackground = new GradientDrawable();
            paneBackground.setColor(Color.WHITE);
            paneBackground.setCornerRadius(dp(28));
            root.setBackground(paneBackground);
            root.setClipToOutline(true);
        }
        filters = new ChipGroup(requireContext());
        filters.setSingleSelection(true);
        filters.setSelectionRequired(true);
        filters.setSingleLine(false);
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
        if (getResources().getConfiguration().screenWidthDp >= 600) {
            ViewGroup host = requireActivity().findViewById(R.id.checkFiltersHost);
            if (host != null && filters.getParent() instanceof ViewGroup) {
                ((ViewGroup) filters.getParent()).removeView(filters);
                filters.setSingleLine(true);
                host.addView(filters, new LinearLayout.LayoutParams(-1, -2));
            }
        }
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
            audits.clear(); if (data != null) audits.addAll(data); auditsLoaded = true; refreshRows();
        });
        inspectionRepository.getAllInspections().observe(getViewLifecycleOwner(), data -> {
            inspections.clear(); if (data != null) inspections.addAll(data); inspectionsLoaded = true; refreshRows();
        });
        verificationRepository.getAllVerifications().observe(getViewLifecycleOwner(), data -> {
            verifications.clear(); if (data != null) verifications.addAll(data); verificationsLoaded = true; refreshRows();
        });
    }

    private void addFilterChip(String label, int filter) {
        Chip chip = new Chip(requireContext());
        chip.setChipDrawable(ChipDrawable.createFromAttributes(
                requireContext(), null, 0, R.style.Widget_AfriNuts_Chip_Checks));
        chip.setText(label);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setCheckedIconVisible(false);
        ColorStateList chipBackground = new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ContextCompat.getColor(requireContext(), R.color.primary),
                        android.graphics.Color.TRANSPARENT});
        chip.setChipBackgroundColor(chipBackground);
        chip.setChipStrokeColor(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ContextCompat.getColor(requireContext(), R.color.primary), 0xFFE9E1CF}));
        chip.setChipStrokeWidth(dp(1));
        chip.setTextColor(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ContextCompat.getColor(requireContext(), android.R.color.white),
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)}));
        chip.setTag(filter);
        chip.setOnClickListener(v -> { selectedFilter = filter; filterSelectionPending = true; refreshRows(); });
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
        adapter.setSelectedCheck(selectedCheckType, selectedCheckId);
        if (auditsLoaded && inspectionsLoaded && verificationsLoaded
                && (!initialSelectionApplied || filterSelectionPending
                || !containsSelectedCheck())) {
            initialSelectionApplied = true;
            filterSelectionPending = false;
            selectFirstVisibleCheck();
        }
    }

    private boolean containsSelectedCheck() {
        if (selectedCheckType == TYPE_NONE || selectedCheckId == null) return false;
        for (Entry entry : visibleEntries) {
            if (entry.type == selectedCheckType && selectedCheckId.equals(entry.id)) return true;
        }
        return false;
    }

    private void selectFirstVisibleCheck() {
        if (visibleEntries.isEmpty()) {
            selectedCheckType = TYPE_NONE;
            selectedCheckId = null;
            adapter.setSelectedCheck(TYPE_NONE, null);
            notifyTabletSelection(TYPE_NONE, null);
            return;
        }
        Entry first = visibleEntries.get(0);
        // The Checks tab opens on the audit workflow; when All is selected, show
        // the newest audit by default while keeping the combined list date-sorted.
        if (selectedFilter == ALL) {
            for (Entry entry : visibleEntries) {
                if (entry.type == TYPE_AUDIT) {
                    first = entry;
                    break;
                }
            }
        }
        selectedCheckType = first.type;
        selectedCheckId = first.id;
        adapter.setSelectedCheck(selectedCheckType, selectedCheckId);
        notifyTabletSelection(selectedCheckType, selectedCheckId);
    }

    private void notifyTabletSelection(int type, String id) {
        if (getResources().getConfiguration().screenWidthDp >= 600
                && getActivity() instanceof OnCheckSelectedListener) {
            ((OnCheckSelectedListener) getActivity()).onCheckSelected(type, id);
        }
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
        int type; String id; String title, subtitle; long date; Object model;
        static Entry audit(AuditEntity a) {
            Entry e = new Entry(); e.type = TYPE_AUDIT; e.id = a.getAuditId(); e.date = a.getAuditDate(); e.model = a;
            e.title = "Audit · " + a.getAuditId();
            e.subtitle = join(safeAuditType(a), a.getAuditorBody(), a.getScope(), safeAuditStatus(a)); return e;
        }
        static Entry inspection(InspectionEntity i) {
            Entry e = new Entry(); e.type = TYPE_INSPECTION; e.id = i.getInspectionId(); e.date = i.getInspectionDate(); e.model = i;
            e.title = "Compliance inspection · " + i.getInspectionId();
            e.subtitle = join(i.getCertificationName(), safeInspectionCategory(i), safeInspectionStatus(i)); return e;
        }
        static Entry verification(VerificationEntity v) {
            Entry e = new Entry(); e.type = TYPE_VERIFICATION; e.id = v.getVerificationId(); e.date = v.getVerificationDate(); e.model = v;
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
        private int selectedType = TYPE_NONE;
        private String selectedId;
        CheckAdapter(List<Entry> entries) { this.entries = entries; }
        void setSelectedCheck(int type, String id) {
            selectedType = type;
            selectedId = id;
            notifyDataSetChanged();
        }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int secondaryTextColor = getResources().getConfiguration().screenWidthDp >= 600
                    ? 0xFF59544C
                    : ContextCompat.getColor(parent.getContext(), R.color.text_secondary);
            LinearLayout card = new LinearLayout(parent.getContext()); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(parent.getContext(), R.color.surface)); bg.setCornerRadius(dp(24));
            card.setBackground(bg); RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
            if (getResources().getConfiguration().screenWidthDp < 600) {
                params.bottomMargin = dp(8);
            }
            card.setLayoutParams(params);
            LinearLayout heading = new LinearLayout(parent.getContext());
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(Gravity.TOP);
            card.addView(heading, new LinearLayout.LayoutParams(-1, -2));
            TextView title = new TextView(parent.getContext());
            title.setTextColor(getResources().getConfiguration().screenWidthDp >= 600
                    ? 0xFF151413
                    : ContextCompat.getColor(parent.getContext(), R.color.text_primary));
            title.setTextSize(15); title.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);
            heading.addView(title, titleParams);
            TextView date = new TextView(parent.getContext()); date.setTextColor(secondaryTextColor);
            date.setTextSize(11); date.setGravity(Gravity.END | Gravity.TOP);
            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-2, -2);
            dateParams.leftMargin = dp(8);
            heading.addView(date, dateParams);
            TextView subtitle = new TextView(parent.getContext()); subtitle.setTextColor(secondaryTextColor);
            subtitle.setTextSize(13); LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2); subParams.topMargin = dp(6); card.addView(subtitle, subParams);
            View divider = new View(parent.getContext());
            divider.setBackgroundColor(0xFFE9E1CF);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
            dividerParams.topMargin = dp(12);
            dividerParams.leftMargin = dp(4);
            dividerParams.rightMargin = dp(4);
            card.addView(divider, dividerParams);
            return new Holder(card, title, subtitle, date, divider);
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Entry e = entries.get(position); holder.title.setText(e.title); holder.subtitle.setText(e.subtitle);
            holder.date.setText(e.date > 0 ? new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(e.date)) : "Date not set");
            boolean isTablet = getResources().getConfiguration().screenWidthDp >= 600;
            if (isTablet) {
                boolean selected = e.type == selectedType && selectedId != null && selectedId.equals(e.id);
                GradientDrawable background = new GradientDrawable();
                background.setColor(selected ? 0xFFE3F0E7 : Color.TRANSPARENT);
                background.setCornerRadius(dp(24));
                holder.itemView.setBackground(background);
                holder.divider.setVisibility(!selected && position < entries.size() - 1 ? View.VISIBLE : View.GONE);
            } else {
                holder.divider.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> {
                if (isTablet && getActivity() instanceof OnCheckSelectedListener) {
                    selectedType = e.type;
                    selectedId = e.id;
                    notifyDataSetChanged();
                    ((OnCheckSelectedListener) getActivity()).onCheckSelected(e.type, e.id);
                    return;
                }
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
            View divider;
            Holder(@NonNull View item, TextView title, TextView subtitle, TextView date, View divider) { super(item); this.title=title; this.subtitle=subtitle; this.date=date; this.divider=divider; }
        }
    }
}
