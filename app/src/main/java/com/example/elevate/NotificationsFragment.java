package com.example.elevate;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationsFragment extends Fragment {

    private static final String PREFS = "notifications_prefs";

    // keys
    private static final String KEY_PUSH_MASTER    = "push_master";
    private static final String KEY_MSGS           = "notif_msgs";
    private static final String KEY_AFFIRMATIONS   = "notif_affirmations";
    private static final String KEY_TASKS          = "notif_tasks";
    private static final String KEY_CALENDAR       = "notif_calendar";
    private static final String KEY_MOOD           = "notif_mood";

    // unique work names (match your GeneralSettings ones where applicable!)
    private static final String WTAG_AFFIRMATIONS   = "wt_affirmations";     // matches your existing tag
    private static final String WTAG_PLANT_MESSAGES = "wt_plant_messages";
    private static final String WTAG_TASKS          = "wt_tasks_daily";
    private static final String WTAG_CALENDAR       = "wt_calendar_daily";
    private static final String WTAG_MOOD_WEEKLY    = "wt_weekly_mood";      // matches your existing weekly mood if you want

    // channels (make sure these exist in ElevateApp)
    private static final String CH_PLANT_MESSAGES = "plant_messages";
    private static final String CH_TASKS          = "tasks";
    private static final String CH_CALENDAR       = "calendar";
    private static final String CH_AFFIRMATIONS   = ElevateApp.CH_AFFIRMATIONS; // already defined
    private static final String CH_WEEKLY_MOOD    = ElevateApp.CH_WEEKLY_MOOD;  // already defined

    private SharedPreferences prefs;

    private LinearLayout optionsContainer;
    private Switch swMaster, swMsgs, swAffirm, swTasks, swCalendar, swMood;

    public NotificationsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS, 0);

        ImageButton back = v.findViewById(R.id.btn_back);
        optionsContainer = v.findViewById(R.id.optionsContainer);

        swMaster   = v.findViewById(R.id.switch_push);
        swMsgs     = v.findViewById(R.id.switch_messages);
        swAffirm   = v.findViewById(R.id.switch_affirmations);
        swTasks    = v.findViewById(R.id.switch_tasks);
        swCalendar = v.findViewById(R.id.switch_calendar);
        swMood     = v.findViewById(R.id.switch_mood);

        back.setOnClickListener(x ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_notificationsFragment_to_settingsFragment)
        );

        // restore state
        boolean master = prefs.getBoolean(KEY_PUSH_MASTER, false);
        swMaster.setChecked(master);
        optionsContainer.setVisibility(master ? View.VISIBLE : View.GONE);

        swMsgs.setChecked(prefs.getBoolean(KEY_MSGS, false));
        swAffirm.setChecked(prefs.getBoolean(KEY_AFFIRMATIONS, false));
        swTasks.setChecked(prefs.getBoolean(KEY_TASKS, false));
        swCalendar.setChecked(prefs.getBoolean(KEY_CALENDAR, false));
        swMood.setChecked(prefs.getBoolean(KEY_MOOD, false));

        // listeners
        swMaster.setOnCheckedChangeListener((b, enabled) -> {
            if (enabled) {
                if (ensurePostNotifPermission()) {
                    optionsContainer.setVisibility(View.VISIBLE);
                    prefs.edit().putBoolean(KEY_PUSH_MASTER, true).apply();
                } else {
                    // permission requested; we'll flip back off for now
                    swMaster.setChecked(false);
                }
            } else {
                optionsContainer.setVisibility(View.GONE);
                prefs.edit().putBoolean(KEY_PUSH_MASTER, false).apply();
                // also cancel all works and turn child switches off
                setChildSwitches(false);
                cancelAllWorks();
            }
        });

        swMsgs.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(KEY_MSGS, on).apply();
            if (on) scheduleDaily(WTAG_PLANT_MESSAGES, 12, 0,
                    "A note from your plant",
                    "Psst 🌿 remember to check in on your tasks!",
                    CH_PLANT_MESSAGES, 3001);
            else cancel(WTAG_PLANT_MESSAGES);
        });

        swAffirm.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(KEY_AFFIRMATIONS, on).apply();
            if (on) scheduleDaily(WTAG_AFFIRMATIONS, 7, 30,
                    "Daily Affirmation",
                    "You’ve got this. One step at a time 🌱",
                    CH_AFFIRMATIONS, 2003);
            else cancel(WTAG_AFFIRMATIONS);
        });

        swTasks.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(KEY_TASKS, on).apply();
            if (on) scheduleDaily(WTAG_TASKS, 18, 0,
                    "Task Reminder",
                    "Quick sweep: any tasks to wrap up today?",
                    CH_TASKS, 3002);
            else cancel(WTAG_TASKS);
        });

        swCalendar.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(KEY_CALENDAR, on).apply();
            if (on) scheduleDaily(WTAG_CALENDAR, 8, 0,
                    "Today's Calendar",
                    "Review your events for today 🌤️",
                    CH_CALENDAR, 3003);
            else cancel(WTAG_CALENDAR);
        });

        swMood.setOnCheckedChangeListener((b, on) -> {
            prefs.edit().putBoolean(KEY_MOOD, on).apply();
            if (on) scheduleWeekly(WTAG_MOOD_WEEKLY, Calendar.MONDAY, 9, 0,
                    "Weekly Mood Report",
                    "Your mood report is ready to review.",
                    CH_WEEKLY_MOOD, 2002);
            else cancel(WTAG_MOOD_WEEKLY);
        });
    }

    // ---- Permission (Android 13+) ----
    private boolean ensurePostNotifPermission() {
        if (Build.VERSION.SDK_INT < 33) return true;
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1010);
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 1010) {
            boolean granted = res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                swMaster.setChecked(true); // re-enable
            }
        }
    }

    // ---- Work scheduling helpers ----
    private void scheduleDaily(String uniqueName, int hour24, int min,
                               String title, String text, String channelId, int notifId) {
        long delay = nextDelayMillis(hour24, min, -1);
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(SimpleReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putString(SimpleReminderWorker.KEY_TITLE, title)
                        .putString(SimpleReminderWorker.KEY_TEXT, text)
                        .putString(SimpleReminderWorker.KEY_CHANNEL, channelId)
                        .putInt(SimpleReminderWorker.KEY_NOTIF_ID, notifId)
                        .build())
                .build();
        WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.UPDATE, work);
    }

    private void scheduleWeekly(String uniqueName, int dayOfWeek, int hour24, int min,
                                String title, String text, String channelId, int notifId) {
        long delay = nextDelayMillis(hour24, min, dayOfWeek);
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(SimpleReminderWorker.class, 7, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(new Data.Builder()
                        .putString(SimpleReminderWorker.KEY_TITLE, title)
                        .putString(SimpleReminderWorker.KEY_TEXT, text)
                        .putString(SimpleReminderWorker.KEY_CHANNEL, channelId)
                        .putInt(SimpleReminderWorker.KEY_NOTIF_ID, notifId)
                        .build())
                .build();
        WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.UPDATE, work);
    }

    private void cancel(String uniqueName) {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(uniqueName);
    }

    private void cancelAllWorks() {
        cancel(WTAG_PLANT_MESSAGES);
        cancel(WTAG_AFFIRMATIONS);
        cancel(WTAG_TASKS);
        cancel(WTAG_CALENDAR);
        cancel(WTAG_MOOD_WEEKLY);
    }

    private void setChildSwitches(boolean on) {
        swMsgs.setChecked(on);
        swAffirm.setChecked(on);
        swTasks.setChecked(on);
        swCalendar.setChecked(on);
        swMood.setChecked(on);
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
}
