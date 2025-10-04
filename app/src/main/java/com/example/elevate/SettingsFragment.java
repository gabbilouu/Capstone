package com.example.elevate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private NavController navC;
    private static final String PREFS_NAME = "LoginStreakPrefs";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_LOGIN = "lastLogin";
    private static final String KEY_GOAL = "userGoal";

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        // --- User since ---
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView userSince = view.findViewById(R.id.userSince);
        if (user != null && user.getMetadata() != null) {
            String signupDate = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(new Date(user.getMetadata().getCreationTimestamp()));
            userSince.setText("User Since: " + signupDate);
        } else {
            userSince.setText("User Since: Unknown");
        }

        // --- Load streak and radar chart ---
        updateAndDisplayStreak(view);
        setupPersonalityRadarChart();

        // --- Bottom navigation buttons ---
        ImageButton taskButton = view.findViewById(R.id.TaskButton);
        ImageButton calendarButton = view.findViewById(R.id.CalendarButton);
        ImageButton homeButton = view.findViewById(R.id.HomeButton);

        taskButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_taskListFragment));
        calendarButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_eventFragment));
        homeButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_homePageFragment));

        // --- Settings buttons ---
        Button myGoalsButton = view.findViewById(R.id.myGoalsButton);
        Button moodReportButton = view.findViewById(R.id.moodReportButton);
        Button generalButton = view.findViewById(R.id.generalButton);
        Button aboutButton = view.findViewById(R.id.aboutButton);
        Button notificationsButton = view.findViewById(R.id.notificationsButton);
        Button faqButton = view.findViewById(R.id.faqButton);
        Button contactUsButton = view.findViewById(R.id.contactUsButton);

        // My Goals button — shows user's goal and lets them change it
        myGoalsButton.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
            String currentGoal = prefs.getString(KEY_GOAL, "No goal set");

            new AlertDialog.Builder(requireContext())
                    .setTitle("Your Goal")
                    .setMessage("You selected:\n\n" + currentGoal)
                    .setPositiveButton("Change Goal", (dialog, which) ->
                            navC.navigate(R.id.action_settingsFragment_to_questionaireFragment))
                    .setNegativeButton("Close", null)
                    .show();
        });

        // Other button navigations
        generalButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_generalSettingsFragment));
        aboutButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_aboutFragment));
        notificationsButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_notificationsFragment));
        faqButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_FAQFragment));
        contactUsButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_contactUsFragment));
    }

    // --- Display streak info ---
    private void updateAndDisplayStreak(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        int streak = prefs.getInt(KEY_STREAK, 0);
        String lastLoginStr = prefs.getString(KEY_LAST_LOGIN, "");

        Calendar today = Calendar.getInstance();
        Calendar lastLogin = Calendar.getInstance();

        if (!lastLoginStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                lastLogin.setTime(sdf.parse(lastLoginStr));
                long diffDays = (today.getTimeInMillis() - lastLogin.getTimeInMillis()) / (1000 * 60 * 60 * 24);
                if (diffDays > 1) streak = 0;
                else if (diffDays == 1) streak++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            streak = 1;
        }

        prefs.edit()
                .putInt(KEY_STREAK, streak)
                .putString(KEY_LAST_LOGIN, new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today.getTime()))
                .apply();

        TextView streakTextView = requireView().findViewById(R.id.streakText);
        streakTextView.setText(streak > 0 ? "🌱 " + streak + " day streak!" : "🌱 No streak yet");
    }

    // --- Radar chart setup ---
    private void setupPersonalityRadarChart() {
        RadarChart radarChart = requireView().findViewById(R.id.personalityRadarChart);
        SharedPreferences prefs = requireContext().getSharedPreferences("MoodPrefs", 0);

        String[] moods = {"Very Happy", "Happy", "Neutral", "Sad", "Very Sad"};
        int[] counts = new int[moods.length];

        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            String dayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.getTime());
            String mood = prefs.getString(dayKey, null);
            if (mood != null) {
                for (int j = 0; j < moods.length; j++) {
                    if (mood.equals(moods[j])) counts[j]++;
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        ArrayList<RadarEntry> entries = new ArrayList<>();
        for (int c : counts) entries.add(new RadarEntry(c));

        RadarDataSet dataSet = new RadarDataSet(entries, "Past 30 Days Mood");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setFillColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(2f);

        RadarData data = new RadarData(dataSet);
        radarChart.setData(data);

        XAxis xAxis = radarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(moods));
        YAxis yAxis = radarChart.getYAxis();
        yAxis.setAxisMinimum(0f);

        radarChart.getDescription().setEnabled(false);
        radarChart.invalidate();
    }
}
