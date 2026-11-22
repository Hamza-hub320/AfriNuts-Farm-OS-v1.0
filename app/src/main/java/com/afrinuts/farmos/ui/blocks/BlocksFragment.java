package com.afrinuts.farmos.ui.blocks;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

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
    private TextView totalBlocksSummary;
    private TextView averageSurvivalSummary;
    private TextInputEditText searchInput;
    private ChipGroup blockFilterGroup;
    private String searchQuery = "";
    private String blockFilter = "ALL";
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
        totalBlocksSummary = view.findViewById(R.id.totalBlocksSummary);
        averageSurvivalSummary = view.findViewById(R.id.averageSurvivalSummary);
        searchInput = view.findViewById(R.id.etBlockSearch);
        blockFilterGroup = view.findViewById(R.id.blockFilterGroup);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                if (allBlocksList != null) processBlocks(allBlocksList);
            }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        setupBlockFilters();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
    }

    private void setupBlockFilters() {
        int[] ids = {R.id.chipBlockAll, R.id.chipBlockPlanted, R.id.chipBlockNotPlanted, R.id.chipBlockAttention};
        String[] filters = {"ALL", "PLANTED", "NOT_PLANTED", "ATTENTION"};
        for (int i = 0; i < ids.length; i++) {
            final String filter = filters[i];
            Chip chip = blockFilterGroup.findViewById(ids[i]);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) { blockFilter = filter; if (allBlocksList != null) processBlocks(allBlocksList); }
            });
        }
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
        List<BlockEntity> visibleBlocks = new ArrayList<>();
        for (BlockEntity block : allBlocks) {
            String name = block.getBlockName() == null ? "" : block.getBlockName().trim();
            boolean matchesFilter = "ALL".equals(blockFilter)
                    || ("PLANTED".equals(blockFilter) && block.isPlanted())
                    || ("NOT_PLANTED".equals(blockFilter) && !block.isPlanted())
                    || ("ATTENTION".equals(blockFilter) && block.isPlanted() && block.getSurvivalRate() < 70);
            if (!matchesFilter || (!searchQuery.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(searchQuery))) continue;
            visibleBlocks.add(block);
            String upperName = name.toUpperCase(Locale.ROOT);
            String row = !upperName.isEmpty() && upperName.charAt(0) >= 'A' && upperName.charAt(0) <= 'E'
                    ? upperName.substring(0, 1) : "OTHER";
            if (!groupedBlocks.containsKey(row)) {
                groupedBlocks.put(row, new ArrayList<>());
            }
            groupedBlocks.get(row).add(block);
        }

        List<BlockGroup> newGroups = new ArrayList<>();
        String[] rows = {"A", "B", "C", "D", "E", "OTHER"};

        for (String row : rows) {
            if (groupedBlocks.containsKey(row)) {
                BlockGroup group = new BlockGroup(row);
                group.setExpanded(true);
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

        final List<BlockEntity> finalAllBlocks = new ArrayList<>(allBlocks);
        final List<BlockEntity> finalVisibleBlocks = visibleBlocks;
        final List<BlockGroup> finalNewGroups = newGroups;

        requireActivity().runOnUiThread(() -> {
            updateFarmSummary(finalAllBlocks);
            if (finalNewGroups.isEmpty()) {
                showEmpty(true);
            } else {
                showEmpty(false);
                blockGroups = finalNewGroups;

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

        totalBlocksSummary.setText(String.valueOf(totalBlocks));
        double average = plantedBlocks == 0 ? 0 : allBlocks.stream().filter(BlockEntity::isPlanted)
                .mapToDouble(BlockEntity::getSurvivalRate).average().orElse(0);
        averageSurvivalSummary.setText(plantedBlocks == 0 ? "—%" : String.format(Locale.getDefault(), "%.0f%%", average));
        ((Chip) blockFilterGroup.findViewById(R.id.chipBlockAll)).setText("All · " + totalBlocks);
        ((Chip) blockFilterGroup.findViewById(R.id.chipBlockPlanted)).setText("Planted · " + plantedBlocks);
        ((Chip) blockFilterGroup.findViewById(R.id.chipBlockNotPlanted)).setText("Not planted · " + (totalBlocks - plantedBlocks));
        long attention = allBlocks.stream().filter(b -> b.isPlanted() && b.getSurvivalRate() < 70).count();
        ((Chip) blockFilterGroup.findViewById(R.id.chipBlockAttention)).setText("Attention · " + attention);
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
