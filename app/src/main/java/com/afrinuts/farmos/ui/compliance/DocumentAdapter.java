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
import com.afrinuts.farmos.data.local.entity.DocumentEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<DocumentEntity> documents;
    private OnDocumentClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

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
        DocumentEntity document = documents.get(position);
        holder.bind(document, listener);
    }

    @Override
    public int getItemCount() {
        return documents != null ? documents.size() : 0;
    }

    public void updateDocuments(List<DocumentEntity> newDocuments) {
        this.documents = newDocuments;
        notifyDataSetChanged();
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {

        private TextView tvDocumentId;
        private TextView tvDocumentType;
        private ImageView ivDocumentTypeIcon;
        private TextView tvDocumentName;
        private TextView tvIssueDate;
        private TextView tvExpiryDate;
        private TextView tvStatus;
        private ImageView ivStatusIcon;
        private LinearLayout statusContainer;
        private CardView cardView;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocumentId = itemView.findViewById(R.id.tvDocumentId);
            tvDocumentType = itemView.findViewById(R.id.tvDocumentType);
            ivDocumentTypeIcon = itemView.findViewById(R.id.ivDocumentTypeIcon);
            tvDocumentName = itemView.findViewById(R.id.tvDocumentName);
            tvIssueDate = itemView.findViewById(R.id.tvIssueDate);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            statusContainer = itemView.findViewById(R.id.statusContainer);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(DocumentEntity document, OnDocumentClickListener listener) {
            tvDocumentId.setText(document.getDocumentId());
            tvDocumentName.setText(document.getDocumentName());
            tvIssueDate.setText("Issued: " + document.getFormattedIssueDate());
            tvExpiryDate.setText("Expires: " + document.getFormattedExpiryDate());

            // ✅ Set document type with icon
            int docTypeIconResId = document.getDocumentTypeIconResId();
            ivDocumentTypeIcon.setImageResource(docTypeIconResId);
            ivDocumentTypeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            tvDocumentType.setText(document.getDocumentTypeDisplay());

            // ✅ Set status with icon
            int statusIconResId = document.getStatusIconResId();
            ivStatusIcon.setImageResource(statusIconResId);
            tvStatus.setText(document.getStatusDisplay());

            // ✅ Set status colors
            int bgDrawable;
            int bgColorRes;
            switch (document.getStatus()) {
                case APPROVED:
                    bgDrawable = R.drawable.bg_status_certified;
                    bgColorRes = R.color.status_certified;
                    break;
                case PENDING:
                    bgDrawable = R.drawable.bg_status_audit_ready;
                    bgColorRes = R.color.status_audit_ready;
                    break;
                case DRAFT:
                    bgDrawable = R.drawable.bg_status_not_started;
                    bgColorRes = R.color.status_not_started;
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

            // ✅ Show expiry warning
            if (document.isExpiringSoon()) {
                tvExpiryDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_warning));
            } else if (document.isExpired()) {
                tvExpiryDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_fail));
            } else {
                tvExpiryDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDocumentClick(document);
                }
            });
        }
    }
}