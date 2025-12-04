package com.example.elevate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

public class TaskResetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tasks")
                .get()
                .addOnSuccessListener(query -> {
                    query.getDocuments().forEach(doc ->
                            doc.getReference().update("done", false));
                    Log.d("TaskResetReceiver", "All tasks reset to false.");
                })
                .addOnFailureListener(e ->
                        Log.e("TaskResetReceiver", "Reset failed", e));

        // Reschedule for next midnight
        TaskResetScheduler.scheduleMidnightReset(context);
    }
}
