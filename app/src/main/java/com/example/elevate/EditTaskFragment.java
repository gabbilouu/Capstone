package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

public class EditTaskFragment extends Fragment {

    private EditText etTaskName, etNotes;
    private Spinner spinnerRepeat, spinnerTaskType;
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
        btnSave = view.findViewById(R.id.btnAdd);
        btnCancel = view.findViewById(R.id.btnCancel);

        db = FirebaseFirestore.getInstance();

        // Get task from bundle
        if (getArguments() != null) {
            task = (Task) getArguments().getSerializable("task");
            taskId = getArguments().getString("taskId");
            prefillData();
        }

        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void prefillData() {
        if (task == null) return;
        etTaskName.setText(task.getName());
        etNotes.setText(task.getNotes());
        // Spinner selection code here (optional: match strings)
    }

    private void saveTask() {
        if (task == null || taskId == null) return;

        task.setName(etTaskName.getText().toString().trim());
        task.setNotes(etNotes.getText().toString());
        task.setRepeatType(spinnerRepeat.getSelectedItem().toString());
        task.setTaskType(spinnerTaskType.getSelectedItem().toString());

        db.collection("tasks").document(taskId).set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Task updated", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
