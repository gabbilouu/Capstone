package com.example.elevate;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SimpleReminderWorker extends Worker {

    public static final String KEY_TITLE = "title";
    public static final String KEY_TEXT = "text";
    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_NOTIF_ID = "notif_id";

    public SimpleReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = getInputData().getString(KEY_TITLE);
        String text = getInputData().getString(KEY_TEXT);
        String channel = getInputData().getString(KEY_CHANNEL);
        int id = getInputData().getInt(KEY_NOTIF_ID, 1000);
        NotificationUtils.show(getApplicationContext(), channel, id, title, text);
        return Result.success();
    }
}
