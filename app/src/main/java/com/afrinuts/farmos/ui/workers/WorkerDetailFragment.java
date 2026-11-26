package com.afrinuts.farmos.ui.workers;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.database.AppDatabase;
import com.afrinuts.farmos.data.local.entity.FarmEntity;
import com.afrinuts.farmos.data.local.entity.TaskAssignmentHistoryEntity;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.data.repository.TaskAssignmentRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tablet inline worker detail. Phone worker navigation remains in WorkerDetailActivity. */
public class WorkerDetailFragment extends Fragment {
    private static final String ARG_WORKER_NAME = "worker_name";

    public static WorkerDetailFragment newInstance(String workerName) {
        WorkerDetailFragment fragment = new WorkerDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WORKER_NAME, workerName);
        fragment.setArguments(args);
        return fragment;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AppDatabase database;
    private TaskAssignmentRepository assignmentRepository;
    private String name;
    private TextView avatar, title, joined, completion, totalTasks, completed, rate, totalTime, averageTime, efficiency;
    private ProgressBar efficiencyBar;
    private LinearLayout taskTypes, recentTasks;
    private final Map<Long, TaskEntity> tasksById = new HashMap<>();
    private final Map<Long, String> blockNames = new HashMap<>();
    private final SimpleDateFormat fullDate = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat shortDate = new SimpleDateFormat("MMM d", Locale.getDefault());

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.fragment_worker_detail_tablet, container, false);
        database = AppDatabase.getInstance(requireContext());
        name = getArguments() == null ? null : getArguments().getString(ARG_WORKER_NAME);
        avatar = root.findViewById(R.id.workerPaneAvatar);
        title = root.findViewById(R.id.workerPaneName);
        joined = root.findViewById(R.id.workerPaneJoined);
        completion = root.findViewById(R.id.workerPaneCompletion);
        totalTasks = root.findViewById(R.id.workerPaneTotalTasks);
        completed = root.findViewById(R.id.workerPaneCompleted);
        rate = root.findViewById(R.id.workerPaneRate);
        totalTime = root.findViewById(R.id.workerPaneTotalTime);
        averageTime = root.findViewById(R.id.workerPaneAverageTime);
        efficiency = root.findViewById(R.id.workerPaneEfficiency);
        efficiencyBar = root.findViewById(R.id.workerPaneEfficiencyBar);
        taskTypes = root.findViewById(R.id.workerPaneTaskTypes);
        recentTasks = root.findViewById(R.id.workerPaneRecentTasks);
        title.setText(titleCase(name));
        avatar.setText(initials(name));
        load();
        return root;
    }

    private void load() {
        if (name == null || name.trim().isEmpty()) return;
        android.content.Context appContext = requireContext().getApplicationContext();
        executor.execute(() -> {
            FarmEntity farm = database.farmDao().getFirstFarm();
            long farmId = farm == null ? 1 : farm.getId();
            if (farm != null) {
                List<com.afrinuts.farmos.data.local.entity.BlockEntity> blocks = database.blockDao().getBlocksByFarmId(farmId);
                if (blocks != null) for (com.afrinuts.farmos.data.local.entity.BlockEntity block : blocks) blockNames.put(block.getId(), block.getBlockName());
            }
            assignmentRepository = new TaskAssignmentRepository(appContext, farmId);
            if (!isAdded()) { assignmentRepository.shutdown(); return; }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) { assignmentRepository.shutdown(); return; }
                assignmentRepository.getAssignmentsForWorkerLiveData(name).observe(getViewLifecycleOwner(), this::loadTasks);
            });
        });
    }

    private void loadTasks(List<TaskAssignmentHistoryEntity> assignments) {
        if (assignments == null) return;
        List<TaskAssignmentHistoryEntity> snapshot = new ArrayList<>(assignments);
        executor.execute(() -> {
            tasksById.clear();
            for (TaskAssignmentHistoryEntity assignment : snapshot) {
                TaskEntity task = database.taskDao().getTaskById(assignment.getTaskId());
                if (task != null) tasksById.put(assignment.getTaskId(), task);
            }
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (isAdded()) render(snapshot);
            });
        });
    }

    private void render(List<TaskAssignmentHistoryEntity> assignments) {
        WorkerPerformance performance = new WorkerPerformance(name);
        long joinedAt = Long.MAX_VALUE;
        for (TaskAssignmentHistoryEntity assignment : assignments) {
            performance.addAssignment(assignment);
            joinedAt = Math.min(joinedAt, assignment.getAssignedAt());
            TaskEntity task = tasksById.get(assignment.getTaskId());
            if (task != null && !performance.getTaskTypes().contains(task.getType().getDisplayName())) performance.getTaskTypes().add(task.getType().getDisplayName());
        }
        title.setText(titleCase(name));
        avatar.setText(initials(name));
        joined.setText("Field Worker · Joined " + (joinedAt == Long.MAX_VALUE ? "—" : fullDate.format(new Date(joinedAt))));
        double completionRate = performance.getCompletionRate();
        completion.setText("✓  " + String.format(Locale.getDefault(), "%.0f%% completion", completionRate));
        totalTasks.setText(String.valueOf(performance.getTotalTasks()));
        completed.setText(String.valueOf(performance.getCompletedTasks()));
        rate.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));
        totalTime.setText(performance.getFormattedTotalTime());
        averageTime.setText(performance.getFormattedAverageTime());
        efficiency.setText(String.format(Locale.getDefault(), "%.0f%%", completionRate));
        efficiencyBar.setProgress((int) completionRate);
        renderTaskTypes(assignments);
        renderRecentTasks(assignments);
    }

    private void renderTaskTypes(List<TaskAssignmentHistoryEntity> assignments) {
        taskTypes.removeAllViews();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TaskAssignmentHistoryEntity assignment : assignments) {
            TaskEntity task = tasksById.get(assignment.getTaskId());
            if (task == null || assignment.getUnassignedAt() == null) continue;
            String taskType = task.getType().getDisplayName();
            counts.put(taskType, counts.getOrDefault(taskType, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(13), 0, dp(13));
            TextView label = text(entry.getKey(), 18, 0xFF1C1B1A, true);
            TextView count = text(String.valueOf(entry.getValue()), 16, 0xFF075C32, true);
            count.setGravity(Gravity.CENTER);
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(0xFFE3F0E7);
            pill.setCornerRadius(dp(99));
            count.setBackground(pill);
            count.setPadding(dp(14), dp(7), dp(14), dp(7));
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(count, new LinearLayout.LayoutParams(-2, -2));
            taskTypes.addView(row);
            addDivider(taskTypes);
        }
        if (counts.isEmpty()) {
            TextView empty = text("No task types yet", 16, 0xFF514C45, false);
            empty.setPadding(0, dp(12), 0, dp(12));
            taskTypes.addView(empty);
        }
    }

    private void renderRecentTasks(List<TaskAssignmentHistoryEntity> assignments) {
        recentTasks.removeAllViews();
        assignments.sort((a, b) -> Long.compare(b.getAssignedAt(), a.getAssignedAt()));
        int shown = 0;
        for (TaskAssignmentHistoryEntity assignment : assignments) {
            TaskEntity task = tasksById.get(assignment.getTaskId());
            if (task == null) continue;
            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(14), 0, dp(14));
            LinearLayout texts = new LinearLayout(requireContext());
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView taskTitle = text(task.getTitle() + " — " + blockLabel(task), 18, 0xFF1C1B1A, true);
            taskTitle.setMaxLines(1);
            taskTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView taskDates = text(formatRange(assignment), 16, 0xFF514C45, false);
            texts.addView(taskTitle);
            LinearLayout.LayoutParams dates = new LinearLayout.LayoutParams(-1, -2);
            dates.topMargin = dp(3);
            texts.addView(taskDates, dates);
            row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
            TextView status = text(task.getStatus().getDisplayName().toUpperCase(Locale.ROOT), 13, 0xFF075C32, true);
            status.setBackgroundResource(R.drawable.bg_worker_completion_badge);
            status.setPadding(dp(10), dp(5), dp(10), dp(5));
            row.addView(status, new LinearLayout.LayoutParams(-2, -2));
            recentTasks.addView(row);
            if (++shown < Math.min(5, assignments.size())) addDivider(recentTasks);
        }
        if (shown == 0) {
            TextView empty = text("No tasks assigned yet", 16, 0xFF514C45, false);
            empty.setPadding(0, dp(12), 0, dp(12));
            recentTasks.addView(empty);
        }
    }

    private String blockLabel(TaskEntity task) {
        if (task.getBlockId() == null) return "Farm-Wide";
        String block = blockNames.get(task.getBlockId());
        return block == null ? "Unknown Block" : block.startsWith("Block ") ? block : "Block " + block;
    }

    private String formatRange(TaskAssignmentHistoryEntity assignment) {
        if (assignment.getUnassignedAt() == null) return fullDate.format(new Date(assignment.getAssignedAt()));
        return shortDate.format(new Date(assignment.getAssignedAt())) + " – " + fullDate.format(new Date(assignment.getUnassignedAt()));
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(requireContext());
        divider.setBackgroundColor(0xFFE9E1CF);
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView result = new TextView(requireContext());
        result.setText(value);
        result.setTextSize(size);
        result.setTextColor(color);
        result.setTypeface(android.graphics.Typeface.create("sans-serif", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        return result;
    }

    private String initials(String value) {
        if (value == null || value.trim().isEmpty()) return "—";
        String[] parts = value.trim().split("\\s+");
        return parts.length > 1 ? (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT) : parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String titleCase(String value) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder();
        for (String part : value.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (result.length() > 0) result.append(' ');
            if (!part.isEmpty()) result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return result.toString();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override public void onDestroy() {
        super.onDestroy();
        if (assignmentRepository != null) assignmentRepository.shutdown();
        executor.shutdown();
    }
}
