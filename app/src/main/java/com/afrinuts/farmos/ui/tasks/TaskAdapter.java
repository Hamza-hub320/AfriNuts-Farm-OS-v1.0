package com.afrinuts.farmos.ui.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.TaskEntity;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskWithBlockName> tasks;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(TaskEntity task);
    }

    public TaskAdapter(List<TaskWithBlockName> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskWithBlockName item = tasks.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private TextView tvTaskIcon;
        private TextView tvTaskTitle;
        private TextView tvTaskScope;
        private TextView tvTaskStatus;
        private TextView tvTaskDescription;
        private TextView tvDueDate;
        private TextView tvOverdueWarning;
        private CardView cardView;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskIcon = itemView.findViewById(R.id.tvTaskIcon);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskScope = itemView.findViewById(R.id.tvTaskScope);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvOverdueWarning = itemView.findViewById(R.id.tvOverdueWarning);
            cardView = (CardView) itemView;
        }

        void bind(TaskWithBlockName item, OnTaskClickListener listener) {
            TaskEntity task = item.getTask();

            // Set task icon based on type
            tvTaskIcon.setText(task.getType().getIcon());

            // Set title and scope
            tvTaskTitle.setText(task.getTitle());
            tvTaskScope.setText(item.getScopeDisplay());

            // Set status with color
            tvTaskStatus.setText(item.getStatusIcon() + " " + task.getStatus().getDisplayName());
            int statusColor = ContextCompat.getColor(itemView.getContext(), item.getStatusColor());
            tvTaskStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));

            // Set description (if available)
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                tvTaskDescription.setText(task.getDescription());
                tvTaskDescription.setVisibility(View.VISIBLE);
            } else {
                tvTaskDescription.setVisibility(View.GONE);
            }

            // Set due date
            tvDueDate.setText("📅 Due: " + item.getFormattedDueDate());

            // Show overdue warning if needed
            if (item.isOverdue()) {
                tvOverdueWarning.setVisibility(View.VISIBLE);
                tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
            } else {
                tvOverdueWarning.setVisibility(View.GONE);
                tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.navy));
            }

            // Click listener
            cardView.setOnClickListener(v -> listener.onTaskClick(task));
        }
    }
}