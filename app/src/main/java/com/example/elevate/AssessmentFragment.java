package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AssessmentFragment extends Fragment {

    private ImageButton selectedEmoji = null; // Track selected emoji

    public AssessmentFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assessment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NavController navController = NavHostFragment.findNavController(this);

        // Emoji buttons
        ImageButton emojiVeryHappy = view.findViewById(R.id.emojiVeryHappy);
        ImageButton emojiHappy = view.findViewById(R.id.emojiHappy);
        ImageButton emojiNeutral = view.findViewById(R.id.emojiNeutral);
        ImageButton emojiSad = view.findViewById(R.id.emojiSad);
        ImageButton emojiVerySad = view.findViewById(R.id.emojiVerySad);

        final ImageButton[] emojis = {emojiVeryHappy, emojiHappy, emojiNeutral, emojiSad, emojiVerySad};

        for (ImageButton emoji : emojis) {
            emoji.setOnClickListener(v -> {
                if (selectedEmoji != null) {
                    selectedEmoji.setBackground(null); // remove previous highlight
                }
                selectedEmoji = emoji;
                selectedEmoji.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            });
        }

        // Next button
        Button nextButton = view.findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            if (selectedEmoji == null) {
                Toast.makeText(getContext(), "Please select an emoji before proceeding", Toast.LENGTH_SHORT).show();
                return;
            }

            // Determine mood string
            int id = selectedEmoji.getId();
            String mood;

            if (id == R.id.emojiVeryHappy) {
                mood = "Very Happy";
            } else if (id == R.id.emojiHappy) {
                mood = "Happy";
            } else if (id == R.id.emojiNeutral) {
                mood = "Neutral";
            } else if (id == R.id.emojiSad) {
                mood = "Sad";
            } else if (id == R.id.emojiVerySad) {
                mood = "Very Sad";
            } else {
                mood = "Unknown";
            }


            // Save today's mood to SharedPreferences
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);

            String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().getTime());

            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                // Save the mood
                prefs.edit().putString(todayKey + "_mood", mood).apply();
                // Mark assessment done for today
                prefs.edit().putBoolean("assessmentDone_" + userId + "_" + todayKey, true).apply();
            }

            // Optional: show confirmation
            Toast.makeText(getContext(), "Mood saved: " + mood, Toast.LENGTH_SHORT).show();

            // Navigate to streak page or next fragment
            Bundle bundle = new Bundle();
            bundle.putString("selectedMood", mood);
            navController.navigate(R.id.action_assessmentFragment_to_loginStreakFragment, bundle);
        });

    }
}
