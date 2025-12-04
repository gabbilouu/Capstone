package com.example.elevate;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class ElevateApp extends Application {

    // Existing channels
    public static final String CH_DAILY_MOOD    = "ch_daily_mood";
    public static final String CH_WEEKLY_MOOD   = "ch_weekly_mood";
    public static final String CH_AFFIRMATIONS  = "ch_affirmations";

    // NEW channels used by NotificationsFragment
    public static final String CH_PLANT_MESSAGES = "ch_plant_messages";
    public static final String CH_TASKS          = "ch_tasks";
    public static final String CH_CALENDAR       = "ch_calendar";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm == null) return;

            // Existing
            createChannel(nm, CH_DAILY_MOOD,   "Daily Mood",        "Daily mood check-in reminders.");
            createChannel(nm, CH_WEEKLY_MOOD,  "Weekly Mood",       "Your weekly mood report.");
            createChannel(nm, CH_AFFIRMATIONS, "Affirmations",      "Daily positive affirmations.");

            // New
            createChannel(nm, CH_PLANT_MESSAGES, "Plant Messages",  "Notes and encouragement from your plant.");
            createChannel(nm, CH_TASKS,          "Task Reminders",  "Daily reminders to complete tasks.");
            createChannel(nm, CH_CALENDAR,       "Calendar Alerts", "Daily reminders to review your events.");
        }
    }

    private void createChannel(NotificationManager nm, String id, String name, String description) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription(description);
            nm.createNotificationChannel(ch);
        }
    }
}
