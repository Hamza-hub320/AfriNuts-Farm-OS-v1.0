package com.afrinuts.farmos.ui.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final List<TaskWithBlockName> tasks;
    private final OnTaskClickListener listener;
    public interface OnTaskClickListener { void onTaskClick(TaskEntity task); }
    public TaskAdapter(List<TaskWithBlockName> tasks, OnTaskClickListener listener) { this.tasks = tasks; this.listener = listener; }
    @NonNull @Override public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) { holder.bind(tasks.get(position), listener, position); }
    @Override public int getItemCount() { return tasks.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        final TextView avatar, title, scope, due, status;
        final View dot;
        TaskViewHolder(@NonNull View view) {
            super(view);
            avatar = view.findViewById(R.id.tvTaskAvatar);
            title = view.findViewById(R.id.tvTaskTitle);
            scope = view.findViewById(R.id.tvTaskScope);
            due = view.findViewById(R.id.tvDueDate);
            status = view.findViewById(R.id.tvTaskStatus);
            dot = view.findViewById(R.id.ivTaskDot);
        }
        void bind(TaskWithBlockName row, OnTaskClickListener listener, int position) {
            TaskEntity task = row.getTask();
            title.setText(task.getTitle());
            String assignee = row.getAssigneeName();
            String initials = "—";
            if (assignee != null && !assignee.trim().isEmpty()) {
                String[] parts = assignee.trim().split("\\s+");
                initials = parts.length > 1 ? parts[0].substring(0, 1) + parts[1].substring(0, 1) : parts[0].substring(0, 1);
            }
            avatar.setText(initials.toUpperCase(Locale.ROOT));
            int[] bgs = {R.drawable.bg_supplier_avatar_green, R.drawable.bg_supplier_avatar_sand, R.drawable.bg_supplier_avatar_plain};
            int[] fg = {0xFF0A5530, 0xFF7A5F22, 0xFF6B675F};
            avatar.setBackgroundResource(bgs[position % bgs.length]);
            avatar.setTextColor(fg[position % fg.length]);
            scope.setText(row.getScopeDisplay() + " · " + (assignee == null || assignee.isEmpty() ? "Unassigned" : assignee.split("\\s+")[0]));
            boolean overdue = row.isOverdue();
            due.setText(overdue ? "Overdue" : row.getFormattedDueDate());
            due.setTextColor(overdue ? 0xFFB3402A : 0xFF1C1B1A);
            status.setText(overdue ? daysLate(task.getDueDate()) + " days late" : task.getStatus().getDisplayName());
            status.setTextColor(overdue ? 0xFFB3402A : 0xFF59544C);
            dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(overdue ? 0xFFB3402A : 0xFF0E6B3C));
            itemView.setOnClickListener(v -> listener.onTaskClick(task));
        }
        private long daysLate(long dueDate) { return Math.max(1, (System.currentTimeMillis() - dueDate) / (24L * 60 * 60 * 1000)); }
    }
}
