package com.example.elevate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final List<String> docIds;

    // existing: open editor
    private OnItemClickListener itemClickListener;

    // NEW: completion toggle callback
    public interface OnCompletionToggleListener {
        void onToggle(String taskId, Task task, boolean completed);
    }
    private OnCompletionToggleListener toggleListener;

    public TaskAdapter(List<Task> taskList, List<String> docIds) {
        this.taskList = taskList;
        this.docIds = docIds;
    }

    public interface OnItemClickListener {
        void onItemClick(Task task, String taskId);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnCompletionToggleListener(OnCompletionToggleListener listener) {
        this.toggleListener = listener;
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
        Task task = taskList.get(position);

        holder.tvEmoji.setText(task.getEmoji());
        holder.tvName.setText(task.getName());

        // Row click → open editor
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    itemClickListener.onItemClick(taskList.get(pos), docIds.get(pos));
                }
            }
        });

        // Checkbox binding (avoid recycle-triggered callbacks)
        holder.silencing = true;
        holder.cbDone.setChecked(task.isCompleted());
        holder.silencing = false;

        holder.cbDone.setOnCheckedChangeListener((button, isChecked) -> {
            if (holder.silencing) return;         // programmatic set
            if (!button.isPressed()) return;      // user-only
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            // optimistic local state; Firestore listener will reconcile anyway
            taskList.get(pos).setCompleted(isChecked);

            if (toggleListener != null) {
                toggleListener.onToggle(docIds.get(pos), taskList.get(pos), isChecked);
            }
        });
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName;
        CheckBox cbDone;
        boolean silencing = false;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.ivTaskEmoji);
            tvName  = itemView.findViewById(R.id.tvTaskName);
            cbDone  = itemView.findViewById(R.id.checkTaskDone); // <-- your id
        }
    }
}
