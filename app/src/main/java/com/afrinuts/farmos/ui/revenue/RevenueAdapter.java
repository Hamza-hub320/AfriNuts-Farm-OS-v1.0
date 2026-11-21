package com.afrinuts.farmos.ui.revenue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RevenueAdapter extends RecyclerView.Adapter<RevenueAdapter.RevenueViewHolder> {

    private List<RevenueEntity> revenues = new ArrayList<>();
    private OnRevenueClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnRevenueClickListener {
        void onRevenueClick(RevenueEntity revenue);
        void onRevenueLongClick(RevenueEntity revenue);
    }

    public void setOnRevenueClickListener(OnRevenueClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RevenueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_revenue, parent, false);
        return new RevenueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RevenueViewHolder holder, int position) {
        RevenueEntity revenue = revenues.get(position);
        holder.bind(revenue);
    }

    @Override
    public int getItemCount() {
        return revenues.size();
    }

    public void setRevenues(List<RevenueEntity> revenues) {
        this.revenues = revenues;
        notifyDataSetChanged();
    }

    class RevenueViewHolder extends RecyclerView.ViewHolder {
        private TextView tvBatchNumber;
        private TextView tvSupplier;
        private TextView tvAmount;
        private TextView tvQuantity;
        private TextView tvPrice;
        private TextView tvQualityBadge;
        private ImageView tvQualityBadgeIcon;
        private TextView tvDate;
        private TextView tvBuyer;
        private ImageView tvQualityIcon;
        private View qualityIconContainer;
        private View qualityBadge;

        public RevenueViewHolder(@NonNull View itemView) {
            super(itemView);

            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvSupplier = itemView.findViewById(R.id.tvSupplier);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQualityBadge = itemView.findViewById(R.id.tvQualityBadge);
            tvQualityBadgeIcon = itemView.findViewById(R.id.tvQualityBadgeIcon);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBuyer = itemView.findViewById(R.id.tvBuyer);
            tvQualityIcon = itemView.findViewById(R.id.tvQualityIcon);
            qualityIconContainer = itemView.findViewById(R.id.qualityIconContainer);
            qualityBadge = itemView.findViewById(R.id.qualityBadge);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onRevenueClick(revenues.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onRevenueLongClick(revenues.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }

        void bind(RevenueEntity revenue) {
            // Batch Number (show "Processing Center" if no batch)
            if (revenue.getBatchNumber() != null && !revenue.getBatchNumber().isEmpty()) {
                tvBatchNumber.setText(revenue.getBatchNumber());
            } else {
                tvBatchNumber.setText("Processing Center Sale");
            }

            // Supplier Name
            if (revenue.getSupplierName() != null && !revenue.getSupplierName().isEmpty()) {
                tvSupplier.setText(revenue.getSupplierName());
            } else {
                tvSupplier.setText("Supplier not recorded");
            }

            // Amount
            tvAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", revenue.getTotalAmount()));

            // Quantity
            tvQuantity.setText(String.format(Locale.getDefault(), "%,.0f kg · %,.0f XAF/kg",
                    revenue.getQuantityKg(), revenue.getPricePerKg()));

            // Price
            tvPrice.setVisibility(View.GONE);

            // Quality Badge
            if (revenue.getQuality() != null) {
                tvQualityBadge.setText(revenue.getQuality().getDisplayName());

                int iconRes;
                int iconColor;
                int badgeColor;
                int iconBackground;
                int badgeBackground;
                switch (revenue.getQuality()) {
                    case PREMIUM:
                        iconRes = R.drawable.ic_star;
                        iconColor = 0xFF0A5530;
                        badgeColor = 0xFF0A5530;
                        iconBackground = R.drawable.bg_revenue_premium_icon;
                        badgeBackground = R.drawable.bg_revenue_premium_badge;
                        break;
                    case STANDARD:
                        iconRes = R.drawable.ic_check_circle;
                        iconColor = 0xFF7A5F22;
                        badgeColor = 0xFF7A5F22;
                        iconBackground = R.drawable.bg_revenue_standard_icon;
                        badgeBackground = R.drawable.bg_revenue_standard_badge;
                        break;
                    case PROCESSING:
                        iconRes = R.drawable.ic_factory;
                        iconColor = 0xFF59544C;
                        badgeColor = 0xFF59544C;
                        iconBackground = R.drawable.bg_revenue_standard_icon;
                        badgeBackground = R.drawable.bg_revenue_standard_badge;
                        break;
                    default:
                        iconRes = R.drawable.ic_star;
                        iconColor = 0xFF59544C;
                        badgeColor = 0xFF59544C;
                        iconBackground = R.drawable.bg_revenue_standard_icon;
                        badgeBackground = R.drawable.bg_revenue_standard_badge;
                }
                tvQualityBadgeIcon.setImageResource(iconRes);
                tvQualityIcon.setImageResource(iconRes);
                tvQualityIcon.setColorFilter(iconColor);
                tvQualityBadgeIcon.setColorFilter(iconColor);
                tvQualityBadge.setTextColor(badgeColor);
                qualityIconContainer.setBackgroundResource(iconBackground);
                qualityBadge.setBackgroundResource(badgeBackground);
            }

            // Date
            if (revenue.getCreatedAt() > 0) {
                tvDate.setText(dateFormat.format(revenue.getCreatedAt()));
            } else {
                tvDate.setText("Date not set");
            }

            // Buyer
            if (revenue.getBuyer() != null && !revenue.getBuyer().isEmpty()) {
                tvBuyer.setText(revenue.getBuyer());
                tvBuyer.setVisibility(View.VISIBLE);
            } else {
                tvBuyer.setVisibility(View.GONE);
            }

            // Quality Icon (main icon)
            if (revenue.getQuality() != null) {
                int iconRes;
                switch (revenue.getQuality()) {
                    case PREMIUM:
                        iconRes = R.drawable.ic_star;
                        break;
                    case STANDARD:
                        iconRes = R.drawable.ic_check_circle;
                        break;
                    case PROCESSING:
                        iconRes = R.drawable.ic_factory;
                        break;
                    default:
                        iconRes = R.drawable.ic_star;
                }
                tvQualityIcon.setImageResource(iconRes);
            }
        }
    }
}
