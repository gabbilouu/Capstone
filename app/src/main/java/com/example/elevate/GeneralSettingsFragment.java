package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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

    private static final String KEY_LOGIN_STREAKS   = "login_streaks_enabled";
    private static final String KEY_DAILY_MOOD      = "daily_mood_enabled";
    private static final String KEY_WEEKLY_MOOD     = "weekly_mood_enabled";
    private static final String KEY_AFFIRMATIONS    = "affirmations_enabled";
    private static final String KEY_SOUND_EFFECTS   = "sound_effects_enabled";
    private static final String KEY_HAPTIC          = "haptic_enabled";

    // Work tags (unique)
    private static final String WTAG_DAILY_MOOD   = "wt_daily_mood";
    private static final String WTAG_WEEKLY_MOOD  = "wt_weekly_mood";
    private static final String WTAG_AFFIRMATIONS = "wt_affirmations";

    private SharedPreferences prefs;

    private SwitchMaterial swLoginStreaks, swDailyMood, swWeeklyMood, swAffirmations, swSoundEffects, swHaptic;

    public GeneralSettingsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_general_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Toolbar back arrow
        Toolbar toolbar = v.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(x ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        // Bind switches
        swLoginStreaks  = v.findViewById(R.id.switch_login_streaks);
        swDailyMood     = v.findViewById(R.id.switch_daily_mood);
        swWeeklyMood    = v.findViewById(R.id.switch_weekly_mood);
        swAffirmations  = v.findViewById(R.id.switch_affirmations);
        swSoundEffects  = v.findViewById(R.id.switch_sound_effects);
        swHaptic        = v.findViewById(R.id.switch_haptic);

        // Hide the Launch Lock row entirely (since we're holding off on PIN)
        SwitchMaterial launchLockSwitch = v.findViewById(R.id.switch_launch_lock);
        if (launchLockSwitch != null) {
            View parentRow = (View) launchLockSwitch.getParent();
            if (parentRow != null) parentRow.setVisibility(View.GONE);
        }

        // Initial states
        swLoginStreaks.setChecked(prefs.getBoolean(KEY_LOGIN_STREAKS, false));
        swDailyMood.setChecked(prefs.getBoolean(KEY_DAILY_MOOD, false));
        swWeeklyMood.setChecked(prefs.getBoolean(KEY_WEEKLY_MOOD, false));
        swAffirmations.setChecked(prefs.getBoolean(KEY_AFFIRMATIONS, false));
        swSoundEffects.setChecked(prefs.getBoolean(KEY_SOUND_EFFECTS, true));
        swHaptic.setChecked(prefs.getBoolean(KEY_HAPTIC, true));

        // Listeners
        swLoginStreaks.setOnCheckedChangeListener((b, v1) -> putBool(KEY_LOGIN_STREAKS, v1));

        swDailyMood.setOnCheckedChangeListener((b, enabled) -> {
            putBool(KEY_DAILY_MOOD, enabled);
            if (enabled) scheduleDailyMood(); else cancelWork(WTAG_DAILY_MOOD);
        });

        swWeeklyMood.setOnCheckedChangeListener((b, enabled) -> {
            putBool(KEY_WEEKLY_MOOD, enabled);
            if (enabled) scheduleWeeklyMood(); else cancelWork(WTAG_WEEKLY_MOOD);
        });

        swAffirmations.setOnCheckedChangeListener((b, enabled) -> {
            putBool(KEY_AFFIRMATIONS, enabled);
            if (enabled) scheduleAffirmations(); else cancelWork(WTAG_AFFIRMATIONS);
        });

        swSoundEffects.setOnCheckedChangeListener((b, v12) -> putBool(KEY_SOUND_EFFECTS, v12));
        swHaptic.setOnCheckedChangeListener((b, v13) -> putBool(KEY_HAPTIC, v13));

        // Auto (re)schedule if already enabled
        if (swDailyMood.isChecked()) scheduleDailyMood();
        if (swWeeklyMood.isChecked()) scheduleWeeklyMood();
        if (swAffirmations.isChecked()) scheduleAffirmations();
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

    private void cancelWork(String tag) {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(tag);
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
