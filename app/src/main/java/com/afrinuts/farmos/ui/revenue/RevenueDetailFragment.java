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

    public interface Listener { void onRevenueDeleted(long id); }

    public static RevenueDetailFragment newInstance(long id) {
        RevenueDetailFragment fragment = new RevenueDetailFragment();
        Bundle args = new Bundle(); args.putLong(ARG_ID, id); fragment.setArguments(args); return fragment;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        root = inflater.inflate(R.layout.activity_revenue_detail, container, false);
        if (root instanceof ViewGroup && ((ViewGroup) root).getChildCount() > 0) ((ViewGroup) root).getChildAt(0).setVisibility(View.GONE);
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
            RevenueEntity loaded = AppDatabase.getInstance(requireContext()).revenueDao().getRevenueById(id);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> { if (loaded != null && isAdded()) bind(loaded); });
        });
    }

    private void bind(RevenueEntity item) {
        revenue = item;
        set(R.id.tvQualityGrade, item.getQuality() == null ? "Revenue" : item.getQuality().getDisplayName() + " Grade");
        set(R.id.tvSource, join(item.getBuyer(), item.getSupplierName(), date(item.getCreatedAt())));
        set(R.id.tvBatchNumber, blank(item.getBatchNumber()) ? "Processing Center Sale" : item.getBatchNumber());
        set(R.id.tvQuantity, String.format(Locale.getDefault(), "%.1f kg", item.getQuantityKg()));
        set(R.id.tvPricePerKg, String.format(Locale.getDefault(), "%,.0f XAF/kg", item.getPricePerKg()));
        set(R.id.tvTotalAmount, String.format(Locale.getDefault(), "%,.0f XAF", item.getTotalAmount()));
        set(R.id.tvHarvestDate, date(item.getCreatedAt()));
        set(R.id.tvBuyer, blank(item.getBuyer()) ? "Buyer not recorded" : item.getBuyer());
        set(R.id.tvNotes, blank(item.getNotes()) ? "No notes recorded." : item.getNotes());
        set(R.id.tvBatchNumber, blank(item.getBatchNumber()) ? "Processing Center Sale" : item.getBatchNumber());
        TextView supplier = root.findViewById(R.id.tvSupplierName);
        if (supplier != null) supplier.setText(blank(item.getSupplierName()) ? "Supplier not recorded" : item.getSupplierName());
        View procurement = root.findViewById(R.id.layoutProcurementInfo);
        if (procurement != null) procurement.setVisibility(item.isProcurementRelated() ? View.VISIBLE : View.GONE);
        View delete = root.findViewById(R.id.btnDelete);
        if (delete != null) delete.setOnClickListener(v -> confirmDelete());
        View edit = root.findViewById(R.id.btnEdit);
        if (edit != null) edit.setOnClickListener(v -> openPhoneDetail());
        View batch = root.findViewById(R.id.btnViewBatch);
        if (batch != null) batch.setOnClickListener(v -> openBatch());
        View supplierAction = root.findViewById(R.id.btnViewSupplier);
        if (supplierAction != null) supplierAction.setOnClickListener(v -> openSupplier());
        CardView editFields = root.findViewById(R.id.layoutEditFields);
        if (editFields != null) editFields.setVisibility(View.GONE);
    }

    private void openPhoneDetail() {
        Intent intent = new Intent(requireContext(), RevenueDetailActivity.class);
        intent.putExtra(RevenueDetailActivity.EXTRA_REVENUE_ID, revenue.getId());
        startActivity(intent);
    }
    private void openBatch() {
        if (revenue.getBatchId() == null) return;
        Intent intent = new Intent(requireContext(), QualityBatchDetailActivity.class);
        intent.putExtra(QualityBatchDetailActivity.EXTRA_BATCH_ID, revenue.getBatchId()); startActivity(intent);
    }
    private void openSupplier() {
        if (revenue.getSupplierId() == null) return;
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
