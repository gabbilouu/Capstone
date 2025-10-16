package com.example.elevate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditEventFragment extends Fragment {

    private EditText etEventName, etLocation, etNotes;
    private Spinner spinnerTag, spRepeat;
    private CheckBox cbAllDay;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime, btnAdd, btnCancel;
    private TextView tvTitle;

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private NavController navC;
    private String eventId;

    // Formats must match how you store/display elsewhere
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma", Locale.getDefault()); // e.g., 5:30PM

    public EditEventFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = NavHostFragment.findNavController(this);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // UI references
        etEventName  = view.findViewById(R.id.etEventName);
        etLocation   = view.findViewById(R.id.etLocation);
        etNotes      = view.findViewById(R.id.etNotes);
        spinnerTag   = view.findViewById(R.id.etTag);          // tag spinner in XML
        spRepeat     = view.findViewById(R.id.spRepeat);       // repeat spinner in XML
        cbAllDay     = view.findViewById(R.id.swAllDay);
        btnStartDate = view.findViewById(R.id.btnStartDate);
        btnStartTime = view.findViewById(R.id.btnStartTime);
        btnEndDate   = view.findViewById(R.id.btnEndDate);
        btnEndTime   = view.findViewById(R.id.btnEndTime);
        btnAdd       = view.findViewById(R.id.btnAdd);
        btnCancel    = view.findViewById(R.id.btnCancel);
        tvTitle      = view.findViewById(R.id.tvTitle);

        if (tvTitle != null) tvTitle.setText("Edit Event");

        setupTagSpinner();      // from @array/tags
        setupRepeatSpinner();   // from @array/repeat_options

        updateTimePickersEnabledState();
        updateButtonLabels();

        // Get eventId from args and load
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId != null) {
                loadEventDetails(eventId);
            }
        }

        cbAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateTimePickersEnabledState();
            updateButtonLabels();
        });

        btnStartDate.setOnClickListener(v -> pickDate(startCal, btnStartDate));
        btnStartTime.setOnClickListener(v -> pickTime(startCal, btnStartTime));
        btnEndDate.setOnClickListener(v -> pickDate(endCal, btnEndDate));
        btnEndTime.setOnClickListener(v -> pickTime(endCal, btnEndTime));

        btnAdd.setOnClickListener(v -> saveEventChanges());
        btnCancel.setOnClickListener(v -> navC.navigate(R.id.action_editEventFragment_to_eventFragment));
    }

    // Use XML arrays to keep options in one place
    private void setupTagSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.tags,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTag.setAdapter(adapter);
    }

    private void setupRepeatSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.repeat_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRepeat.setAdapter(adapter);
    }

    // Load existing event details and prefill fields (including Repeat)
    private void loadEventDetails(String id) {
        db.collection("events").document(id).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    etEventName.setText(document.getString("name"));
                    etLocation.setText(document.getString("location"));
                    etNotes.setText(document.getString("notes"));

                    // Tag (case-insensitive)
                    String tag = document.getString("tag");
                    if (tag != null) {
                        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerTag.getAdapter();
                        int sel = indexOfIgnoreCase(adapter, tag);
                        if (sel >= 0) spinnerTag.setSelection(sel);
                    }

                    // Repeat (case-insensitive; default to "None" if missing)
                    String repeat = document.getString("repeat");
                    if (repeat == null) repeat = "None";
                    ArrayAdapter<?> repAdapter = (ArrayAdapter<?>) spRepeat.getAdapter();
                    int repSel = indexOfIgnoreCase(repAdapter, repeat);
                    if (repSel >= 0) spRepeat.setSelection(repSel);

                    boolean allDay = document.getBoolean("allDay") != null && document.getBoolean("allDay");
                    cbAllDay.setChecked(allDay);

                    try {
                        // Dates
                        String startDateStr = document.getString("startDate"); // e.g., "Oct 9, 2025"
                        String endDateStr   = document.getString("endDate");
                        if (startDateStr != null) {
                            Calendar tmp = Calendar.getInstance();
                            tmp.setTime(dateFormat.parse(startDateStr));
                            startCal.set(Calendar.YEAR,  tmp.get(Calendar.YEAR));
                            startCal.set(Calendar.MONTH, tmp.get(Calendar.MONTH));
                            startCal.set(Calendar.DAY_OF_MONTH, tmp.get(Calendar.DAY_OF_MONTH));
                        }
                        if (endDateStr != null) {
                            Calendar tmp = Calendar.getInstance();
                            tmp.setTime(dateFormat.parse(endDateStr));
                            endCal.set(Calendar.YEAR,  tmp.get(Calendar.YEAR));
                            endCal.set(Calendar.MONTH, tmp.get(Calendar.MONTH));
                            endCal.set(Calendar.DAY_OF_MONTH, tmp.get(Calendar.DAY_OF_MONTH));
                        }

                        // Times (don’t clobber dates)
                        String startTimeStr = document.getString("startTime"); // "All Day" or "5:30PM"
                        String endTimeStr   = document.getString("endTime");
                        if (!allDay && startTimeStr != null && !"All Day".equalsIgnoreCase(startTimeStr)) {
                            Calendar t = Calendar.getInstance();
                            t.setTime(timeFormat.parse(startTimeStr));
                            startCal.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
                            startCal.set(Calendar.MINUTE,      t.get(Calendar.MINUTE));
                            startCal.set(Calendar.SECOND, 0);
                            startCal.set(Calendar.MILLISECOND, 0);
                        } else {
                            startCal.set(Calendar.HOUR_OF_DAY, 0);
                            startCal.set(Calendar.MINUTE, 0);
                            startCal.set(Calendar.SECOND, 0);
                            startCal.set(Calendar.MILLISECOND, 0);
                        }

                        if (!allDay && endTimeStr != null && !"All Day".equalsIgnoreCase(endTimeStr)) {
                            Calendar t = Calendar.getInstance();
                            t.setTime(timeFormat.parse(endTimeStr));
                            endCal.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
                            endCal.set(Calendar.MINUTE,      t.get(Calendar.MINUTE));
                            endCal.set(Calendar.SECOND, 0);
                            endCal.set(Calendar.MILLISECOND, 0);
                        } else {
                            endCal.set(Calendar.HOUR_OF_DAY, 23);
                            endCal.set(Calendar.MINUTE, 59);
                            endCal.set(Calendar.SECOND, 0);
                            endCal.set(Calendar.MILLISECOND, 0);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Date/time parse error", Toast.LENGTH_SHORT).show();
                    }

                    updateTimePickersEnabledState();
                    updateButtonLabels();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show());
    }

    private int indexOfIgnoreCase(ArrayAdapter<?> adapter, String value) {
        if (adapter == null || value == null) return -1;
        for (int i = 0; i < adapter.getCount(); i++) {
            Object item = adapter.getItem(i);
            if (item != null && value.equalsIgnoreCase(item.toString())) return i;
        }
        return -1;
    }

    private void pickDate(Calendar calendar, Button button) {
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateButtonLabels();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void pickTime(Calendar calendar, Button button) {
        new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    if (cbAllDay.isChecked()) cbAllDay.setChecked(false);
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    updateButtonLabels();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void updateTimePickersEnabledState() {
        boolean allDay = cbAllDay.isChecked();
        btnStartTime.setEnabled(!allDay);
        btnEndTime.setEnabled(!allDay);
    }

    private void updateButtonLabels() {
        boolean allDay = cbAllDay.isChecked();
        btnStartDate.setText(dateFormat.format(startCal.getTime()));
        btnEndDate.setText(dateFormat.format(endCal.getTime()));
        btnStartTime.setText(allDay ? "All Day" : timeFormat.format(startCal.getTime()));
        btnEndTime.setText(allDay ? "All Day" : timeFormat.format(endCal.getTime()));
    }

    private void saveEventChanges() {
        String name     = etEventName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String notes    = etNotes.getText().toString().trim();
        String tag      = spinnerTag.getSelectedItem() != null ? spinnerTag.getSelectedItem().toString() : "General";
        String repeat   = spRepeat.getSelectedItem()  != null ? spRepeat.getSelectedItem().toString()  : "None";
        boolean allDay  = cbAllDay.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter an event name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate end >= start
        Calendar startCheck = (Calendar) startCal.clone();
        Calendar endCheck   = (Calendar) endCal.clone();
        if (allDay) {
            startCheck.set(Calendar.HOUR_OF_DAY, 0);
            startCheck.set(Calendar.MINUTE, 0);
            startCheck.set(Calendar.SECOND, 0);
            startCheck.set(Calendar.MILLISECOND, 0);

            endCheck.set(Calendar.HOUR_OF_DAY, 23);
            endCheck.set(Calendar.MINUTE, 59);
            endCheck.set(Calendar.SECOND, 0);
            endCheck.set(Calendar.MILLISECOND, 0);
        }
        if (endCheck.before(startCheck)) {
            Toast.makeText(requireContext(), "End must be after start", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("location", location);
        updates.put("notes", notes);
        updates.put("tag", tag);
        updates.put("repeat", repeat); // <-- save repeat
        updates.put("allDay", allDay);
        updates.put("startDate", dateFormat.format(startCal.getTime()));
        updates.put("endDate",   dateFormat.format(endCal.getTime()));
        updates.put("startTime", allDay ? "All Day" : timeFormat.format(startCal.getTime()));
        updates.put("endTime",   allDay ? "All Day" : timeFormat.format(endCal.getTime()));

        if (eventId != null) {
            db.collection("events").document(eventId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Event updated", Toast.LENGTH_SHORT).show();
                        navC.navigate(R.id.action_editEventFragment_to_eventFragment);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Failed to update event", Toast.LENGTH_SHORT).show());
        }
    }
}
