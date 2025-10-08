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

public class NewTaskFragment extends Fragment {

    private EditText etTaskName, etNotes;
    private Spinner spinnerRepeat, spinnerTaskType;
    private Button btnSave, btnCancel;
    private FirebaseFirestore db;

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
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);

        db = FirebaseFirestore.getInstance();

        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void saveTask() {
        String name = etTaskName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a task name", Toast.LENGTH_SHORT).show();
            return;
        }

        Task task = new Task();
        task.setName(name);
        task.setNotes(etNotes.getText().toString());
        task.setRepeatType(spinnerRepeat.getSelectedItem().toString());
        task.setTaskType(spinnerTaskType.getSelectedItem().toString());
        task.setCompleted(false);
        task.setEmoji("📝"); // default emoji

        db.collection("tasks").add(task)
                .addOnSuccessListener(documentReference -> Toast.makeText(requireContext(), "Task added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        requireActivity().onBackPressed();
    }
}
