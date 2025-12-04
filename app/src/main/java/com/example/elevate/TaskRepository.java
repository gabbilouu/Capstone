package com.example.elevate;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class TaskRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference col() { return db.collection("tasks"); }

    /** Listen to current user's tasks, newest first. */
    public ListenerRegistration listenForUserTasks(EventListener<QuerySnapshot> listener) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return null;
        return col()
                .whereEqualTo("userId", uid)
                .orderBy("lastModified", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
        // If Firestore asks for an index, follow its console link once.
    }

    /** Create a new task document. */
    public void create(@NonNull Task t) {
        String uid = FirebaseAuth.getInstance().getUid();
        Map<String,Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("name", t.getName());
        data.put("emoji", t.getEmoji());
        data.put("repeatType", t.getRepeatType());
        data.put("repeatDays", t.getRepeatDays());
        data.put("startDate", t.getStartDate());
        data.put("startTime", t.getStartTime());
        data.put("endDate", t.getEndDate());
        data.put("endTime", t.getEndTime());
        data.put("taskType", t.getTaskType());
        data.put("notes", t.getNotes());
        data.put("completionKey", null); // completion status lives here
        data.put("lastModified", FieldValue.serverTimestamp());
        col().add(data);
    }

    /** Toggle completion for the current period. */
    public void setCompleted(String docId, Task task, boolean completed) {
        String key = PeriodKeyUtil.currentKeyFor(task);
        Map<String,Object> upd = new HashMap<>();
        upd.put("completionKey", completed ? key : null);
        upd.put("lastModified", FieldValue.serverTimestamp());
        col().document(docId).update(upd);
    }

    /** Generic update map. */
    public void update(String docId, Map<String,Object> updates) {
        updates.put("lastModified", FieldValue.serverTimestamp());
        col().document(docId).update(updates);
    }

    public void delete(String docId) {
        col().document(docId).delete();
    }
}
