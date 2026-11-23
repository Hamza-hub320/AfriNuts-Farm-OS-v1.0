package com.afrinuts.farmos.ui.compliance;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.CertificationEntity;

import java.util.List;
import java.util.Locale;

public class CertificationAdapter extends RecyclerView.Adapter<CertificationAdapter.CertificationViewHolder> {

    private List<CertificationEntity> certifications;
    private OnCertificationClickListener listener;

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

        private TextView tvCertificationName;
        private TextView tvCertificationBody;
        private TextView tvStatus;
        private TextView tvDates;
        private CardView cardView;

        CertificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCertificationName = itemView.findViewById(R.id.tvCertificationName);
            tvCertificationBody = itemView.findViewById(R.id.tvCertificationBody);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDates = itemView.findViewById(R.id.tvDates);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(CertificationEntity certification, OnCertificationClickListener listener) {
            tvCertificationName.setText(certification.getCertificationName());
            String body = certification.getCertificationBody();
            String id = certification.getCertificationId();
            tvCertificationBody.setText(body == null || body.trim().isEmpty()
                    ? id : body + " · " + id);
            tvStatus.setText(certification.getEffectiveStatusDisplay().toUpperCase(Locale.ROOT));

            int statusBackground;
            int statusForeground;
            switch (certification.getEffectiveStatus()) {
                case CERTIFIED:
                    statusBackground = 0xFFE3F0E7;
                    statusForeground = 0xFF0A5530;
                    break;
                case IN_PROGRESS:
                    statusBackground = 0xFFF3EAD8;
                    statusForeground = 0xFF735A2B;
                    break;
                case AUDIT_READY:
                    statusBackground = 0xFFF7EDCF;
                    statusForeground = 0xFF8A6414;
                    break;
                case EXPIRED:
                    statusBackground = 0xFFFCE7E2;
                    statusForeground = 0xFF9F3528;
                    break;
                case SUSPENDED:
                    statusBackground = 0xFFFCE7E2;
                    statusForeground = 0xFF9F3528;
                    break;
                default:
                    statusBackground = 0xFFEEEADF;
                    statusForeground = 0xFF716B62;
                    break;
            }
            GradientDrawable statusPill = new GradientDrawable();
            statusPill.setColor(statusBackground);
            statusPill.setCornerRadius(999 * itemView.getResources().getDisplayMetrics().density);
            tvStatus.setBackground(statusPill);
            tvStatus.setTextColor(statusForeground);

            tvDates.setText(certification.getFormattedTargetDate());

            // Navigate to detail
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCertificationClick(certification);
                }
            });
        }
    }
}
