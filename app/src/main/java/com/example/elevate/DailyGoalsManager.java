package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class DailyGoalsManager {

    private static final String PREFS = "DailyGoalsPrefs";

    private static final String KEY_DATE = "date"; // yyyyMMdd

    private static final String KEY_TASKS_COMPLETED_COUNT = "tasksCompletedCount";
    private static final String KEY_COMPLETED_TASK_IDS = "completedTaskIds";

    private static final String KEY_FEATURED_DONE = "featuredDone";
    private static final String KEY_CALENDAR_DONE = "calendarDone";
    private static final String KEY_PLANT_DONE = "plantDone";
    private static final String KEY_LOGIN_DONE = "loginDone";

    private static final String KEY_POINTS = "points";
    private static final String KEY_DAILY_REWARD_GRANTED = "dailyRewardGranted";

    private static final String KEY_HYDRATION = "hydration"; // 0..100

    private static final int DAILY_REWARD_POINTS = 20;
    private static final int HYDRATION_PER_TASK = 10; // tweak as you like

    private DailyGoalsManager() {}

    /* ===== core prefs ===== */

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String todayKey() {
        Calendar c = Calendar.getInstance();
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(c.getTime());
    }

    private static void ensureToday(Context ctx) {
        SharedPreferences p = prefs(ctx);
        String storedDate = p.getString(KEY_DATE, null);
        String today = todayKey();
        if (today.equals(storedDate)) return;

        // New day: reset all daily stuff, keep total points
        int points = p.getInt(KEY_POINTS, 0);

        p.edit()
                .clear()
                .putString(KEY_DATE, today)
                .putInt(KEY_POINTS, points)
                .apply();
    }

    /* ===== events ===== */

    public static void onAppOpenedHome(Context ctx) {
        ensureToday(ctx);
        SharedPreferences p = prefs(ctx);
        if (!p.getBoolean(KEY_LOGIN_DONE, false)) {
            p.edit().putBoolean(KEY_LOGIN_DONE, true).apply();
            maybeGrantDailyReward(ctx);
        }
    }

    public static void onTaskCompleted(Context ctx, String taskId, boolean isFeatured) {
        ensureToday(ctx);
        SharedPreferences p = prefs(ctx);

        // Avoid double-counting same task in one day
        Set<String> ids = p.getStringSet(KEY_COMPLETED_TASK_IDS, null);
        if (ids == null) ids = new HashSet<>();
        if (!ids.contains(taskId)) {
            ids.add(taskId);

            int currentCount = p.getInt(KEY_TASKS_COMPLETED_COUNT, 0);
            currentCount++;

            int hydration = p.getInt(KEY_HYDRATION, 0);
            hydration = Math.min(100, hydration + HYDRATION_PER_TASK);

            SharedPreferences.Editor ed = p.edit();
            ed.putStringSet(KEY_COMPLETED_TASK_IDS, ids);
            ed.putInt(KEY_TASKS_COMPLETED_COUNT, currentCount);
            ed.putInt(KEY_HYDRATION, hydration);

            if (isFeatured && !p.getBoolean(KEY_FEATURED_DONE, false)) {
                ed.putBoolean(KEY_FEATURED_DONE, true);
            }

            ed.apply();
        }

        maybeGrantDailyReward(ctx);
    }

    public static void onCalendarOpened(Context ctx) {
        ensureToday(ctx);
        SharedPreferences p = prefs(ctx);
        if (!p.getBoolean(KEY_CALENDAR_DONE, false)) {
            p.edit().putBoolean(KEY_CALENDAR_DONE, true).apply();
            maybeGrantDailyReward(ctx);
        }
    }

    public static void onPlantTapped(Context ctx) {
        ensureToday(ctx);
        SharedPreferences p = prefs(ctx);
        if (!p.getBoolean(KEY_PLANT_DONE, false)) {
            p.edit().putBoolean(KEY_PLANT_DONE, true).apply();
            maybeGrantDailyReward(ctx);
        }
    }

    /* ===== getters for UI ===== */

    public static int getTasksCompletedCount(Context ctx) {
        ensureToday(ctx);
        return prefs(ctx).getInt(KEY_TASKS_COMPLETED_COUNT, 0);
    }

    public static boolean isGoal5TasksDone(Context ctx) {
        return getTasksCompletedCount(ctx) >= 5;
    }

    public static boolean isFeaturedDone(Context ctx) {
        ensureToday(ctx);
        return prefs(ctx).getBoolean(KEY_FEATURED_DONE, false);
    }

    public static boolean isCalendarDone(Context ctx) {
        ensureToday(ctx);
        return prefs(ctx).getBoolean(KEY_CALENDAR_DONE, false);
    }

    public static boolean isPlantDone(Context ctx) {
        ensureToday(ctx);
        return prefs(ctx).getBoolean(KEY_PLANT_DONE, false);
    }

    public static boolean isLoginDone(Context ctx) {
        ensureToday(ctx);
        return prefs(ctx).getBoolean(KEY_LOGIN_DONE, false);
    }

    public static int getHydrationPercent(Context ctx) {
        ensureToday(ctx);
        return prefs(ctx).getInt(KEY_HYDRATION, 0);
    }

    public static int getPoints(Context ctx) {
        ensureToday(ctx);
        return prefs(ctx).getInt(KEY_POINTS, 0);
    }

    public static int getDailyGoalsProgress(Context ctx) {
        ensureToday(ctx);
        int totalGoals = 5; // login + 5 tasks + featured + calendar + plant
        int completed = 0;

        if (isLoginDone(ctx)) completed++;
        if (isGoal5TasksDone(ctx)) completed++;
        if (isFeaturedDone(ctx)) completed++;
        if (isCalendarDone(ctx)) completed++;
        if (isPlantDone(ctx)) completed++;

        return (int) (completed * 100f / totalGoals);
    }

    public static boolean areAllGoalsCompleted(Context ctx) {
        return isLoginDone(ctx)
                && isGoal5TasksDone(ctx)
                && isFeaturedDone(ctx)
                && isCalendarDone(ctx)
                && isPlantDone(ctx);
    }

    /* ===== reward logic ===== */

    private static void maybeGrantDailyReward(Context ctx) {
        ensureToday(ctx);
        SharedPreferences p = prefs(ctx);

        if (!areAllGoalsCompleted(ctx)) return;
        if (p.getBoolean(KEY_DAILY_REWARD_GRANTED, false)) return;

        int points = p.getInt(KEY_POINTS, 0);
        points += DAILY_REWARD_POINTS;

        p.edit()
                .putInt(KEY_POINTS, points)
                .putBoolean(KEY_DAILY_REWARD_GRANTED, true)
                .apply();
    }
}
