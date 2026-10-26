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
import com.afrinuts.farmos.data.local.entity.AuditEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AuditAdapter extends RecyclerView.Adapter<AuditAdapter.AuditViewHolder> {

    private List<AuditEntity> audits;
    private OnAuditClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnAuditClickListener {
        void onAuditClick(AuditEntity audit);
    }

    public AuditAdapter(List<AuditEntity> audits, OnAuditClickListener listener) {
        this.audits = audits;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AuditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_audit, parent, false);
        return new AuditViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuditViewHolder holder, int position) {
        AuditEntity audit = audits.get(position);
        holder.bind(audit, listener);
    }

    @Override
    public int getItemCount() {
        return audits != null ? audits.size() : 0;
    }

    public void updateAudits(List<AuditEntity> newAudits) {
        this.audits = newAudits;
        notifyDataSetChanged();
    }

    static class AuditViewHolder extends RecyclerView.ViewHolder {

        private TextView tvAuditId;
        private TextView tvAuditType;
        private TextView tvAuditorName;
        private TextView tvAuditDate;
        private TextView tvStatus;
        private ImageView ivStatusIcon;
        private LinearLayout statusContainer;
        private TextView tvFindingsCount;
        private CardView cardView;

        AuditViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuditId = itemView.findViewById(R.id.tvAuditId);
            tvAuditType = itemView.findViewById(R.id.tvAuditType);
            tvAuditorName = itemView.findViewById(R.id.tvAuditorName);
            tvAuditDate = itemView.findViewById(R.id.tvAuditDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            statusContainer = itemView.findViewById(R.id.statusContainer);
            tvFindingsCount = itemView.findViewById(R.id.tvFindingsCount);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(AuditEntity audit, OnAuditClickListener listener) {
            tvAuditId.setText(audit.getAuditId());
            tvAuditType.setText(audit.getAuditTypeDisplay());
            tvAuditorName.setText(audit.getAuditorName() != null ? audit.getAuditorName() : "No auditor");
            tvAuditDate.setText(audit.getFormattedAuditDate());

            // Set status text (just the display name, no icon)
            tvStatus.setText(audit.getStatusDisplay());

            // Set status icon
            int iconResId = audit.getStatusIconResId();
            ivStatusIcon.setImageResource(iconResId);

            int bgDrawable;
            switch (audit.getStatus()) {
                case PASS:
                    bgDrawable = R.drawable.bg_status_certified;
                    break;
                case PASS_WITH_CONDITIONS:
                    bgDrawable = R.drawable.bg_status_audit_ready;
                    break;
                case FAIL:
                    bgDrawable = R.drawable.bg_status_expired;
                    break;
                default:
                    bgDrawable = R.drawable.bg_status_not_started;
            }

            // ✅ Background gets the status color
            statusContainer.setBackgroundResource(bgDrawable);

            // ✅ ICON gets WHITE so it's visible on the colored background
            ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

            // ✅ TEXT also gets WHITE
            tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

            // Findings count
            int findingsCount = audit.getFindingsCount();
            int openFindings = audit.getOpenFindingsCount();

            if (findingsCount > 0) {
                tvFindingsCount.setVisibility(View.VISIBLE);
                tvFindingsCount.setText(findingsCount + " finding" + (findingsCount > 1 ? "s" : "") +
                        " (" + openFindings + " open)");

                if (openFindings > 0) {
                    tvFindingsCount.setBackgroundResource(R.drawable.bg_status_audit_ready);
                    tvFindingsCount.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
                } else {
                    tvFindingsCount.setBackgroundResource(R.drawable.bg_status_certified);
                    tvFindingsCount.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
                }
            } else {
                tvFindingsCount.setVisibility(View.GONE);
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAuditClick(audit);
                }
            });
        }
    }
}