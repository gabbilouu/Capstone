package com.example.elevate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskListFragment extends Fragment implements View.OnClickListener {

    private NavController navC;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private List<String> docIds;
    private FirebaseFirestore db;

    public TaskListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);
        db = FirebaseFirestore.getInstance();

        taskList = new ArrayList<>();
        docIds = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new TaskAdapter(taskList, docIds);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener((task, taskId) -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("task", task);
            bundle.putString("taskId", taskId);
            navC.navigate(R.id.action_taskListFragment_to_editTaskFragment, bundle);
        });

        listenToFirebaseTasks();
        resetTasksIfNeeded();

        // Bottom nav buttons
        ImageButton homeButton = view.findViewById(R.id.HomeButton);
        ImageButton listButton = view.findViewById(R.id.CalendarButton);
        ImageButton profileButton = view.findViewById(R.id.SettingsButton);
        ImageButton taskButton = view.findViewById(R.id.TaskButton);

        if (homeButton != null) homeButton.setOnClickListener(this);
        if (listButton != null) listButton.setOnClickListener(this);
        if (profileButton != null) profileButton.setOnClickListener(this);
        if (taskButton != null) taskButton.setOnClickListener(v ->
                navC.navigate(R.id.action_taskListFragment_to_newTaskFragment));
    }

    private void listenToFirebaseTasks() {
        db.collection("tasks").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Listen failed.", error);
                return;
            }
            if (value == null) return;

            taskList.clear();
            docIds.clear();

            for (DocumentChange dc : value.getDocumentChanges()) {
                Task task = dc.getDocument().toObject(Task.class);
                String docId = dc.getDocument().getId();

                switch (dc.getType()) {
                    case ADDED:
                        taskList.add(task);
                        docIds.add(docId);
                        break;
                    case MODIFIED:
                        int index = findTaskIndex(docId);
                        if (index != -1) taskList.set(index, task);
                        break;
                    case REMOVED:
                        index = findTaskIndex(docId);
                        if (index != -1) {
                            taskList.remove(index);
                            docIds.remove(index);
                        }
                        break;
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private int findTaskIndex(String docId) {
        return docIds.indexOf(docId);
    }

    private void resetTasksIfNeeded() {
        db.collection("tasks").get().addOnSuccessListener(querySnapshot -> {
            Calendar today = Calendar.getInstance();

            for (var doc : querySnapshot.getDocuments()) {
                Task task = doc.toObject(Task.class);
                if (task == null) continue;

                boolean shouldReset = false;

                switch (task.getRepeatType()) {
                    case "Daily": shouldReset = true; break;
                    case "Weekly":
                        shouldReset = today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
                        break;
                    case "Monthly":
                        shouldReset = today.get(Calendar.DAY_OF_MONTH) == 1;
                        break;
                    case "Select Days":
                        String[] weekdays = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
                        Calendar yesterday = (Calendar) today.clone();
                        yesterday.add(Calendar.DAY_OF_MONTH, -1);
                        String yesterdayStr = weekdays[yesterday.get(Calendar.DAY_OF_WEEK)-1];
                        if (task.getRepeatDays() != null && task.getRepeatDays().contains(yesterdayStr))
                            shouldReset = true;
                        break;
                }

                if (shouldReset && task.isCompleted()) {
                    Map<String,Object> updates = new HashMap<>();
                    updates.put("completed", false);
                    db.collection("tasks").document(doc.getId()).update(updates);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (navC == null) return;
        int id = v.getId();
        if (id == R.id.HomeButton) navC.navigate(R.id.action_taskListFragment_to_homePageFragment);
        else if (id == R.id.CalendarButton) navC.navigate(R.id.action_taskListFragment_to_eventFragment);
        else if (id == R.id.SettingsButton) navC.navigate(R.id.action_taskListFragment_to_settingsFragment);
    }
}
