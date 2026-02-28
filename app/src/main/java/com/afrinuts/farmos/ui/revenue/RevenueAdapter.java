package com.afrinuts.farmos.ui.revenue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.RevenueEntity;

import java.util.List;

public class RevenueAdapter extends RecyclerView.Adapter<RevenueAdapter.RevenueViewHolder> {

    private List<RevenueWithBlockName> revenues;
    private OnRevenueClickListener listener;

    public interface OnRevenueClickListener {
        void onRevenueClick(RevenueEntity revenue);
    }

    public RevenueAdapter(List<RevenueWithBlockName> revenues, OnRevenueClickListener listener) {
        this.revenues = revenues;
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
        RevenueWithBlockName item = revenues.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return revenues.size();
    }

    static class RevenueViewHolder extends RecyclerView.ViewHolder {

        private TextView tvQualityIcon;
        private TextView tvQuality;
        private TextView tvSource;
        private TextView tvAmount;
        private TextView tvQuantity;
        private TextView tvPrice;
        private TextView tvDate;
        private TextView tvBuyer;
        private CardView cardView;

        RevenueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQualityIcon = itemView.findViewById(R.id.tvQualityIcon);
            tvQuality = itemView.findViewById(R.id.tvQuality);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBuyer = itemView.findViewById(R.id.tvBuyer);
            cardView = (CardView) itemView;
        }

        void bind(RevenueWithBlockName item, OnRevenueClickListener listener) {
            RevenueEntity revenue = item.getRevenue();

            // Set quality icon and name
            tvQualityIcon.setText(revenue.getQuality().getIcon());
            tvQuality.setText(revenue.getQuality().getDisplayName() + " Grade");

            // Set source (block or processing center)
            tvSource.setText(item.getSourceDisplay());

            // Set amount
            tvAmount.setText(item.getFormattedAmount());

            // Set quantity and price
            tvQuantity.setText("📦 " + item.getFormattedQuantity());
            tvPrice.setText("💰 " + String.format(Locale.getDefault(),
                    "%,.0f XAF/kg", revenue.getPricePerKg()));

            // Set date
            tvDate.setText("📅 " + item.getFormattedDate());

            // Set buyer (or placeholder)
            if (revenue.getBuyer() != null && !revenue.getBuyer().isEmpty()) {
                tvBuyer.setText("👤 " + revenue.getBuyer());
            } else {
                tvBuyer.setText("👤 No buyer");
            }

            // Click listener
            cardView.setOnClickListener(v -> listener.onRevenueClick(revenue));
        }
    }
}