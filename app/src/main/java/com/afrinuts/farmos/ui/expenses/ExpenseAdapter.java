package com.afrinuts.farmos.ui.expenses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<ExpenseWithBlockName> expenses = new ArrayList<>();
    private OnExpenseClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public interface OnExpenseClickListener {
        void onExpenseClick(ExpenseEntity expense);
    }

    public ExpenseAdapter(List<ExpenseWithBlockName> expenses, OnExpenseClickListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        ExpenseWithBlockName item = expenses.get(position);
        ExpenseEntity expense = item.getExpense();
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategory;
        private TextView tvSourceInfo;
        private TextView tvProcurementInfo;
        private TextView tvAmount;
        private TextView tvDate;
        private TextView tvDescription;
        private ImageView ivCategoryIcon;
        private LinearLayout layoutProcurementBadge;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvSourceInfo = itemView.findViewById(R.id.tvSourceInfo);
            tvProcurementInfo = itemView.findViewById(R.id.tvProcurementInfo);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            layoutProcurementBadge = itemView.findViewById(R.id.layoutProcurementBadge);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onExpenseClick(expenses.get(getAdapterPosition()).getExpense());
                }
            });
        }

        void bind(ExpenseWithBlockName item) {
            ExpenseEntity expense = item.getExpense();
            String blockName = item.getBlockName();

            // Category
            if (expense.getCategory() != null) {
                tvCategory.setText(expense.getCategory().getDisplayName());

                // Set category icon
                int iconRes = expense.getCategory().getIconResId();
                ivCategoryIcon.setImageResource(iconRes);
            }

            // Source Info
            boolean isProcurement = expense.isProcurementRelated();

            if (isProcurement) {
                StringBuilder source = new StringBuilder();
                if (expense.getBatchNumber() != null && !expense.getBatchNumber().trim().isEmpty()) {
                    source.append("Batch ").append(expense.getBatchNumber().trim());
                }
                if (expense.getSupplierName() != null && !expense.getSupplierName().trim().isEmpty()) {
                    if (source.length() > 0) source.append(" · ");
                    source.append("Supplier ").append(expense.getSupplierName().trim());
                }
                tvSourceInfo.setText(source.length() == 0 ? "Procurement source unavailable" : source.toString());
                tvSourceInfo.setTextColor(0xFF59544C);
                tvProcurementInfo.setVisibility(View.GONE);
                layoutProcurementBadge.setVisibility(View.GONE);

            } else {
                // Farm expense
                layoutProcurementBadge.setVisibility(View.GONE);
                tvProcurementInfo.setVisibility(View.GONE);

                if (blockName != null) {
                    tvSourceInfo.setText("Block " + blockName);
                    tvSourceInfo.setTextColor(0xFF59544C);
                } else {
                    tvSourceInfo.setText("Farm-Wide");
                    tvSourceInfo.setTextColor(0xFF59544C);
                }
            }

            // Amount
            tvAmount.setText(String.format(Locale.getDefault(), "%,.0f XAF", expense.getAmount()));

            // Date
            if (expense.getDate() > 0) {
                tvDate.setText(dateFormat.format(expense.getDate()));
            }

            // Description
            if (expense.getDescription() != null && !expense.getDescription().trim().isEmpty()) {
                tvDescription.setText(expense.getDescription().trim());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }
        }
    }
}
