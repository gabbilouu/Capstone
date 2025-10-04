package com.example.elevate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class QuestionaireFragment extends Fragment {

    private NavController navC;

    private Button option1, option2, option3, option4, option5, nextButton;
    private Button selectedOption = null;

    // ✅ SharedPreferences keys
    private static final String PREFS_NAME = "UserChoicesPrefs";
    private static final String KEY_GOAL = "userGoal";

    public QuestionaireFragment() { }

    public static QuestionaireFragment newInstance(String param1, String param2) {
        QuestionaireFragment fragment = new QuestionaireFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questionaire, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        // Initialize buttons
        option1 = view.findViewById(R.id.option1);
        option2 = view.findViewById(R.id.option2);
        option3 = view.findViewById(R.id.option3);
        option4 = view.findViewById(R.id.option4);
        option5 = view.findViewById(R.id.option5);
        nextButton = view.findViewById(R.id.nextButton);

        // ✅ Load previous selection (if any)
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        String savedGoal = prefs.getString(KEY_GOAL, null);

        // ✅ Highlight the previously selected button
        if (savedGoal != null) {
            if (option1.getText().toString().equals(savedGoal)) selectedOption = option1;
            else if (option2.getText().toString().equals(savedGoal)) selectedOption = option2;
            else if (option3.getText().toString().equals(savedGoal)) selectedOption = option3;
            else if (option4.getText().toString().equals(savedGoal)) selectedOption = option4;
            else if (option5.getText().toString().equals(savedGoal)) selectedOption = option5;

            if (selectedOption != null)
                selectedOption.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }

        // ✅ Click listener for options
        View.OnClickListener optionClickListener = v -> {
            // Reset previous selection
            if (selectedOption != null) {
                selectedOption.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }
            // Highlight current selection
            selectedOption = (Button) v;
            selectedOption.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));

            // ✅ Save selected goal
            String selectedGoal = selectedOption.getText().toString();
            prefs.edit().putString(KEY_GOAL, selectedGoal).apply();
        };

        option1.setOnClickListener(optionClickListener);
        option2.setOnClickListener(optionClickListener);
        option3.setOnClickListener(optionClickListener);
        option4.setOnClickListener(optionClickListener);
        option5.setOnClickListener(optionClickListener);

        // ✅ "Next" button logic
        nextButton.setOnClickListener(v -> {
            if (selectedOption == null) {
                Toast.makeText(getContext(), "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Save again (to ensure it's persisted)
            String selectedGoal = selectedOption.getText().toString();
            prefs.edit().putString(KEY_GOAL, selectedGoal).apply();

            // ✅ Go back to SettingsFragment instead of ThanksFragment
            navC.navigate(R.id.action_questionaireFragment_to_settingsFragment);
        });
    }
}
