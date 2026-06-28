package com.afrinuts.farmos.ui.suppliers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SupplierDeliveryAdapter extends RecyclerView.Adapter<SupplierDeliveryAdapter.DeliveryViewHolder> {

    private List<SupplierEntity.Delivery> deliveries;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public SupplierDeliveryAdapter(List<SupplierEntity.Delivery> deliveries) {
        this.deliveries = deliveries;
    }

    @NonNull
    @Override
    public DeliveryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_supplier_delivery, parent, false);
        return new DeliveryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeliveryViewHolder holder, int position) {
        SupplierEntity.Delivery delivery = deliveries.get(position);
        holder.bind(delivery);
    }

    @Override
    public int getItemCount() {
        return deliveries.size();
    }

    public void updateDeliveries(List<SupplierEntity.Delivery> newDeliveries) {
        this.deliveries = newDeliveries;
        notifyDataSetChanged();
    }

    static class DeliveryViewHolder extends RecyclerView.ViewHolder {

        private TextView tvDeliveryBatch;
        private TextView tvDeliveryGrade;
        private TextView tvDeliveryDate;
        private TextView tvDeliveryQuantity;
        private TextView tvDeliveryPrice;

        DeliveryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeliveryBatch = itemView.findViewById(R.id.tvDeliveryBatch);
            tvDeliveryGrade = itemView.findViewById(R.id.tvDeliveryGrade);
            tvDeliveryDate = itemView.findViewById(R.id.tvDeliveryDate);
            tvDeliveryQuantity = itemView.findViewById(R.id.tvDeliveryQuantity);
            tvDeliveryPrice = itemView.findViewById(R.id.tvDeliveryPrice);
        }

        void bind(SupplierEntity.Delivery delivery) {
            tvDeliveryBatch.setText("#" + delivery.getBatchNumber());
            tvDeliveryGrade.setText(delivery.getGrade());
            tvDeliveryDate.setText(delivery.getFormattedDate());
            tvDeliveryQuantity.setText(delivery.getFormattedQuantity());
            tvDeliveryPrice.setText(String.format(Locale.getDefault(),
                    "%,.0f XAF/kg", delivery.getPricePerKg()));
        }
    }
}