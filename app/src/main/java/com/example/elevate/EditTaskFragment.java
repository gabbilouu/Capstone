// com/example/elevate/EditTaskFragment.java
package com.example.elevate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTaskFragment extends Fragment {

    private EditText etTaskName, etNotes;
    private Spinner spinnerRepeat, spinnerTaskType;
    private TextView ivTaskEmoji;
    private Button btnSave, btnCancel;
    private Button btnStartDate, btnStartTime, btnEndDate, btnEndTime;

    private final Calendar startCal = Calendar.getInstance();
    private final Calendar endCal   = Calendar.getInstance();
    private Task task;       // comes from args
    private String taskId;   // comes from args

    private final SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mma", Locale.getDefault());

    public EditTaskFragment() {}

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_task, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etTaskName      = view.findViewById(R.id.etTaskName);
        etNotes         = view.findViewById(R.id.etNotes);
        spinnerRepeat   = view.findViewById(R.id.spinnerRepeat);
        spinnerTaskType = view.findViewById(R.id.spinnerTaskType);
        ivTaskEmoji     = view.findViewById(R.id.ivTaskEmoji);
        btnSave         = view.findViewById(R.id.btnAdd);
        btnCancel       = view.findViewById(R.id.btnCancel);

        btnStartDate = view.findViewById(R.id.btnStartDate);
        btnStartTime = view.findViewById(R.id.btnStartTime);
        btnEndDate   = view.findViewById(R.id.btnEndDate);
        btnEndTime   = view.findViewById(R.id.btnEndTime);

        if (ivTaskEmoji != null && (ivTaskEmoji.getText() == null || ivTaskEmoji.getText().length() == 0)) {
            ivTaskEmoji.setText("📝");
        }

        // Adapters
        ArrayAdapter<CharSequence> repeatAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.repeat_options, android.R.layout.simple_spinner_item);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(repeatAdapter);

        ArrayAdapter<CharSequence> taskTypeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.task_type_options, R.layout.spinner_task_type_selected);
        taskTypeAdapter.setDropDownViewResource(R.layout.spinner_task_type_dropdown);
        spinnerTaskType.setAdapter(taskTypeAdapter);

        // Get args (from TaskList on click)
        if (getArguments() != null) {
            task = (Task) getArguments().getSerializable("task");
            taskId = getArguments().getString("taskId");
        }

        // Prefill all fields
        prefillData();

        // Pickers
        btnStartDate.setOnClickListener(v -> pickDate(startCal, btnStartDate));
        btnStartTime.setOnClickListener(v -> pickTime(startCal, btnStartTime));
        btnEndDate.setOnClickListener(v -> pickDate(endCal, btnEndDate));
        btnEndTime.setOnClickListener(v -> pickTime(endCal, btnEndTime));

        // Emoji updates (optional, same as NewTask)
        AIEmojiGenerator aiEmoji = new AIEmojiGenerator(requireContext());
        etTaskName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String name = s != null ? s.toString().trim() : "";
                if (name.isEmpty()) { ivTaskEmoji.setText("📝"); return; }
                String type = spinnerTextOr(spinnerTaskType, "General");
                aiEmoji.generateEmoji(name, type, new AIEmojiGenerator.EmojiCallback() {
                    @Override public void onEmojiGenerated(String emoji) { if (!isAdded()) return; ivTaskEmoji.setText(emoji); }
                    @Override public void onError(Exception e) { if (!isAdded()) return; ivTaskEmoji.setText("📝"); }
                });
            }
        });
        spinnerTaskType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                String name = textOrEmpty(etTaskName);
                if (name.isEmpty()) return;
                String type = spinnerTextOr(spinnerTaskType, "General");
                aiEmoji.generateEmoji(name, type, new AIEmojiGenerator.EmojiCallback() {
                    @Override public void onEmojiGenerated(String emoji) { if (!isAdded()) return; ivTaskEmoji.setText(emoji); }
                    @Override public void onError(Exception e) { if (!isAdded()) return; ivTaskEmoji.setText("📝"); }
                });
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void prefillData() {
        if (task == null) return;

        etTaskName.setText(nullToEmpty(task.getName()));
        etNotes.setText(nullToEmpty(task.getNotes()));
        ivTaskEmoji.setText(charSeqOr(task.getEmoji(), "📝"));

        // Spinners
        setSpinnerSelectionByText(spinnerRepeat, nullToEmpty(task.getRepeatType()));
        setSpinnerSelectionByText(spinnerTaskType, nullToEmpty(task.getTaskType()));

        // Dates/times → calendars → button labels
        parseIntoCalendar(startCal, task.getStartDate(), task.getStartTime());
        parseIntoCalendar(endCal,   task.getEndDate(),   task.getEndTime());
        updateButtons();
    }

    private void saveTask() {
        if (taskId == null) return;

        // Basic validation
        if (textOrEmpty(etTaskName).isEmpty()) {
            Toast.makeText(requireContext(), "Enter a task name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!endCal.after(startCal)) {
            Toast.makeText(requireContext(), "End must be after start", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read UI
        String name      = textOrEmpty(etTaskName);
        String notes     = textOrEmpty(etNotes);
        String emoji     = charSeqOr(ivTaskEmoji != null ? ivTaskEmoji.getText() : null, "📝");
        String repeatRaw = spinnerTextOr(spinnerRepeat, "None");
        // Map "None"/"Does not repeat" to "Daily" so PeriodKeyUtil logic still works,
        // or keep your exact value if you’ve adjusted PeriodKeyUtil for one-off tasks.
        String repeatType =
                (repeatRaw.equalsIgnoreCase("None") || repeatRaw.equalsIgnoreCase("Does not repeat"))
                        ? "Daily" : repeatRaw;
        String taskType  = spinnerTextOr(spinnerTaskType, "General");

        String startDate = dateFmt.format(startCal.getTime());
        String startTime = timeFmt.format(startCal.getTime());
        String endDate   = dateFmt.format(endCal.getTime());
        String endTime   = timeFmt.format(endCal.getTime());

        // If you support "Select Days", populate this from your UI (e.g., ["Mon","Wed"])
        java.util.List<String> repeatDays = task != null ? task.getRepeatDays() : null;

        // Build partial update map (don’t overwrite userId/completionKey/lastModified)
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", name);
        updates.put("emoji", emoji);
        updates.put("repeatType", repeatType);
        updates.put("repeatDays", repeatDays);
        updates.put("startDate", startDate);
        updates.put("startTime", startTime);
        updates.put("endDate", endDate);
        updates.put("endTime", endTime);
        updates.put("taskType", taskType);
        updates.put("notes", notes);

        new TaskRepository().update(taskId, updates);

        Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show();
        NavHostFragment.findNavController(this).navigateUp(); // TaskList will refresh via listener
    }


    // ---- date/time helpers ----
    private void pickDate(Calendar cal, Button btn) {
        new DatePickerDialog(requireContext(),
                (view, y, m, d) -> { cal.set(y, m, d); updateButtons(); },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
    private void pickTime(Calendar cal, Button btn) {
        new TimePickerDialog(requireContext(),
                (view, h, min) -> { cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); updateButtons(); },
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false
        ).show();
    }
    private void updateButtons() {
        btnStartDate.setText(dateFmt.format(startCal.getTime()));
        btnStartTime.setText(timeFmt.format(startCal.getTime()));
        btnEndDate.setText(dateFmt.format(endCal.getTime()));
        btnEndTime.setText(timeFmt.format(endCal.getTime()));
    }
    private void parseIntoCalendar(Calendar cal, String dateStr, String timeStr) {
        try {
            if (dateStr != null) cal.setTime(dateFmt.parse(dateStr));
            if (timeStr != null) {
                Calendar t = Calendar.getInstance();
                t.setTime(timeFmt.parse(timeStr));
                cal.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
                cal.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
            }
        } catch (ParseException ignored) {}
    }

    // ---- tiny utils ----
    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String textOrEmpty(EditText et) { return (et != null && et.getText()!=null) ? et.getText().toString().trim() : ""; }
    private static String spinnerTextOr(Spinner sp, String fb) { Object sel = sp!=null ? sp.getSelectedItem() : null; return sel!=null ? sel.toString() : fb; }
    private static String charSeqOr(CharSequence cs, String fb){ return cs!=null && cs.length()>0 ? cs.toString() : fb; }
    private static void setSpinnerSelectionByText(Spinner sp, String val){
        if (sp==null || val==null) return;
        SpinnerAdapter ad = sp.getAdapter(); if (ad==null) return;
        for (int i=0;i<ad.getCount();i++){ Object it = ad.getItem(i); if (it!=null && val.equals(it.toString())) { sp.setSelection(i); return; } }
    }
}
