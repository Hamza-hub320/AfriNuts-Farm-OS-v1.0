package com.afrinuts.farmos.ui.expenses;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.ExpenseEntity;
import com.afrinuts.farmos.ui.expenses.ExpenseDetailActivity;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<ExpenseWithBlockName> expenses;
    private OnExpenseClickListener listener;

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
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {

        // Changed from TextView to ImageView
        private ImageView ivCategoryIcon;
        private TextView tvCategory;
        private TextView tvBlockInfo;
        private TextView tvAmount;
        private TextView tvDate;
        private TextView tvDescription;
        private CardView cardView;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            // Updated to use ivCategoryIcon instead of tvCategoryIcon
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvBlockInfo = itemView.findViewById(R.id.tvBlockInfo);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            cardView = (CardView) itemView;
        }

        void bind(ExpenseWithBlockName item, OnExpenseClickListener listener) {
            ExpenseEntity expense = item.getExpense();

            // Set category icon based on expense category
            switch (expense.getCategory()) {
                case LAND_CLEARING:
                    ivCategoryIcon.setImageResource(R.drawable.ic_construction);
                    break;
                case PLOWING:
                    ivCategoryIcon.setImageResource(R.drawable.ic_grain);
                    break;
                case SEEDLINGS:
                    ivCategoryIcon.setImageResource(R.drawable.ic_grass);
                    break;
                case LABOR:
                    ivCategoryIcon.setImageResource(R.drawable.ic_people);
                    break;
                case SECURITY:
                    ivCategoryIcon.setImageResource(R.drawable.ic_security);
                    break;
                case FENCING:
                    ivCategoryIcon.setImageResource(R.drawable.ic_fence);
                    break;
                case FERTILIZER:
                    ivCategoryIcon.setImageResource(R.drawable.ic_fertilizer);
                    break;
                case IRRIGATION:
                    ivCategoryIcon.setImageResource(R.drawable.ic_water_drop);
                    break;
                case EQUIPMENT:
                    ivCategoryIcon.setImageResource(R.drawable.ic_construction);
                    break;
                case MAINTENANCE:
                    ivCategoryIcon.setImageResource(R.drawable.ic_construction);
                    break;
                case PROCESSING_CENTER:
                    ivCategoryIcon.setImageResource(R.drawable.ic_factory);
                    break;
                default:
                    ivCategoryIcon.setImageResource(R.drawable.ic_attach_money);
                    break;
            }

            // Set category name
            tvCategory.setText(expense.getCategory().getDisplayName());

            // Set block info
            tvBlockInfo.setText(item.getDisplayName());

            // Set amount
            tvAmount.setText(item.getFormattedAmount());

            // Set date
            tvDate.setText(item.getFormattedDate());

            // Set description
            if (expense.getDescription() != null && !expense.getDescription().isEmpty()) {
                tvDescription.setText(expense.getDescription());
            } else {
                tvDescription.setText("No description");
            }

            // Click listener
            cardView.setOnClickListener(v -> {
                android.content.Context context = itemView.getContext();
                android.content.Intent intent =
                        new android.content.Intent(context, ExpenseDetailActivity.class);
                intent.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, expense.getId());
                context.startActivity(intent);
            });
        }
    }
}