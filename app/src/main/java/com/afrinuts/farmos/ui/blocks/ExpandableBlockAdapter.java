package com.afrinuts.farmos.ui.blocks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
                return true;
            }
            cumulative += 1;
            if (group.isExpanded()) {
                cumulative += group.getBlocks().size();
            }
        }
        return false;
    }

    private int getGroupAndPosition(int position) {
        int cumulative = 0;
        for (int i = 0; i < blockGroups.size(); i++) {
            BlockGroup group = blockGroups.get(i);

            if (position == cumulative) {
                return i;
            }
            cumulative += 1;

            if (group.isExpanded()) {
                int itemCount = group.getBlocks().size();
                if (position < cumulative + itemCount) {
                    return -(i + 1);
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
                    .inflate(R.layout.item_farm_block_card, parent, false);
            return new BlockViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int result = getGroupAndPosition(position);

        if (holder instanceof HeaderViewHolder) {
            int groupIndex = result;
            if (groupIndex >= 0 && groupIndex < blockGroups.size()) {
                ((HeaderViewHolder) holder).bind(blockGroups.get(groupIndex));
            }
        } else if (holder instanceof BlockViewHolder) {
            int groupIndex = -result - 1;
            if (groupIndex >= 0 && groupIndex < blockGroups.size()) {
                BlockGroup group = blockGroups.get(groupIndex);

                int cumulative = 0;
                for (int i = 0; i < groupIndex; i++) {
                    cumulative += 1;
                    if (blockGroups.get(i).isExpanded()) {
                        cumulative += blockGroups.get(i).getBlocks().size();
                    }
                }
                cumulative += 1;

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
            count += 1;
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
        ImageView expandIcon;
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

        void bind(BlockGroup group) {
            rowNameText.setText("ROW " + group.getRowName());
            rowRangeText.setText("(" + group.getRowRange() + ")");

            int planted = group.getPlantedCount();
            int total = group.getTotalBlocks();
            plantedCountText.setText(String.format("%d/%d", planted, total));

            double avgSurvival = group.getAverageSurvivalRate();
            survivalRateText.setText(String.format("%.0f%%", avgSurvival));

            int alive = group.getTotalAliveTrees();
            int expected = group.getTotalExpectedTrees();
            treesText.setText(String.format("%d/%d", alive, expected));

            int progress = expected > 0 ? (alive * 100 / expected) : 0;
            rowProgressBar.setProgress(progress);

            if (group.isExpanded()) {
                expandIcon.setImageResource(R.drawable.ic_expand_less);
            } else {
                expandIcon.setImageResource(R.drawable.ic_expand_more);
            }

            headerLayout.setClickable(false);
        }
    }

    // Block ViewHolder - FIXED: Removed statsIcon reference
    static class BlockViewHolder extends RecyclerView.ViewHolder {
        private View expandableContent;
        private View expandHeader;
        private ImageView expandIcon;
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
            TextView survivalLabel = itemView.findViewById(R.id.survivalLabel);
            View survivalContainer = itemView.findViewById(R.id.survivalContainer);
            View plantedDot = itemView.findViewById(R.id.plantedDot);

            blockNameText.setText(block.getBlockName());
            boolean attention = block.isPlanted() && block.getSurvivalRate() < 70;
            statusText.setVisibility(attention ? View.VISIBLE : View.GONE);
            statusText.setText("ATTENTION");

            Long relevantDate = block.getRelevantDate();
            if (relevantDate != null && relevantDate > 0) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("MMM dd, yyyy",
                                java.util.Locale.getDefault());
                dateText.setText(block.isPlanted() ? "Planted · " + new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date(relevantDate)) : block.getStatus().getDisplayName());
            } else {
                dateText.setText(block.isPlanted() ? "Planted" : "Not planted");
            }

            if (block.isPlanted()) {
                statsText.setText(String.format(java.util.Locale.getDefault(), "%,d of 100 trees alive", block.getAliveTrees()));

                replacementText.setText(block.getReplacementCount() + " trees");
                survivalProgress.setProgress((int) block.getSurvivalRate());
                survivalText.setText(String.format("%.1f%%", block.getSurvivalRate()));
                survivalLabel.setText(String.format(java.util.Locale.getDefault(), "%.0f%%", block.getSurvivalRate()));
                survivalLabel.setVisibility(View.VISIBLE);
                survivalLabel.setTextColor(attention ? 0xFF8A6414 : 0xFF0A5530);
                survivalProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(attention ? 0xFFC9A86A : 0xFF0E6B3C));
                survivalContainer.setVisibility(View.VISIBLE);
                plantedDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF0E6B3C));
            } else {
                statsText.setText("100 trees planned · not planted yet");

                replacementText.setText("—");
                survivalProgress.setProgress(0);
                survivalText.setText("—");
                survivalLabel.setVisibility(View.GONE);
                survivalContainer.setVisibility(View.GONE);
                plantedDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFC9C2B4));
            }

            if (expandHeader != null) {
                expandHeader.setOnClickListener(v -> itemView.performClick());
            }

            itemView.setOnClickListener(v -> listener.onBlockClick(block));
        }
    }
}
