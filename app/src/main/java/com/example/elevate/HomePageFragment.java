package com.example.elevate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomePageFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "HomePageFragment";

    private ListenerRegistration plantNameReg;

    private static final int HYDRATION_GAIN_PER_TASK = 12;
    private static final int MAX_MOOD = 100;
    private static final int MAX_HYDRATION = 100;

    // ===== Local prefs keys =====
    private static final String PREFS = "home_state";
    private static final String KEY_LAST_RESET = "last_reset_ymd";
    private static final String KEY_LAST_LOGIN = "last_login_ymd";
    private static final String KEY_STREAK     = "streak";
    private static final String KEY_MOOD       = "mood";
    private static final String KEY_HYDRATION  = "hydration";
    private static final String KEY_POINTS     = "points_total";
    private static final String KEY_TODAY_SNAPSHOT_DATE   = "today_snapshot_date";
    private static final String KEY_TODAY_COMPLETED_SNAP  = "today_completed_snapshot";
    private static final String KEY_YEST_DUE   = "yesterday_due";
    private static final String KEY_YEST_DONE  = "yesterday_done";
    private static final String KEY_YEST_DATE  = "yesterday_date";

    // ===== Firestore field names =====
    private static final String FS_POINTS     = "pointsTotal";
    private static final String FS_HYDRATION  = "hydration";
    private static final String FS_MOOD       = "mood";
    private static final String FS_STREAK     = "streak";
    private static final String FS_LAST_LOGIN = "lastLoginYMD";
    private static final String FS_LAST_RESET = "lastResetYMD";

    // Plant name persistence
    private static final String KEY_PLANT_NAME = "plant_name";
    private static final String DEFAULT_PLANT_NAME = "Perry";

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private NavController navC;

    private TextView tvName;
    private ProgressBar progressExp;
    private TextView tvStreak;
    private TextView tvPoints;
    private ProgressBar progressMood;
    private ProgressBar progressHydration;
    private ProgressBar progressDailyTasks;

    private TextView tvTasksCount;
    private TextView tvTaskBoardTitle;

    private ImageButton taskButton, calendarButton, settingsButton;

    // Daily log-in check icon
    private ImageView imgDailyLoginCheck;

    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    private final SimpleDateFormat YMD = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final String[] DAYS_ABBR = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};

    public HomePageFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        navC = Navigation.findNavController(v);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (user == null) {
            navC.navigate(R.id.action_homePageFragment_to_LoginPageFragment);
            return;
        }
        db = FirebaseFirestore.getInstance();

        bindViews(v);

        prefs = requireContext().getSharedPreferences(PREFS, 0);
        prefListener = (p, key) -> {
            if (KEY_POINTS.equals(key) || KEY_HYDRATION.equals(key) ||
                    KEY_TODAY_COMPLETED_SNAP.equals(key) || KEY_TODAY_SNAPSHOT_DATE.equals(key)) {
                refreshQuickFromPrefs();
            }
        };

        wireBottomNav();

        // Plant name: quick local default, + realtime listener + tap to rename
        if (tvName != null) {
            String local = prefs.getString(KEY_PLANT_NAME, DEFAULT_PLANT_NAME);
            tvName.setText(TextUtils.isEmpty(local) ? DEFAULT_PLANT_NAME : local);
            tvName.setOnClickListener(x -> promptRenamePlant());
            listenForPlantName();
        }

        // First pull cloud state then do the daily/reset pipeline.
        fetchCloudStateThenApply(() ->
                handleLoginAndDailyReset(this::loadTasksAndRender)
        );
    }

    private void bindViews(View v) {
        tvName = v.findViewById(R.id.tvName);

        progressExp = v.findViewById(R.id.progressExp);
        tvStreak    = v.findViewById(R.id.tvStreak);
        tvPoints    = v.findViewById(R.id.tvPoints);

        progressMood       = v.findViewById(R.id.progressMood);
        progressHydration  = v.findViewById(R.id.progressHydration);
        progressDailyTasks = v.findViewById(R.id.progressDailyTasks);

        tvTaskBoardTitle = v.findViewById(R.id.tvTaskBoardTitle);
        tvTasksCount     = null; // Optional

        taskButton     = v.findViewById(R.id.TaskButton);
        calendarButton = v.findViewById(R.id.CalendarButton);
        settingsButton = v.findViewById(R.id.SettingsButton);

        imgDailyLoginCheck = v.findViewById(R.id.imgDailyLoginCheck);
    }

    private void wireBottomNav() {
        if (taskButton != null) taskButton.setOnClickListener(this);
        if (calendarButton != null) calendarButton.setOnClickListener(this);
        if (settingsButton != null) settingsButton.setOnClickListener(this);
    }

    /* =========================
       Cloud state (read & write)
       ========================= */

    private DocumentReference userDoc() {
        return db.collection("users").document(user.getUid());
    }

    /** Pulls cloud values, writes them into local prefs (if present), then invokes next. */
    private void fetchCloudStateThenApply(Runnable next) {
        userDoc().get().addOnSuccessListener(snap -> {
            if (snap != null && snap.exists()) {
                SharedPreferences.Editor ed = prefs.edit();

                Integer points    = safeInt(snap.getLong(FS_POINTS));
                Integer hydration = safeInt(snap.getLong(FS_HYDRATION));
                Integer mood      = safeInt(snap.getLong(FS_MOOD));
                Integer streak    = safeInt(snap.getLong(FS_STREAK));
                String  lastLogin = snap.getString(FS_LAST_LOGIN);
                String  lastReset = snap.getString(FS_LAST_RESET);

                if (points != null)    ed.putInt(KEY_POINTS, points);
                if (hydration != null) ed.putInt(KEY_HYDRATION, clamp0_100(hydration));
                if (mood != null)      ed.putInt(KEY_MOOD, clamp0_100(mood));
                if (streak != null)    ed.putInt(KEY_STREAK, Math.max(0, streak));
                if (!TextUtils.isEmpty(lastLogin)) ed.putString(KEY_LAST_LOGIN, lastLogin);
                if (!TextUtils.isEmpty(lastReset)) ed.putString(KEY_LAST_RESET, lastReset);

                ed.apply();
            }
        }).addOnCompleteListener(t -> {
            if (next != null) next.run();
        });
    }

    /** Writes selected fields to the user's document (merge). */
    private void saveCloudState(Integer points, Integer hydration, Integer mood,
                                Integer streak, String lastLoginYMD, String lastResetYMD) {
        Map<String, Object> data = new HashMap<>();
        if (points != null)       data.put(FS_POINTS, points);
        if (hydration != null)    data.put(FS_HYDRATION, clamp0_100(hydration));
        if (mood != null)         data.put(FS_MOOD, clamp0_100(mood));
        if (streak != null)       data.put(FS_STREAK, Math.max(0, streak));
        if (lastLoginYMD != null) data.put(FS_LAST_LOGIN, lastLoginYMD);
        if (lastResetYMD != null) data.put(FS_LAST_RESET, lastResetYMD);
        if (data.isEmpty()) return;

        userDoc().set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.w(TAG, "saveCloudState failed", e));
    }

    private Integer safeInt(Long v) {
        return v == null ? null : (int) (long) v;
    }

    /* =========================
       Plant name: Firestore I/O
       ========================= */

    private void listenForPlantName() {
        if (plantNameReg != null) {
            plantNameReg.remove();
            plantNameReg = null;
        }

        plantNameReg = userDoc().addSnapshotListener(
                (DocumentSnapshot snap, FirebaseFirestoreException e) -> {
                    if (e != null) {
                        Log.w(TAG, "plantName listener error", e);
                        return;
                    }
                    String name = null;
                    if (snap != null && snap.exists()) {
                        name = snap.getString("plantName");
                    }
                    if (TextUtils.isEmpty(name)) name = DEFAULT_PLANT_NAME;

                    if (tvName != null) tvName.setText(name);
                    prefs.edit().putString(KEY_PLANT_NAME, name).apply();
                });
    }

    private void promptRenamePlant() {
        if (getContext() == null || tvName == null) return;

        TextInputLayout til = new TextInputLayout(requireContext());
        til.setHint("Plant name (max 24)");
        final TextInputEditText et = new TextInputEditText(til.getContext());
        et.setText(tvName.getText());
        et.setSelection(et.getText() != null ? et.getText().length() : 0);
        et.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(24) });
        til.addView(et);

        final androidx.appcompat.app.AlertDialog dlg =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Rename plant")
                        .setView(til)
                        .setPositiveButton("Save", null)
                        .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                        .create();

        dlg.setOnShowListener(d -> {
            dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(btn -> {
                        String newName = et.getText() == null ? "" : et.getText().toString().trim();
                        if (TextUtils.isEmpty(newName)) {
                            til.setError("Name is required");
                            return;
                        }
                        if (!newName.matches("^[\\p{L}0-9 ._'-]{1,24}$")) {
                            til.setError("Use letters/numbers/spaces ._'- (max 24)");
                            return;
                        }
                        til.setError(null);
                        Map<String, Object> data = new HashMap<>();
                        data.put("plantName", newName);
                        userDoc().set(data, SetOptions.merge())
                                .addOnSuccessListener(unused -> {
                                    if (tvName != null) tvName.setText(newName);
                                    prefs.edit().putString(KEY_PLANT_NAME, newName).apply();
                                    Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
                                    dlg.dismiss();
                                })
                                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                        "Couldn't save. Check connection.", Toast.LENGTH_LONG).show());
                    });
        });

        dlg.show();
    }

    /* ==============================
       Daily reset + dashboard render
       ============================== */

    private void handleLoginAndDailyReset(Runnable done) {
        String today = YMD.format(new Date());
        String lastLogin = getPrefs().getString(KEY_LAST_LOGIN, null);
        String lastReset = getPrefs().getString(KEY_LAST_RESET, null);

        int streak = getPrefs().getInt(KEY_STREAK, 0);
        boolean loginAdvanced = false;

        if (!TextUtils.equals(lastLogin, today)) {
            if (isYesterday(lastLogin)) streak += 1;
            else streak = 1;
            getPrefs().edit().putInt(KEY_STREAK, streak).putString(KEY_LAST_LOGIN, today).apply();
            loginAdvanced = true;
        }
        if (tvStreak != null) tvStreak.setText(String.valueOf(streak));
        updateDailyLoginCheck();

        // Make final copies for lambdas
        final int streakFinal = streak;
        final String todayFinal = today;

        // If we already reset today, repaint UI and (if login changed) sync streak/lastLogin.
        if (TextUtils.equals(lastReset, today)) {
            if (progressMood != null) progressMood.setProgress(getPrefs().getInt(KEY_MOOD, 80));
            if (progressHydration != null) progressHydration.setProgress(getPrefs().getInt(KEY_HYDRATION, 80));

            if (loginAdvanced) {
                saveCloudState(
                        null,
                        getPrefs().getInt(KEY_HYDRATION, 80),
                        getPrefs().getInt(KEY_MOOD, 80),
                        streakFinal,
                        todayFinal,
                        todayFinal
                );
            }
            done.run();
            return;
        }

        // Compute yesterday penalties using tasks
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = YMD.format(cal.getTime());

        Query tasksQ = db.collection("tasks").whereEqualTo("userId", user.getUid());
        tasksQ.get().addOnSuccessListener(snap -> {
            int dueYesterday = 0;
            int doneYesterday = 0;
            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    Task t = ds.toObject(Task.class);
                    if (t == null) continue;
                    if (isTaskDueOn(t, cal)) {
                        dueYesterday++;
                        if (Boolean.TRUE.equals(t.isCompleted())) doneYesterday++;
                    }
                }
            }

            getPrefs().edit()
                    .putString(KEY_YEST_DATE, yesterday)
                    .putInt(KEY_YEST_DUE, dueYesterday)
                    .putInt(KEY_YEST_DONE, doneYesterday)
                    .apply();

            int mood = clamp0_100(getPrefs().getInt(KEY_MOOD, 80));
            int hydration = clamp0_100(getPrefs().getInt(KEY_HYDRATION, 80));

            // Overnight decay rules
            hydration = Math.max(0, hydration / 2);

            boolean missedLoginYesterday = !isYesterday(lastLogin);
            if (missedLoginYesterday) mood = Math.max(0, mood - 1);
            boolean missedAnyTasksYesterday = (dueYesterday > 0 && doneYesterday < dueYesterday);
            if (missedAnyTasksYesterday) mood = Math.max(0, mood - 1);

            getPrefs().edit()
                    .putInt(KEY_MOOD, mood)
                    .putInt(KEY_HYDRATION, hydration)
                    .putString(KEY_LAST_RESET, todayFinal)
                    .putString(KEY_TODAY_SNAPSHOT_DATE, todayFinal)
                    .putInt(KEY_TODAY_COMPLETED_SNAP, 0)
                    .apply();

            // UI
            if (progressMood != null) progressMood.setProgress(mood);
            if (progressHydration != null) progressHydration.setProgress(hydration);
            updateDailyLoginCheck();

            // Sync cloud state after penalties / date updates
            saveCloudState(
                    getPrefs().getInt(KEY_POINTS, 0),
                    hydration,
                    mood,
                    streakFinal,
                    todayFinal,
                    todayFinal
            );

            done.run();
        }).addOnFailureListener(e -> {
            Log.w(TAG, "Failed to compute yesterday penalties; minimal reset", e);
            int mood = Math.max(0, getPrefs().getInt(KEY_MOOD, 80) - 1);
            int hydration = Math.max(0, getPrefs().getInt(KEY_HYDRATION, 80) / 2);

            getPrefs().edit()
                    .putInt(KEY_MOOD, mood)
                    .putInt(KEY_HYDRATION, hydration)
                    .putString(KEY_LAST_RESET, todayFinal)
                    .putString(KEY_TODAY_SNAPSHOT_DATE, todayFinal)
                    .putInt(KEY_TODAY_COMPLETED_SNAP, 0)
                    .apply();

            if (progressMood != null) progressMood.setProgress(mood);
            if (progressHydration != null) progressHydration.setProgress(hydration);
            updateDailyLoginCheck();

            // Sync anyway
            saveCloudState(
                    getPrefs().getInt(KEY_POINTS, 0),
                    hydration,
                    mood,
                    streakFinal,
                    todayFinal,
                    todayFinal
            );

            done.run();
        });
    }

    private void loadTasksAndRender() {
        String today = YMD.format(new Date());
        Calendar todayCal = Calendar.getInstance();

        Query tasksQ = db.collection("tasks")
                .whereEqualTo("userId", user.getUid());

        tasksQ.get().addOnSuccessListener(snap -> {
            int dueToday = 0;
            int completedToday = 0;

            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    Task t = ds.toObject(Task.class);
                    if (t == null) continue;

                    if (isTaskDueOn(t, todayCal)) {
                        dueToday++;
                        if (Boolean.TRUE.equals(t.isCompleted())) completedToday++;
                    }
                }
            }

            // Daily task board
            if (progressDailyTasks != null) {
                progressDailyTasks.setMax(Math.max(1, dueToday));
                progressDailyTasks.setProgress(completedToday);
            }
            if (tvTasksCount != null) {
                tvTasksCount.setText(completedToday + "/" + Math.max(1, dueToday));
            } else if (tvTaskBoardTitle != null) {
                tvTaskBoardTitle.setText("Daily Task Board (" + completedToday + "/" + Math.max(1, dueToday) + ")");
            }

            // Points & EXP (delta from previous snapshot)
            int points = getPrefs().getInt(KEY_POINTS, 0);
            String snapDate = getPrefs().getString(KEY_TODAY_SNAPSHOT_DATE, null);
            int prevCompletedSnap = getPrefs().getInt(KEY_TODAY_COMPLETED_SNAP, 0);
            if (!TextUtils.equals(snapDate, today)) {
                prevCompletedSnap = 0;
            }
            int deltaNewCompletions = Math.max(0, completedToday - prevCompletedSnap);
            boolean pointsChanged = false;
            boolean hydrationChanged = false;

            if (deltaNewCompletions > 0) {
                points += deltaNewCompletions;
                getPrefs().edit()
                        .putInt(KEY_POINTS, points)
                        .putString(KEY_TODAY_SNAPSHOT_DATE, today)
                        .putInt(KEY_TODAY_COMPLETED_SNAP, completedToday)
                        .apply();
                pointsChanged = true;

                int hydration = clamp0_100(getPrefs().getInt(KEY_HYDRATION, 80)
                        + deltaNewCompletions * HYDRATION_GAIN_PER_TASK);
                getPrefs().edit().putInt(KEY_HYDRATION, hydration).apply();
                hydrationChanged = true;

                if (progressHydration != null) progressHydration.setProgress(hydration);
            } else {
                if (!TextUtils.equals(snapDate, today)) {
                    getPrefs().edit()
                            .putString(KEY_TODAY_SNAPSHOT_DATE, today)
                            .putInt(KEY_TODAY_COMPLETED_SNAP, completedToday)
                            .apply();
                }
                if (progressHydration != null) progressHydration.setProgress(getPrefs().getInt(KEY_HYDRATION, 80));
            }

            if (tvPoints != null) tvPoints.setText(String.valueOf(points));

            LevelState ls = deriveLevelFromPoints(points);
            if (progressExp != null) {
                progressExp.setMax(ls.threshold);
                progressExp.setProgress(ls.expInLevel);
            }

            if (progressMood != null) progressMood.setProgress(getPrefs().getInt(KEY_MOOD, 80));
            if (tvStreak != null) tvStreak.setText(String.valueOf(getPrefs().getInt(KEY_STREAK, 0)));

            // Sync cloud if points or hydration changed (mood/streak handled during reset/login)
            if (pointsChanged || hydrationChanged) {
                saveCloudState(
                        getPrefs().getInt(KEY_POINTS, 0),
                        getPrefs().getInt(KEY_HYDRATION, 80),
                        null, // mood unchanged here
                        null, // streak unchanged here
                        null,
                        null
                );
            }

        }).addOnFailureListener(e -> Log.e(TAG, "loadTasksAndRender failed", e));
    }

    /* ==================
       Helpers & calcs
       ================== */

    private static class LevelState {
        int level;
        int expInLevel;
        int threshold;
    }

    private LevelState deriveLevelFromPoints(int points) {
        int n = 0;
        while (true) {
            int need = 5 * ((n + 1) * (n + 2)) / 2;
            if (points < need) break;
            n++;
        }
        int completedForN = 5 * (n * (n + 1)) / 2;
        LevelState ls = new LevelState();
        ls.level = n;
        ls.expInLevel = points - completedForN;
        ls.threshold = 5 * (n + 1);
        return ls;
    }

    private boolean isYesterday(String ymdStr) {
        if (TextUtils.isEmpty(ymdStr)) return false;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return YMD.format(cal.getTime()).equals(ymdStr);
    }

    private boolean isTodayVal(String ymdStr) {
        if (TextUtils.isEmpty(ymdStr)) return false;
        return YMD.format(new Date()).equals(ymdStr);
    }

    private int clamp0_100(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private SharedPreferences getPrefs() {
        return requireContext().getSharedPreferences(PREFS, 0);
    }

    private void refreshQuickFromPrefs() {
        if (tvPoints != null) {
            int points = prefs.getInt(KEY_POINTS, 0);
            tvPoints.setText(String.valueOf(points));
            LevelState ls = deriveLevelFromPoints(points);
            if (progressExp != null) {
                progressExp.setMax(ls.threshold);
                progressExp.setProgress(ls.expInLevel);
            }
        }
        if (progressHydration != null) {
            progressHydration.setProgress(prefs.getInt(KEY_HYDRATION, 80));
        }
    }

    // === Daily login row helpers ===
    private void updateDailyLoginCheck() {
        String lastLogin = getPrefs().getString(KEY_LAST_LOGIN, null);
        boolean doneToday = isTodayVal(lastLogin);
        setCheck(imgDailyLoginCheck, doneToday);
    }

    private void setCheck(ImageView view, boolean checked) {
        if (view == null) return;
        view.setImageResource(checked
                ? android.R.drawable.checkbox_on_background
                : android.R.drawable.checkbox_off_background);
        view.setContentDescription(checked ? "Completed" : "Incomplete");
        view.setAlpha(checked ? 1f : 0.6f);
    }

    /* ===========
       Clicks
       =========== */
    @Override
    public void onClick(View v) {
        if (navC == null) return;
        int id = v.getId();
        if (id == R.id.TaskButton) {
            navC.navigate(R.id.action_homePageFragment_to_taskListFragment);
        } else if (id == R.id.CalendarButton) {
            navC.navigate(R.id.action_homePageFragment_to_eventFragment);
        } else if (id == R.id.SettingsButton) {
            navC.navigate(R.id.action_homePageFragment_to_settingsFragment);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (prefs != null && prefListener != null) {
            prefs.registerOnSharedPreferenceChangeListener(prefListener);
        }
        refreshQuickFromPrefs();
        updateDailyLoginCheck();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (prefs != null && prefListener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (plantNameReg != null) {
            plantNameReg.remove();
            plantNameReg = null;
        }
    }

    /* =========================
       Task schedule calculation
       ========================= */
    private boolean isTaskDueOn(Task t, Calendar day) {
        String type = t.getRepeatType();
        if (type == null) return false;

        switch (type) {
            case "Daily":
                return true;
            case "Weekly":
                return day.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
            case "Monthly":
                return day.get(Calendar.DAY_OF_MONTH) == 1;
            case "Select Days":
                List<String> days = t.getRepeatDays();
                if (days == null || days.isEmpty()) return false;
                String abbr = DAYS_ABBR[day.get(Calendar.DAY_OF_WEEK) - 1];
                return days.contains(abbr);
            default:
                return false;
        }
    }
}
