package com.afrinuts.farmos.ui.blocks;

import com.afrinuts.farmos.data.local.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class BlockGroup {
    private String rowName; // "A", "B", "C", "D", "E"
    private List<BlockEntity> blocks;
    private boolean isExpanded;

    public BlockGroup(String rowName) {
        this.rowName = rowName;
        this.blocks = new ArrayList<>();
        this.isExpanded = true; // Default to expanded
    }

    public String getRowName() {
        return rowName;
    }

    public List<BlockEntity> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<BlockEntity> blocks) {
        this.blocks = blocks;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    // Helper methods for row-level statistics
    public int getPlantedCount() {
        int count = 0;
        for (BlockEntity block : blocks) {
            if (block.isPlanted()) count++;
        }
        return count;
    }

    public int getTotalBlocks() {
        return blocks.size();
    }

    public double getAverageSurvivalRate() {
        if (blocks.isEmpty()) return 0;

        double total = 0;
        int plantedCount = 0;

        for (BlockEntity block : blocks) {
            if (block.isPlanted()) {
                total += block.getSurvivalRate();
                plantedCount++;
            }
        }

        return plantedCount > 0 ? total / plantedCount : 0;
    }

    public int getTotalAliveTrees() {
        int total = 0;
        for (BlockEntity block : blocks) {
            total += block.getAliveTrees();
        }
        return total;
    }

    public int getTotalExpectedTrees() {
        return blocks.size() * 100; // 100 trees per block
    }

    public String getRowRange() {
        if (blocks.isEmpty()) return rowName + "1 - " + rowName + "7";

        int min = Integer.MAX_VALUE;
        int max = 0;

        for (BlockEntity block : blocks) {
            String name = block.getBlockName();
            try {
                int num = Integer.parseInt(name.substring(1));
                min = Math.min(min, num);
                max = Math.max(max, num);
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        if (min == Integer.MAX_VALUE) {
            return rowName + "1 - " + rowName + "7";
        }

        return rowName + min + " - " + rowName + max;
    }
}