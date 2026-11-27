package com.afrinuts.farmos.ui.compliance;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<DocumentEntity> documents;
    private final OnDocumentClickListener listener;
    private String selectedDocumentId;

    public interface OnDocumentClickListener {
        void onDocumentClick(DocumentEntity document);
    }

    public DocumentAdapter(List<DocumentEntity> documents, OnDocumentClickListener listener) {
        this.documents = documents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        holder.bind(documents.get(position));
    }

    @Override
    public int getItemCount() {
        return documents == null ? 0 : documents.size();
    }

    public void updateDocuments(List<DocumentEntity> newDocuments) {
        documents = newDocuments;
        notifyDataSetChanged();
    }

    public String getSelectedDocumentId() { return selectedDocumentId; }

    public void setSelectedDocumentId(String documentId) {
        selectedDocumentId = documentId;
        notifyDataSetChanged();
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView type;
        private final TextView id;
        private final TextView issueDate;
        private final TextView expiryDate;
        private final TextView status;
        private final ImageView typeIcon;
        private final FrameLayout typeTile;
        private final View tabletDivider;
        private final LinearLayout content;
        private final LinearLayout metadata;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvDocumentName);
            type = itemView.findViewById(R.id.tvDocumentType);
            id = itemView.findViewById(R.id.tvDocumentId);
            issueDate = itemView.findViewById(R.id.tvIssueDate);
            expiryDate = itemView.findViewById(R.id.tvExpiryDate);
            status = itemView.findViewById(R.id.tvStatus);
            typeIcon = itemView.findViewById(R.id.ivDocumentTypeIcon);
            typeTile = itemView.findViewById(R.id.documentTypeTile);
            content = (LinearLayout) ((androidx.cardview.widget.CardView) itemView).getChildAt(0);
            metadata = (LinearLayout) content.getChildAt(1);
            tabletDivider = new View(itemView.getContext());
            tabletDivider.setBackgroundColor(0xFFE9E1CF);
            itemView.setClipToOutline(true);
            ((androidx.cardview.widget.CardView) itemView).addView(tabletDivider,
                    new FrameLayout.LayoutParams(-1, Math.round(1 * itemView.getResources().getDisplayMetrics().density),
                            android.view.Gravity.BOTTOM));
        }

        void bind(DocumentEntity document) {
            name.setText(document.getDocumentName());
            type.setText(document.getDocumentTypeDisplay());
            id.setText(document.getDocumentId());
            issueDate.setText(document.getFormattedIssueDate());
            // Keep the stored expiry value visible even when it is missing (formatted as a dash).
            expiryDate.setText(document.getFormattedExpiryDate());

            DocumentEntity.DocumentType documentType = document.getDocumentType();
            int icon = documentType == null ? R.drawable.ic_document : documentType.getIconResId();
            typeIcon.setImageResource(icon);
            int tileColor;
            int iconColor;
            if (documentType == DocumentEntity.DocumentType.CERTIFICATE
                    || documentType == DocumentEntity.DocumentType.INPUT_CERTIFICATE) {
                tileColor = Color.parseColor("#E3F0E7");
                iconColor = Color.parseColor("#0A5530");
            } else if (documentType == DocumentEntity.DocumentType.AUDIT_REPORT
                    || documentType == DocumentEntity.DocumentType.LICENSE) {
                tileColor = Color.parseColor("#F3EAD8");
                iconColor = Color.parseColor("#7A5F22");
            } else {
                tileColor = Color.parseColor("#EFEAE0");
                iconColor = Color.parseColor("#6B675F");
            }
            GradientDrawable tile = new GradientDrawable();
            tile.setColor(tileColor);
            tile.setCornerRadius(itemView.getResources().getDisplayMetrics().density * 11);
            typeTile.setBackground(tile);
            typeIcon.setColorFilter(iconColor);

            bindStatus(document);
            boolean tablet = itemView.getResources().getConfiguration().screenWidthDp >= 600;
            if (tablet) {
                boolean selected = document.getDocumentId() != null
                        && document.getDocumentId().equals(selectedDocumentId);
                androidx.cardview.widget.CardView card = (androidx.cardview.widget.CardView) itemView;
                card.setCardBackgroundColor(selected ? 0xFFE3F0E7 : Color.TRANSPARENT);
                card.setRadius(selected ? itemView.getResources().getDisplayMetrics().density * 18 : 0);
                content.setPadding(dp(12), dp(10), dp(12), dp(10));
                LinearLayout header = (LinearLayout) content.getChildAt(0);
                if (header.getChildCount() > 1) header.removeViewAt(1);
                if (status.getParent() instanceof ViewGroup)
                    ((ViewGroup) status.getParent()).removeView(status);
                LinearLayout statusColumn = new LinearLayout(itemView.getContext());
                statusColumn.setOrientation(LinearLayout.VERTICAL);
                statusColumn.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
                statusColumn.addView(status, new LinearLayout.LayoutParams(-2, -2));
                TextView expires = new TextView(itemView.getContext());
                boolean expired = document.getStatus() == DocumentEntity.DocumentStatus.APPROVED && document.isExpired();
                expires.setText((expired ? "Expired " : "Expires ") + document.getFormattedExpiryDate());
                expires.setTextColor(0xFF59544C);
                expires.setTextSize(13.5f);
                expires.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(itemView.getContext(), R.font.manrope_semibold));
                LinearLayout.LayoutParams expiresParams = new LinearLayout.LayoutParams(-2, -2);
                expiresParams.topMargin = dp(4);
                statusColumn.addView(expires, expiresParams);
                header.addView(statusColumn, new LinearLayout.LayoutParams(-2, -2));
                name.setTextSize(16);
                name.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(itemView.getContext(), R.font.manrope_extrabold));
                typeTile.setVisibility(View.GONE);
                id.setVisibility(View.GONE);
                type.setText(document.getDocumentTypeDisplay() + " · " + document.getDocumentId());
                type.setTextColor(0xFF59544C);
                type.setTextSize(13.5f);
                type.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(itemView.getContext(), R.font.manrope_medium));
                metadata.setPadding(0, dp(4), 0, 0);
                View internalDivider = content.getChildAt(2);
                if (internalDivider != tabletDivider && internalDivider != null) internalDivider.setVisibility(View.GONE);
                View dates = content.getChildAt(3);
                if (dates != null) dates.setVisibility(View.GONE);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
                params.bottomMargin = 0;
                card.setLayoutParams(params);
                FrameLayout.LayoutParams dividerParams = (FrameLayout.LayoutParams) tabletDivider.getLayoutParams();
                dividerParams.leftMargin = dp(12);
                dividerParams.rightMargin = dp(12);
                tabletDivider.setLayoutParams(dividerParams);
                tabletDivider.setVisibility(!selected && getAdapterPosition() < documents.size() - 1
                        ? View.VISIBLE : View.GONE);
            } else {
                tabletDivider.setVisibility(View.GONE);
            }
            itemView.setOnClickListener(v -> {
                if (tablet) {
                    selectedDocumentId = document.getDocumentId();
                    notifyDataSetChanged();
                }
                if (listener != null) listener.onDocumentClick(document);
            });
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }

        private void bindStatus(DocumentEntity document) {
            String label;
            int background;
            int foreground;
            if (document.getStatus() == DocumentEntity.DocumentStatus.APPROVED && document.isExpired()) {
                label = "EXPIRED";
                background = Color.parseColor("#F8E4DD");
                foreground = Color.parseColor("#B3402A");
            } else if (document.getStatus() == DocumentEntity.DocumentStatus.APPROVED
                    && document.isExpiringSoon()) {
                label = "EXPIRING SOON";
                background = Color.parseColor("#F6ECD2");
                foreground = Color.parseColor("#8A6414");
            } else if (document.getStatus() == DocumentEntity.DocumentStatus.APPROVED) {
                label = "VALID";
                background = Color.parseColor("#E3F0E7");
                foreground = Color.parseColor("#0A5530");
            } else {
                label = document.getStatusDisplay().toUpperCase(java.util.Locale.ROOT);
                background = Color.parseColor("#EFEAE0");
                foreground = Color.parseColor("#6B675F");
            }
            status.setText(label);
            status.setTextColor(foreground);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(background);
            pill.setCornerRadius(itemView.getResources().getDisplayMetrics().density * 999);
            status.setBackground(pill);
        }
    }
}
