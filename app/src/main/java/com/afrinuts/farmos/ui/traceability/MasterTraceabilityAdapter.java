package com.afrinuts.farmos.ui.traceability;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.MasterBatchTraceabilityEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MasterTraceabilityAdapter extends RecyclerView.Adapter<MasterTraceabilityAdapter.MasterViewHolder> {

    private List<MasterBatchTraceabilityEntity> masters;
    private OnMasterClickListener listener;
    private long selectedMasterId = -1;

    public interface OnMasterClickListener {
        void onMasterClick(MasterBatchTraceabilityEntity master);
        void onMasterLongClick(MasterBatchTraceabilityEntity master);
    }

    public MasterTraceabilityAdapter(List<MasterBatchTraceabilityEntity> masters, OnMasterClickListener listener) {
        this.masters = masters;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MasterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_master_traceability, parent, false);
        return new MasterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MasterViewHolder holder, int position) {
        MasterBatchTraceabilityEntity master = masters.get(position);
        holder.bind(master, listener, selectedMasterId);
    }

    @Override
    public int getItemCount() {
        return masters != null ? masters.size() : 0;
    }

    public void updateMasters(List<MasterBatchTraceabilityEntity> newMasters) {
        this.masters = newMasters;
        notifyDataSetChanged();
    }

    public void setSelectedMasterId(long masterId) {
        selectedMasterId = masterId;
        notifyDataSetChanged();
    }

    static class MasterViewHolder extends RecyclerView.ViewHolder {

        private TextView tvMasterBatchNumber;
        private TextView tvShipmentName;
        private TextView tvBuyer;
        private TextView tvDate;
        private TextView tvWeight;
        private TextView tvBatches;
        private CardView cardView;

        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        MasterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMasterBatchNumber = itemView.findViewById(R.id.tvMasterBatchNumber);
            tvShipmentName = itemView.findViewById(R.id.tvShipmentName);
            tvBuyer = itemView.findViewById(R.id.tvBuyer);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvBatches = itemView.findViewById(R.id.tvBatches);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(MasterBatchTraceabilityEntity master, OnMasterClickListener listener, long selectedMasterId) {
            tvMasterBatchNumber.setText("#" + master.getMasterBatchNumber());
            tvShipmentName.setText(master.getShipmentName());
            tvBuyer.setText(master.getBuyer() != null ? master.getBuyer() : "No buyer");
            tvDate.setText(master.getFormattedExportDate());
            tvWeight.setText(master.getFormattedWeight());
            tvBatches.setText(master.getTotalBatches() + " batches");

            LinearLayout content = itemView.findViewById(R.id.masterTraceabilityItemContent);
            if (content != null) {
                GradientDrawable background = new GradientDrawable();
                background.setColor(master.getId() == selectedMasterId ? 0xFFE2F1E8 : 0xFFFFFFFF);
                float radius = 18 * itemView.getResources().getDisplayMetrics().density;
                background.setCornerRadius(radius);
                content.setBackground(background);
            }

            cardView.setOnClickListener(v -> listener.onMasterClick(master));
            cardView.setOnLongClickListener(v -> {
                listener.onMasterLongClick(master);
                return true;
            });
        }
    }
}
