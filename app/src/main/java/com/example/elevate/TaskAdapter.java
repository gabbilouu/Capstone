package com.example.elevate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskEditListener {
        void onEdit(int position);
    }

    private List<Task> taskList;
    private OnTaskEditListener editListener;

    public TaskAdapter(List<Task> taskList, OnTaskEditListener editListener) {
        this.taskList = taskList;
        this.editListener = editListener;
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
        holder.tvIcon.setText(task.getIcon());
        holder.tvTaskName.setText(task.getName());
        holder.tvStatus.setText(task.isDone() ? "✔️" : "❌");

        // Edit when clicking the task name
        holder.itemView.setOnClickListener(v -> editListener.onEdit(position));
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTaskName, tvStatus;

        TaskViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
