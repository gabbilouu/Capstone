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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TaskListFragment extends Fragment implements View.OnClickListener {

    private NavController navC;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private final List<Task> taskList = new ArrayList<>();
    private final List<String> docIds = new ArrayList<>();
    private TextView tvTimer;
    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private TaskRepository repo;
    private ListenerRegistration registration;

    public TaskListFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = new TaskRepository();
        navC = NavHostFragment.findNavController(this);

        // Timer setup
        tvTimer = view.findViewById(R.id.tvTimer);
        startMidnightCountdown();

        // RecyclerView and adapter
        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TaskAdapter(taskList, docIds);
        recyclerView.setAdapter(adapter);

        // Toggle completion -> Firestore (always set to true; no unchecking)
        adapter.setOnCompletionToggleListener((taskId, task, completed) ->
                repo.setCompleted(taskId, task, true)
        );

        // Add task bar
        View addTaskBar = view.findViewById(R.id.addTaskBar);
        if (addTaskBar != null) {
            addTaskBar.setOnClickListener(v ->
                    navC.navigate(R.id.action_taskListFragment_to_newTaskFragment));
        }

        // Item click -> edit
        adapter.setOnItemClickListener((task, taskId) -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("task", task);
            bundle.putString("taskId", taskId);
            navC.navigate(R.id.action_taskListFragment_to_editTaskFragment, bundle);
        });

        // Swipe to delete (with Undo)
        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    private final ColorDrawable background = new ColorDrawable(Color.RED);
                    private final Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);

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

                        Snackbar.make(recyclerView, "Task deleted", Snackbar.LENGTH_LONG)
                                .setAction("Undo", v -> {
                                    taskList.add(position, deletedTask);
                                    docIds.add(position, docId);
                                    adapter.notifyItemInserted(position);
                                })
                                .addCallback(new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                            // Commit delete to Firestore only if not undone
                                            repo.delete(docId);
                                        }
                                    }
                                })
                                .show();
                    }

                    @Override
                    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh,
                                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);

                        View itemView = vh.itemView;
                        int backgroundCornerOffset = 20;

                        if (dX < 0) { // Swiping left
                            background.setBounds(
                                    itemView.getRight() + (int) dX - backgroundCornerOffset,
                                    itemView.getTop(),
                                    itemView.getRight(),
                                    itemView.getBottom()
                            );
                            background.draw(c);

                            if (deleteIcon != null) {
                                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                                int iconTop = itemView.getTop() + iconMargin;
                                int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                                int iconRight = itemView.getRight() - iconMargin;
                                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                                deleteIcon.draw(c);
                            }
                        }
                    }
                };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);

        // (Optional) result listener; realtime updates already handle insertions
        getParentFragmentManager().setFragmentResultListener("newTask", this,
                (requestKey, bundle) -> Log.d("TaskListFragment", "New task added"));

        // Start realtime import (single call)
        listenToFirebaseTasks();

        // Bottom nav buttons
        ImageButton homeButton = view.findViewById(R.id.HomeButton);
        ImageButton listButton = view.findViewById(R.id.CalendarButton);
        ImageButton profileButton = view.findViewById(R.id.SettingsButton);
        ImageButton taskButton = view.findViewById(R.id.TaskButton);

        if (homeButton != null) homeButton.setOnClickListener(this);
        if (listButton != null) listButton.setOnClickListener(this);
        if (profileButton != null) profileButton.setOnClickListener(this);
        if (taskButton != null) taskButton.setOnClickListener(v -> { /* no-op */ });
    }

    private void listenToFirebaseTasks() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        registration = repo.listenForUserTasks((snap, error) -> {
            if (error != null || snap == null) {
                if (error != null) Log.e("Firestore", "Listen failed.", error);
                return;
            }

            taskList.clear();
            docIds.clear();

            for (DocumentSnapshot doc : snap.getDocuments()) {
                Task task = doc.toObject(Task.class);
                if (task == null) continue;

                // Derive completion from completionKey for the current period
                boolean isCompleted = PeriodKeyUtil.currentKeyFor(task)
                        .equals(task.getCompletionKey());
                task.setCompleted(isCompleted); // UI-only flag

                // Safe defaults
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
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (navC == null) return;
        int id = v.getId();
        if (id == R.id.HomeButton) {
            navC.navigate(R.id.action_taskListFragment_to_homePageFragment);
        } else if (id == R.id.CalendarButton) {
            navC.navigate(R.id.action_taskListFragment_to_eventFragment);
        } else if (id == R.id.SettingsButton) {
            navC.navigate(R.id.action_taskListFragment_to_settingsFragment);
        }
    }
}
