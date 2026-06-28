package com.afrinuts.farmos.ui.suppliers;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.SupplierEntity;

import java.util.List;
import java.util.Locale;

public class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.SupplierViewHolder> {

    private List<SupplierEntity> suppliers;
    private OnSupplierClickListener listener;

    public interface OnSupplierClickListener {
        void onSupplierClick(SupplierEntity supplier);
    }

    public SupplierAdapter(List<SupplierEntity> suppliers, OnSupplierClickListener listener) {
        this.suppliers = suppliers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SupplierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_supplier, parent, false);
        return new SupplierViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SupplierViewHolder holder, int position) {
        SupplierEntity supplier = suppliers.get(position);
        holder.bind(supplier, listener);
    }

    @Override
    public int getItemCount() {
        return suppliers != null ? suppliers.size() : 0;
    }

    public void updateSuppliers(List<SupplierEntity> newSuppliers) {
        this.suppliers = newSuppliers;
        notifyDataSetChanged();
    }

    static class SupplierViewHolder extends RecyclerView.ViewHolder {

        private TextView tvSupplierName;
        private TextView tvSupplierType;
        private ImageView ivRatingIcon;
        private TextView tvRating;
        private TextView tvContactPerson;
        private TextView tvPhone;
        private ImageView ivStatusIcon;
        private TextView tvStatus;
        private TextView tvVolume;
        private TextView tvCertification;
        private View layoutCertification;
        private CardView cardView;

        SupplierViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSupplierName = itemView.findViewById(R.id.tvSupplierName);
            tvSupplierType = itemView.findViewById(R.id.tvSupplierType);
            ivRatingIcon = itemView.findViewById(R.id.ivRatingIcon);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvContactPerson = itemView.findViewById(R.id.tvContactPerson);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvVolume = itemView.findViewById(R.id.tvVolume);
            tvCertification = itemView.findViewById(R.id.tvCertification);
            layoutCertification = itemView.findViewById(R.id.layoutCertification);
            cardView = (CardView) itemView;
        }

        void bind(SupplierEntity supplier, OnSupplierClickListener listener) {
            tvSupplierName.setText(supplier.getName());
            tvSupplierType.setText(supplier.getType().getDisplayName());
            tvRating.setText(String.valueOf(supplier.getRating()));

            // ✅ Set rating icon color
            ivRatingIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.accent));

            // Contact info
            tvContactPerson.setText(supplier.getContactPerson() != null ?
                    supplier.getContactPerson() : "No contact");
            tvPhone.setText(supplier.getPhone() != null ?
                    supplier.getPhone() : "No phone");

            // ✅ Status with icon
            tvStatus.setText(supplier.getStatus().getDisplayName());
            switch (supplier.getStatus()) {
                case ACTIVE:
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_light));
                    break;
                case PENDING:
                    ivStatusIcon.setImageResource(R.drawable.ic_hourglass);
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_orange_light));
                    break;
                case SUSPENDED:
                    ivStatusIcon.setImageResource(R.drawable.ic_warning);
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_light));
                    break;
                case INACTIVE:
                    ivStatusIcon.setImageResource(R.drawable.ic_cancel);
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
                    break;
                default:
                    ivStatusIcon.setImageResource(R.drawable.ic_check_circle);
                    ivStatusIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                    break;
            }

            // Volume with icon
            tvVolume.setText(String.format(Locale.getDefault(),
                    "%.0f kg", supplier.getAnnualVolumeKg()));

            // Certification badge
            if (supplier.isCertified() && supplier.getCertificationType() != null) {
                layoutCertification.setVisibility(View.VISIBLE);
                tvCertification.setText(supplier.getCertificationType());
            } else {
                layoutCertification.setVisibility(View.GONE);
            }

            // Click listener
            cardView.setOnClickListener(v -> {
                android.content.Context context = itemView.getContext();
                android.content.Intent intent = new android.content.Intent(context, SupplierDetailActivity.class);
                intent.putExtra(SupplierDetailActivity.EXTRA_SUPPLIER_ID, supplier.getId());
                context.startActivity(intent);
            });
        }
    }
}