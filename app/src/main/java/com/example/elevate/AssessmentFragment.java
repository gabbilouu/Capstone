package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class AssessmentFragment extends Fragment {

    private ImageView emojiVeryHappy, emojiHappy, emojiNeutral, emojiSad, emojiVerySad;
    private Button nextButton;
    private int selectedMood = -1;
    private NavController navC;

    public AssessmentFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assessment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        // Initialize emoji views
        emojiVeryHappy = view.findViewById(R.id.emojiVeryHappy);
        emojiHappy = view.findViewById(R.id.emojiHappy);
        emojiNeutral = view.findViewById(R.id.emojiNeutral);
        emojiSad = view.findViewById(R.id.emojiSad);
        emojiVerySad = view.findViewById(R.id.emojiVerySad);
        nextButton = view.findViewById(R.id.nextButton);

        // Emoji click listeners
        emojiVeryHappy.setOnClickListener(v -> selectMood(0));
        emojiHappy.setOnClickListener(v -> selectMood(1));
        emojiNeutral.setOnClickListener(v -> selectMood(2));
        emojiSad.setOnClickListener(v -> selectMood(3));
        emojiVerySad.setOnClickListener(v -> selectMood(4));

        // Next button
        nextButton.setOnClickListener(v -> {
            if (selectedMood == -1) {
                Toast.makeText(requireContext(), "Please select a mood first.", Toast.LENGTH_SHORT).show();
            } else {
                // Navigate to LoginStreakFragment
                navC.navigate(R.id.action_assessmentFragment_to_loginStreakFragment);
            }
        });
    }

    private void selectMood(int mood) {
        selectedMood = mood;

        // Reset alpha for all
        emojiVeryHappy.setAlpha(0.5f);
        emojiHappy.setAlpha(0.5f);
        emojiNeutral.setAlpha(0.5f);
        emojiSad.setAlpha(0.5f);
        emojiVerySad.setAlpha(0.5f);

        // Highlight selected mood
        switch (mood) {
            case 0:
                emojiVeryHappy.setAlpha(1f);
                break;
            case 1:
                emojiHappy.setAlpha(1f);
                break;
            case 2:
                emojiNeutral.setAlpha(1f);
                break;
            case 3:
                emojiSad.setAlpha(1f);
                break;
            case 4:
                emojiVerySad.setAlpha(1f);
                break;
        }
    }
}
