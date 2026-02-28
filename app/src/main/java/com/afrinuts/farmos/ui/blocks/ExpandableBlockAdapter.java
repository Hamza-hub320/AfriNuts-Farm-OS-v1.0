package com.afrinuts.farmos.ui.blocks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.BlockEntity;

import java.util.List;

public class ExpandableBlockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<BlockGroup> blockGroups;
    private OnBlockClickListener listener;

    public interface OnBlockClickListener {
        void onBlockClick(BlockEntity block);
    }

    public ExpandableBlockAdapter(List<BlockGroup> blockGroups, OnBlockClickListener listener) {
        this.blockGroups = blockGroups;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || isHeaderPosition(position)) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    private boolean isHeaderPosition(int position) {
        int cumulative = 0;
        for (BlockGroup group : blockGroups) {
            if (position == cumulative) {
                return true; // This is a header
            }
            cumulative += 1; // Add header
            if (group.isExpanded()) {
                cumulative += group.getBlocks().size(); // Add expanded items
            }
        }
        return false;
    }

    private int getGroupAndPosition(int position) {
        // This method will help us find which group and position within group
        int cumulative = 0;
        for (int i = 0; i < blockGroups.size(); i++) {
            BlockGroup group = blockGroups.get(i);

            // Check if this is the header
            if (position == cumulative) {
                return i; // Return group index
            }
            cumulative += 1; // Skip header

            // Check items in this group if expanded
            if (group.isExpanded()) {
                int itemCount = group.getBlocks().size();
                if (position < cumulative + itemCount) {
                    return -(i + 1); // Negative indicates item, value is group index + 1 (negated)
                }
                cumulative += itemCount;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_block_group_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_block_card, parent, false);
            return new BlockViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int result = getGroupAndPosition(position);

        if (holder instanceof HeaderViewHolder) {
            // It's a header
            int groupIndex = result;
            if (groupIndex >= 0 && groupIndex < blockGroups.size()) {
                ((HeaderViewHolder) holder).bind(blockGroups.get(groupIndex), position);
            }
        } else if (holder instanceof BlockViewHolder) {
            // It's a block item
            int groupIndex = -result - 1;
            if (groupIndex >= 0 && groupIndex < blockGroups.size()) {
                BlockGroup group = blockGroups.get(groupIndex);

                // Find which item in this group
                int cumulative = 0;
                for (int i = 0; i < groupIndex; i++) {
                    cumulative += 1; // header
                    if (blockGroups.get(i).isExpanded()) {
                        cumulative += blockGroups.get(i).getBlocks().size();
                    }
                }
                cumulative += 1; // current group header

                int itemIndex = position - cumulative;

                if (itemIndex >= 0 && itemIndex < group.getBlocks().size()) {
                    BlockEntity block = group.getBlocks().get(itemIndex);
                    ((BlockViewHolder) holder).bind(block, listener);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (BlockGroup group : blockGroups) {
            count += 1; // Add header
            if (group.isExpanded()) {
                count += group.getBlocks().size();
            }
        }
        return count;
    }

    // Header ViewHolder
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView rowNameText;
        TextView rowRangeText;
        TextView expandIcon;
        TextView plantedCountText;
        TextView survivalRateText;
        TextView treesText;
        ProgressBar rowProgressBar;
        View headerLayout;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            rowNameText = itemView.findViewById(R.id.rowNameText);
            rowRangeText = itemView.findViewById(R.id.rowRangeText);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            plantedCountText = itemView.findViewById(R.id.plantedCountText);
            survivalRateText = itemView.findViewById(R.id.survivalRateText);
            treesText = itemView.findViewById(R.id.treesText);
            rowProgressBar = itemView.findViewById(R.id.rowProgressBar);
            headerLayout = itemView.findViewById(R.id.headerLayout);
        }

        void bind(BlockGroup group, int position) {
            rowNameText.setText("BLOCK " + group.getRowName());
            rowRangeText.setText("(" + group.getRowRange() + ")");

            // Update stats
            int planted = group.getPlantedCount();
            int total = group.getTotalBlocks();
            plantedCountText.setText(String.format("🌱 Planted: %d/%d", planted, total));

            double avgSurvival = group.getAverageSurvivalRate();
            survivalRateText.setText(String.format("📊 Avg: %.0f%%", avgSurvival));

            int alive = group.getTotalAliveTrees();
            int expected = group.getTotalExpectedTrees();
            treesText.setText(String.format("🌳 %d/%d", alive, expected));

            // Update progress bar
            int progress = expected > 0 ? (alive * 100 / expected) : 0;
            rowProgressBar.setProgress(progress);

            // Update expand/collapse icon
            expandIcon.setText(group.isExpanded() ? "▼" : "▶");

            // Handle click to expand/collapse
            headerLayout.setOnClickListener(v -> {
                group.setExpanded(!group.isExpanded());
                notifyDataSetChanged(); // Simple but effective for demo
            });
        }
    }

    static class BlockViewHolder extends RecyclerView.ViewHolder {

        private View expandableContent;
        private View expandHeader;
        private TextView expandIcon;
        private boolean isExpanded = false;

        BlockViewHolder(@NonNull View itemView) {
            super(itemView);

            expandableContent = itemView.findViewById(R.id.expandableContent);
            expandHeader = itemView.findViewById(R.id.expandHeader);
            expandIcon = itemView.findViewById(R.id.expandIcon);
        }

        void bind(BlockEntity block, OnBlockClickListener listener) {

            TextView blockNameText = itemView.findViewById(R.id.blockNameText);
            TextView statusText = itemView.findViewById(R.id.statusText);
            TextView statsText = itemView.findViewById(R.id.statsText);
            TextView dateText = itemView.findViewById(R.id.dateText);
            TextView replacementText = itemView.findViewById(R.id.replacementText);
            TextView survivalText = itemView.findViewById(R.id.survivalText);
            ProgressBar survivalProgress = itemView.findViewById(R.id.survivalProgress);

            blockNameText.setText(block.getBlockName());
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

            statusText.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(statusColor));

            Long relevantDate = block.getRelevantDate();
            if (relevantDate != null && relevantDate > 0) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("MMM dd, yyyy",
                                java.util.Locale.getDefault());
                dateText.setText(block.getDateLabel() + ": " +
                        sdf.format(new java.util.Date(relevantDate)));
            } else {
                dateText.setText(block.getDateLabel() + ": Not recorded");
            }

            if (block.isPlanted()) {
                statsText.setText(String.format("🌱 Alive: %d | 💀 Dead: %d",
                        block.getAliveTrees(), block.getDeadTrees()));

                replacementText.setText(block.getReplacementCount() + " trees");
                survivalProgress.setProgress((int) block.getSurvivalRate());
                survivalText.setText(String.format("%.1f%%",
                        block.getSurvivalRate()));
            } else {
                statsText.setText("⏳ " +
                        block.getStatus().getDisplayName() +
                        " - No trees yet");

                replacementText.setText("—");
                survivalProgress.setProgress(0);
                survivalText.setText("—");
            }

            if (expandHeader != null) {
                expandHeader.setOnClickListener(v -> {
                    isExpanded = !isExpanded;

                    if (expandableContent != null) {
                        expandableContent.setVisibility(
                                isExpanded ? View.VISIBLE : View.GONE);
                    }

                    if (expandIcon != null) {
                        expandIcon.setText(isExpanded ? "▲" : "▼");
                    }
                });
            }

            itemView.setOnClickListener(v -> listener.onBlockClick(block));
        }
    }
}