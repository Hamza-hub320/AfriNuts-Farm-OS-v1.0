package com.afrinuts.farmos.ui.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.afrinuts.farmos.R;
import com.afrinuts.farmos.data.local.entity.TaskEntity;
import com.afrinuts.farmos.ui.tasks.TaskDetailActivity;

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

        // Changed from TextView to ImageView
        private ImageView tvTaskIcon;
        private TextView tvTaskTitle;
        private TextView tvTaskScope;
        private TextView tvTaskStatus;
        private TextView tvTaskDescription;
        private TextView tvDueDate;
        private TextView tvOverdueWarning;
        private View overdueLayout;
        private CardView cardView;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            // Updated to find ImageView
            tvTaskIcon = itemView.findViewById(R.id.tvTaskIcon);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskScope = itemView.findViewById(R.id.tvTaskScope);
            tvTaskStatus = itemView.findViewById(R.id.tvTaskStatus);
            tvTaskDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvOverdueWarning = itemView.findViewById(R.id.tvOverdueWarning);
            overdueLayout = itemView.findViewById(R.id.overdueLayout);
            cardView = (CardView) itemView;
        }

        void bind(TaskWithBlockName item, OnTaskClickListener listener) {
            TaskEntity task = item.getTask();

            // Set task icon based on type - now using setImageResource instead of setText
            switch (task.getType()) {
                case CLEARING:
                    tvTaskIcon.setImageResource(R.drawable.ic_tractor);
                    break;
                case PLOWING:
                    tvTaskIcon.setImageResource(R.drawable.ic_grain);
                    break;
                case PLANTING:
                    tvTaskIcon.setImageResource(R.drawable.ic_grass);
                    break;
                case REPLACEMENT:
                    tvTaskIcon.setImageResource(R.drawable.ic_sync);
                    break;
                case FERTILIZING:
                    tvTaskIcon.setImageResource(R.drawable.ic_water_drop);
                    break;
                case PRUNING:
                    tvTaskIcon.setImageResource(R.drawable.ic_cut);
                    break;
                case WEEDING:
                    tvTaskIcon.setImageResource(R.drawable.ic_grass);
                    break;
                case HARVEST:
                    tvTaskIcon.setImageResource(R.drawable.ic_download);
                    break;
                case IRRIGATION:
                    tvTaskIcon.setImageResource(R.drawable.ic_water_drop);
                    break;
                case MAINTENANCE:
                    tvTaskIcon.setImageResource(R.drawable.ic_construction);
                    break;
                default:
                    tvTaskIcon.setImageResource(R.drawable.ic_note);
                    break;
            }

            // Apply tint to match brand color
            tvTaskIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.primary));

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
            tvDueDate.setText(item.getFormattedDueDate());

            // Show overdue warning if needed
            if (item.isOverdue()) {
                overdueLayout.setVisibility(View.VISIBLE);
                tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
            } else {
                overdueLayout.setVisibility(View.GONE);
                tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.navy));
            }

            // Click listener
            cardView.setOnClickListener(v -> {
                android.content.Context context = itemView.getContext();
                android.content.Intent intent =
                        new android.content.Intent(context, TaskDetailActivity.class);
                intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task.getId());
                context.startActivity(intent);
            });
        }
    }
}