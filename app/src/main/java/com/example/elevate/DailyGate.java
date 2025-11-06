package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

public final class DailyGate {
    private static final String PREFS = "DailyGates";

    private DailyGate() {}

    private static String todayKey() {
        Calendar c = Calendar.getInstance(); // device local time
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));
    }

    /** Returns true if the given gate is already marked done today. */
    public static boolean isDoneToday(Context ctx, String gateKey) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String stored = p.getString(gateKey, null);
        return todayKey().equals(stored);
    }

    /** Mark the given gate as done today. Call this when the user finishes the assessment. */
    public static void markDoneToday(Context ctx, String gateKey) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(gateKey, todayKey()).apply();
    }

    /** Clear the stored state for a specific gate (so isDoneToday() will return false). */
    public static void clearKey(Context ctx, String gateKey) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().remove(gateKey).apply();
    }

    /** (Optional) Clear all gates. */
    public static void clearAll(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().clear().apply();
    }
}
