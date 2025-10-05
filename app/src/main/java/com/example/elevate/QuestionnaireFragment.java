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

public class QuestionnaireFragment extends Fragment {

    private NavController navC;

    private Button option1, option2, option3, option4, option5, nextButton;
    private Button selectedOption = null;

    private static final String PREFS_NAME = "UserChoicesPrefs";
    private static final String KEY_GOAL = "userGoal";

    // ✅ Argument key to determine flow
    private static final String ARG_FROM_WELCOME = "from_welcome";
    private boolean fromWelcome = false;

    public QuestionnaireFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questionnaire, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        // ✅ Check if this fragment was opened from WelcomeFragment
        if (getArguments() != null) {
            fromWelcome = getArguments().getBoolean(ARG_FROM_WELCOME, false);
        }

        // Initialize buttons
        option1 = view.findViewById(R.id.option1);
        option2 = view.findViewById(R.id.option2);
        option3 = view.findViewById(R.id.option3);
        option4 = view.findViewById(R.id.option4);
        option5 = view.findViewById(R.id.option5);
        nextButton = view.findViewById(R.id.nextButton);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        String savedGoal = prefs.getString(KEY_GOAL, null);

        // Highlight previously selected option
        if (savedGoal != null) {
            if (option1.getText().toString().equals(savedGoal)) selectedOption = option1;
            else if (option2.getText().toString().equals(savedGoal)) selectedOption = option2;
            else if (option3.getText().toString().equals(savedGoal)) selectedOption = option3;
            else if (option4.getText().toString().equals(savedGoal)) selectedOption = option4;
            else if (option5.getText().toString().equals(savedGoal)) selectedOption = option5;

            if (selectedOption != null)
                selectedOption.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }

        View.OnClickListener optionClickListener = v -> {
            if (selectedOption != null) {
                selectedOption.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }
            selectedOption = (Button) v;
            selectedOption.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));

            prefs.edit().putString(KEY_GOAL, selectedOption.getText().toString()).apply();
        };

        option1.setOnClickListener(optionClickListener);
        option2.setOnClickListener(optionClickListener);
        option3.setOnClickListener(optionClickListener);
        option4.setOnClickListener(optionClickListener);
        option5.setOnClickListener(optionClickListener);

        nextButton.setOnClickListener(v -> {
            if (selectedOption == null) {
                Toast.makeText(getContext(), "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save selection
            prefs.edit().putString(KEY_GOAL, selectedOption.getText().toString()).apply();

            // ✅ Conditional navigation
            if (fromWelcome) {
                navC.navigate(R.id.action_questionnaireFragment_to_thanksFragment);
            } else {
                navC.navigate(R.id.action_questionnaireFragment_to_settingsFragment);
            }
        });
    }

    // ✅ Helper method to create instance with argument
    public static QuestionnaireFragment newInstance(boolean fromWelcome) {
        QuestionnaireFragment fragment = new QuestionnaireFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FROM_WELCOME, fromWelcome);
        fragment.setArguments(args);
        return fragment;
    }
}
