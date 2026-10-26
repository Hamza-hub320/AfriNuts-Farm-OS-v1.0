package com.afrinuts.farmos.ui.compliance;

import android.content.Intent;
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
import com.afrinuts.farmos.data.local.entity.CertificationEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CertificationAdapter extends RecyclerView.Adapter<CertificationAdapter.CertificationViewHolder> {

    private List<CertificationEntity> certifications;
    private OnCertificationClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnCertificationClickListener {
        void onCertificationClick(CertificationEntity certification);
    }

    public CertificationAdapter(List<CertificationEntity> certifications, OnCertificationClickListener listener) {
        this.certifications = certifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CertificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_certification, parent, false);
        return new CertificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CertificationViewHolder holder, int position) {
        CertificationEntity certification = certifications.get(position);
        holder.bind(certification, listener);
    }

    @Override
    public int getItemCount() {
        return certifications != null ? certifications.size() : 0;
    }

    public void updateCertifications(List<CertificationEntity> newCertifications) {
        this.certifications = newCertifications;
        notifyDataSetChanged();
    }

    static class CertificationViewHolder extends RecyclerView.ViewHolder {

        private TextView tvCertificationId;
        private TextView tvCertificationName;
        private TextView tvCertificationBody;
        private TextView tvStatus;
        private ImageView ivStatusIcon;  // ✅ Add status icon
        private TextView tvScope;
        private TextView tvDates;
        private TextView tvStandards;
        private CardView cardView;

        CertificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCertificationId = itemView.findViewById(R.id.tvCertificationId);
            tvCertificationName = itemView.findViewById(R.id.tvCertificationName);
            tvCertificationBody = itemView.findViewById(R.id.tvCertificationBody);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);  // ✅ Initialize icon view
            tvScope = itemView.findViewById(R.id.tvScope);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvStandards = itemView.findViewById(R.id.tvStandards);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(CertificationEntity certification, OnCertificationClickListener listener) {
            // ✅ Show certification ID
            tvCertificationId.setText(certification.getCertificationId());
            tvCertificationName.setText(certification.getCertificationName());
            tvCertificationBody.setText("Issued by: " + certification.getCertificationBody());

            // ✅ Set status text (without icon)
            tvStatus.setText(certification.getStatusDisplay());

            // ✅ Set status icon
            int iconResId = certification.getStatus().getIconResId();
            ivStatusIcon.setImageResource(iconResId);

            // ✅ Color the icon based on status
            int colorResId;
            switch (certification.getStatus()) {
                case CERTIFIED:
                    colorResId = R.color.status_certified;
                    break;
                case IN_PROGRESS:
                    colorResId = R.color.status_in_progress;
                    break;
                case AUDIT_READY:
                    colorResId = R.color.status_audit_ready;
                    break;
                case EXPIRED:
                    colorResId = R.color.status_expired;
                    break;
                case SUSPENDED:
                    colorResId = R.color.status_suspended;
                    break;
                default:
                    colorResId = R.color.status_not_started;
                    break;
            }
            ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), colorResId));

            // Status background color
            switch (certification.getStatus()) {
                case CERTIFIED:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_certified);
                    break;
                case IN_PROGRESS:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_in_progress);
                    break;
                case AUDIT_READY:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_audit_ready);
                    break;
                case EXPIRED:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_expired);
                    break;
                case SUSPENDED:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_suspended);
                    break;
                default:
                    tvStatus.setBackgroundResource(R.drawable.bg_status_not_started);
                    break;
            }

            tvScope.setText(certification.getScope() != null ? certification.getScope() : "No scope defined");
            tvDates.setText("Target: " + certification.getFormattedTargetDate() +
                    " | Expires: " + certification.getFormattedExpiryDate());
            tvStandards.setText("Standards: " + certification.getStandardsDisplay());

            // Navigate to detail
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCertificationClick(certification);
                }
            });
        }
    }
}