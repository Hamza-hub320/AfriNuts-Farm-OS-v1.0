package com.afrinuts.farmos.ui.revenue;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;
import com.afrinuts.farmos.data.repository.RevenueRepository;
import com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity;
import com.afrinuts.farmos.ui.suppliers.SupplierDetailActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RevenueDetailFragment extends Fragment {
    private static final String ARG_ID = "revenue_id";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RevenueEntity revenue;
    private View root;
    private boolean tabletLayout;

    public interface Listener { void onRevenueDeleted(long id); }

    public static RevenueDetailFragment newInstance(long id) {
        RevenueDetailFragment fragment = new RevenueDetailFragment();
        Bundle args = new Bundle(); args.putLong(ARG_ID, id); fragment.setArguments(args); return fragment;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        tabletLayout = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        root = inflater.inflate(tabletLayout ? R.layout.fragment_revenue_detail_tablet : R.layout.activity_revenue_detail, container, false);
        if (!tabletLayout && root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) ((ViewGroup) root).getChildAt(0).setVisibility(View.GONE);
        hideEditFields(root);
        View phoneFab = root.findViewById(R.id.farmDetailBack);
        if (phoneFab != null) phoneFab.setVisibility(View.GONE);
        View headerEdit = root.findViewById(R.id.farmDetailEdit);
        if (headerEdit != null) headerEdit.setVisibility(View.GONE);
        View more = root.findViewById(R.id.farmDetailMore);
        if (more != null) more.setVisibility(View.GONE);
        load();
        return root;
    }

    private void hideEditFields(View view) {
        View editFields = view.findViewById(R.id.layoutEditFields);
        if (editFields != null) editFields.setVisibility(View.GONE);
        View save = view.findViewById(R.id.btnSave);
        if (save != null) save.setVisibility(View.GONE);
    }

    private void load() {
        long id = requireArguments().getLong(ARG_ID);
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            RevenueEntity loaded = db.revenueDao().getRevenueById(id);
            QualityBatchEntity batch = loaded == null || loaded.getBatchId() == null
                    ? null : db.qualityBatchDao().getBatchById(loaded.getBatchId());
            SupplierEntity supplier = loaded == null || loaded.getSupplierId() == null
                    ? null : db.supplierDao().getSupplierById(loaded.getSupplierId());
            int batchCount = supplier == null ? 0 : db.qualityBatchDao().getBatchCountBySupplierId(supplier.getId());
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> { if (loaded != null && isAdded()) bind(loaded, batch, supplier, batchCount); });
        });
    }

    private void bind(RevenueEntity item, QualityBatchEntity linkedBatch, SupplierEntity linkedSupplier, int batchCount) {
        revenue = item;
        set(R.id.tvQualityGrade, item.getQuality() == null ? "Revenue" : item.getQuality().getDisplayName() + (tabletLayout ? "" : " Grade"));
        set(R.id.tvSource, join(item.getBuyer(), item.getSupplierName(), date(item.getCreatedAt())));
        set(R.id.tvBatchNumber, blank(item.getBatchNumber()) ? "Processing Center Sale" : item.getBatchNumber());
        set(R.id.tvQuantity, String.format(Locale.getDefault(), tabletLayout ? "%.0f kg" : "%.1f kg", item.getQuantityKg()));
        set(R.id.tvPricePerKg, String.format(Locale.getDefault(), tabletLayout ? "%,.0f XAF" : "%,.0f XAF/kg", item.getPricePerKg()));
        set(R.id.tvTotalAmount, String.format(Locale.getDefault(), "%,.0f XAF", item.getTotalAmount()));
        set(R.id.tvHarvestDate, date(item.getCreatedAt()));
        set(R.id.tvBuyer, blank(item.getBuyer()) ? "Buyer not recorded" : item.getBuyer());
        set(R.id.tvNotes, blank(item.getNotes()) ? "No notes recorded." : item.getNotes());
        set(R.id.tvBatchNumber, blank(item.getBatchNumber()) ? "Processing Center Sale" : item.getBatchNumber());
        TextView supplier = root.findViewById(R.id.tvSupplierName);
        if (supplier != null) supplier.setText(blank(item.getSupplierName()) ? "Supplier not recorded" : item.getSupplierName());
        if (tabletLayout) {
            String supplierName = !blank(item.getSupplierName()) ? item.getSupplierName()
                    : linkedSupplier == null ? "" : linkedSupplier.getName();
            set(R.id.tvSource, join(supplierName, item.getBuyer(), date(item.getCreatedAt())));
        }
        View procurement = root.findViewById(R.id.layoutProcurementInfo);
        if (procurement != null) procurement.setVisibility(item.isProcurementRelated() ? View.VISIBLE : View.GONE);
        if (tabletLayout) {
            String grade = item.getQuality() == null ? "Not recorded" : item.getQuality().getDisplayName();
            String supplierDisplayName = !blank(item.getSupplierName()) ? item.getSupplierName()
                    : linkedSupplier == null ? "Not recorded" : linkedSupplier.getName();
            TextView gradeBadge = root.findViewById(R.id.tvQualityGrade);
            if (gradeBadge != null) {
                gradeBadge.setBackgroundResource(R.drawable.bg_revenue_premium_badge);
                gradeBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F0E7));
                gradeBadge.setTextColor(0xFF075C37);
                int inset = Math.round(12 * getResources().getDisplayMetrics().density);
                gradeBadge.setPadding(inset, gradeBadge.getPaddingTop(), inset, gradeBadge.getPaddingBottom());
            }
            setRow(R.id.rowRevenueGrade, "Grade", grade);
            setRow(R.id.rowRevenueSaleSource, "Source", "RCN sale");
            setRow(R.id.rowRevenueHarvestDate, "Harvest date", date(item.getCreatedAt()));
            setRow(R.id.rowRevenueBuyer, "Buyer", blank(item.getBuyer()) ? "Not recorded" : item.getBuyer());
            setRow(R.id.rowRevenueBatch, "Batch", blank(item.getBatchNumber()) ? "Not linked" : item.getBatchNumber());
            setRow(R.id.rowRevenueSupplier, "Supplier", blank(supplierDisplayName) ? "Not recorded" : supplierDisplayName);
            setRow(R.id.rowRevenueKor, "KOR", linkedBatch == null || linkedBatch.getKorGPerKg() <= 0
                    ? "Not recorded" : String.format(Locale.getDefault(), "%.0f g/kg", linkedBatch.getKorGPerKg()));
            setRow(R.id.rowRevenueReceived, "Received", linkedBatch == null || linkedBatch.getProcessingDate() <= 0
                    ? "Not recorded" : date(linkedBatch.getProcessingDate()));
            setRow(R.id.rowRevenueVillage, "Village", linkedSupplier == null || blank(linkedSupplier.getLocation())
                    ? "Not recorded" : linkedSupplier.getLocation());
            setRow(R.id.rowRevenueBatchesSupplied, "Batches supplied", linkedSupplier == null
                    ? "Not recorded" : String.valueOf(batchCount));
            View batchAction = root.findViewById(R.id.btnViewBatch);
            if (batchAction != null) batchAction.setVisibility(item.getBatchId() == null ? View.GONE : View.VISIBLE);
            View supplierAction = root.findViewById(R.id.btnViewSupplier);
            if (supplierAction != null) supplierAction.setVisibility(item.getSupplierId() == null ? View.GONE : View.VISIBLE);
        }
        View delete = root.findViewById(R.id.btnDelete);
        if (delete != null) delete.setOnClickListener(v -> confirmDelete());
        View edit = root.findViewById(R.id.btnEdit);
        if (edit != null) edit.setOnClickListener(v -> openPhoneDetail());
        View batch = root.findViewById(R.id.btnViewBatch);
        if (batch != null) batch.setOnClickListener(v -> openBatch());
        View supplierAction = root.findViewById(R.id.btnViewSupplier);
        if (supplierAction != null) supplierAction.setOnClickListener(v -> openSupplier());
        View editFields = root.findViewById(R.id.layoutEditFields);
        if (editFields != null) editFields.setVisibility(View.GONE);
    }

    private void setRow(int rowId, String label, String value) {
        View row = root.findViewById(rowId);
        if (!(row instanceof ViewGroup)) return;
        TextView key = row.findViewById(R.id.financeDetailLabel);
        TextView val = row.findViewById(R.id.financeDetailValue);
        if (key != null) key.setText(label);
        if (val != null) val.setText(value);
    }

    private void openPhoneDetail() {
        Intent intent = new Intent(requireContext(), RevenueDetailActivity.class);
        intent.putExtra(RevenueDetailActivity.EXTRA_REVENUE_ID, revenue.getId());
        startActivity(intent);
    }
    private void openBatch() {
        if (revenue.getBatchId() == null) return;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            Intent intent = new Intent(requireContext(), com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity.class);
            intent.putExtra(com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity.EXTRA_OPEN_BATCH_ID, revenue.getBatchId());
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(requireContext(), QualityBatchDetailActivity.class);
        intent.putExtra(QualityBatchDetailActivity.EXTRA_BATCH_ID, revenue.getBatchId()); startActivity(intent);
    }
    private void openSupplier() {
        if (revenue.getSupplierId() == null) return;
        if (getResources().getConfiguration().smallestScreenWidthDp >= 600) {
            Intent intent = new Intent(requireContext(), com.afrinuts.farmos.ui.farm.FarmHubActivity.class);
            intent.putExtra(com.afrinuts.farmos.ui.farm.FarmHubActivity.EXTRA_OPEN_SUPPLIER_ID, revenue.getSupplierId());
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(requireContext(), SupplierDetailActivity.class);
        intent.putExtra(SupplierDetailActivity.EXTRA_SUPPLIER_ID, revenue.getSupplierId()); startActivity(intent);
    }
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Delete revenue")
                .setMessage("Delete this revenue record?").setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> {
                    RevenueRepository repository = new RevenueRepository(requireContext(), revenue.getFarmId());
                    repository.deleteRevenue(revenue, new com.afrinuts.farmos.data.repository.BaseRepository.RepositoryCallback<Boolean>() {
                        @Override public void onSuccess(Boolean success) {
                            if (isAdded() && getActivity() instanceof Listener) ((Listener) getActivity()).onRevenueDeleted(revenue.getId());
                            repository.shutdown();
                        }
                        @Override public void onError(String error) {
                            repository.shutdown();
                        }
                    });
                }).show();
    }
    private void set(int id, String value) { TextView view = root.findViewById(id); if (view != null) view.setText(value); }
    private String date(long millis) { return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(millis)); }
    private String join(String... values) { StringBuilder b = new StringBuilder(); for (String v : values) if (!blank(v)) { if (b.length() > 0) b.append(" · "); b.append(v); } return b.toString(); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    @Override public void onDestroyView() { root = null; super.onDestroyView(); }
    @Override public void onDestroy() { executor.shutdown(); super.onDestroy(); }
}
