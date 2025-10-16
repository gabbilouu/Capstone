package com.example.elevate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class NewEventFragment extends Fragment {

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

    public NewEventFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_event, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = NavHostFragment.findNavController(this);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etEventName = view.findViewById(R.id.etEventName);
        etLocation = view.findViewById(R.id.etLocation);
        etNotes = view.findViewById(R.id.etNotes);
        spinnerTag = view.findViewById(R.id.etTag);
        cbAllDay = view.findViewById(R.id.swAllDay);
        btnStartDate = view.findViewById(R.id.btnStartDate);
        btnStartTime = view.findViewById(R.id.btnStartTime);
        btnEndDate = view.findViewById(R.id.btnEndDate);
        btnEndTime = view.findViewById(R.id.btnEndTime);
        btnAdd = view.findViewById(R.id.btnAdd);
        btnCancel = view.findViewById(R.id.btnCancel);
        tvTitle = view.findViewById(R.id.tvTitle);

        setupTagSpinner();
        updateButtonLabels();

        // Disable time pickers if "All Day" is checked
        cbAllDay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnStartTime.setEnabled(!isChecked);
            btnEndTime.setEnabled(!isChecked);
        });

        btnStartDate.setOnClickListener(v -> pickDate(startCal, btnStartDate));
        btnStartTime.setOnClickListener(v -> pickTime(startCal, btnStartTime));

        btnEndDate.setOnClickListener(v -> pickDate(endCal, btnEndDate));
        btnEndTime.setOnClickListener(v -> pickTime(endCal, btnEndTime));

        btnAdd.setOnClickListener(v -> saveEvent());
        btnCancel.setOnClickListener(v -> navC.navigate(R.id.action_newEventFragment_to_eventFragment));
    }

    private void setupTagSpinner() {
        // You can replace this with dynamic category loading later
        String[] categories = {"School", "Personal", "Work", "Orgs.", "Events", "Family", "Birthdays", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerTag.setAdapter(adapter);
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

        btnStartDate.setText(dateFormat.format(startCal.getTime()));
        btnEndDate.setText(dateFormat.format(endCal.getTime()));
        btnStartTime.setText(timeFormat.format(startCal.getTime()));
        btnEndTime.setText(timeFormat.format(endCal.getTime()));
    }

    private void saveEvent() {
        String name = etEventName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();
        String tag = spinnerTag.getSelectedItem() != null ? spinnerTag.getSelectedItem().toString() : "General";
        boolean allDay = cbAllDay.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter an event name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate time order
        if (!allDay && !endCal.after(startCal)) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("location", location);
        event.put("notes", notes);
        event.put("tag", tag);
        event.put("allDay", allDay);
        event.put("startDate", dateFormat.format(startCal.getTime()));
        event.put("endDate", dateFormat.format(endCal.getTime()));
        event.put("startTime", allDay ? "All Day" : timeFormat.format(startCal.getTime()));
        event.put("endTime", allDay ? "All Day" : timeFormat.format(endCal.getTime()));

        if (auth.getCurrentUser() != null) {
            event.put("userId", auth.getCurrentUser().getUid());
        }

        db.collection("events")
                .add(event)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(requireContext(), "Event added successfully!", Toast.LENGTH_SHORT).show();
                    navC.navigate(R.id.action_newEventFragment_to_eventFragment);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to save event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
