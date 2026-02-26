package com.afrinuts.farmos.ui.blocks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlockAdapter extends RecyclerView.Adapter<BlockAdapter.BlockViewHolder> {

    private List<BlockEntity> blocks;
    private OnBlockClickListener listener;
    private int expandedPosition = -1; // Track which item is expanded

    public interface OnBlockClickListener {
        void onBlockClick(BlockEntity block);
    }

    public BlockAdapter(List<BlockEntity> blocks, OnBlockClickListener listener) {
        this.blocks = blocks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_block_card, parent, false);
        return new BlockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockViewHolder holder, int position) {
        BlockEntity block = blocks.get(position);
        holder.bind(block, listener, position == expandedPosition);

        // Handle expand/collapse
        holder.expandHeader.setOnClickListener(v -> {
            int previousExpandedPosition = expandedPosition;
            expandedPosition = holder.getAdapterPosition();

            // Notify previous item to collapse
            if (previousExpandedPosition != -1 && previousExpandedPosition != expandedPosition) {
                notifyItemChanged(previousExpandedPosition);
            }

            // Notify current item to expand
            notifyItemChanged(expandedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return blocks.size();
    }

    static class BlockViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView blockNameText;
        TextView statusText;
        TextView statsText;
        TextView dateText;
        TextView replacementText;
        TextView survivalText;
        TextView notesText;
        ProgressBar survivalProgress;
        LinearLayout expandableContent;
        LinearLayout expandHeader;
        TextView expandIcon;
        LinearLayout notesSection;

        public BlockViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = (CardView) itemView;
            blockNameText = itemView.findViewById(R.id.blockNameText);
            statusText = itemView.findViewById(R.id.statusText);
            statsText = itemView.findViewById(R.id.statsText);
            dateText = itemView.findViewById(R.id.dateText);
            replacementText = itemView.findViewById(R.id.replacementText);
            survivalText = itemView.findViewById(R.id.survivalText);
            notesText = itemView.findViewById(R.id.notesText);
            survivalProgress = itemView.findViewById(R.id.survivalProgress);
            expandableContent = itemView.findViewById(R.id.expandableContent);
            expandHeader = itemView.findViewById(R.id.expandHeader);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            notesSection = itemView.findViewById(R.id.notesSection);
        }

        public void bind(BlockEntity block, OnBlockClickListener listener, boolean isExpanded) {
            blockNameText.setText(block.getBlockName());

            // Status with color coding
            statusText.setText(block.getStatus().getDisplayName());
            int statusColor;
            switch (block.getStatus()) {
                case NOT_CLEARED:
                    statusColor = itemView.getContext().getColor(android.R.color.holo_red_dark);
                    break;
                case CLEARED:
                    statusColor = itemView.getContext().getColor(android.R.color.holo_orange_dark);
                    break;
                case PLOWED:
                    statusColor = itemView.getContext().getColor(android.R.color.holo_blue_dark);
                    break;
                case PLANTED:
                    statusColor = itemView.getContext().getColor(android.R.color.holo_green_dark);
                    break;
                default:
                    statusColor = itemView.getContext().getColor(R.color.primary);
            }
            statusText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));

            // Get the relevant date based on status
            Long relevantDate = block.getRelevantDate();
            if (relevantDate != null && relevantDate > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                dateText.setText(block.getDateLabel() + ": " + sdf.format(new Date(relevantDate)));
            } else {
                dateText.setText(block.getDateLabel() + ": Not recorded");
            }

            // Only show planting details if block is planted
            if (block.isPlanted()) {

                replacementText.setText(block.getReplacementCount() + " trees");

                double survivalRate = block.getSurvivalRate();
                survivalProgress.setProgress((int) survivalRate);
                survivalText.setText(String.format(Locale.getDefault(), "%.1f%%", survivalRate));

                String stats = String.format("🌱 Alive: %d | 💀 Dead: %d",
                        block.getAliveTrees(), block.getDeadTrees());
                statsText.setText(stats);

            } else {

                // For non-planted blocks, show different stats
                statsText.setText("⏳ " + block.getStatus().getDisplayName() + " - No trees yet");
                replacementText.setText("—");
                survivalProgress.setProgress(0);
                survivalText.setText("—");
            }

            // Notes section (show only if notes exist)
            if (block.getNotes() != null && !block.getNotes().isEmpty()) {
                notesSection.setVisibility(View.VISIBLE);
                notesText.setText(block.getNotes());
            } else {
                notesSection.setVisibility(View.GONE);
            }

            // Handle expand/collapse
            if (isExpanded) {
                expandableContent.setVisibility(View.VISIBLE);
                expandIcon.setText("▲");
            } else {
                expandableContent.setVisibility(View.GONE);
                expandIcon.setText("▼");
            }

            // Card click listener
            cardView.setOnClickListener(v -> listener.onBlockClick(block));
        }
    }
}