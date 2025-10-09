package com.example.elevate;

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

public class EditTaskFragment extends Fragment {

    private EditText etTaskName, etNotes;
    private Spinner spinnerRepeat, spinnerTaskType;
    private TextView ivTaskEmoji;
    private Button btnSave, btnCancel;
    private FirebaseFirestore db;
    private Task task;
    private String taskId;

    public EditTaskFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_task, container, false);

        etTaskName = view.findViewById(R.id.etTaskName);
        etNotes = view.findViewById(R.id.etNotes);
        spinnerRepeat = view.findViewById(R.id.spinnerRepeat);
        spinnerTaskType = view.findViewById(R.id.spinnerTaskType);
        ivTaskEmoji = view.findViewById(R.id.ivTaskEmoji);
        btnSave = view.findViewById(R.id.btnAdd);
        btnCancel = view.findViewById(R.id.btnCancel);

        db = FirebaseFirestore.getInstance();

        // Get task from bundle
        if (getArguments() != null) {
            task = (Task) getArguments().getSerializable("task");
            taskId = getArguments().getString("taskId");
            prefillData();
        }

        // ✅ Reuse the same AI Emoji generator (with cache)
        AIEmojiGenerator aiEmoji = new AIEmojiGenerator(requireContext());

        etTaskName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                String selectedType = spinnerTaskType.getSelectedItem() != null
                        ? spinnerTaskType.getSelectedItem().toString()
                        : "";
                if (text.isEmpty()) {
                    ivTaskEmoji.setText("📝");
                    return;
                }

                aiEmoji.generateEmoji(text, selectedType, new AIEmojiGenerator.EmojiCallback() {
                    @Override
                    public void onEmojiGenerated(String emoji) {
                        requireActivity().runOnUiThread(() -> ivTaskEmoji.setText(emoji));
                    }

                    @Override
                    public void onError(Exception e) {
                        requireActivity().runOnUiThread(() -> ivTaskEmoji.setText("📝"));
                    }
                });
            }
        });

// Also update emoji when task type changes
        spinnerTaskType.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(String selectedType) {
                String taskName = etTaskName.getText().toString();
                if (taskName.isEmpty()) return;

                aiEmoji.generateEmoji(taskName, selectedType, new AIEmojiGenerator.EmojiCallback() {
                    @Override
                    public void onEmojiGenerated(String emoji) {
                        requireActivity().runOnUiThread(() -> ivTaskEmoji.setText(emoji));
                    }

                    @Override
                    public void onError(Exception e) {
                        requireActivity().runOnUiThread(() -> ivTaskEmoji.setText("📝"));
                    }
                });
            }
        });


        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void prefillData() {
        if (task == null) return;

        etTaskName.setText(task.getName());
        etNotes.setText(task.getNotes());
        ivTaskEmoji.setText(task.getEmoji());

        // Optional: set spinners to previous selections
        // (If your spinner items match string names)
        // Utility method to match and set spinner selection
        SpinnerUtils.setSpinnerSelection(spinnerRepeat, task.getRepeatType());
        SpinnerUtils.setSpinnerSelection(spinnerTaskType, task.getTaskType());
    }

    private void saveTask() {
        if (task == null || taskId == null) return;

        task.setName(etTaskName.getText().toString().trim());
        task.setNotes(etNotes.getText().toString());
        task.setRepeatType(spinnerRepeat.getSelectedItem().toString());
        task.setTaskType(spinnerTaskType.getSelectedItem().toString());
        task.setEmoji(ivTaskEmoji.getText().toString());

        db.collection("tasks").document(taskId).set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
