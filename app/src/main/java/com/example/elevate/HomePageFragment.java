package com.example.elevate;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomePageFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "HomePageFragment";

    private static final int HYDRATION_GAIN_PER_TASK = 12;
    private static final int MAX_MOOD = 100;
    private static final int MAX_HYDRATION = 100;

    private static final String PREFS = "home_state";
    private static final String KEY_LAST_RESET = "last_reset_ymd";
    private static final String KEY_LAST_LOGIN = "last_login_ymd";
    private static final String KEY_STREAK     = "streak";
    private static final String KEY_MOOD       = "mood";
    private static final String KEY_HYDRATION  = "hydration";
    private static final String KEY_POINTS     = "points_total";
    private static final String KEY_TODAY_SNAPSHOT_DATE   = "today_snapshot_date";
    private static final String KEY_TODAY_COMPLETED_SNAP  = "today_completed_snapshot";
    private static final String KEY_YEST_DUE   = "yesterday_due";
    private static final String KEY_YEST_DONE  = "yesterday_done";
    private static final String KEY_YEST_DATE  = "yesterday_date";

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private NavController navC;

    private TextView tvName;
    private ProgressBar progressExp;
    private TextView tvStreak;
    private TextView tvPoints;
    private ProgressBar progressMood;
    private ProgressBar progressHydration;
    private ProgressBar progressDailyTasks;

    // If you add this TextView to XML later, we’ll use it. Otherwise we’ll write into the title.
    private TextView tvTasksCount;
    // You already have this in XML:
    private TextView tvTaskBoardTitle;

    private ImageButton taskButton, calendarButton, settingsButton;
    private android.content.SharedPreferences prefs;
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    private final SimpleDateFormat YMD = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final String[] DAYS_ABBR = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};

    public HomePageFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        navC = Navigation.findNavController(v);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (user == null) {
            navC.navigate(R.id.action_homePageFragment_to_LoginPageFragment);
            return;
        }
        db = FirebaseFirestore.getInstance();

        bindViews(v);
        prefs = requireContext().getSharedPreferences(PREFS, 0);
        prefListener = (p, key) -> {
            if (KEY_POINTS.equals(key) || KEY_HYDRATION.equals(key) ||
                    KEY_TODAY_COMPLETED_SNAP.equals(key) || KEY_TODAY_SNAPSHOT_DATE.equals(key)) {
                refreshQuickFromPrefs();
            }
        };

        wireBottomNav();

        if (tvName != null) tvName.setText("Perry"); // or your stored plant name

        handleLoginAndDailyReset(this::loadTasksAndRender);
    }

    private void bindViews(View v) {
        tvName = v.findViewById(R.id.tvName);

        // use IDs that exist in your XML
        progressExp = v.findViewById(R.id.progressExp);
        tvStreak    = v.findViewById(R.id.tvStreak);
        tvPoints    = v.findViewById(R.id.tvPoints);

        progressMood       = v.findViewById(R.id.progressMood);
        progressHydration  = v.findViewById(R.id.progressHydration);
        progressDailyTasks = v.findViewById(R.id.progressDailyTasks);

        tvTaskBoardTitle = v.findViewById(R.id.tvTaskBoardTitle);

        taskButton     = v.findViewById(R.id.TaskButton);
        calendarButton = v.findViewById(R.id.CalendarButton);
        settingsButton = v.findViewById(R.id.SettingsButton);
    }

    private void wireBottomNav() {
        if (taskButton != null) taskButton.setOnClickListener(this);
        if (calendarButton != null) calendarButton.setOnClickListener(this);
        if (settingsButton != null) settingsButton.setOnClickListener(this);
    }

    private void handleLoginAndDailyReset(Runnable done) {
        String today = YMD.format(new Date());
        String lastLogin = getPrefs().getString(KEY_LAST_LOGIN, null);
        String lastReset = getPrefs().getString(KEY_LAST_RESET, null);

        int streak = getPrefs().getInt(KEY_STREAK, 0);
        if (!TextUtils.equals(lastLogin, today)) {
            if (isYesterday(lastLogin)) streak += 1;
            else streak = 1;
            getPrefs().edit().putInt(KEY_STREAK, streak).putString(KEY_LAST_LOGIN, today).apply();
        }
        if (tvStreak != null) tvStreak.setText(String.valueOf(streak));

        if (TextUtils.equals(lastReset, today)) {
            if (progressMood != null) progressMood.setProgress(getPrefs().getInt(KEY_MOOD, 80));
            if (progressHydration != null) progressHydration.setProgress(getPrefs().getInt(KEY_HYDRATION, 80));
            done.run();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = YMD.format(cal.getTime());

        db.collection("tasks").get().addOnSuccessListener(snap -> {
            int dueYesterday = 0;
            int doneYesterday = 0;
            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    Task t = ds.toObject(Task.class);
                    if (t == null) continue;
                    if (isTaskDueOn(t, cal)) {
                        dueYesterday++;
                        if (Boolean.TRUE.equals(t.isCompleted())) doneYesterday++;
                    }
                }
            }

            getPrefs().edit()
                    .putString(KEY_YEST_DATE, yesterday)
                    .putInt(KEY_YEST_DUE, dueYesterday)
                    .putInt(KEY_YEST_DONE, doneYesterday)
                    .apply();

            int mood = clamp0_100(getPrefs().getInt(KEY_MOOD, 80));
            int hydration = clamp0_100(getPrefs().getInt(KEY_HYDRATION, 80));

            hydration = Math.max(0, hydration / 2);

            boolean missedLoginYesterday = !isYesterday(lastLogin);
            if (missedLoginYesterday) mood = Math.max(0, mood - 1);
            boolean missedAnyTasksYesterday = (dueYesterday > 0 && doneYesterday < dueYesterday);
            if (missedAnyTasksYesterday) mood = Math.max(0, mood - 1);

            getPrefs().edit()
                    .putInt(KEY_MOOD, mood)
                    .putInt(KEY_HYDRATION, hydration)
                    .putString(KEY_LAST_RESET, today)
                    .putString(KEY_TODAY_SNAPSHOT_DATE, today)
                    .putInt(KEY_TODAY_COMPLETED_SNAP, 0)
                    .apply();

            if (progressMood != null) progressMood.setProgress(mood);
            if (progressHydration != null) progressHydration.setProgress(hydration);

            done.run();
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Failed to compute yesterday penalties; doing minimal reset", e);
            int mood = Math.max(0, getPrefs().getInt(KEY_MOOD, 80) - 1);
            int hydration = Math.max(0, getPrefs().getInt(KEY_HYDRATION, 80) / 2);

            getPrefs().edit()
                    .putInt(KEY_MOOD, mood)
                    .putInt(KEY_HYDRATION, hydration)
                    .putString(KEY_LAST_RESET, today)
                    .putString(KEY_TODAY_SNAPSHOT_DATE, today)
                    .putInt(KEY_TODAY_COMPLETED_SNAP, 0)
                    .apply();

            if (progressMood != null) progressMood.setProgress(mood);
            if (progressHydration != null) progressHydration.setProgress(hydration);
            done.run();
        });
    }

    private void loadTasksAndRender() {
        String today = YMD.format(new Date());

        db.collection("tasks").get().addOnSuccessListener(snap -> {
            int dueToday = 0;
            int completedToday = 0;

            Calendar todayCal = Calendar.getInstance();

            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    Task t = ds.toObject(Task.class);
                    if (t == null) continue;

                    if (isTaskDueOn(t, todayCal)) {
                        dueToday++;
                        if (Boolean.TRUE.equals(t.isCompleted())) completedToday++;
                    }
                }
            }

            // Daily task board
            if (progressDailyTasks != null) {
                progressDailyTasks.setMax(Math.max(1, dueToday));
                progressDailyTasks.setProgress(completedToday);
            }
            if (tvTasksCount != null) {
                tvTasksCount.setText(completedToday + "/" + Math.max(1, dueToday));
            } else if (tvTaskBoardTitle != null) {
                tvTaskBoardTitle.setText("Daily Task Board (" + completedToday + "/" + Math.max(1, dueToday) + ")");
            }

            // Points & EXP (new completions delta)
            int points = getPrefs().getInt(KEY_POINTS, 0);
            String snapDate = getPrefs().getString(KEY_TODAY_SNAPSHOT_DATE, null);
            int prevCompletedSnap = getPrefs().getInt(KEY_TODAY_COMPLETED_SNAP, 0);
            if (!TextUtils.equals(snapDate, today)) {
                prevCompletedSnap = 0;
            }
            int deltaNewCompletions = Math.max(0, completedToday - prevCompletedSnap);
            if (deltaNewCompletions > 0) {
                points += deltaNewCompletions;
                getPrefs().edit()
                        .putInt(KEY_POINTS, points)
                        .putString(KEY_TODAY_SNAPSHOT_DATE, today)
                        .putInt(KEY_TODAY_COMPLETED_SNAP, completedToday)
                        .apply();
            } else if (!TextUtils.equals(snapDate, today)) {
                getPrefs().edit()
                        .putString(KEY_TODAY_SNAPSHOT_DATE, today)
                        .putInt(KEY_TODAY_COMPLETED_SNAP, completedToday)
                        .apply();
            }
            if (tvPoints != null) tvPoints.setText(String.valueOf(points));

            LevelState ls = deriveLevelFromPoints(points);
            if (progressExp != null) {
                progressExp.setMax(ls.threshold);
                progressExp.setProgress(ls.expInLevel);
            }

            if (deltaNewCompletions > 0) {
                int hydration = clamp0_100(getPrefs().getInt(KEY_HYDRATION, 80)
                        + deltaNewCompletions * HYDRATION_GAIN_PER_TASK);
                getPrefs().edit().putInt(KEY_HYDRATION, hydration).apply();
                if (progressHydration != null) progressHydration.setProgress(hydration);
            } else {
                if (progressHydration != null) progressHydration.setProgress(getPrefs().getInt(KEY_HYDRATION, 80));
            }

            if (progressMood != null) progressMood.setProgress(getPrefs().getInt(KEY_MOOD, 80));
            if (tvStreak != null) tvStreak.setText(String.valueOf(getPrefs().getInt(KEY_STREAK, 0)));

        }).addOnFailureListener(e -> Log.e(TAG, "loadTasksAndRender failed", e));
    }

    private boolean isTaskDueOn(Task t, Calendar day) {
        String type = t.getRepeatType();
        if (type == null) return false;

        switch (type) {
            case "Daily":
                return true;
            case "Weekly":
                return day.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
            case "Monthly":
                return day.get(Calendar.DAY_OF_MONTH) == 1;
            case "Select Days":
                List<String> days = t.getRepeatDays();
                if (days == null || days.isEmpty()) return false;
                String abbr = DAYS_ABBR[day.get(Calendar.DAY_OF_WEEK) - 1];
                return days.contains(abbr);
            default:
                return false;
        }
    }

    private static class LevelState {
        int level;
        int expInLevel;
        int threshold;
    }

    private LevelState deriveLevelFromPoints(int points) {
        int n = 0;
        while (true) {
            int need = 5 * ((n + 1) * (n + 2)) / 2;
            if (points < need) break;
            n++;
        }
        int completedForN = 5 * (n * (n + 1)) / 2;
        LevelState ls = new LevelState();
        ls.level = n;
        ls.expInLevel = points - completedForN;
        ls.threshold = 5 * (n + 1);
        return ls;
    }

    private boolean isYesterday(String ymdStr) {
        if (TextUtils.isEmpty(ymdStr)) return false;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return YMD.format(cal.getTime()).equals(ymdStr);
    }

    private int clamp0_100(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private android.content.SharedPreferences getPrefs() {
        return requireContext().getSharedPreferences(PREFS, 0);
    }

    private void refreshQuickFromPrefs() {
        if (tvPoints != null) {
            int points = prefs.getInt(KEY_POINTS, 0);
            tvPoints.setText(String.valueOf(points));
            LevelState ls = deriveLevelFromPoints(points);
            if (progressExp != null) {
                progressExp.setMax(ls.threshold);
                progressExp.setProgress(ls.expInLevel);
            }
        }
        if (progressHydration != null) {
            progressHydration.setProgress(prefs.getInt(KEY_HYDRATION, 80));
        }
    }

    @Override
    public void onClick(View v) {
        if (navC == null) return;
        int id = v.getId();
        if (id == R.id.TaskButton) {
            navC.navigate(R.id.action_homePageFragment_to_taskListFragment);
        } else if (id == R.id.CalendarButton) {
            navC.navigate(R.id.action_homePageFragment_to_eventFragment);
        } else if (id == R.id.SettingsButton) {
            navC.navigate(R.id.action_homePageFragment_to_settingsFragment);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (prefs != null && prefListener != null) {
            prefs.registerOnSharedPreferenceChangeListener(prefListener);
        }
        refreshQuickFromPrefs();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (prefs != null && prefListener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        }
    }
}
