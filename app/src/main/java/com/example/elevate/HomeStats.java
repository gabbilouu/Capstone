package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class HomeStats {

    private HomeStats() {}

    // MUST match keys used by HomePageFragment
    private static final String PREFS = "home_state";

    private static final String KEY_POINTS     = "points_total";
    private static final String KEY_HYDRATION  = "hydration";
    private static final String KEY_TODAY_SNAPSHOT_DATE  = "today_snapshot_date";
    private static final String KEY_TODAY_COMPLETED_SNAP = "today_completed_snapshot";

    // keep in sync with HomePageFragment
    private static final int HYDRATION_GAIN_PER_TASK = 12;

    private static final SimpleDateFormat YMD = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String today() {
        return YMD.format(new Date());
    }

    /** Call when a task changes from incomplete -> complete (today). */
    public static void applyNewCompletion(Context c) {
        SharedPreferences p = prefs(c);
        String t = today();
        String snapDate = p.getString(KEY_TODAY_SNAPSHOT_DATE, null);
        if (!TextUtils.equals(snapDate, t)) {
            p.edit()
                    .putString(KEY_TODAY_SNAPSHOT_DATE, t)
                    .putInt(KEY_TODAY_COMPLETED_SNAP, 0)
                    .apply();
        }

        int points = p.getInt(KEY_POINTS, 0) + 1;
        int hydration = p.getInt(KEY_HYDRATION, 80) + HYDRATION_GAIN_PER_TASK;

        // bump today's snapshot (so we don't double-count later)
        int snap = p.getInt(KEY_TODAY_COMPLETED_SNAP, 0) + 1;

        p.edit()
                .putInt(KEY_POINTS, Math.max(0, points))
                .putInt(KEY_HYDRATION, clamp0_100(hydration))
                .putString(KEY_TODAY_SNAPSHOT_DATE, t)
                .putInt(KEY_TODAY_COMPLETED_SNAP, Math.max(0, snap))
                .apply();
    }

    /** Optional: call if a task changes from complete -> incomplete (same day). */
    public static void revertCompletion(Context c) {
        SharedPreferences p = prefs(c);
        String t = today();
        String snapDate = p.getString(KEY_TODAY_SNAPSHOT_DATE, null);
        if (!TextUtils.equals(snapDate, t)) {
            // different day; nothing to revert safely
            return;
        }

        int points = p.getInt(KEY_POINTS, 0) - 1;
        int hydration = p.getInt(KEY_HYDRATION, 80) - HYDRATION_GAIN_PER_TASK;
        int snap = p.getInt(KEY_TODAY_COMPLETED_SNAP, 0) - 1;

        p.edit()
                .putInt(KEY_POINTS, Math.max(0, points))
                .putInt(KEY_HYDRATION, clamp0_100(hydration))
                .putInt(KEY_TODAY_COMPLETED_SNAP, Math.max(0, snap))
                .apply();
    }

    private static int clamp0_100(int v) { return Math.max(0, Math.min(100, v)); }
}
