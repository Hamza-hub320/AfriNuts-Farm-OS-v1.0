package com.afrinuts.farmos.ui.quality;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        private TextView tvSupplierName;
        private TextView tvSourceType;
        private TextView tvBatchNumber;
        private TextView tvGrade;
        private TextView tvQualityScore;
        private TextView tvMoisture;
        private TextView tvDefectiveRate;
        private TextView tvKernelRatio;
        private TextView tvWeight;
        private TextView tvDate;
        private CardView cardView;
        private ImageView ivSupplierIcon;
        private ImageView ivSourceIcon;
        private ImageView ivGradeIcon;

        BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSupplierName = itemView.findViewById(R.id.tvSupplierName);
            tvSourceType = itemView.findViewById(R.id.tvSourceType);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvQualityScore = itemView.findViewById(R.id.tvQualityScore);
            tvMoisture = itemView.findViewById(R.id.tvMoisture);
            tvDefectiveRate = itemView.findViewById(R.id.tvDefectiveRate);
            tvKernelRatio = itemView.findViewById(R.id.tvKernelRatio);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvDate = itemView.findViewById(R.id.tvDate);
            cardView = itemView.findViewById(R.id.cardView);
            ivSupplierIcon = itemView.findViewById(R.id.ivSupplierIcon);
            ivSourceIcon = itemView.findViewById(R.id.ivSourceIcon);
            ivGradeIcon = itemView.findViewById(R.id.ivGradeIcon);
        }

        void bind(QualityBatchEntity batch, OnBatchClickListener listener) {
            // ✅ Fix: Use the correct color reference or remove color filter
            int textColor = itemView.getContext().getColor(R.color.on_surface);

            // Supplier name with vector icon
            if (batch.getSupplierName() != null && !batch.getSupplierName().isEmpty()) {
                tvSupplierName.setText(batch.getSupplierName());
                tvSupplierName.setVisibility(View.VISIBLE);
                if (ivSupplierIcon != null) {
                    ivSupplierIcon.setVisibility(View.VISIBLE);
                    ivSupplierIcon.setImageResource(R.drawable.ic_person);
                    // Use a valid color
                    ivSupplierIcon.setColorFilter(itemView.getContext().getColor(R.color.on_surface));
                }
            } else {
                tvSupplierName.setVisibility(View.GONE);
                if (ivSupplierIcon != null) {
                    ivSupplierIcon.setVisibility(View.GONE);
                }
            }

            // Source type with vector icon
            if (batch.getSourceType() != null) {
                tvSourceType.setText(batch.getSourceType().getDisplayName());
                tvSourceType.setVisibility(View.VISIBLE);
                if (ivSourceIcon != null) {
                    ivSourceIcon.setVisibility(View.VISIBLE);
                    // Set appropriate icon based on source type
                    switch (batch.getSourceType()) {
                        case OWN_FARM:
                            ivSourceIcon.setImageResource(R.drawable.ic_farm);
                            break;
                        case COOPERATIVE:
                            ivSourceIcon.setImageResource(R.drawable.ic_handshake);
                            break;
                        case INDEPENDENT:
                            ivSourceIcon.setImageResource(R.drawable.ic_person);
                            break;
                        case REGIONAL_SUPPLIER:
                            ivSourceIcon.setImageResource(R.drawable.ic_local_shipping);
                            break;
                        case PROCESSING_CENTER:
                            ivSourceIcon.setImageResource(R.drawable.ic_factory);
                            break;
                        default:
                            ivSourceIcon.setImageResource(R.drawable.ic_handshake);
                            break;
                    }
                    ivSourceIcon.setColorFilter(itemView.getContext().getColor(R.color.primary));
                }
            } else {
                tvSourceType.setVisibility(View.GONE);
                if (ivSourceIcon != null) {
                    ivSourceIcon.setVisibility(View.GONE);
                }
            }

            // ✅ FIXED: Set the actual batch number
            String batchNumber = batch.getBatchNumber();
            if (batchNumber != null && !batchNumber.isEmpty()) {
                tvBatchNumber.setText("#" + batchNumber);
            } else {
                tvBatchNumber.setText("#" + batch.getBatchNumber());
            }

            // ✅ FIXED: Removed getIcon() call - just set the display name
            tvGrade.setText(batch.getQualityGrade().getDisplayName());

            // Grade with vector icon
            if (ivGradeIcon != null) {
                ivGradeIcon.setVisibility(View.VISIBLE);
                switch (batch.getQualityGrade()) {
                    case PREMIUM:
                        ivGradeIcon.setImageResource(R.drawable.ic_star);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.accent));
                        break;
                    case STANDARD:
                        ivGradeIcon.setImageResource(R.drawable.ic_check_circle);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.primary));
                        break;
                    case PROCESSING:
                        ivGradeIcon.setImageResource(R.drawable.ic_factory);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.navy));
                        break;
                    default:
                        ivGradeIcon.setImageResource(R.drawable.ic_star);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.accent));
                        break;
                }
            }

            // Quality score
            tvQualityScore.setText(String.valueOf((int) batch.getQualityScore()));

            // Metrics
            tvMoisture.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
            tvDefectiveRate.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate()));
            tvKernelRatio.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getKernelRatio()));
            tvWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

            // Date
            tvDate.setText(batch.getFormattedDate());

            // Click listener
            cardView.setOnClickListener(v -> listener.onBatchClick(batch));
        }
    }
}