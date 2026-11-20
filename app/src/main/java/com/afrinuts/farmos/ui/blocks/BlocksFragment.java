package com.afrinuts.farmos.ui.blocks;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.BlockEntity;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.repository.BlockRepository;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlocksFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExpandableBlockAdapter expandableAdapter;
    private ProgressBar progressBar;
    private LinearLayout emptyView;
    private LinearLayout summaryLayout;
    private List<BlockGroup> blockGroups = new ArrayList<>();

    private BlockRepository blockRepository;
    private FarmEntity currentFarm;
    private List<BlockEntity> allBlocksList; // Store all blocks for name generation
    private AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blocks, container, false);

        database = AppDatabase.getInstance(requireContext());
        initViews(view);
        loadFarmData();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        summaryLayout = view.findViewById(R.id.summaryLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
    }

    private void loadFarmData() {
        showLoading(true);

        executor.execute(() -> {
            currentFarm = database.farmDao().getFirstFarm();

            if (currentFarm != null) {
                long farmId = currentFarm.getId();
                blockRepository = new BlockRepository(requireContext(), farmId);

                blockRepository = new BlockRepository(requireContext(), farmId);

                requireActivity().runOnUiThread(() -> {
                    blockRepository.getAllBlocks().observe(getViewLifecycleOwner(), new Observer<List<BlockEntity>>() {
                        @Override
                        public void onChanged(List<BlockEntity> blocks) {
                            allBlocksList = blocks; // Store for name generation
                            processBlocks(blocks);
                        }
                    });
                });
            } else {
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void processBlocks(List<BlockEntity> allBlocks) {
        if (allBlocks == null) allBlocks = new ArrayList<>();

        Map<String, List<BlockEntity>> groupedBlocks = new HashMap<>();
        for (BlockEntity block : allBlocks) {
            String row = block.getBlockName().substring(0, 1);
            if (!groupedBlocks.containsKey(row)) {
                groupedBlocks.put(row, new ArrayList<>());
            }
            groupedBlocks.get(row).add(block);
        }

        List<BlockGroup> newGroups = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E"};

        for (String row : rows) {
            if (groupedBlocks.containsKey(row)) {
                BlockGroup group = new BlockGroup(row);
                List<BlockEntity> blocksForRow = groupedBlocks.get(row);
                blocksForRow.sort((b1, b2) -> {
                    String name1 = b1.getBlockName() == null ? "" : b1.getBlockName().trim();
                    String name2 = b2.getBlockName() == null ? "" : b2.getBlockName().trim();
                    int number1 = leadingBlockNumber(name1);
                    int number2 = leadingBlockNumber(name2);
                    int numericOrder = Integer.compare(number1, number2);
                    return numericOrder != 0 ? numericOrder : name1.compareToIgnoreCase(name2);
                });
                group.setBlocks(blocksForRow);
                newGroups.add(group);
            }
        }

        final List<BlockEntity> finalAllBlocks = allBlocks;
        final List<BlockGroup> finalNewGroups = newGroups;

        requireActivity().runOnUiThread(() -> {
            if (finalNewGroups.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                blockGroups = finalNewGroups;
                updateFarmSummary(finalAllBlocks);

                expandableAdapter = new ExpandableBlockAdapter(blockGroups, block -> {
                    Intent intent = new Intent(getContext(), BlockDetailActivity.class);
                    intent.putExtra(BlockDetailActivity.EXTRA_BLOCK_ID, block.getId());
                    startActivity(intent);
                });
                recyclerView.setAdapter(expandableAdapter);
            }
            showLoading(false);
        });
    }

    private int leadingBlockNumber(String blockName) {
        int index = 0;
        while (index < blockName.length() && Character.isLetter(blockName.charAt(index))) index++;
        int digitStart = index;
        while (index < blockName.length() && Character.isDigit(blockName.charAt(index))) index++;
        if (digitStart == index) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(blockName.substring(digitStart, index));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private void updateFarmSummary(List<BlockEntity> allBlocks) {
        int totalBlocks = allBlocks.size();
        int plantedBlocks = 0;
        int totalAliveTrees = 0;
        int totalExpectedTrees = totalBlocks * 100;

        for (BlockEntity block : allBlocks) {
            if (block.isPlanted()) {
                plantedBlocks++;
                totalAliveTrees += block.getAliveTrees();
            }
        }

        if (summaryLayout == null) return;

        summaryLayout.removeAllViews();
        summaryLayout.setVisibility(View.VISIBLE);

        View summaryView = getLayoutInflater().inflate(R.layout.item_farm_summary, summaryLayout, false);

        TextView blocksPlantedText = summaryView.findViewById(R.id.blocksPlantedText);
        TextView treesAliveText = summaryView.findViewById(R.id.treesAliveText);
        ProgressBar blocksProgress = summaryView.findViewById(R.id.blocksProgress);
        ProgressBar treesProgress = summaryView.findViewById(R.id.treesProgress);
        TextView blocksPercentText = summaryView.findViewById(R.id.blocksPercentText);
        TextView treesPercentText = summaryView.findViewById(R.id.treesPercentText);

        int blocksPercent = totalBlocks > 0 ? (plantedBlocks * 100 / totalBlocks) : 0;
        int treesPercent = totalExpectedTrees > 0 ? (totalAliveTrees * 100 / totalExpectedTrees) : 0;

        blocksPlantedText.setText(String.format(Locale.getDefault(),
                "%d/%d blocks planted", plantedBlocks, totalBlocks));
        treesAliveText.setText(String.format(Locale.getDefault(),
                "%d/%d trees alive", totalAliveTrees, totalExpectedTrees));

        blocksProgress.setProgress(blocksPercent);
        treesProgress.setProgress(treesPercent);

        blocksPercentText.setText(blocksPercent + "%");
        treesPercentText.setText(treesPercent + "%");

        summaryLayout.addView(summaryView);
    }

    /**
     * Get the next available block name in the 5x7 grid (A1-E7)
     * Returns "FULL" if all 35 blocks are already created
     */
    private void getNextAvailableBlockName(BlockNameCallback callback) {
        if (blockRepository == null) {
            callback.onResult("A1");
            return;
        }

        // Run database query on background thread
        executor.execute(() -> {
            List<BlockEntity> allBlocks = blockRepository.getBlocksSync();
            java.util.HashSet<String> existingNames = new java.util.HashSet<>();
            for (BlockEntity block : allBlocks) {
                existingNames.add(block.getBlockName());
            }

            char[] rows = {'A', 'B', 'C', 'D', 'E'};
            int columns = 7;

            for (char row : rows) {
                for (int col = 1; col <= columns; col++) {
                    String blockName = row + String.valueOf(col);
                    if (!existingNames.contains(blockName)) {
                        callback.onResult(blockName);
                        return;
                    }
                }
            }
            callback.onResult("FULL");
        });
    }

    // Callback interface for async result
    public interface BlockNameCallback {
        void onResult(String blockName);
    }

    /**
     * Show the Add Block dialog with the next available block name pre-filled
     */
    public void showAddBlockDialog() {
        if (currentFarm != null && getView() != null) {
            getNextAvailableBlockName(blockName -> {
                requireActivity().runOnUiThread(() -> {
                    if (blockName.equals("FULL")) {
                        Snackbar.make(getView(),
                                "All 35 blocks (A1-E7) have been created. Cannot add more.",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    BlockDialog dialog = BlockDialog.newInstance(currentFarm.getId());
                    dialog.setNextBlockName(blockName);
                    dialog.setOnBlockAddedListener(() -> loadFarmData());
                    dialog.show(getParentFragmentManager(), "BlockDialog");
                });
            });
        }
    }

    private void showLoading(boolean show) {
        requireActivity().runOnUiThread(() -> {
            if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            if (summaryLayout != null) summaryLayout.setVisibility(show ? View.GONE : View.VISIBLE);
            if (emptyView != null) emptyView.setVisibility(View.GONE);
        });
    }

    private void showEmpty(boolean show) {
        if (emptyView != null) emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (recyclerView != null) recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        if (summaryLayout != null) summaryLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (blockRepository != null) {
            blockRepository.shutdown();
        }
        executor.shutdown();
    }
}