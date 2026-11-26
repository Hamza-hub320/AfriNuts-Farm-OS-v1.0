package com.afrinuts.farmos.ui.suppliers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.SupplierViewHolder> {
    private List<SupplierEntity> suppliers;
    private final OnSupplierClickListener listener;
    private Map<Long, Double> trustScores = Collections.emptyMap();
    private Map<Long, Integer> batchCounts = Collections.emptyMap();
    private long selectedSupplierId = -1;

    public interface OnSupplierClickListener { void onSupplierClick(SupplierEntity supplier); }

    public SupplierAdapter(List<SupplierEntity> suppliers, OnSupplierClickListener listener) {
        this.suppliers = suppliers;
        this.listener = listener;
    }

    public void setTrustData(Map<Long, Double> scores, Map<Long, Integer> counts) {
        trustScores = scores == null ? Collections.emptyMap() : scores;
        batchCounts = counts == null ? Collections.emptyMap() : counts;
        notifyDataSetChanged();
    }

    public void setSelectedSupplierId(long id) {
        selectedSupplierId = id;
        notifyDataSetChanged();
    }

    @NonNull @Override public SupplierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SupplierViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supplier, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull SupplierViewHolder holder, int position) {
        SupplierEntity supplier = suppliers.get(position);
        holder.bind(supplier, trustScores.get(supplier.getId()), batchCounts.getOrDefault(supplier.getId(), 0), position, listener,
                supplier.getId() == selectedSupplierId);
    }

    @Override public int getItemCount() { return suppliers == null ? 0 : suppliers.size(); }
    public void updateSuppliers(List<SupplierEntity> newSuppliers) { suppliers = newSuppliers; notifyDataSetChanged(); }

    static class SupplierViewHolder extends RecyclerView.ViewHolder {
        final TextView name, meta, score, avatar;
        final ImageView statusDot;
        SupplierViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvSupplierName);
            meta = itemView.findViewById(R.id.tvSupplierType);
            score = itemView.findViewById(R.id.tvRating);
            avatar = itemView.findViewById(R.id.tvSupplierAvatar);
            statusDot = itemView.findViewById(R.id.ivStatusIcon);
        }
        void bind(SupplierEntity supplier, Double trust, int batches, int position, OnSupplierClickListener listener, boolean selected) {
            String fullName = supplier.getName() == null ? "" : supplier.getName().trim();
            name.setText(fullName);
            String[] parts = fullName.split("\\s+");
            String initials = parts.length > 1 ? parts[0].substring(0, 1) + parts[1].substring(0, 1)
                    : (fullName.isEmpty() ? "?" : fullName.substring(0, 1));
            avatar.setText(initials.toUpperCase(Locale.ROOT));
            int[] backgrounds = {R.drawable.bg_supplier_avatar_green, R.drawable.bg_supplier_avatar_sand, R.drawable.bg_supplier_avatar_plain};
            int[] foregrounds = {0xFF0A5530, 0xFF7A5F22, 0xFF6B675F};
            avatar.setBackgroundResource(backgrounds[position % backgrounds.length]);
            avatar.setTextColor(foregrounds[position % foregrounds.length]);
            String location = supplier.getLocation() == null || supplier.getLocation().trim().isEmpty()
                    ? "Location unavailable" : supplier.getLocation().trim();
            meta.setText(String.format(Locale.getDefault(), "%s · %s · %d batches", supplier.getStatus().getDisplayName(), location, batches));
            score.setText(trust == null ? "—" : String.format(Locale.getDefault(), "%.0f", trust));
            score.setTextColor(trust != null && trust < 40 ? 0xFFB3402A : 0xFF1C1B1A);
            int dotColor = supplier.getStatus() == SupplierEntity.SupplierStatus.ACTIVE ? 0xFF0E6B3C
                    : supplier.getStatus() == SupplierEntity.SupplierStatus.PENDING ? 0xFFC9A227 : 0xFFB3402A;
            statusDot.setImageTintList(android.content.res.ColorStateList.valueOf(dotColor));
            android.graphics.drawable.GradientDrawable rowBackground = new android.graphics.drawable.GradientDrawable();
            rowBackground.setColor(selected ? 0xFFE3F0E7 : android.graphics.Color.TRANSPARENT);
            rowBackground.setCornerRadius(20 * itemView.getResources().getDisplayMetrics().density);
            itemView.setBackground(rowBackground);
            itemView.setOnClickListener(v -> listener.onSupplierClick(supplier));
        }
    }
}
