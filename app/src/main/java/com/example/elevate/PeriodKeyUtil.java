package com.example.elevate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class PeriodKeyUtil {
    private PeriodKeyUtil() {}

    public static String dailyKey(Calendar cal) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    public static String weeklyKey(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        c.setFirstDayOfWeek(Calendar.MONDAY);
        int week = c.get(Calendar.WEEK_OF_YEAR);
        int year = c.get(Calendar.YEAR);
        return String.format(Locale.US, "%04d-W%02d", year, week);
    }

    public static String monthlyKey(Calendar cal) {
        int y = cal.get(Calendar.YEAR), m = cal.get(Calendar.MONTH) + 1;
        return String.format(Locale.US, "%04d-%02d", y, m);
    }

    /** The key that defines "completion for the current period" for this task. */
    public static String currentKeyFor(Task task) {
        Calendar now = Calendar.getInstance();
        String type = task.getRepeatType();
        if (type == null || type.equals("Daily")) return dailyKey(now);
        if (type.equals("Weekly")) return weeklyKey(now);
        if (type.equals("Monthly")) return monthlyKey(now);
        if (type.equals("Select Days")) {
            String[] wk = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
            String today = wk[now.get(Calendar.DAY_OF_WEEK)-1];
            return (task.getRepeatDays()!=null && task.getRepeatDays().contains(today))
                    ? dailyKey(now) : "__inactive__";
        }
        return dailyKey(now);
    }
}
