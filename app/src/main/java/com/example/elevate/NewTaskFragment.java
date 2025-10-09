package com.example.elevate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NewTaskFragment extends Fragment {

    private EditText etTaskName, etNotes;
    private Spinner spinnerRepeat, spinnerTaskType;
    private Button btnSave, btnCancel;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime;
    private TextView ivTaskEmoji;
    private FirebaseFirestore db;

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();

    public NewTaskFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_task, container, false);

        etTaskName = view.findViewById(R.id.etTaskName);
        etNotes = view.findViewById(R.id.etNotes);
        spinnerRepeat = view.findViewById(R.id.spinnerRepeat);
        spinnerTaskType = view.findViewById(R.id.spinnerTaskType);
        btnSave = view.findViewById(R.id.btnAdd);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnStartDate = view.findViewById(R.id.btnStartDate);
        btnStartTime = view.findViewById(R.id.btnStartTime);
        btnEndDate = view.findViewById(R.id.btnEndDate);
        btnEndTime = view.findViewById(R.id.btnEndTime);
        ivTaskEmoji = view.findViewById(R.id.ivTaskEmoji);

        db = FirebaseFirestore.getInstance();

        // Update emoji automatically based on task name
        etTaskName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                if (!text.isEmpty()) {
                    ivTaskEmoji.setText(text.substring(0, 1) + "📝");
                } else {
                    ivTaskEmoji.setText("📝");
                }
            }
        });

        // Start date/time pickers
        btnStartDate.setOnClickListener(v -> pickDate(startCalendar, btnStartDate));
        btnStartTime.setOnClickListener(v -> pickTime(startCalendar, btnStartTime));

        // End date/time pickers
        btnEndDate.setOnClickListener(v -> pickDate(endCalendar, btnEndDate));
        btnEndTime.setOnClickListener(v -> pickTime(endCalendar, btnEndTime));

        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        // Initialize buttons with default values
        updateButtonLabels();

        return view;
    }

    private void updateButtonLabels() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

        btnStartDate.setText(dateFormat.format(startCalendar.getTime()));
        btnStartTime.setText(timeFormat.format(startCalendar.getTime()));
        btnEndDate.setText(dateFormat.format(endCalendar.getTime()));
        btnEndTime.setText(timeFormat.format(endCalendar.getTime()));
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

    private void saveTask() {
        String name = etTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a task name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endCalendar.after(startCalendar)) {
            Toast.makeText(requireContext(), "End date/time cannot be before start date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task();
        task.setName(name);
        task.setNotes(etNotes.getText().toString());
        task.setRepeatType(spinnerRepeat.getSelectedItem().toString());
        task.setTaskType(spinnerTaskType.getSelectedItem().toString());
        task.setCompleted(false);
        task.setEmoji(ivTaskEmoji.getText().toString());

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

        task.setStartDate(dateFormat.format(startCalendar.getTime()));
        task.setStartTime(timeFormat.format(startCalendar.getTime()));
        task.setEndDate(dateFormat.format(endCalendar.getTime()));
        task.setEndTime(timeFormat.format(endCalendar.getTime()));

        db.collection("tasks").add(task)
                .addOnSuccessListener(documentReference -> {
                    task.setId(documentReference.getId()); // optional
                    Toast.makeText(requireContext(), "Task added", Toast.LENGTH_SHORT).show();

                    Bundle result = new Bundle();
                    result.putBoolean("taskAdded", true);
                    getParentFragmentManager().setFragmentResult("newTask", result);

                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
