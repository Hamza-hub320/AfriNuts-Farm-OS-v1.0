package com.afrinuts.farmos.ui.compliance;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
    private String selectedCertificationId;

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
        holder.bind(certification, listener, certification.getCertificationId() != null
                && certification.getCertificationId().equals(selectedCertificationId),
                position, getItemCount());
    }

    @Override
    public int getItemCount() {
        return certifications != null ? certifications.size() : 0;
    }

    public void updateCertifications(List<CertificationEntity> newCertifications) {
        this.certifications = newCertifications;
        notifyDataSetChanged();
    }

    public void setSelectedCertificationId(String certificationId) {
        selectedCertificationId = certificationId;
        notifyDataSetChanged();
    }

    public String getSelectedCertificationId() {
        return selectedCertificationId;
    }

    static class CertificationViewHolder extends RecyclerView.ViewHolder {

        private TextView tvCertificationName;
        private TextView tvCertificationBody;
        private TextView tvStatus;
        private TextView tvDates;
        private CardView cardView;
        private View divider;
        private TextView targetDateLabel;

        CertificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCertificationName = itemView.findViewById(R.id.tvCertificationName);
            tvCertificationBody = itemView.findViewById(R.id.tvCertificationBody);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDates = itemView.findViewById(R.id.tvDates);
            cardView = itemView.findViewById(R.id.cardView);
            divider = itemView.findViewById(R.id.itemDivider);
            targetDateLabel = itemView.findViewById(R.id.targetDateLabel);
        }

        void bind(CertificationEntity certification, OnCertificationClickListener listener,
                  boolean selected, int position, int itemCount) {
            String body = certification.getCertificationBody();
            String id = certification.getCertificationId();
            String name = certification.getCertificationName();
            if (itemView.getResources().getConfiguration().screenWidthDp >= 600
                    && selected
                    && body != null && !body.trim().isEmpty()
                    && (name == null || !name.toLowerCase(Locale.ROOT)
                    .contains(body.toLowerCase(Locale.ROOT)))) {
                name = name + " (" + body + ")";
            }
            tvCertificationName.setText(name);
            boolean tablet = itemView.getResources().getConfiguration().screenWidthDp >= 600;
            tvCertificationBody.setText(body == null || body.trim().isEmpty()
                    ? id : (tablet && !selected ? body : body + " · " + id));
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

            tvDates.setText(tablet ? "Target " + certification.getFormattedTargetDate()
                    : certification.getFormattedTargetDate());
            cardView.setCardBackgroundColor(selected ? 0xFFE3F0E7 : 0xFFFFFFFF);
            if (tablet) {
                targetDateLabel.setVisibility(View.GONE);
                divider.setVisibility(!selected && position < itemCount - 1 ? View.VISIBLE : View.GONE);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardView.getLayoutParams();
                params.topMargin = params.bottomMargin = selected ? dp(6) : 0;
                cardView.setLayoutParams(params);
                cardView.setRadius(selected ? dp(18) : 0);
                LinearLayout itemContent = (LinearLayout) cardView.getChildAt(0);
                itemContent.setPadding(dp(12), dp(selected ? 12 : 8), dp(12), dp(selected ? 8 : 4));
                View dateRow = (View) tvCertificationBody.getParent();
                ViewGroup.MarginLayoutParams dateParams = (ViewGroup.MarginLayoutParams) dateRow.getLayoutParams();
                dateParams.topMargin = dp(selected ? 4 : 2);
                dateRow.setLayoutParams(dateParams);
                ViewGroup.MarginLayoutParams dividerParams = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
                dividerParams.topMargin = dp(6);
                divider.setLayoutParams(dividerParams);
                View spacer = itemContent.getChildAt(itemContent.getChildCount() - 1);
                ViewGroup.LayoutParams spacerParams = spacer.getLayoutParams();
                spacerParams.height = dp(selected ? 8 : 4);
                spacer.setLayoutParams(spacerParams);
            }

            // Navigate to detail
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCertificationClick(certification);
                }
            });
        }

        private int dp(float value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }
    }
}
