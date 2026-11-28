package com.afrinuts.farmos.ui.suppliers;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.QualityBatchEntity;
import com.afrinuts.farmos.ui.quality.QualityBatchDetailActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SupplierBatchAdapter extends RecyclerView.Adapter<SupplierBatchAdapter.BatchViewHolder> {

    private List<QualityBatchEntity> batches;
    private OnBatchUpdatedListener onBatchUpdatedListener;
    private OnBatchDisputeClickListener disputeListener;
    private final boolean compactRows;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnBatchUpdatedListener {
        void onBatchUpdated();
    }

    public interface OnBatchDisputeClickListener {
        void onBatchDisputeClick(QualityBatchEntity batch);
    }

    public SupplierBatchAdapter(List<QualityBatchEntity> batches) {
        this(batches, false);
    }

    public SupplierBatchAdapter(List<QualityBatchEntity> batches, boolean compactRows) {
        this.batches = batches != null ? batches : new ArrayList<>();
        this.compactRows = compactRows;
    }

    public void setOnBatchUpdatedListener(OnBatchUpdatedListener listener) {
        this.onBatchUpdatedListener = listener;
    }

    public void setOnBatchDisputeClickListener(OnBatchDisputeClickListener l) {
        this.disputeListener = l;
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(compactRows ? R.layout.item_supplier_batch_compact : R.layout.item_supplier_batch, parent, false);
        return new BatchViewHolder(view, compactRows);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        if (position < batches.size()) {
            QualityBatchEntity batch = batches.get(position);
            holder.bind(batch, onBatchUpdatedListener, disputeListener, position == batches.size() - 1);
        }
    }

    @Override
    public int getItemCount() {
        return batches != null ? batches.size() : 0;
    }

    public void updateBatches(List<QualityBatchEntity> newBatches) {
        this.batches = newBatches != null ? newBatches : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class BatchViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvBatchNumber;
        private final TextView tvDate;
        private final TextView tvWeight;
        private final TextView tvGrade;
        private final TextView tvQualityScore;
        private final ImageView ivGradeIcon;
        private final View cardView;
        private final View btnDisputeBatch;
        private final boolean compact;
        private final View gradeDot;
        private final View rowDivider;

        BatchViewHolder(@NonNull View itemView, boolean compact) {
            super(itemView);
            this.compact = compact;
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvQualityScore = itemView.findViewById(R.id.tvQualityScore);
            ivGradeIcon = itemView.findViewById(R.id.ivGradeIcon);
            cardView = itemView.findViewById(R.id.cardView);
            btnDisputeBatch = itemView.findViewById(R.id.btnDisputeBatch);
            gradeDot = itemView.findViewById(R.id.viewGradeDot);
            rowDivider = itemView.findViewById(R.id.viewRowDivider);
        }

        void bind(QualityBatchEntity batch,
                  OnBatchUpdatedListener listener,
                  OnBatchDisputeClickListener disputeClick,
                  boolean isLast) {
            tvBatchNumber.setText(compact ? batch.getBatchNumber() : "#" + batch.getBatchNumber());
            if (compact) {
                tvDate.setText(new SimpleDateFormat("MMM d", Locale.getDefault()).format(batch.getProcessingDate())
                        + " · KOR " + String.format(Locale.getDefault(), "%.0f", batch.getKorGPerKg()) + " g/kg");
            } else {
                tvDate.setText(batch.getFormattedDate());
            }
            tvWeight.setText(String.format(Locale.getDefault(), "%,.0f kg", batch.getWeightKg()));
            tvGrade.setText(batch.getQualityGrade().getDisplayName());

            if (compact) {
                tvGrade.setText(batch.getQualityGrade().getDisplayName().toUpperCase(Locale.ROOT));
                int dotColor;
                int tagColor;
                int tagTextColor;
                switch (batch.getQualityGrade()) {
                    case PREMIUM:
                        dotColor = 0xFF0E6B3C; tagColor = 0xFFE3F0E7; tagTextColor = 0xFF0A5530; break;
                    case REJECT:
                        dotColor = 0xFFB3402A; tagColor = 0xFFF8E4DD; tagTextColor = 0xFFB3402A; break;
                    default:
                        dotColor = 0xFFC9A227; tagColor = 0xFFF3EAD8; tagTextColor = 0xFF7A5F22; break;
                }
                if (gradeDot != null) {
                    android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
                    dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    dot.setColor(dotColor);
                    gradeDot.setBackground(dot);
                }
                android.graphics.drawable.GradientDrawable tag = new android.graphics.drawable.GradientDrawable();
                tag.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                tag.setCornerRadius(999 * itemView.getResources().getDisplayMetrics().density);
                tag.setColor(tagColor);
                tvGrade.setBackground(tag);
                tvGrade.setTextColor(tagTextColor);
                if (rowDivider != null) rowDivider.setVisibility(isLast ? View.GONE : View.VISIBLE);
            } else if (ivGradeIcon != null) {
                ivGradeIcon.setVisibility(View.VISIBLE);
                switch (batch.getQualityGrade()) {
                    case PREMIUM:
                        ivGradeIcon.setImageResource(R.drawable.ic_star);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.accent_ink));
                        break;
                    case STANDARD:
                        ivGradeIcon.setImageResource(R.drawable.ic_check_circle);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.primary));
                        break;
                    case REJECT:
                        ivGradeIcon.setImageResource(R.drawable.ic_factory);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.navy));
                        break;
                    default:
                        ivGradeIcon.setImageResource(R.drawable.ic_star);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.accent_ink));
                        break;
                }
            }

            if (tvQualityScore != null) tvQualityScore.setText(String.valueOf((int) batch.getQualityScore()));

            cardView.setOnClickListener(v -> {
                if (itemView.getResources().getConfiguration().smallestScreenWidthDp >= 600) {
                    Intent intent = new Intent(itemView.getContext(),
                            com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity.class);
                    intent.putExtra(com.afrinuts.farmos.ui.analytics.AnalyticsHubActivity.EXTRA_OPEN_BATCH_ID,
                            batch.getId());
                    itemView.getContext().startActivity(intent);
                    return;
                }
                Intent intent = new Intent(itemView.getContext(),
                        QualityBatchDetailActivity.class);
                intent.putExtra(QualityBatchDetailActivity.EXTRA_BATCH_ID, batch.getId());
                itemView.getContext().startActivity(intent);
            });

            if (compact) {
                cardView.setOnLongClickListener(null);
            } else {
                cardView.setOnLongClickListener(v -> {
                    showEditDeliveryDialog(batch.getId(), listener);
                    return true;
                });
            }

            // 3.4: dispute this specific batch. The button intercepts its own
            // click, so the card's onClick does not fire when it is tapped.
            if (btnDisputeBatch != null) {
                btnDisputeBatch.setOnClickListener(v -> {
                    if (disputeClick != null) {
                        disputeClick.onBatchDisputeClick(batch);
                    }
                });
            }
        }

        private void showEditDeliveryDialog(long batchId, OnBatchUpdatedListener listener) {
            EditRecordDeliveryDialog dialog = EditRecordDeliveryDialog.newInstance(batchId);
            dialog.setOnDeliveryUpdatedListener(() -> {
                if (listener != null) listener.onBatchUpdated();
            });
            dialog.show(((FragmentActivity) itemView.getContext()).getSupportFragmentManager(),
                    "EditRecordDeliveryDialog");
        }
    }
}
