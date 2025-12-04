package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class beginFragment extends Fragment {

    private NavController navC;

    public beginFragment() { }

    public static beginFragment newInstance(String param1, String param2) {
        beginFragment fragment = new beginFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_begin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        Button yesButton = view.findViewById(R.id.yesButton);
        Button noButton = view.findViewById(R.id.noButton);

        yesButton.setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_beginFragment_to_dailyWeeklyFragment);
            }
        });

        noButton.setOnClickListener(v -> {
            // Create an instance of NotificationsManager
            NotificationsManager notificationsManager = new NotificationsManager(requireContext());

            // Disable scheduled notifications
            notificationsManager.disableNotifications();

            if (navC != null) {
                navC.navigate(R.id.action_beginFragment_to_optOutFragment);
            }
        });

    }
}
