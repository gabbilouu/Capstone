package com.example.elevate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter; // <-- added
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

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

    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();

    // Debounce handler for AI calls
    private final Handler emojiHandler = new Handler();
    private Runnable emojiRunnable;

    public NewTaskFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // Default emoji so we never read null (fixed precedence)
        if (ivTaskEmoji != null && (ivTaskEmoji.getText() == null || ivTaskEmoji.getText().length() == 0)) {
            ivTaskEmoji.setText("📝");
        }

        ArrayAdapter<CharSequence> repeatAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.repeat_options,                     // e.g. ["None","Daily","Weekly","Monthly","Select Days"]
                android.R.layout.simple_spinner_item
        );
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(repeatAdapter);
        spinnerRepeat.setSelection(0);

        ArrayAdapter<CharSequence> taskTypeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.task_type_options,                  // e.g. ["General","School","Work","Personal"]
                R.layout.spinner_task_type_selected
        );
        taskTypeAdapter.setDropDownViewResource(R.layout.spinner_task_type_dropdown);
        spinnerTaskType.setAdapter(taskTypeAdapter);
        spinnerTaskType.setSelection(0);

        AIEmojiGenerator aiEmoji = new AIEmojiGenerator(requireContext());

        // Debounced AI emoji update based on task name
        etTaskName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (emojiRunnable != null) emojiHandler.removeCallbacks(emojiRunnable);

                final String text = s != null ? s.toString().trim() : "";
                emojiRunnable = () -> {
                    if (text.isEmpty()) {
                        ivTaskEmoji.setText("📝");
                        return;
                    }
                    String type = spinnerTextOr(spinnerTaskType, "General");
                    aiEmoji.generateEmoji(text, type, new AIEmojiGenerator.EmojiCallback() {
                        @Override public void onEmojiGenerated(String emoji) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> ivTaskEmoji.setText(emoji));
                        }
                        @Override public void onError(Exception e) {
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> ivTaskEmoji.setText("📝"));
                            Log.e("AIEmojiGenerator", "Error generating emoji (debounced)", e);
                        }
                    });
                };
                // Delay execution by 1 second after user stops typing
                emojiHandler.postDelayed(emojiRunnable, 1000);
            }
        });

        // Update emoji when task type changes
        spinnerTaskType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String taskName = textOrEmpty(etTaskName);
                if (taskName.isEmpty()) return;

                String selectedType = spinnerTextOr(spinnerTaskType, "General");
                AIEmojiGenerator aiEmojiLocal = new AIEmojiGenerator(requireContext());
                aiEmojiLocal.generateEmoji(taskName, selectedType, new AIEmojiGenerator.EmojiCallback() {
                    @Override public void onEmojiGenerated(String emoji) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> ivTaskEmoji.setText(emoji));
                    }
                    @Override public void onError(Exception e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> ivTaskEmoji.setText("📝"));
                    }
                });
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Start date/time pickers
        btnStartDate.setOnClickListener(v -> pickDate(startCalendar, btnStartDate));
        btnStartTime.setOnClickListener(v -> pickTime(startCalendar, btnStartTime));

        // End date/time pickers
        btnEndDate.setOnClickListener(v -> pickDate(endCalendar, btnEndDate));
        btnEndTime.setOnClickListener(v -> pickTime(endCalendar, btnEndTime));

        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        // Initialize buttons with default values
        updateButtonLabels();
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
        String name = textOrEmpty(etTaskName);
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a task name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endCalendar.after(startCalendar)) {
            Toast.makeText(requireContext(), "End date/time cannot be before start date/time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read UI
        String repeatRaw = spinnerTextOr(spinnerRepeat, "Daily"); // e.g. "None", "Daily", "Weekly", ...
        // If your array uses "None" or "Does not repeat", map it to something your PeriodKeyUtil understands.
        // For now we map both to "Daily" so completion still works with the current logic.
        String repeatType =
                (repeatRaw.equalsIgnoreCase("None") || repeatRaw.equalsIgnoreCase("Does not repeat"))
                        ? "Daily" : repeatRaw;

        String taskType = spinnerTextOr(spinnerTaskType, "General");
        String notes = textOrEmpty(etNotes);
        String emoji = charSeqOr(ivTaskEmoji != null ? ivTaskEmoji.getText() : null, "📝");

        // If you later add a "Select Days" UI, put the chosen days here (e.g., ["Mon","Wed"])
        java.util.List<String> repeatDays = null;

        // Format calendar values to match your schema
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault());
        java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("hh:mma", java.util.Locale.getDefault());

        String startDate = df.format(startCalendar.getTime());
        String startTime = tf.format(startCalendar.getTime());
        String endDate   = df.format(endCalendar.getTime());
        String endTime   = tf.format(endCalendar.getTime());

        // Build Task model
        Task t = new Task();
        t.setName(name);
        t.setEmoji(emoji);
        t.setRepeatType(repeatType);
        t.setRepeatDays(repeatDays);
        t.setStartDate(startDate);
        t.setStartTime(startTime);
        t.setEndDate(endDate);
        t.setEndTime(endTime);
        t.setTaskType(taskType);
        t.setNotes(notes);

        // Firestore create (UI will update via your realtime listener)
        new TaskRepository().create(t);

        Toast.makeText(requireContext(), "Task added", Toast.LENGTH_SHORT).show();
        androidx.navigation.fragment.NavHostFragment.findNavController(this).navigateUp();
    }

    // ---------------- helpers ----------------

    private static String textOrEmpty(EditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }

    private static String spinnerTextOr(Spinner sp, String fallback) {
        if (sp == null) return fallback;
        Object sel = sp.getSelectedItem();
        return (sel != null) ? sel.toString() : fallback;
    }

    private static String charSeqOr(CharSequence cs, String fallback) {
        return (cs != null && cs.length() > 0) ? cs.toString() : fallback;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (emojiRunnable != null) emojiHandler.removeCallbacks(emojiRunnable);
    }
}
