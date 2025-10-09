package com.example.elevate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final List<String> docIds;
    private OnItemClickListener listener;

    public TaskAdapter(List<Task> taskList, List<String> docIds) {
        this.taskList = taskList;
        this.docIds = docIds;
    }

    public interface OnItemClickListener {
        void onItemClick(Task task, String taskId);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvEmoji.setText(task.getEmoji());
        holder.tvName.setText(task.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(task, docIds.get(position));
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.ivTaskEmoji);
            tvName = itemView.findViewById(R.id.tvTaskName);
        }
    }
}
