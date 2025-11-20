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

import androidx.annotation.DrawableRes;
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

    // Level thresholds for plant evolution:
    // Stage 1: levels 1–5
    // Stage 2: levels 6–15 (5 levels gained from level 1)
    // Stage 3: levels 16+ (another 10 levels)
    private static final int LEVEL_STAGE_2 = 6;
    private static final int LEVEL_STAGE_3 = 16;

    // ===== Local prefs keys =====
    private static final String PREFS = "home_state";
    private static final String KEY_LAST_RESET = "last_reset_ymd";
    private static final String KEY_LAST_LOGIN = "last_login_ymd";
    private static final String KEY_STREAK     = "streak";
    private static final String KEY_MOOD       = "mood";
    private static final String KEY_HYDRATION  = "hydration";

    // Points = store currency
    private static final String KEY_POINTS     = "points_total";

    // XP = used for EXP bar / level (separate from points)
    private static final String KEY_XP         = "xp_total";

    private static final String KEY_TODAY_SNAPSHOT_DATE   = "today_snapshot_date";
    private static final String KEY_TODAY_COMPLETED_SNAP  = "today_completed_snapshot";

    // Yesterday stats (for penalties)
    private static final String KEY_YEST_DUE   = "yesterday_due";
    private static final String KEY_YEST_DONE  = "yesterday_done";
    private static final String KEY_YEST_DATE  = "yesterday_date";

    // Track the date when the daily “all goals done” bonus was last granted
    private static final String KEY_DAILY_BONUS_DATE      = "daily_bonus_date";

    // Daily goals persistence (per-day)
    private static final String KEY_GOAL_OPEN_CAL_DATE    = "goal_open_calendar_date";
    private static final String KEY_GOAL_TAP_PLANT_DATE   = "goal_tap_plant_date";

    // Shop prefs (shared with ShopFragment)
    private static final String SHOP_PREFS   = "shop_prefs";
    private static final String KEY_EQ_PLANT = "eq_plant";
    private static final String KEY_EQ_POT   = "eq_pot";
    private static final String KEY_EQ_ROOM  = "eq_room";

    // ===== Firestore field names =====
    private static final String FS_POINTS     = "pointsTotal";
    private static final String FS_XP         = "xpTotal";   // sync XP
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
    private TextView tvLevel;     // level label
    private ProgressBar progressExp;
    private TextView tvStreak;
    private TextView tvPoints;
    private ProgressBar progressMood;
    private ProgressBar progressHydration;
    private ProgressBar progressDailyTasks;
    private TextView tvTasksCount;
    private TextView tvTaskBoardTitle;

    private ImageButton taskButton, calendarButton, settingsButton;
    private ImageView imgDailyLoginCheck;

    // Goal views
    private TextView tvGoal5Count, tvGoalFeatured, tvGoalCalendar, tvGoalPlant;
    private ImageView ivGoal5Check, ivGoalFeaturedCheck, ivGoalCalendarCheck, ivGoalPlantCheck;

    // Plant scene views
    private ImageView imgCarpet, imgTable, imgPot, imgPlant, imgFace;

    // Store area view
    private View storeArea;

    private SharedPreferences prefs;     // home_state
    private SharedPreferences shopPrefs; // shop_prefs
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
        shopPrefs = requireContext().getSharedPreferences(SHOP_PREFS, 0);

        prefListener = (p, key) -> {
            if (KEY_POINTS.equals(key) || KEY_HYDRATION.equals(key) ||
                    KEY_TODAY_COMPLETED_SNAP.equals(key) || KEY_TODAY_SNAPSHOT_DATE.equals(key) ||
                    KEY_MOOD.equals(key)) {
                refreshQuickFromPrefs();
                applyEquippedCosmetics();
            }
        };

        wireBottomNav();

        // Tap plant -> mark "Tap the plant" goal done
        if (imgPlant != null) {
            imgPlant.setOnClickListener(view1 -> {
                markGoalTapPlantDone();
                // Refresh goals immediately
                loadTasksAndRender();
            });
        }

        // Store area click → Shop
        if (storeArea != null) {
            storeArea.setOnClickListener(x -> {
                if (navC == null) return;
                try {
                    navC.navigate(R.id.action_homePageFragment_to_shopFragment);
                } catch (Exception e) {
                    try { navC.navigate(R.id.shopFragment); } catch (Exception ignored) {}
                }
            });
        }

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
        tvName  = v.findViewById(R.id.tvName);
        tvLevel = v.findViewById(R.id.tvLevel);

        progressExp = v.findViewById(R.id.progressExp);
        tvStreak    = v.findViewById(R.id.tvStreak);
        tvPoints    = v.findViewById(R.id.tvPoints);

        progressMood       = v.findViewById(R.id.progressMood);
        progressHydration  = v.findViewById(R.id.progressHydration);
        progressDailyTasks = v.findViewById(R.id.progressDailyTasks);

        tvTaskBoardTitle = v.findViewById(R.id.tvTaskBoardTitle);
        tvTasksCount     = null; // not used now; title shows "Daily Goals"

        taskButton     = v.findViewById(R.id.TaskButton);
        calendarButton = v.findViewById(R.id.CalendarButton);
        settingsButton = v.findViewById(R.id.SettingsButton);

        imgDailyLoginCheck = v.findViewById(R.id.imgDailyLoginCheck);

        // Goal rows
        tvGoal5Count     = v.findViewById(R.id.tvGoal5Count);
        tvGoalFeatured   = v.findViewById(R.id.tvGoalFeatured);
        tvGoalCalendar   = v.findViewById(R.id.tvGoalCalendar);
        tvGoalPlant      = v.findViewById(R.id.tvGoalPlant);

        ivGoal5Check        = v.findViewById(R.id.ivGoal5Check);
        ivGoalFeaturedCheck = v.findViewById(R.id.ivGoalFeaturedCheck);
        ivGoalCalendarCheck = v.findViewById(R.id.ivGoalCalendarCheck);
        ivGoalPlantCheck    = v.findViewById(R.id.ivGoalPlantCheck);

        // Plant scene
        imgCarpet = v.findViewById(R.id.imgCarpet);
        imgTable  = v.findViewById(R.id.imgTable);
        imgPot    = v.findViewById(R.id.imgPot);
        imgPlant  = v.findViewById(R.id.imgPlant);
        imgFace   = v.findViewById(R.id.imgFace);

        // Store clickable area
        storeArea = v.findViewById(R.id.storeArea);
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

                // XP from Firestore
                Integer xp        = safeInt(snap.getLong(FS_XP));

                if (points != null)    ed.putInt(KEY_POINTS, points);
                if (hydration != null) ed.putInt(KEY_HYDRATION, clamp0_100(hydration));
                if (mood != null)      ed.putInt(KEY_MOOD, clamp0_100(mood));
                if (streak != null)    ed.putInt(KEY_STREAK, Math.max(0, streak));
                if (!TextUtils.isEmpty(lastLogin)) ed.putString(KEY_LAST_LOGIN, lastLogin);
                if (!TextUtils.isEmpty(lastReset)) ed.putString(KEY_LAST_RESET, lastReset);

                // XP: if present, use it; otherwise default to points for old users
                if (xp != null) {
                    ed.putInt(KEY_XP, xp);
                } else if (points != null) {
                    ed.putInt(KEY_XP, points);
                }

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

        // Always sync XP from prefs (fallback to points if XP missing)
        int xpToSave = getPrefs().getInt(KEY_XP,
                getPrefs().getInt(KEY_POINTS, 0));
        data.put(FS_XP, xpToSave);

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

            applyEquippedCosmetics();

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

            applyEquippedCosmetics();

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

            applyEquippedCosmetics();

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

    /**
     * Recomputes "today" state from Firestore tasks:
     * - Counts due-today tasks
     * - Counts completed-today tasks (using PeriodKeyUtil + completionKey)
     * - Awards points / XP / hydration for new completions since last snapshot
     * - Updates daily goals (5 tasks, featured, open calendar, tap plant)
     */
    private void loadTasksAndRender() {
        String today = YMD.format(new Date());
        Calendar todayCal = Calendar.getInstance();

        Query tasksQ = db.collection("tasks")
                .whereEqualTo("userId", user.getUid());

        tasksQ.get().addOnSuccessListener(snap -> {
            int dueToday = 0;
            int completedToday = 0;

            String firstDueTitle = null;
            String featuredTaskTitle = null;
            boolean featuredTaskCompleted = false;

            if (snap != null) {
                for (DocumentSnapshot ds : snap.getDocuments()) {
                    Task t = ds.toObject(Task.class);
                    if (t == null) continue;

                    // === NEW: derive completion from completionKey + PeriodKeyUtil,
                    //          not from transient t.isCompleted().
                    boolean doneForCurrentPeriod = false;
                    try {
                        String curKey = PeriodKeyUtil.currentKeyFor(t);
                        String compKey = t.getCompletionKey();
                        doneForCurrentPeriod = (compKey != null && compKey.equals(curKey));
                        t.setCompleted(doneForCurrentPeriod);
                    } catch (Exception ex) {
                        Log.w(TAG, "Error deriving completion for task " + t.getName(), ex);
                    }

                    if (isTaskDueOn(t, todayCal)) {
                        dueToday++;

                        if (doneForCurrentPeriod) {
                            completedToday++;
                        }

                        String title = t.getName();

                        if (firstDueTitle == null && !TextUtils.isEmpty(title)) {
                            firstDueTitle = title;
                        }

                        // Prefer a completed task as featured if possible
                        if (doneForCurrentPeriod
                                && TextUtils.isEmpty(featuredTaskTitle)
                                && !TextUtils.isEmpty(title)) {
                            featuredTaskTitle = title;
                            featuredTaskCompleted = true;
                        }
                    }
                }
            }

            // If no completed due-today task found, fall back to first due task as featured
            if (TextUtils.isEmpty(featuredTaskTitle) && !TextUtils.isEmpty(firstDueTitle)) {
                featuredTaskTitle = firstDueTitle;
                featuredTaskCompleted = false;
            }

            // Daily task board (label is "Daily Goals" now)
            if (progressDailyTasks != null) {
                // actual progress/max is overridden in updateDailyGoals
                progressDailyTasks.setMax(100);
                progressDailyTasks.setProgress(0);
            }
            if (tvTasksCount != null) {
                tvTasksCount.setText(completedToday + "/" + Math.max(1, dueToday));
            }

            // ===== Points & XP logic =====
            int points = getPrefs().getInt(KEY_POINTS, 0);

            // XP is a separate counter (default to points if not yet set to preserve old progress)
            int xp = getPrefs().getInt(KEY_XP, points);

            String snapDate = getPrefs().getString(KEY_TODAY_SNAPSHOT_DATE, null);
            int prevCompletedSnap = getPrefs().getInt(KEY_TODAY_COMPLETED_SNAP, 0);
            if (!TextUtils.equals(snapDate, today)) {
                prevCompletedSnap = 0;
            }

            int deltaNewCompletions = Math.max(0, completedToday - prevCompletedSnap);
            boolean pointsChanged = false;
            boolean hydrationChanged = false;

            if (deltaNewCompletions > 0) {
                // Each newly completed *due-today* task gives +1 point and +1 XP
                points += deltaNewCompletions;
                xp += deltaNewCompletions;

                SharedPreferences.Editor ed = getPrefs().edit();
                ed.putInt(KEY_POINTS, points);
                ed.putInt(KEY_XP, xp);
                ed.putString(KEY_TODAY_SNAPSHOT_DATE, today);
                ed.putInt(KEY_TODAY_COMPLETED_SNAP, completedToday);
                ed.apply();
                pointsChanged = true;

                // Hydration gain per newly completed task
                int hydration = clamp0_100(getPrefs().getInt(KEY_HYDRATION, 80)
                        + deltaNewCompletions * HYDRATION_GAIN_PER_TASK);
                getPrefs().edit().putInt(KEY_HYDRATION, hydration).apply();
                hydrationChanged = true;

                if (progressHydration != null) progressHydration.setProgress(hydration);
            } else {
                // No new completions; just make sure snapshot date is up to date
                if (!TextUtils.equals(snapDate, today)) {
                    getPrefs().edit()
                            .putString(KEY_TODAY_SNAPSHOT_DATE, today)
                            .putInt(KEY_TODAY_COMPLETED_SNAP, completedToday)
                            .apply();
                }
                if (progressHydration != null) {
                    progressHydration.setProgress(getPrefs().getInt(KEY_HYDRATION, 80));
                }
            }

            // UI updates: points & EXP
            if (tvPoints != null) tvPoints.setText(String.valueOf(points));

            // EXP bar uses XP only (bonus points don't increase XP)
            LevelState ls = deriveLevelFromPoints(xp);
            int userLevel = ls.level + 1;  // make it 1-based for display

            if (progressExp != null) {
                progressExp.setMax(ls.threshold);
                progressExp.setProgress(ls.expInLevel);
            }
            if (tvLevel != null) {
                tvLevel.setText("Lvl. " + userLevel);
            }

            if (progressMood != null) progressMood.setProgress(getPrefs().getInt(KEY_MOOD, 80));
            if (tvStreak != null) tvStreak.setText(String.valueOf(getPrefs().getInt(KEY_STREAK, 0)));

            applyEquippedCosmetics();

            // Update daily goals UI (login, 5 tasks, featured, open calendar, tap plant)
            updateDailyGoals(dueToday, completedToday, featuredTaskTitle, featuredTaskCompleted);

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
        int level;       // 0-based internal
        int expInLevel;  // XP progress inside current level
        int threshold;   // XP needed for next level
    }

    private LevelState deriveLevelFromPoints(int pointsForXp) {
        int n = 0;
        while (true) {
            int need = 5 * ((n + 1) * (n + 2)) / 2;
            if (pointsForXp < need) break;
            n++;
        }
        int completedForN = 5 * (n * (n + 1)) / 2;
        LevelState ls = new LevelState();
        ls.level = n;  // internal 0-based
        ls.expInLevel = pointsForXp - completedForN;
        ls.threshold = 5 * (n + 1);
        return ls;
    }

    /** Returns the current 1-based level derived from XP stored in prefs. */
    private int getCurrentUserLevel() {
        int points = getPrefs().getInt(KEY_POINTS, 0);
        int xp = getPrefs().getInt(KEY_XP, points);
        LevelState ls = deriveLevelFromPoints(xp);
        return ls.level + 1;
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
            int xp = prefs.getInt(KEY_XP, points); // default XP = points if not set yet
            tvPoints.setText(String.valueOf(points));
            LevelState ls = deriveLevelFromPoints(xp);
            int userLevel = ls.level + 1;

            if (progressExp != null) {
                progressExp.setMax(ls.threshold);
                progressExp.setProgress(ls.expInLevel);
            }
            if (tvLevel != null) {
                tvLevel.setText("Lvl. " + userLevel);
            }
        }
        if (progressHydration != null) {
            progressHydration.setProgress(prefs.getInt(KEY_HYDRATION, 80));
        }
        if (progressMood != null) {
            progressMood.setProgress(prefs.getInt(KEY_MOOD, 80));
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
       Daily Goals logic
       =========== */

    private void updateDailyGoals(int dueToday, int completedToday,
                                  String featuredTaskTitle, boolean featuredTaskCompleted) {
        String today = YMD.format(new Date());
        SharedPreferences p = getPrefs();

        // Goal 1: Daily login
        boolean loginDone = isTodayVal(p.getString(KEY_LAST_LOGIN, null));
        setCheck(imgDailyLoginCheck, loginDone);

        // Goal 2: Complete 5 tasks today
        boolean goal5Done = completedToday >= 5;
        if (tvGoal5Count != null) {
            tvGoal5Count.setText(completedToday + "/5");
        }
        if (ivGoal5Check != null) {
            setCheck(ivGoal5Check, goal5Done);
        }

        // Goal 3: Featured task (show some task title if available)
        if (tvGoalFeatured != null) {
            if (!TextUtils.isEmpty(featuredTaskTitle)) {
                tvGoalFeatured.setText(featuredTaskTitle);
            } else {
                tvGoalFeatured.setText("Featured task");
            }
        }
        boolean goalFeaturedDone = featuredTaskCompleted;
        if (ivGoalFeaturedCheck != null) {
            setCheck(ivGoalFeaturedCheck, goalFeaturedDone);
        }

        // Goal 4: Open Calendar (date-based)
        boolean openCalDone = today.equals(p.getString(KEY_GOAL_OPEN_CAL_DATE, null));
        if (ivGoalCalendarCheck != null) {
            setCheck(ivGoalCalendarCheck, openCalDone);
        }

        // Goal 5: Tap the plant (date-based)
        boolean tapPlantDone = today.equals(p.getString(KEY_GOAL_TAP_PLANT_DATE, null));
        if (ivGoalPlantCheck != null) {
            setCheck(ivGoalPlantCheck, tapPlantDone);
        }

        // Count completed goals
        int goalsTotal = 5;
        int goalsDoneCount = 0;
        if (loginDone)        goalsDoneCount++;
        if (goal5Done)        goalsDoneCount++;
        if (goalFeaturedDone) goalsDoneCount++;
        if (openCalDone)      goalsDoneCount++;
        if (tapPlantDone)     goalsDoneCount++;

        if (progressDailyTasks != null) {
            progressDailyTasks.setMax(goalsTotal);
            progressDailyTasks.setProgress(goalsDoneCount);
        }

        // Bonus +20 once per day if all daily goals completed
        if (goalsDoneCount == goalsTotal) {
            String lastBonusDate = p.getString(KEY_DAILY_BONUS_DATE, null);
            if (!today.equals(lastBonusDate)) {
                int points = p.getInt(KEY_POINTS, 0) + 20;
                p.edit()
                        .putInt(KEY_POINTS, points)
                        .putString(KEY_DAILY_BONUS_DATE, today)
                        .apply();
                if (tvPoints != null) {
                    tvPoints.setText(String.valueOf(points));
                }
                Toast.makeText(requireContext(),
                        "All daily goals complete! +20 points 🌸",
                        Toast.LENGTH_SHORT).show();

                // Sync the new points to Firestore
                saveCloudState(points, null, null, null, null, null);
            }
        }
    }

    private void markGoalOpenCalendarDone() {
        String today = YMD.format(new Date());
        getPrefs().edit().putString(KEY_GOAL_OPEN_CAL_DATE, today).apply();
    }

    private void markGoalTapPlantDone() {
        String today = YMD.format(new Date());
        getPrefs().edit().putString(KEY_GOAL_TAP_PLANT_DATE, today).apply();
    }

    /* ===========
       Plant cosmetics & faces
       =========== */

    private void applyEquippedCosmetics() {
        if (imgCarpet == null || imgPot == null || imgPlant == null || imgFace == null) return;
        if (shopPrefs == null) {
            shopPrefs = requireContext().getSharedPreferences(SHOP_PREFS, 0);
        }

        String eqPlant = shopPrefs.getString(KEY_EQ_PLANT, "plant_cactus");
        String eqPot   = shopPrefs.getString(KEY_EQ_POT,   "pot_classic");
        String eqRoom  = shopPrefs.getString(KEY_EQ_ROOM,  "room_green_carpet");

        // Carpet (room)
        if ("room_blue_carpet".equals(eqRoom)) {
            imgCarpet.setImageResource(R.drawable.carpet_blue);
        } else {
            imgCarpet.setImageResource(R.drawable.carpet_green);
        }

        // Pot
        if ("pot_rounded".equals(eqPot)) {
            imgPot.setImageResource(R.drawable.pot_rounded);
        } else {
            imgPot.setImageResource(R.drawable.pot_classic);
        }

        int hydration = getPrefs().getInt(KEY_HYDRATION, 80);
        int mood = getPrefs().getInt(KEY_MOOD, 80);

        int userLevel = getCurrentUserLevel();

        // Plant sprite chosen by equipped plant + level
        imgPlant.setImageResource(resolvePlantDrawableForLevel(eqPlant, userLevel));

        // Face sprite chosen by mood + hydration
        imgFace.setImageResource(resolveFaceDrawable(mood, hydration));
    }

    /**
     * Decide which plant drawable to use based on the equipped plant key and the user's level.
     * - Cactus grows across 3 stages by level.
     * - Succulent is a single-stage plant for now.
     */
    private int resolvePlantDrawableForLevel(String eqPlant, int userLevel) {
        // Succulent: only one stage at the moment.
        if ("plant_succulent".equals(eqPlant)) {
            return R.drawable.succulent_stage_1;
        }

        // Default: cactus with level-based stages.
        if (userLevel >= LEVEL_STAGE_3) {
            return R.drawable.cactus_stage_3;
        } else if (userLevel >= LEVEL_STAGE_2) {
            return R.drawable.cactus_stage_2;
        } else {
            return R.drawable.cactus_stage_1;
        }
    }

    private int resolveFaceDrawable(int mood, int hydration) {
        // Simple rules combining mood + hydration into a face
        if (mood >= 80 && hydration >= 60) {
            return R.drawable.face_happy;
        } else if (mood >= 60 && hydration >= 40) {
            return R.drawable.face_default;
        } else if (mood < 30 && hydration < 30) {
            return R.drawable.face_angry;
        } else if (mood < 40) {
            return R.drawable.face_sad;
        } else if (hydration < 40) {
            return R.drawable.face_tired;
        } else {
            return R.drawable.face_upset;
        }
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
            // Mark "Open Calendar" goal done
            markGoalOpenCalendarDone();
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
        applyEquippedCosmetics();
        // Recompute tasks & daily goals when coming back to this screen
        loadTasksAndRender();
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

    private void applySelectedPlant(@DrawableRes int plantResId) {
        ImageView imgPlant = requireView().findViewById(R.id.imgPlant);
        imgPlant.setImageResource(plantResId);
        imgPlant.setBackground(null);      // just in case
        imgPlant.setColorFilter(null);     // no tint overlays
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
