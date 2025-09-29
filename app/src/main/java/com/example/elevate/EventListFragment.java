package com.example.elevate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.Calendar;

public class EventListFragment extends Fragment implements View.OnClickListener {

    private NavController navC;

    // Bottom navigation buttons
    private ImageButton homeButton, checkButton, profileButton;

    public EventListFragment() {}

    public static EventListFragment newInstance(String param1, String param2) {
        EventListFragment fragment = new EventListFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        // Bottom nav buttons
        homeButton = view.findViewById(R.id.HomeButton);
        checkButton = view.findViewById(R.id.TaskButton);
        profileButton = view.findViewById(R.id.SettingsButton);

        if (homeButton != null) homeButton.setOnClickListener(this);
        if (checkButton != null) checkButton.setOnClickListener(this);
        if (profileButton != null) profileButton.setOnClickListener(this);

        // Calendar setup
        CalendarView calendarView = view.findViewById(R.id.calendarView);

        // Set calendar to show today
        Calendar today = Calendar.getInstance();
        calendarView.setDate(today.getTimeInMillis(), false, true);

        // Handle date selection
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            String selectedDate = (month + 1) + "/" + dayOfMonth + "/" + year;
            Toast.makeText(getContext(), "Selected: " + selectedDate, Toast.LENGTH_SHORT).show();
            // TODO: Update "Today's Schedule" section here
        });

        // Add Event button navigation
        Button addEventButton = view.findViewById(R.id.addEventButton);
        addEventButton.setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_eventFragment_to_newEventFragment);
            } else {
                Log.e("EventListFragment", "NavController is null!");
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (navC == null) return;

        int id = v.getId();
        if (id == R.id.HomeButton) {
            navC.navigate(R.id.action_eventFragment_to_homePageFragment);
        } else if (id == R.id.TaskButton) {
            navC.navigate(R.id.action_eventFragment_to_taskListFragment);
        } else if (id == R.id.SettingsButton) {
            navC.navigate(R.id.action_eventFragment_to_settingsFragment);
        }
    }
}
