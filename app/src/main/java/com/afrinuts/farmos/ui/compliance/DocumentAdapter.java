package com.afrinuts.farmos.ui.compliance;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.DocumentEntity;

import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<DocumentEntity> documents;
    private final OnDocumentClickListener listener;

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

    class DocumentViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView type;
        private final TextView id;
        private final TextView issueDate;
        private final TextView expiryDate;
        private final TextView status;
        private final ImageView typeIcon;
        private final FrameLayout typeTile;

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
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDocumentClick(document);
            });
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
