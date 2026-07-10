package com.afrinuts.farmos.ui.traceability;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BatchTraceabilityEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TraceabilityAdapter extends RecyclerView.Adapter<TraceabilityAdapter.TraceabilityViewHolder> {

    private List<BatchTraceabilityEntity> traceabilities;
    private OnTraceabilityClickListener listener;

    public interface OnTraceabilityClickListener {
        void onTraceabilityClick(BatchTraceabilityEntity traceability);
    }

    public TraceabilityAdapter(List<BatchTraceabilityEntity> traceabilities, OnTraceabilityClickListener listener) {
        this.traceabilities = traceabilities != null ? traceabilities : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public TraceabilityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_traceability, parent, false);
        return new TraceabilityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TraceabilityViewHolder holder, int position) {
        BatchTraceabilityEntity traceability = traceabilities.get(position);
        holder.bind(traceability, listener);
    }

    @Override
    public int getItemCount() {
        return traceabilities != null ? traceabilities.size() : 0;
    }

    // ✅ Method for TraceabilityFragment
    public void updateTraceabilities(List<BatchTraceabilityEntity> newTraceabilities) {
        this.traceabilities = newTraceabilities != null ? newTraceabilities : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ✅ Method for MasterTraceabilityDetailActivity
    public void updateBatches(List<BatchTraceabilityEntity> newTraceabilities) {
        this.traceabilities = newTraceabilities != null ? newTraceabilities : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ✅ Set data method (alternative)
    public void setData(List<BatchTraceabilityEntity> newTraceabilities) {
        this.traceabilities = newTraceabilities != null ? newTraceabilities : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class TraceabilityViewHolder extends RecyclerView.ViewHolder {

        private TextView tvBatchNumber;
        private TextView tvGrade;
        private TextView tvDate;
        private TextView tvWeight;
        private TextView tvBuyer;
        private TextView tvStatus;
        private CardView cardView;

        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        TraceabilityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvBuyer = itemView.findViewById(R.id.tvBuyer);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(BatchTraceabilityEntity traceability, OnTraceabilityClickListener listener) {
            tvBatchNumber.setText("#" + traceability.getBatchNumber());
            tvGrade.setText(traceability.getQualityGrade() != null ?
                    traceability.getQualityGrade() : "—");
            tvDate.setText(traceability.getFormattedProcessingDate());
            tvWeight.setText(traceability.getFormattedWeight());

            tvBuyer.setText(traceability.getBuyer() != null ?
                    traceability.getBuyer() : "No buyer");

            // Status
            if (traceability.isExported()) {
                tvStatus.setText("Exported");
                tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_light));
            } else {
                tvStatus.setText("Processing");
                tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_orange_light));
            }

            cardView.setOnClickListener(v -> listener.onTraceabilityClick(traceability));
        }
    }
}