package com.example.elevate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;


public class SettingsFragment extends Fragment implements View.OnClickListener {

    private NavController navC;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No additional setup needed here for now
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        // Navigation buttons
        ImageButton checkButton = view.findViewById(R.id.TaskButton);
        ImageButton listButton = view.findViewById(R.id.CalendarButton);
        ImageButton homeButton = view.findViewById(R.id.HomeButton);

        checkButton.setOnClickListener(this);
        listButton.setOnClickListener(this);
        homeButton.setOnClickListener(this);

        // Settings buttons
        Button myGoalsButton = view.findViewById(R.id.myGoalsButton);
        Button moodReportButton = view.findViewById(R.id.moodReportButton);
        Button generalButton = view.findViewById(R.id.generalButton);
        Button aboutButton = view.findViewById(R.id.aboutButton);
        Button notificationsButton = view.findViewById(R.id.notificationsButton);
        Button faqButton = view.findViewById(R.id.faqButton);
        Button contactUsButton = view.findViewById(R.id.contactUsButton);
        Button socialsButton = view.findViewById(R.id.socialsButton);

        myGoalsButton.setOnClickListener(this);
        moodReportButton.setOnClickListener(this);
        generalButton.setOnClickListener(this);
        aboutButton.setOnClickListener(this);
        notificationsButton.setOnClickListener(this);
        faqButton.setOnClickListener(this);
        contactUsButton.setOnClickListener(this);
        socialsButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (navC == null) return;

        switch (v.getId()) {
            // Navigation buttons
            case R.id.TaskButton:
                navC.navigate(R.id.action_settingsFragment_to_taskListFragment);
                break;
            case R.id.CalendarButton:
                navC.navigate(R.id.action_settingsFragment_to_eventFragment);
                break;
            case R.id.HomeButton:
                navC.navigate(R.id.action_settingsFragment_to_homePageFragment);
                break;

            // Settings buttons
            case R.id.myGoalsButton:
                Log.d("SettingsFragment", "My Goals clicked");
                break;
            case R.id.moodReportButton:
                Log.d("SettingsFragment", "Mood Report clicked");
                break;
            case R.id.generalButton:
                Log.d("SettingsFragment", "General clicked");
                break;
            case R.id.aboutButton:
                Log.d("SettingsFragment", "About clicked");
                break;
            case R.id.notificationsButton:
                Log.d("SettingsFragment", "Notifications clicked");
                break;
            case R.id.faqButton:
                Log.d("SettingsFragment", "FAQ clicked");
                break;
            case R.id.contactUsButton:
                Log.d("SettingsFragment", "Contact Us clicked");
                break;
            case R.id.socialsButton:
                Log.d("SettingsFragment", "Socials clicked");
                break;

            default:
                Log.w("SettingsFragment", "Unknown button clicked!");
        }
    }
}
