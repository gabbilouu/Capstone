package com.example.elevate;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TaskListFragment extends Fragment implements View.OnClickListener {

    private NavController navC;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private List<String> docIds;
    private FirebaseFirestore db;
    private TextView tvTimer;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    public TaskListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = NavHostFragment.findNavController(this);
        db = FirebaseFirestore.getInstance();

        // Timer setup
        tvTimer = view.findViewById(R.id.tvTimer);
        startMidnightCountdown();

        // RecyclerView and adapter
        taskList = new ArrayList<>();
        docIds = new ArrayList<>();
        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(taskList, docIds);
        recyclerView.setAdapter(adapter);

        // Add task bar
        View addTaskBar = view.findViewById(R.id.addTaskBar);
        addTaskBar.setOnClickListener(v ->
                navC.navigate(R.id.action_taskListFragment_to_newTaskFragment)
        );

        // Item click listener
        adapter.setOnItemClickListener((task, taskId) -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("task", task);
            bundle.putString("taskId", taskId);
            navC.navigate(R.id.action_taskListFragment_to_editTaskFragment, bundle);
        });
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(Color.RED);
            private final Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete); // add this icon to res/drawable

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String docId = docIds.get(position);
                Task deletedTask = taskList.get(position);

                // Remove locally for instant UI feedback
                taskList.remove(position);
                docIds.remove(position);
                adapter.notifyItemRemoved(position);

                // Show Snackbar with Undo
                Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> {
                            // Reinsert locally
                            taskList.add(position, deletedTask);
                            docIds.add(position, docId);
                            adapter.notifyItemInserted(position);
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                    // Delete from Firestore only if not undone
                                    db.collection("tasks").document(docId).delete()
                                            .addOnSuccessListener(aVoid ->
                                                    Log.d("TaskListFragment", "Task deleted from Firestore"))
                                            .addOnFailureListener(e ->
                                                    Log.e("TaskListFragment", "Error deleting task", e));
                                }
                            }
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                int backgroundCornerOffset = 20;

                if (dX < 0) { // Swiping left
                    background.setBounds(
                            itemView.getRight() + (int) dX - backgroundCornerOffset,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );
                    background.draw(c);

                    // Draw trash icon
                    if (deleteIcon != null) {
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(c);
                    }
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        // Listen for new task results (optional, snapshot listener handles updates)
        getParentFragmentManager().setFragmentResultListener("newTask", this,
                (requestKey, bundle) -> {
                    boolean added = bundle.getBoolean("taskAdded", false);
                    if (added) Log.d("TaskListFragment", "New task added, listener updates UI.");
                });

        // Load tasks and reset if needed
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

        // Task button is inert
        if (taskButton != null) taskButton.setOnClickListener(v -> {
            // Do nothing
        });
    }

    private void startMidnightCountdown() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                Calendar midnight = Calendar.getInstance();
                midnight.add(Calendar.DAY_OF_YEAR, 1);
                midnight.set(Calendar.HOUR_OF_DAY, 0);
                midnight.set(Calendar.MINUTE, 0);
                midnight.set(Calendar.SECOND, 0);
                midnight.set(Calendar.MILLISECOND, 0);

                long diffMillis = midnight.getTimeInMillis() - now.getTimeInMillis();
                long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis) % 60;

                String formatted = String.format(Locale.getDefault(), "⟳ %02d hrs %02d mins", hours, minutes);
                tvTimer.setText(formatted);

                timerHandler.postDelayed(this, 60 * 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
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

            for (var doc : value.getDocuments()) {
                Task task = doc.toObject(Task.class);
                if (task == null) continue;

                // Ensure no null fields
                if (task.getStartDate() == null) task.setStartDate("");
                if (task.getEndDate() == null) task.setEndDate("");
                if (task.getStartTime() == null) task.setStartTime("");
                if (task.getEndTime() == null) task.setEndTime("");
                if (task.getEmoji() == null) task.setEmoji("📝");
                if (task.getName() == null) task.setName("Unnamed Task");

                taskList.add(task);
                docIds.add(doc.getId());
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void resetTasksIfNeeded() {
        db.collection("tasks").get().addOnSuccessListener(querySnapshot -> {
            Calendar today = Calendar.getInstance();
            String[] weekdays = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};

            for (var doc : querySnapshot.getDocuments()) {
                Task task = doc.toObject(Task.class);
                if (task == null) continue;

                boolean shouldReset = false;
                String repeatType = task.getRepeatType();
                if (repeatType == null) continue;

                switch (repeatType) {
                    case "Daily": shouldReset = true; break;
                    case "Weekly": shouldReset = today.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY; break;
                    case "Monthly": shouldReset = today.get(Calendar.DAY_OF_MONTH) == 1; break;
                    case "Select Days":
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
