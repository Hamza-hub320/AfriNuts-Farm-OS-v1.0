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
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnBatchUpdatedListener {
        void onBatchUpdated();
    }

    public interface OnBatchDisputeClickListener {
        void onBatchDisputeClick(QualityBatchEntity batch);
    }

    public SupplierBatchAdapter(List<QualityBatchEntity> batches) {
        this.batches = batches != null ? batches : new ArrayList<>();
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
                .inflate(R.layout.item_supplier_batch, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        if (position < batches.size()) {
            QualityBatchEntity batch = batches.get(position);
            holder.bind(batch, onBatchUpdatedListener, disputeListener);
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
        private final CardView cardView;
        private final View btnDisputeBatch;

        BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvQualityScore = itemView.findViewById(R.id.tvQualityScore);
            ivGradeIcon = itemView.findViewById(R.id.ivGradeIcon);
            cardView = itemView.findViewById(R.id.cardView);
            btnDisputeBatch = itemView.findViewById(R.id.btnDisputeBatch);
        }

        void bind(QualityBatchEntity batch,
                  OnBatchUpdatedListener listener,
                  OnBatchDisputeClickListener disputeClick) {
            tvBatchNumber.setText("#" + batch.getBatchNumber());
            tvDate.setText(batch.getFormattedDate());
            tvWeight.setText(String.format(Locale.getDefault(), "%.0f kg", batch.getWeightKg()));
            tvGrade.setText(batch.getQualityGrade().getDisplayName());

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
                    case REJECT:
                        ivGradeIcon.setImageResource(R.drawable.ic_factory);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.navy));
                        break;
                    default:
                        ivGradeIcon.setImageResource(R.drawable.ic_star);
                        ivGradeIcon.setColorFilter(itemView.getContext().getColor(R.color.accent));
                        break;
                }
            }

            tvQualityScore.setText(String.valueOf((int) batch.getQualityScore()));

            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(),
                        QualityBatchDetailActivity.class);
                intent.putExtra(QualityBatchDetailActivity.EXTRA_BATCH_ID, batch.getId());
                itemView.getContext().startActivity(intent);
            });

            cardView.setOnLongClickListener(v -> {
                showEditDeliveryDialog(batch.getId(), listener);
                return true;
            });

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