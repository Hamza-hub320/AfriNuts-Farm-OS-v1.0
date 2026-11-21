package com.afrinuts.farmos.ui.quality;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
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
        private TextView tvMetricKernelLabel;
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
            tvMetricKernelLabel = itemView.findViewById(R.id.tvMetricKernelLabel);
            ivThirdMetricIcon = itemView.findViewById(R.id.ivThirdMetricIcon);
        }

        void bind(QualityBatchEntity batch, OnBatchClickListener listener) {
            boolean isRcn = "RCN".equalsIgnoreCase(batch.getProductType());

            // --- Supplier (unchanged) ---
            if (batch.getSupplierName() != null && !batch.getSupplierName().isEmpty()) {
                String source = batch.getSourceType() == null ? "" : batch.getSourceType().getDisplayName();
                tvSupplierName.setText(source.isEmpty() ? batch.getSupplierName()
                        : batch.getSupplierName() + " · " + source);
                tvSupplierName.setVisibility(View.VISIBLE);
                if (ivSupplierIcon != null) ivSupplierIcon.setVisibility(View.GONE);
            } else {
                tvSupplierName.setVisibility(View.GONE);
                if (ivSupplierIcon != null) ivSupplierIcon.setVisibility(View.GONE);
            }

            // --- ✅ Product type chip ---
            if (tvProductType != null) {
                tvProductType.setText(isRcn ? "RCN" : "KERNEL");
                int typeFill = isRcn ? Color.rgb(239, 234, 224) : Color.rgb(227, 240, 231);
                int typeInk = isRcn ? Color.rgb(61, 58, 53) : Color.rgb(10, 85, 48);
                tvProductType.setTextColor(typeInk);
                GradientDrawable typePill = new GradientDrawable();
                typePill.setColor(typeFill);
                typePill.setCornerRadius(itemView.getResources().getDisplayMetrics().density * 999f);
                tvProductType.setBackground(typePill);
            }

            // --- Source type (unchanged) ---
            if (batch.getSourceType() != null) {
                tvSourceType.setText(batch.getSourceType().getDisplayName());
                tvSourceType.setVisibility(View.GONE);
                if (ivSourceIcon != null) ivSourceIcon.setVisibility(View.GONE);
                // Source is included in the supplier subtitle for this card.
            } else {
                tvSourceType.setVisibility(View.GONE);
                if (ivSourceIcon != null) ivSourceIcon.setVisibility(View.GONE);
            }

            // --- Batch number (unchanged) ---
            String batchNumber = batch.getBatchNumber();
            tvBatchNumber.setText(batchNumber != null && !batchNumber.isEmpty()
                    ? "#" + batchNumber : "#—");

            // --- Grade (unchanged — repository computes it) ---
            tvGrade.setText(batch.getQualityGrade().getDisplayName().toUpperCase(Locale.ROOT));
            int badgeBackground;
            int badgeForeground;
            switch (batch.getQualityGrade()) {
                case PREMIUM:
                    badgeBackground = Color.rgb(227, 240, 231);
                    badgeForeground = Color.rgb(10, 85, 48);
                    break;
                case STANDARD:
                    badgeBackground = Color.rgb(243, 234, 216);
                    badgeForeground = Color.rgb(122, 95, 34);
                    break;
                case REJECT:
                    badgeBackground = Color.rgb(248, 229, 226);
                    badgeForeground = Color.rgb(179, 64, 42);
                    break;
                default:
                    badgeBackground = Color.rgb(227, 240, 231);
                    badgeForeground = Color.rgb(10, 85, 48);
                    break;
            }
            tvGrade.setTextColor(badgeForeground);
            GradientDrawable gradePill = new GradientDrawable();
            gradePill.setColor(badgeBackground);
            gradePill.setCornerRadius(itemView.getResources().getDisplayMetrics().density * 999f);
            tvGrade.setBackground(gradePill);
            if (ivGradeIcon != null) ivGradeIcon.setVisibility(View.GONE);

            // --- Score: use the correct score for the product type ---
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
                // RCN cards report KOR in g/kg in the third metric cell.
                if (tvMetricKernelLabel != null) tvMetricKernelLabel.setText("KOR");
                tvThirdMetricValue.setText(String.format(Locale.getDefault(), "%.0f g/kg", batch.getKorGPerKg()));
                if (ivThirdMetricIcon != null) {
                    ivThirdMetricIcon.setImageResource(R.drawable.ic_grain);
                }
            } else {
                // Kernel cards report the kernel ratio as a percentage.
                if (tvMetricKernelLabel != null) tvMetricKernelLabel.setText("KERNEL");
                tvThirdMetricValue.setText(String.format(Locale.getDefault(), "%.1f%%", batch.getKernelRatio()));
                if (ivThirdMetricIcon != null) {
                    ivThirdMetricIcon.setImageResource(R.drawable.ic_grain);
                }
            }

            tvWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));

            // --- Date (unchanged) ---
            tvDate.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(new Date(batch.getRelevantDate())));

            // --- Click (unchanged) ---
            cardView.setOnClickListener(v -> listener.onBatchClick(batch));
        }
    }
}