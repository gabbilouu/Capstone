package com.example.elevate;

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

        // Click listener for options
        View.OnClickListener optionClickListener = v -> {
            // Reset previous selection
            if (selectedOption != null) {
                selectedOption.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            }
            // Highlight current selection
            selectedOption = (Button) v;
            selectedOption.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        };

        option1.setOnClickListener(optionClickListener);
        option2.setOnClickListener(optionClickListener);
        option3.setOnClickListener(optionClickListener);
        option4.setOnClickListener(optionClickListener);
        option5.setOnClickListener(optionClickListener);

        // Next button
        nextButton.setOnClickListener(v -> {
            if (selectedOption == null) {
                Toast.makeText(getContext(), "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }
            navC.navigate(R.id.action_questionaireFragment_to_thanksFragment);
        });
    }
}
