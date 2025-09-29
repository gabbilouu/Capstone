package com.example.elevate;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LoginStreakFragment extends Fragment {

    private NavController navC;
    private TextView streakNumber;
    private TextView[] daysOfWeek = new TextView[7];
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "LoginStreakPrefs";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_LOGIN = "lastLogin";

    public LoginStreakFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login_streak, container, false);

        // Initialize views
        streakNumber = view.findViewById(R.id.streakNumber);
        daysOfWeek[0] = view.findViewById(R.id.day_sun);
        daysOfWeek[1] = view.findViewById(R.id.day_mon);
        daysOfWeek[2] = view.findViewById(R.id.day_tue);
        daysOfWeek[3] = view.findViewById(R.id.day_wed);
        daysOfWeek[4] = view.findViewById(R.id.day_thu);
        daysOfWeek[5] = view.findViewById(R.id.day_fri);
        daysOfWeek[6] = view.findViewById(R.id.day_sat);

        // Load SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);

        updateStreak();

        // Use NavHostFragment to safely get NavController
        navC = androidx.navigation.fragment.NavHostFragment.findNavController(this);

        // Navigate via Navigation Component
        view.findViewById(R.id.startButton).setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_loginStreakFragment_to_homePageFragment);
            }
        });

        return view;
    }





    private void updateStreak() {
        int streak = prefs.getInt(KEY_STREAK, 0);
        String lastLoginStr = prefs.getString(KEY_LAST_LOGIN, "");
        Calendar today = Calendar.getInstance();

        // Check if last login was yesterday (for streak continuation)
        Calendar lastLogin = Calendar.getInstance();
        if (!lastLoginStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                lastLogin.setTime(sdf.parse(lastLoginStr));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If last login is not yesterday, reset streak
        if (!lastLoginStr.isEmpty()) {
            long diff = today.getTimeInMillis() - lastLogin.getTimeInMillis();
            long diffDays = diff / (1000 * 60 * 60 * 24);
            if (diffDays > 1) { // missed at least one day
                streak = 0;
            } else {
                streak++; // continue streak
            }
        } else {
            streak = 1; // first login
        }

        // Save updated streak and today's date
        prefs.edit()
                .putInt(KEY_STREAK, streak)
                .putString(KEY_LAST_LOGIN, new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today.getTime()))
                .apply();

        streakNumber.setText(String.valueOf(streak));

        highlightDays(streak, today.get(Calendar.DAY_OF_WEEK));
    }

    private void highlightDays(int streak, int todayWeekDay) {
        // Reset all days to "missed" color
        for (TextView day : daysOfWeek) {
            day.setBackgroundColor(Color.LTGRAY);
            day.setTextColor(Color.WHITE);
        }

        // Highlight days logged in
        int index = todayWeekDay - 1; // Calendar.SUNDAY=1 → index=0
        for (int i = 0; i < streak; i++) {
            int dayIndex = (index - i + 7) % 7;
            daysOfWeek[dayIndex].setBackgroundColor(Color.GREEN);
            daysOfWeek[dayIndex].setTextColor(Color.WHITE);
        }
    }
}
