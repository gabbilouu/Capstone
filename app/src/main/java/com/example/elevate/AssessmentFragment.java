package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AssessmentFragment extends Fragment {

    private static final String GATE_KEY_ASSESSMENT = "assessment_done";

    // General settings prefs + keys (must match GeneralSettingsFragment)
    private static final String GS_PREFS        = "general_settings";
    private static final String KEY_DAILY_MOOD  = "daily_mood_enabled";
    private static final String KEY_WEEKLY_MOOD = "weekly_mood_enabled";

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
        emojiHappy     = view.findViewById(R.id.emojiHappy);
        emojiNeutral   = view.findViewById(R.id.emojiNeutral);
        emojiSad       = view.findViewById(R.id.emojiSad);
        emojiVerySad   = view.findViewById(R.id.emojiVerySad);
        nextButton     = view.findViewById(R.id.nextButton);

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
                Context ctx = requireContext();

                // Read current mood logging mode
                SharedPreferences gs =
                        ctx.getSharedPreferences(GS_PREFS, Context.MODE_PRIVATE);
                boolean weekly = gs.getBoolean(KEY_WEEKLY_MOOD, false);
                boolean daily  = gs.getBoolean(KEY_DAILY_MOOD, true);

                // Build a user-specific gate key so multiple accounts on one device don't collide.
                String gateKey = GATE_KEY_ASSESSMENT;
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    gateKey = gateKey + "_" + user.getUid();
                }

                if (weekly && !daily) {
                    // Weekly mode: mark gate for THIS WEEK
                    DailyGate.markDoneThisWeek(ctx, gateKey);
                } else {
                    // Default / daily: mark gate for TODAY
                    DailyGate.markDoneToday(ctx, gateKey);
                }

                // Optional: prevent double taps
                nextButton.setEnabled(false);

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
            case 0: emojiVeryHappy.setAlpha(1f); break;
            case 1: emojiHappy.setAlpha(1f); break;
            case 2: emojiNeutral.setAlpha(1f); break;
            case 3: emojiSad.setAlpha(1f); break;
            case 4: emojiVerySad.setAlpha(1f); break;
        }
    }
}
