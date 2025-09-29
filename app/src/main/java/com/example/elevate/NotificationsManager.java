package com.example.elevate;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class NotificationsManager {

    private final Context context;
    private static final String CHANNEL_ID = "elevate_channel";

    public NotificationsManager(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    // Schedule daily notifications
    public void enableDailyNotifications() {
        PeriodicWorkRequest dailyWork =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 24, TimeUnit.HOURS)
                        .addTag("daily_notifications")
                        .build();
        WorkManager.getInstance(context).enqueue(dailyWork);
    }

    // Schedule weekly notifications
    public void enableWeeklyNotifications() {
        PeriodicWorkRequest weeklyWork =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 7, TimeUnit.DAYS)
                        .addTag("weekly_notifications")
                        .build();
        WorkManager.getInstance(context).enqueue(weeklyWork);
    }

    // Cancel all scheduled notifications
    public void disableNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag("daily_notifications");
        WorkManager.getInstance(context).cancelAllWorkByTag("weekly_notifications");
    }

    // Create notification channel (required for Android 8+)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Elevate Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for daily/weekly mood tracking");
            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Worker class to send notifications
    public static class NotificationWorker extends Worker {

        public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            Context context = getApplicationContext();
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Elevate")
                    .setContentText("Check in on your mood today!")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            }

            return Result.success();
        }
    }
}
