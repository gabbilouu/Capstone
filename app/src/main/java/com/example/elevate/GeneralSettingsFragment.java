package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class GeneralSettingsFragment extends Fragment {

    private static final String PREFS = "general_settings";

    private static final String KEY_DAILY_MOOD    = "daily_mood_enabled";
    private static final String KEY_WEEKLY_MOOD   = "weekly_mood_enabled";
    private static final String KEY_AFFIRMATIONS  = "affirmations_enabled";

    // Unique work names
    private static final String WTAG_DAILY_MOOD     = "wt_daily_mood";
    private static final String WTAG_WEEKLY_MOOD    = "wt_weekly_mood";
    private static final String WTAG_AFFIRMATIONS   = "wt_affirmations";

    private SharedPreferences prefs;

    private SwitchMaterial swDailyMood;
    private SwitchMaterial swWeeklyMood;
    private SwitchMaterial swAffirmations;

    // Prevent listener recursion when we programmatically flip switches
    private boolean suppressSwitchCallbacks = false;

    public GeneralSettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_general_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Back arrow button in custom header
        ImageButton back = v.findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(click ->
                    androidx.navigation.Navigation.findNavController(click).navigateUp()
            );
        }

        // Bind switches (ensure these IDs exist in fragment_general_settings.xml)
        swDailyMood     = v.findViewById(R.id.switch_daily_mood);
        swWeeklyMood    = v.findViewById(R.id.switch_weekly_mood);
        swAffirmations  = v.findViewById(R.id.switch_affirmations);

        // --- Read persisted values
        boolean daily  = prefs.getBoolean(KEY_DAILY_MOOD, false);
        boolean weekly = prefs.getBoolean(KEY_WEEKLY_MOOD, false);

        // Enforce "both off OR exactly one on" at startup.
        // If both were ON previously, prefer Daily and turn Weekly OFF.
        if (daily && weekly) {
            weekly = false;
            putBool(KEY_WEEKLY_MOOD, false);
            cancelWork(WTAG_WEEKLY_MOOD);
        }

        // Apply to UI (no listeners attached yet)
        if (swDailyMood != null)     swDailyMood.setChecked(daily);
        if (swWeeklyMood != null)    swWeeklyMood.setChecked(weekly);
        if (swAffirmations != null)  swAffirmations.setChecked(prefs.getBoolean(KEY_AFFIRMATIONS, false));

        // Listeners

        if (swDailyMood != null) {
            swDailyMood.setOnCheckedChangeListener((b, enabled) -> {
                if (suppressSwitchCallbacks) return;

                putBool(KEY_DAILY_MOOD, enabled);

                if (enabled) {
                    // If Weekly is ON, turn it OFF (mutual exclusivity)
                    if (swWeeklyMood != null && swWeeklyMood.isChecked()) {
                        suppressSwitchCallbacks = true;
                        swWeeklyMood.setChecked(false);
                        suppressSwitchCallbacks = false;
                        putBool(KEY_WEEKLY_MOOD, false);
                        cancelWork(WTAG_WEEKLY_MOOD);
                    }
                    scheduleDailyMood();
                } else {
                    // Both-off is allowed
                    cancelWork(WTAG_DAILY_MOOD);
                }
            });
        }

        if (swWeeklyMood != null) {
            swWeeklyMood.setOnCheckedChangeListener((b, enabled) -> {
                if (suppressSwitchCallbacks) return;

                putBool(KEY_WEEKLY_MOOD, enabled);

                if (enabled) {
                    // If Daily is ON, turn it OFF (mutual exclusivity)
                    if (swDailyMood != null && swDailyMood.isChecked()) {
                        suppressSwitchCallbacks = true;
                        swDailyMood.setChecked(false);
                        suppressSwitchCallbacks = false;
                        putBool(KEY_DAILY_MOOD, false);
                        cancelWork(WTAG_DAILY_MOOD);
                    }
                    scheduleWeeklyMood();
                } else {
                    // Both-off is allowed
                    cancelWork(WTAG_WEEKLY_MOOD);
                }
            });
        }

        if (swAffirmations != null) {
            swAffirmations.setOnCheckedChangeListener((b, enabled) -> {
                putBool(KEY_AFFIRMATIONS, enabled);
                if (enabled) scheduleAffirmations(); else cancelWork(WTAG_AFFIRMATIONS);
            });
        }

        // Auto (re)schedule based on final states
        if (swDailyMood != null)  { if (swDailyMood.isChecked())  scheduleDailyMood();  else cancelWork(WTAG_DAILY_MOOD); }
        if (swWeeklyMood != null) { if (swWeeklyMood.isChecked()) scheduleWeeklyMood(); else cancelWork(WTAG_WEEKLY_MOOD); }
        if (swAffirmations != null) { if (swAffirmations.isChecked()) scheduleAffirmations(); else cancelWork(WTAG_AFFIRMATIONS); }
    }

    // ---------- Work scheduling ----------
    private void scheduleDailyMood() {
        long delay = nextDelayMillis(9, 0, -1); // daily 9:00 AM
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(SimpleReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putString(SimpleReminderWorker.KEY_TITLE, "Daily Mood Check-in")
                        .putString(SimpleReminderWorker.KEY_TEXT, "How are you feeling today?")
                        .putString(SimpleReminderWorker.KEY_CHANNEL, ElevateApp.CH_DAILY_MOOD)
                        .putInt(SimpleReminderWorker.KEY_NOTIF_ID, 2001)
                        .build())
                .build();
        WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork(WTAG_DAILY_MOOD, ExistingPeriodicWorkPolicy.UPDATE, work);
    }

    private void scheduleWeeklyMood() {
        long delay = nextDelayMillis(9, 0, Calendar.MONDAY); // weekly Monday 9:00 AM
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(SimpleReminderWorker.class, 7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putString(SimpleReminderWorker.KEY_TITLE, "Weekly Mood Review")
                        .putString(SimpleReminderWorker.KEY_TEXT, "Take a minute to reflect on your week.")
                        .putString(SimpleReminderWorker.KEY_CHANNEL, ElevateApp.CH_WEEKLY_MOOD)
                        .putInt(SimpleReminderWorker.KEY_NOTIF_ID, 2002)
                        .build())
                .build();
        WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork(WTAG_WEEKLY_MOOD, ExistingPeriodicWorkPolicy.UPDATE, work);
    }

    private void scheduleAffirmations() {
        long delay = nextDelayMillis(7, 30, -1); // daily 7:30 AM
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(SimpleReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putString(SimpleReminderWorker.KEY_TITLE, "Daily Affirmation")
                        .putString(SimpleReminderWorker.KEY_TEXT, "You’ve got this. One step at a time 🌱")
                        .putString(SimpleReminderWorker.KEY_CHANNEL, ElevateApp.CH_AFFIRMATIONS)
                        .putInt(SimpleReminderWorker.KEY_NOTIF_ID, 2003)
                        .build())
                .build();
        WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork(WTAG_AFFIRMATIONS, ExistingPeriodicWorkPolicy.UPDATE, work);
    }

    private void cancelWork(String uniqueName) {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(uniqueName);
    }

    private long nextDelayMillis(int hour24, int min, int dayOfWeekOrMinus1) {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        next.set(Calendar.MINUTE, min);
        next.set(Calendar.HOUR_OF_DAY, hour24);

        if (dayOfWeekOrMinus1 != -1) {
            next.set(Calendar.DAY_OF_WEEK, dayOfWeekOrMinus1);
            if (!next.after(now)) next.add(Calendar.WEEK_OF_YEAR, 1);
        } else {
            if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return next.getTimeInMillis() - now.getTimeInMillis();
    }

    // ---------- prefs ----------
    private void putBool(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }
}
