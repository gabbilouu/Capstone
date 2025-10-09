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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditEventFragment extends Fragment {

    private EditText etEventName, etLocation, etNotes;
    private Spinner spinnerTag;
    private CheckBox cbAllDay;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime, btnAdd, btnCancel;
    private TextView tvTitle;

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal = Calendar.getInstance();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private NavController navC;
    private String eventId;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

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
        etEventName = view.findViewById(R.id.etEventName);
        etLocation = view.findViewById(R.id.etLocation);
        etNotes = view.findViewById(R.id.etNotes);
        spinnerTag = view.findViewById(R.id.etTag);
        cbAllDay = view.findViewById(R.id.cbAllDay);
        btnStartDate = view.findViewById(R.id.btnStartDate);
        btnStartTime = view.findViewById(R.id.btnStartTime);
        btnEndDate = view.findViewById(R.id.btnEndDate);
        btnEndTime = view.findViewById(R.id.btnEndTime);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnCancel = view.findViewById(R.id.btnCancel);
        tvTitle = view.findViewById(R.id.tvTitle);

        setupTagSpinner();
        updateButtonLabels();

        // Get eventId from arguments
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId != null) {
                loadEventDetails(eventId);
            }
        }

        cbAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnStartTime.setEnabled(!isChecked);
            btnEndTime.setEnabled(!isChecked);
        });

        btnStartDate.setOnClickListener(v -> pickDate(startCal, btnStartDate));
        btnStartTime.setOnClickListener(v -> pickTime(startCal, btnStartTime));
        btnEndDate.setOnClickListener(v -> pickDate(endCal, btnEndDate));
        btnEndTime.setOnClickListener(v -> pickTime(endCal, btnEndTime));

        btnAdd.setOnClickListener(v -> saveEventChanges());
        btnCancel.setOnClickListener(v -> navC.navigate(R.id.action_editEventFragment_to_eventFragment));
    }

    // Populate spinner
    private void setupTagSpinner() {
        String[] categories = {"School", "Personal", "Work", "Orgs.", "Events", "Family", "Birthdays", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerTag.setAdapter(adapter);
    }

    // Load existing event details
    private void loadEventDetails(String id) {
        DocumentReference docRef = db.collection("events").document(id);
        docRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                etEventName.setText(document.getString("name"));
                etLocation.setText(document.getString("location"));
                etNotes.setText(document.getString("notes"));
                cbAllDay.setChecked(document.getBoolean("allDay") != null && document.getBoolean("allDay"));

                // Load tag into spinner
                String tag = document.getString("tag");
                if (tag != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerTag.getAdapter();
                    int pos = adapter.getPosition(tag);
                    if (pos >= 0) spinnerTag.setSelection(pos);
                }

                try {
                    String startDateStr = document.getString("startDate");
                    String endDateStr = document.getString("endDate");
                    String startTimeStr = document.getString("startTime");
                    String endTimeStr = document.getString("endTime");

                    if (startDateStr != null)
                        startCal.setTime(dateFormat.parse(startDateStr));
                    if (endDateStr != null)
                        endCal.setTime(dateFormat.parse(endDateStr));

                    if (startTimeStr != null && !startTimeStr.equals("All Day"))
                        startCal.setTime(timeFormat.parse(startTimeStr));
                    if (endTimeStr != null && !endTimeStr.equals("All Day"))
                        endCal.setTime(timeFormat.parse(endTimeStr));

                } catch (Exception e) {
                    e.printStackTrace();
                }

                updateButtonLabels();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show());
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
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    updateButtonLabels();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        ).show();
    }

    private void updateButtonLabels() {
        btnStartDate.setText(dateFormat.format(startCal.getTime()));
        btnEndDate.setText(dateFormat.format(endCal.getTime()));
        btnStartTime.setText(timeFormat.format(startCal.getTime()));
        btnEndTime.setText(timeFormat.format(endCal.getTime()));
    }

    private void saveEventChanges() {
        String name = etEventName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String tag = spinnerTag.getSelectedItem() != null ? spinnerTag.getSelectedItem().toString() : "General";
        boolean allDay = cbAllDay.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter an event name", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("location", location);
        updates.put("notes", notes);
        updates.put("tag", tag);
        updates.put("allDay", allDay);
        updates.put("startDate", dateFormat.format(startCal.getTime()));
        updates.put("endDate", dateFormat.format(endCal.getTime()));
        updates.put("startTime", allDay ? "All Day" : timeFormat.format(startCal.getTime()));
        updates.put("endTime", allDay ? "All Day" : timeFormat.format(endCal.getTime()));

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
