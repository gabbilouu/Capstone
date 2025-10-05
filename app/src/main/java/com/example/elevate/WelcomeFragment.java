package com.example.elevate;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.Calendar;

public class WelcomeFragment extends Fragment {

    private EditText inputName, inputBirthday, inputOther;
    private Button nextButton;
    private NavController navC;

    public WelcomeFragment() { }

    public static WelcomeFragment newInstance(String param1, String param2) {
        WelcomeFragment fragment = new WelcomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        inputName = view.findViewById(R.id.inputName);
        inputBirthday = view.findViewById(R.id.inputBirthday);
        inputOther = view.findViewById(R.id.inputOther);
        nextButton = view.findViewById(R.id.nextButton);

        // Make birthday EditText open a DatePickerDialog
        inputBirthday.setFocusable(false);
        inputBirthday.setClickable(true);
        inputBirthday.setOnClickListener(v -> showDatePickerDialog());

        nextButton.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            String birthday = inputBirthday.getText().toString().trim();
            String other = inputOther.getText().toString().trim();

            // Require first and last name
            if (TextUtils.isEmpty(name) || !name.contains(" ")) {
                inputName.setError("Please enter both first and last name");
                inputName.requestFocus();
                return;
            }

            // Capitalize first letters of first and last name
            String[] nameParts = name.split("\\s+");
            StringBuilder formattedName = new StringBuilder();
            for (String part : nameParts) {
                if (part.length() > 0) {
                    formattedName.append(Character.toUpperCase(part.charAt(0)))
                            .append(part.substring(1).toLowerCase())
                            .append(" ");
                }
            }
            String finalName = formattedName.toString().trim();

            // Optional: format birthday if needed
            String formattedBirthday = birthday; // you can format this differently if you want

            // Create a bundle to pass to the next fragment
            Bundle bundle = new Bundle();
            bundle.putString("user_name", finalName);
            bundle.putString("user_birthday", formattedBirthday);
            bundle.putString("user_other", other);

            // ✅ Add flag to indicate coming from WelcomeFragment
            bundle.putBoolean("from_welcome", true);

            // Navigate to next fragment with the bundle
            navC.navigate(R.id.action_welcomeFragment_to_questionnaireFragment, bundle);

            // Optional toast
            Toast.makeText(getContext(), "Welcome, " + finalName + "!", Toast.LENGTH_SHORT).show();
        });


    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Month is zero-based
                    String formattedDate = String.format("%02d/%02d/%04d",
                            selectedMonth + 1, selectedDay, selectedYear);
                    inputBirthday.setText(formattedDate);
                }, year, month, day);

        datePickerDialog.show();
    }
}
