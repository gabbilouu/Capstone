package com.example.elevate;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TaskResetManager {

    private static final String TAG = "TaskResetManager";

    public static void scheduleDailyReset(Context context) {
        Calendar now = Calendar.getInstance();
        Calendar nextMidnight = Calendar.getInstance();
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 0);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);

        if (now.after(nextMidnight)) nextMidnight.add(Calendar.DAY_OF_MONTH, 1);

        long delay = nextMidnight.getTimeInMillis() - now.getTimeInMillis();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                resetTasks();
                scheduleDailyReset(context); // reschedule for next day
            }
        }, delay);
    }

    private static void resetTasks() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Calendar today = Calendar.getInstance();
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK); // Sunday=1, Saturday=7
        int dayOfMonth = today.get(Calendar.DAY_OF_MONTH);

        db.collection("tasks").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {

                    String repeatType = doc.getString("repeatType");

                    // Safely handle repeatDays
                    Object repeatDaysObj = doc.get("repeatDays");
                    List<String> repeatDays = new ArrayList<>();
                    if (repeatDaysObj instanceof List<?>) {
                        for (Object o : (List<?>) repeatDaysObj) {
                            if (o instanceof String) repeatDays.add((String) o);
                        }
                    }

                    boolean shouldReset = false;

                    if ("Daily".equals(repeatType)) {
                        shouldReset = true;
                    } else if ("Weekly".equals(repeatType)) {
                        shouldReset = (dayOfWeek == Calendar.MONDAY);
                    } else if ("Monthly".equals(repeatType)) {
                        Calendar tomorrow = Calendar.getInstance();
                        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                        shouldReset = (tomorrow.get(Calendar.DAY_OF_MONTH) == 1);
                    } else if ("Select Days".equals(repeatType) && !repeatDays.isEmpty()) {
                        String yesterday = getDayAbbreviation(dayOfWeek - 1);
                        shouldReset = repeatDays.contains(yesterday);
                    }

                    if (shouldReset) {
                        db.collection("tasks").document(doc.getId()).update("completed", false)
                                .addOnSuccessListener(a -> Log.d(TAG, "Reset: " + doc.getId()))
                                .addOnFailureListener(e -> Log.w(TAG, "Reset failed: " + doc.getId(), e));
                    }
                }
            } else {
                Log.w(TAG, "Failed to fetch tasks for reset", task.getException());
            }
        });
    }

    @NonNull
    private static String getDayAbbreviation(int dayOfWeek) {
        if (dayOfWeek < Calendar.SUNDAY) dayOfWeek = Calendar.SATURDAY; // wrap-around for Sunday
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Mon";
            case Calendar.TUESDAY: return "Tue";
            case Calendar.WEDNESDAY: return "Wed";
            case Calendar.THURSDAY: return "Thu";
            case Calendar.FRIDAY: return "Fri";
            case Calendar.SATURDAY: return "Sat";
            default: return "Sun";
        }
    }
}
