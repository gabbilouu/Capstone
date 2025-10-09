package com.example.elevate;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class EventListFragment extends Fragment implements View.OnClickListener {

    private static final String PREFS_NAME = "CustomCategories";
    private static final String KEY_CUSTOM_CATEGORIES = "customCategoryList";

    private NavController navC;
    private ImageButton homeButton, checkButton, profileButton, addEventButton;
    private Button todayButton, customCategoryButton;
    private TextView monthText, tvNoEvents, tvSelectedDate;
    private SharedPreferences prefs;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MaterialCalendarView calendarView;

    private final Map<CalendarDay, List<Map<String, Object>>> eventsByDay = new HashMap<>();

    public EventListFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        navC = Navigation.findNavController(view);

        homeButton = view.findViewById(R.id.HomeButton);
        checkButton = view.findViewById(R.id.TaskButton);
        profileButton = view.findViewById(R.id.SettingsButton);
        addEventButton = view.findViewById(R.id.addEventButton);
        customCategoryButton = view.findViewById(R.id.customCategoryButton);
        todayButton = view.findViewById(R.id.todayButton);
        monthText = view.findViewById(R.id.monthText);
        tvNoEvents = view.findViewById(R.id.tvNoEvents);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        calendarView = view.findViewById(R.id.calendarView);

        loadSavedCategories();
        loadEventsAndDecorate();

        calendarView.setSelectedDate(CalendarDay.today());
        updateMonthText(Calendar.getInstance());
        updateHeaderDate(CalendarDay.today());
        populateScheduleForDate(CalendarDay.today());

        todayButton.setOnClickListener(v -> {
            calendarView.setSelectedDate(CalendarDay.today());
            updateMonthText(Calendar.getInstance());
            updateHeaderDate(CalendarDay.today());
            populateScheduleForDate(CalendarDay.today());
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            Calendar c = Calendar.getInstance();
            c.set(date.getYear(), date.getMonth(), date.getDay());
            updateMonthText(c);
            updateHeaderDate(date);
            populateScheduleForDate(date);
        });

        addEventButton.setOnClickListener(v -> navC.navigate(R.id.action_eventFragment_to_newEventFragment));
        customCategoryButton.setOnClickListener(v -> showCustomCategoryDialog());
        homeButton.setOnClickListener(this);
        checkButton.setOnClickListener(this);
        profileButton.setOnClickListener(this);
    }

    // ---------- HEADER ----------
    private void updateHeaderDate(CalendarDay date) {
        Calendar cal = Calendar.getInstance();
        cal.set(date.getYear(), date.getMonth(), date.getDay());
        String formatted = new SimpleDateFormat("EEEE — MMMM d, yyyy", Locale.getDefault()).format(cal.getTime());
        tvSelectedDate.setText(formatted);
    }

    private void updateMonthText(Calendar calendar) {
        monthText.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime()));
    }

    // ---------- CUSTOM CATEGORY ----------
    private void showCustomCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_category, null);
        EditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
        final int[] selectedColor = {0xFF2196F3};

        View.OnClickListener colorClickListener = colorView -> {
            int color = ((Button) colorView).getBackgroundTintList().getDefaultColor();
            selectedColor[0] = color;
        };

        dialogView.findViewById(R.id.colorRed).setOnClickListener(colorClickListener);
        dialogView.findViewById(R.id.colorGreen).setOnClickListener(colorClickListener);
        dialogView.findViewById(R.id.colorBlue).setOnClickListener(colorClickListener);
        dialogView.findViewById(R.id.colorYellow).setOnClickListener(colorClickListener);
        dialogView.findViewById(R.id.colorPurple).setOnClickListener(colorClickListener);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add Custom Category")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etCategoryName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    addCustomCategoryButton(name, selectedColor[0]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addCustomCategoryButton(String name, int color) {
        LinearLayout filterRow = requireView().findViewById(R.id.filterRow);
        if (filterRow == null) return;

        Button newButton = new Button(requireContext());
        newButton.setText(name);
        newButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        newButton.setTextColor(getResources().getColor(android.R.color.white));
        newButton.setPadding(16, 8, 16, 8);
        filterRow.addView(newButton);
        saveCustomCategory(name, color);
    }

    private void saveCustomCategory(String name, int color) {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_CUSTOM_CATEGORIES, "[]"));
            for (int i = 0; i < array.length(); i++) {
                if (array.getJSONObject(i).getString("name").equalsIgnoreCase(name)) return;
            }

            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("color", color);
            array.put(obj);
            prefs.edit().putString(KEY_CUSTOM_CATEGORIES, array.toString()).apply();

            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                db.collection("users").document(user.getUid()).collection("customCategories")
                        .add(obj)
                        .addOnSuccessListener(r -> Log.d("Firestore", "Saved category " + name))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error saving category", e));
            }
        } catch (Exception e) {
            Log.e("EventListFragment", "Error saving category", e);
        }
    }
    // ---------- LOAD SAVED CUSTOM CATEGORIES ----------
    private void loadSavedCategories() {
        LinearLayout filterRow = requireView().findViewById(R.id.filterRow);
        if (filterRow == null) return;
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            loadFromLocal(filterRow);
            return;
        }

        db.collection("users").document(user.getUid()).collection("customCategories")
                .get()
                .addOnSuccessListener(snap -> {
                    filterRow.removeAllViews();
                    for (QueryDocumentSnapshot doc : snap) {
                        String name = doc.getString("name");
                        int color = doc.contains("color") ? doc.getLong("color").intValue() : 0xFF2196F3;
                        addCategoryButtonToView(filterRow, name, color);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load categories", e);
                    loadFromLocal(filterRow);
                });
    }

    private void loadFromLocal(LinearLayout container) {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_CUSTOM_CATEGORIES, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                addCategoryButtonToView(container, obj.getString("name"), obj.getInt("color"));
            }
        } catch (JSONException e) {
            Log.e("EventListFragment", "Error reading local JSON", e);
        }
    }

    private void addCategoryButtonToView(LinearLayout container, String name, int color) {
        Button newButton = new Button(requireContext());
        newButton.setText(name);
        newButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        newButton.setTextColor(getResources().getColor(android.R.color.white));
        newButton.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(8, 0, 8, 0);
        newButton.setLayoutParams(params);
        container.addView(newButton);
    }

    // ---------- LOAD EVENTS ----------
    private void loadEventsAndDecorate() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("events")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventsByDay.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> event = doc.getData();
                        event.put("id", doc.getId());
                        try {
                            String dateStr = (String) event.get("startDate");
                            Date date = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).parse(dateStr);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            CalendarDay day = CalendarDay.from(cal);
                            eventsByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(event);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    List<DayMultiDotDecorator> decorators = new ArrayList<>();
                    for (Map.Entry<CalendarDay, List<Map<String, Object>>> entry : eventsByDay.entrySet()) {
                        CalendarDay day = entry.getKey();
                        List<Integer> colors = new ArrayList<>();
                        for (Map<String, Object> ev : entry.getValue()) {
                            String tag = (String) ev.getOrDefault("tag", "General");
                            int color = colorForTag(tag);
                            if (!colors.contains(color)) colors.add(color);
                        }
                        decorators.add(new DayMultiDotDecorator(day, colors));
                    }

                    calendarView.removeDecorators();
                    for (DayMultiDotDecorator d : decorators) calendarView.addDecorator(d);
                    calendarView.invalidateDecorators();
                    populateScheduleForDate(CalendarDay.today());
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error loading events", e));
    }

    private int colorForTag(String tag) {
        if (tag == null) return 0xFF607D8B;
        switch (tag.toLowerCase(Locale.ROOT)) {
            case "school": return 0xFF2196F3;
            case "personal": return 0xFFF44336;
            case "work": return 0xFFFF9800;
            case "orgs.": return 0xFF4CAF50;
            case "events": return 0xFF9C27B0;
            case "birthdays": return 0xFFFF69B4;
            case "family": return 0xFF00BCD4;
            default: return 0xFF607D8B;
        }
    }

    // ---------- DOT DECORATOR ----------
    private static class DayMultiDotDecorator implements DayViewDecorator {
        private final CalendarDay day;
        private final List<Integer> colors;

        DayMultiDotDecorator(CalendarDay day, List<Integer> colors) {
            this.day = day;
            this.colors = colors;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return this.day.equals(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new MultipleDotSpan(6f, colors));
        }
    }

    private static class MultipleDotSpan implements LineBackgroundSpan {
        private final float radius;
        private final List<Integer> colors;

        MultipleDotSpan(float radius, List<Integer> colors) {
            this.radius = radius;
            this.colors = colors;
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom,
                                   CharSequence charSequence, int start, int end, int lineNum) {
            if (colors == null || colors.isEmpty()) return;
            float totalWidth = (colors.size() - 1) * (radius * 2 + 4);
            float startX = (left + right) / 2f - totalWidth / 2f;
            float y = bottom - radius - 3;
            for (int i = 0; i < colors.size(); i++) {
                paint.setColor(colors.get(i));
                canvas.drawCircle(startX + i * (radius * 2 + 4), y, radius, paint);
            }
        }
    }

    // ---------- DISPLAY EVENTS ----------
    private void populateScheduleForDate(CalendarDay day) {
        LinearLayout scheduleContainer = requireView().findViewById(R.id.scheduleContainer);
        scheduleContainer.removeAllViews();

        List<Map<String, Object>> events = eventsByDay.get(day);

        if (events == null || events.isEmpty()) {
            scheduleContainer.setVisibility(View.GONE);
            tvNoEvents.setVisibility(View.VISIBLE);
            return;
        }

        tvNoEvents.setVisibility(View.GONE);
        scheduleContainer.setVisibility(View.VISIBLE);

        for (Map<String, Object> e : events) {
            boolean allDay = (boolean) e.getOrDefault("allDay", false);
            String time = (String) e.getOrDefault("startTime", "");
            String name = (String) e.getOrDefault("name", "");
            String notes = (String) e.getOrDefault("notes", "");
            String tag = (String) e.getOrDefault("tag", "");
            int color = colorForTag(tag);

            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 12, 16, 12);
            card.setBackgroundResource(android.R.color.white);
            card.setElevation(3f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8);
            card.setLayoutParams(params);

            TextView title = new TextView(requireContext());
            title.setText(allDay ? ("All day  " + name) : (time + "  " + name));
            title.setTextSize(16);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setTextColor(color);

            TextView note = new TextView(requireContext());
            note.setText(notes);
            note.setTextSize(14);
            note.setTextColor(getResources().getColor(android.R.color.darker_gray));

            card.addView(title);
            card.addView(note);

            // ✅ Long press = Edit/Delete
            card.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Edit or Delete Event")
                        .setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
                            if (which == 0) {
                                // Navigate to edit fragment (future)
                                Bundle bundle = new Bundle();
                                bundle.putString("eventId", (String) e.get("id"));
                                navC.navigate(R.id.action_eventFragment_to_editEventFragment, bundle);
                            } else if (which == 1) {
                                String id = (String) e.get("id");
                                if (id != null) {
                                    db.collection("events").document(id)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
                                                loadEventsAndDecorate();
                                            })
                                            .addOnFailureListener(err -> Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
                                }
                            }
                        })
                        .show();
                return true;
            });

            scheduleContainer.addView(card);
        }
    }

    @Override
    public void onClick(View v) {
        if (navC == null) return;
        int id = v.getId();
        if (id == R.id.HomeButton) navC.navigate(R.id.action_eventFragment_to_homePageFragment);
        else if (id == R.id.TaskButton) navC.navigate(R.id.action_eventFragment_to_taskListFragment);
        else if (id == R.id.SettingsButton) navC.navigate(R.id.action_eventFragment_to_settingsFragment);
    }
}
