package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.VerificationEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class VerificationAdapter extends RecyclerView.Adapter<VerificationAdapter.VerificationViewHolder> {

    private List<VerificationEntity> verifications;
    private OnVerificationClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnVerificationClickListener {
        void onVerificationClick(VerificationEntity verification);
    }

    public VerificationAdapter(List<VerificationEntity> verifications, OnVerificationClickListener listener) {
        this.verifications = verifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VerificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_verification, parent, false);
        return new VerificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VerificationViewHolder holder, int position) {
        VerificationEntity verification = verifications.get(position);
        holder.bind(verification, listener);
    }

    @Override
    public int getItemCount() {
        return verifications != null ? verifications.size() : 0;
    }

    public void updateVerifications(List<VerificationEntity> newVerifications) {
        this.verifications = newVerifications;
        notifyDataSetChanged();
    }

    static class VerificationViewHolder extends RecyclerView.ViewHolder {

        private TextView tvVerificationId;
        private TextView tvVerificationType;
        private TextView tvVerifiedBy;
        private TextView tvVerificationDate;
        private TextView tvStatus;
        private ImageView ivStatusIcon;
        private LinearLayout statusContainer;
        private TextView tvEvidenceCount;
        private ImageView ivTypeIcon;
        private CardView cardView;

        VerificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVerificationId = itemView.findViewById(R.id.tvVerificationId);
            tvVerificationType = itemView.findViewById(R.id.tvVerificationType);
            tvVerifiedBy = itemView.findViewById(R.id.tvVerifiedBy);
            tvVerificationDate = itemView.findViewById(R.id.tvVerificationDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            statusContainer = itemView.findViewById(R.id.statusContainer);
            tvEvidenceCount = itemView.findViewById(R.id.tvEvidenceCount);
            ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(VerificationEntity verification, OnVerificationClickListener listener) {
            tvVerificationId.setText(verification.getVerificationId());
            tvVerifiedBy.setText(verification.getVerifiedBy() != null ?
                    verification.getVerifiedBy() : "Not specified");
            tvVerificationDate.setText(verification.getFormattedVerificationDate());

            // ✅ Set verification type with icon
            int typeIconResId = verification.getVerificationTypeIconResId();
            ivTypeIcon.setImageResource(typeIconResId);
            tvVerificationType.setText(verification.getVerificationTypeDisplay());

            // ✅ Set status with icon
            int statusIconResId = verification.getStatusIconResId();
            ivStatusIcon.setImageResource(statusIconResId);
            tvStatus.setText(verification.getStatusDisplay());

            // ✅ Set status colors
            int bgDrawable;
            int bgColorRes;
            switch (verification.getStatus()) {
                case VERIFIED:
                    bgDrawable = R.drawable.bg_status_certified;
                    bgColorRes = R.color.status_certified;
                    break;
                case PENDING:
                    bgDrawable = R.drawable.bg_status_audit_ready;
                    bgColorRes = R.color.status_audit_ready;
                    break;
                case REJECTED:
                    bgDrawable = R.drawable.bg_status_expired;
                    bgColorRes = R.color.status_expired;
                    break;
                default:
                    bgDrawable = R.drawable.bg_status_not_started;
                    bgColorRes = R.color.status_not_started;
            }

            // ✅ Background gets the status color
            statusContainer.setBackgroundResource(bgDrawable);

            // ✅ Icon gets WHITE so it's visible on the colored background
            ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

            // ✅ Text also gets WHITE
            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

            // ✅ Type icon color (use primary color)
            ivTypeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));

            // ✅ Evidence count
            int evidenceCount = verification.getEvidenceCount();
            if (evidenceCount > 0) {
                tvEvidenceCount.setVisibility(View.VISIBLE);
                tvEvidenceCount.setText(evidenceCount + " evidence" + (evidenceCount > 1 ? "s" : ""));
                tvEvidenceCount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            } else {
                tvEvidenceCount.setVisibility(View.GONE);
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVerificationClick(verification);
                }
            });
        }
    }
}