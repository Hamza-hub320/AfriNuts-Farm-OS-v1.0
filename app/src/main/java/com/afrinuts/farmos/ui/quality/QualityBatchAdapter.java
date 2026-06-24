package com.afrinuts.farmos.ui.quality;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class QualityBatchAdapter extends RecyclerView.Adapter<QualityBatchAdapter.BatchViewHolder> {

    private List<QualityBatchEntity> batches;
    private OnBatchClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnBatchClickListener {
        void onBatchClick(QualityBatchEntity batch);
    }

    public QualityBatchAdapter(List<QualityBatchEntity> batches, OnBatchClickListener listener) {
        this.batches = batches;
        this.listener = listener;
    }

    public void updateBatches(List<QualityBatchEntity> newBatches) {
        this.batches = newBatches;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quality_batch, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        QualityBatchEntity batch = batches.get(position);
        holder.bind(batch, listener);
    }

    @Override
    public int getItemCount() {
        return batches.size();
    }

    static class BatchViewHolder extends RecyclerView.ViewHolder {

        private TextView tvBatchNumber;
        private TextView tvGrade;
        private TextView tvQualityScore;
        private TextView tvMoisture;
        private TextView tvDefectiveRate;
        private TextView tvKernelRatio;
        private TextView tvWeight;
        private TextView tvDate;
        private TextView tvBlockName;
        private CardView cardView;

        BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvQualityScore = itemView.findViewById(R.id.tvQualityScore);
            tvMoisture = itemView.findViewById(R.id.tvMoisture);
            tvDefectiveRate = itemView.findViewById(R.id.tvDefectiveRate);
            tvKernelRatio = itemView.findViewById(R.id.tvKernelRatio);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBlockName = itemView.findViewById(R.id.tvBlockName);
            cardView = (CardView) itemView;
        }

        void bind(QualityBatchEntity batch, OnBatchClickListener listener) {
            // Batch number
            tvBatchNumber.setText("#" + batch.getBatchNumber());

            // Grade with icon
            tvGrade.setText(batch.getQualityGrade().getIcon() + " " +
                    batch.getQualityGrade().getDisplayName());

            // Quality score
            tvQualityScore.setText(String.valueOf((int) batch.getQualityScore()));

            // Metrics
            tvMoisture.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
            tvDefectiveRate.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate()));
            tvKernelRatio.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getKernelRatio()));
            tvWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

            // Date
            tvDate.setText(batch.getFormattedDate());

            // Block name (if available)
            if (batch.getBlockId() != null) {
                tvBlockName.setText("Block " + batch.getBlockId());
                tvBlockName.setVisibility(View.VISIBLE);
            } else {
                tvBlockName.setVisibility(View.GONE);
            }

            // Click listener
            cardView.setOnClickListener(v -> listener.onBatchClick(batch));
        }
    }
}