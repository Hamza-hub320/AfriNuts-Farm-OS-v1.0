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
import com.afrinuts.farmos.data.local.entity.InspectionEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class InspectionAdapter extends RecyclerView.Adapter<InspectionAdapter.InspectionViewHolder> {

    private List<InspectionEntity> inspections;
    private OnInspectionClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnInspectionClickListener {
        void onInspectionClick(InspectionEntity inspection);
    }

    public InspectionAdapter(List<InspectionEntity> inspections, OnInspectionClickListener listener) {
        this.inspections = inspections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public InspectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inspection, parent, false);
        return new InspectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InspectionViewHolder holder, int position) {
        InspectionEntity inspection = inspections.get(position);
        holder.bind(inspection, listener);
    }

    @Override
    public int getItemCount() {
        return inspections != null ? inspections.size() : 0;
    }

    public void updateInspections(List<InspectionEntity> newInspections) {
        this.inspections = newInspections;
        notifyDataSetChanged();
    }

    static class InspectionViewHolder extends RecyclerView.ViewHolder {

        private TextView tvInspectionId;
        private TextView tvRequirementCategory;
        private TextView tvInspectorName;
        private TextView tvInspectionDate;
        private TextView tvStatus;
        private ImageView ivStatusIcon;
        private LinearLayout statusContainer;
        private TextView tvActionsCount;
        private ImageView ivCategoryIcon;
        private CardView cardView;

        InspectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInspectionId = itemView.findViewById(R.id.tvInspectionId);
            tvRequirementCategory = itemView.findViewById(R.id.tvRequirementCategory);
            tvInspectorName = itemView.findViewById(R.id.tvInspectorName);
            tvInspectionDate = itemView.findViewById(R.id.tvInspectionDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            statusContainer = itemView.findViewById(R.id.statusContainer);
            tvActionsCount = itemView.findViewById(R.id.tvActionsCount);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            cardView = itemView.findViewById(R.id.cardView);
        }

        void bind(InspectionEntity inspection, OnInspectionClickListener listener) {
            tvInspectionId.setText(inspection.getInspectionId());
            tvInspectorName.setText(inspection.getInspectorName() != null ?
                    inspection.getInspectorName() : "No inspector");
            tvInspectionDate.setText(inspection.getFormattedInspectionDate());

            // ✅ Set requirement category with icon
            int categoryIconResId = inspection.getRequirementCategoryIconResId();
            ivCategoryIcon.setImageResource(categoryIconResId);
            tvRequirementCategory.setText(inspection.getRequirementCategoryDisplay());

            // ✅ Set status with icon
            int statusIconResId = inspection.getStatusIconResId();
            ivStatusIcon.setImageResource(statusIconResId);
            tvStatus.setText(inspection.getStatusDisplay());

            // ✅ Set status colors
            int bgDrawable;
            int bgColorRes;
            switch (inspection.getStatus()) {
                case PASS:
                    bgDrawable = R.drawable.bg_status_certified;
                    bgColorRes = R.color.status_certified;
                    break;
                case CONDITIONAL_PASS:
                    bgDrawable = R.drawable.bg_status_audit_ready;
                    bgColorRes = R.color.status_audit_ready;
                    break;
                case FAIL:
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

            // ✅ Category icon color (use primary color)
            ivCategoryIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));

            // ✅ Actions count
            int actionsCount = inspection.getCorrectiveActionsCount();
            int openActions = inspection.getOpenCorrectiveActionsCount();

            if (actionsCount > 0) {
                tvActionsCount.setVisibility(View.VISIBLE);
                tvActionsCount.setText(actionsCount + " action" + (actionsCount > 1 ? "s" : "") +
                        " (" + openActions + " open)");

                if (openActions > 0) {
                    tvActionsCount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_warning));
                } else {
                    tvActionsCount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_pass));
                }
            } else {
                tvActionsCount.setVisibility(View.GONE);
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onInspectionClick(inspection);
                }
            });
        }
    }
}