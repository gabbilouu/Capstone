package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class DailyWeeklyFragment extends Fragment {

    private NavController navC;
    private NotificationsManager notificationsManager; // Notification helper class

    public DailyWeeklyFragment() {
        // Required empty public constructor
    }

    public static DailyWeeklyFragment newInstance(String param1, String param2) {
        DailyWeeklyFragment fragment = new DailyWeeklyFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_weekly, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);
        notificationsManager = new NotificationsManager(requireContext()); // Initialize

        Button dailyButton = view.findViewById(R.id.dailyButton);
        Button weeklyButton = view.findViewById(R.id.weeklyButton);

        dailyButton.setOnClickListener(v -> {
            // Enable daily notifications
            notificationsManager.enableDailyNotifications();
            // Navigate to daily fragment
            navC.navigate(R.id.action_dailyWeeklyFragment_to_dailyFragment);
        });

        weeklyButton.setOnClickListener(v -> {
            // Enable weekly notifications
            notificationsManager.enableWeeklyNotifications();
            // Navigate to weekly fragment
            navC.navigate(R.id.action_dailyWeeklyFragment_to_weeklyFragment);
        });
    }
}
