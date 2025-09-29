package com.example.elevate;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment implements View.OnClickListener {

    NavController navC = null;

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;

    public TaskListFragment() {
        // Required empty public constructor
    }

    public static TaskListFragment newInstance(String param1, String param2) {
        TaskListFragment fragment = new TaskListFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        // 🔹 Setup RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Example tasks (replace with real data later)
        taskList = new ArrayList<>();
        taskList.add(new Task("🌞", "Get out of bed", true));
        taskList.add(new Task("🧴", "Do morning skincare routine", false));
        taskList.add(new Task("🤸", "Do morning stretches", false));

        adapter = new TaskAdapter(taskList, position -> showEditDialog(position));
        recyclerView.setAdapter(adapter);

        // 🔹 Bottom navigation buttons
        ImageButton homeButton = view.findViewById(R.id.HomeButton);
        ImageButton listButton = view.findViewById(R.id.CalendarButton);
        ImageButton profileButton = view.findViewById(R.id.SettingsButton);

        if (homeButton != null) homeButton.setOnClickListener(this);
        else Log.e("TaskListFragment", "HomeButton not found!");

        if (listButton != null) listButton.setOnClickListener(this);
        else Log.e("TaskListFragment", "ListButton not found!");

        if (profileButton != null) profileButton.setOnClickListener(this);
        else Log.e("TaskListFragment", "ProfileButton not found!");
    }

    private void showEditDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Task");

        final EditText input = new EditText(requireContext());
        input.setText(taskList.get(position).getName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            taskList.get(position).setName(input.getText().toString());
            adapter.notifyItemChanged(position);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onClick(View v) {
        if (navC != null) {
            if (v.getId() == R.id.HomeButton) {
                navC.navigate(R.id.action_taskListFragment_to_homePageFragment);
            } else if (v.getId() == R.id.CalendarButton) {
                navC.navigate(R.id.action_taskListFragment_to_eventFragment);
            } else if (v.getId() == R.id.SettingsButton) {
                navC.navigate(R.id.action_taskListFragment_to_settingsFragment);
            }
        }
    }
}
