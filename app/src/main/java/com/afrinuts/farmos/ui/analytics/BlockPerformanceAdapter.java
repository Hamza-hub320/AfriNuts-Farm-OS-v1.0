package com.afrinuts.farmos.ui.analytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;

import java.util.List;

public class BlockPerformanceAdapter extends RecyclerView.Adapter<BlockPerformanceAdapter.ViewHolder> {

    private List<BlockPerformance> performances;

    public BlockPerformanceAdapter(List<BlockPerformance> performances) {
        this.performances = performances;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_block_performance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockPerformance perf = performances.get(position);
        holder.bind(perf);
    }

    @Override
    public int getItemCount() {
        return performances.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvRank;
        private TextView tvBlockName;
        private TextView tvSurvivalRate;
        private ProgressBar progressSurvival;
        private TextView tvProfit;
        private TextView tvProfitMargin;
        private ImageView ivTrend;
        private CardView cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvBlockName = itemView.findViewById(R.id.tvBlockName);
            tvSurvivalRate = itemView.findViewById(R.id.tvSurvivalRate);
            progressSurvival = itemView.findViewById(R.id.progressSurvival);
            tvProfit = itemView.findViewById(R.id.tvProfit);
            tvProfitMargin = itemView.findViewById(R.id.tvProfitMargin);
            ivTrend = itemView.findViewById(R.id.ivTrend);
            cardView = (CardView) itemView;
        }

        void bind(BlockPerformance perf) {
            tvRank.setText(perf.getRankingSuffix());
            tvBlockName.setText(perf.getBlock().getBlockName());

            tvSurvivalRate.setText(perf.getFormattedSurvivalRate());
            progressSurvival.setProgress((int) perf.getSurvivalRate());

            tvProfit.setText(perf.getFormattedProfit());
            tvProfitMargin.setText(perf.getFormattedProfitMargin());

            // Set trend icon based on profit
            if (perf.getProfit() > 0) {
                ivTrend.setImageResource(R.drawable.ic_trending_up);
                ivTrend.setColorFilter(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_green_dark));
            } else {
                ivTrend.setImageResource(R.drawable.ic_trending_down);
                ivTrend.setColorFilter(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.holo_red_dark));
            }

            // Highlight top performers
            if (perf.getRanking() == 1) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.primary_light));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        android.R.color.white));
            }
        }
    }
}