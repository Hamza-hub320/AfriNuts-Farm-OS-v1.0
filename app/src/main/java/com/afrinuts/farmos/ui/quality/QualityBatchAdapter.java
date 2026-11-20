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
        private TextView tvProductType;
        private TextView tvThirdMetricValue;
        private ImageView ivThirdMetricIcon;


        BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSupplierName = itemView.findViewById(R.id.tvSupplierName);
            tvSourceType = itemView.findViewById(R.id.tvSourceType);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvQualityScore = itemView.findViewById(R.id.tvQualityScore);
            tvMoisture = itemView.findViewById(R.id.tvMoisture);
            tvDefectiveRate = itemView.findViewById(R.id.tvDefectiveRate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvDate = itemView.findViewById(R.id.tvDate);
            cardView = itemView.findViewById(R.id.cardView);
            ivSupplierIcon = itemView.findViewById(R.id.ivSupplierIcon);
            ivSourceIcon = itemView.findViewById(R.id.ivSourceIcon);
            ivGradeIcon = itemView.findViewById(R.id.ivGradeIcon);
            tvProductType = itemView.findViewById(R.id.tvProductType);
            tvThirdMetricValue = itemView.findViewById(R.id.tvThirdMetricValue);
            ivThirdMetricIcon = itemView.findViewById(R.id.ivThirdMetricIcon);
        }

        void bind(QualityBatchEntity batch, OnBatchClickListener listener) {
            boolean isRcn = "RCN".equalsIgnoreCase(batch.getProductType());

            // --- Supplier (unchanged) ---
            if (batch.getSupplierName() != null && !batch.getSupplierName().isEmpty()) {
                tvSupplierName.setText(batch.getSupplierName());
                tvSupplierName.setVisibility(View.VISIBLE);
                ivSupplierIcon.setVisibility(View.VISIBLE);
                ivSupplierIcon.setImageResource(R.drawable.ic_person);
                ivSupplierIcon.setColorFilter(ContextCompat.getColor(
                        itemView.getContext(), R.color.on_surface));
            } else {
                tvSupplierName.setVisibility(View.GONE);
                ivSupplierIcon.setVisibility(View.GONE);
            }

            // --- ✅ Product type chip ---
            if (tvProductType != null) {
                tvProductType.setText(isRcn ? "RCN" : "KERNEL");
                tvProductType.setTextColor(ContextCompat.getColor(
                        itemView.getContext(),
                        isRcn ? R.color.accent_ink : R.color.primary));
            }

            // --- Source type (unchanged) ---
            if (batch.getSourceType() != null) {
                tvSourceType.setText(batch.getSourceType().getDisplayName());
                tvSourceType.setVisibility(View.VISIBLE);
                ivSourceIcon.setVisibility(View.VISIBLE);
                switch (batch.getSourceType()) {
                    case OWN_FARM:           ivSourceIcon.setImageResource(R.drawable.ic_farm);           break;
                    case COOPERATIVE:        ivSourceIcon.setImageResource(R.drawable.ic_handshake);      break;
                    case INDEPENDENT:        ivSourceIcon.setImageResource(R.drawable.ic_person);         break;
                    case REGIONAL_SUPPLIER:  ivSourceIcon.setImageResource(R.drawable.ic_local_shipping); break;
                    case PROCESSING_CENTER:  ivSourceIcon.setImageResource(R.drawable.ic_factory);        break;
                    default:                 ivSourceIcon.setImageResource(R.drawable.ic_handshake);      break;
                }
                ivSourceIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            } else {
                tvSourceType.setVisibility(View.GONE);
                ivSourceIcon.setVisibility(View.GONE);
            }

            // --- Batch number (unchanged) ---
            String batchNumber = batch.getBatchNumber();
            tvBatchNumber.setText(batchNumber != null && !batchNumber.isEmpty()
                    ? "#" + batchNumber : "#—");

            // --- Grade (unchanged — repository computes it) ---
            tvGrade.setText(batch.getQualityGrade().getDisplayName());
            switch (batch.getQualityGrade()) {
                case PREMIUM:
                    ivGradeIcon.setImageResource(R.drawable.ic_star);
                    ivGradeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.accent_ink));
                    break;
                case STANDARD:
                    ivGradeIcon.setImageResource(R.drawable.ic_check_circle);
                    ivGradeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                    break;
                case REJECT:
                    ivGradeIcon.setImageResource(R.drawable.ic_cancel);
                    ivGradeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
                    break;
                default:
                    ivGradeIcon.setImageResource(R.drawable.ic_star);
                    ivGradeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.accent_ink));
                    break;
            }

            // --- ✅ Score: use the correct score for the product type ---
            double score = isRcn ? batch.getRcnQualityScore() : batch.getKernelQualityScore();
            if (score <= 0 && batch.getQualityScore() > 0) {
                // Fallback for batches created before Step 2 recomputation
                score = batch.getQualityScore();
            }
            tvQualityScore.setText(String.valueOf((int) score));

            // --- ✅ Metrics ---
            tvMoisture.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getMoistureContent()));
            tvDefectiveRate.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getDefectiveRate()));

            if (isRcn) {
                // Third cell = KOR
                tvThirdMetricValue.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getKorPercent()));
                if (ivThirdMetricIcon != null) {
                    ivThirdMetricIcon.setImageResource(R.drawable.ic_grain);
                }
            } else {
                // Third cell = Kernel yield
                tvThirdMetricValue.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getKernelRatio()));
                if (ivThirdMetricIcon != null) {
                    ivThirdMetricIcon.setImageResource(R.drawable.ic_grain);
                }
            }

            tvWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

            // --- Date (unchanged) ---
            tvDate.setText(batch.getFormattedDate());

            // --- Click (unchanged) ---
            cardView.setOnClickListener(v -> listener.onBatchClick(batch));
        }
    }
}