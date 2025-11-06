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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class QuestionnaireFragment extends Fragment {

    private NavController navC;
    private Button option1, option2, option3, option4, option5, nextButton;
    private Button selectedOption = null;

    private static final String PREFS_NAME = "UserChoicesPrefs";
    private static final String KEY_GOAL = "userGoal";
    private static final String ARG_FROM_WELCOME = "from_welcome";
    private boolean fromWelcome = false;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    public QuestionnaireFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_questionnaire, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        if (getArguments() != null) {
            fromWelcome = getArguments().getBoolean(ARG_FROM_WELCOME, false);
        }

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        option1 = view.findViewById(R.id.option1);
        option2 = view.findViewById(R.id.option2);
        option3 = view.findViewById(R.id.option3);
        option4 = view.findViewById(R.id.option4);
        option5 = view.findViewById(R.id.option5);
        nextButton = view.findViewById(R.id.nextButton);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        String savedGoal = prefs.getString(KEY_GOAL, null);

        // Restore from local cache
        if (savedGoal != null) {
            Button[] options = {option1, option2, option3, option4, option5};
            for (Button btn : options) {
                if (btn.getText().toString().equals(savedGoal)) {
                    selectedOption = btn;
                    btn.setBackgroundResource(R.drawable.btn_white_pill_selected);
                }
            }
        }

        // Also try restoring from Firestore (in case user reinstalled or changed devices)
        if (user != null) {
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null && snap.exists()) {
                            String cloudGoal = snap.getString(KEY_GOAL);
                            if (cloudGoal != null && !cloudGoal.trim().isEmpty()) {
                                prefs.edit().putString(KEY_GOAL, cloudGoal).apply();
                                Button[] options = {option1, option2, option3, option4, option5};
                                for (Button btn : options) {
                                    if (btn.getText().toString().equals(cloudGoal)) {
                                        if (selectedOption != null) {
                                            selectedOption.setBackgroundResource(R.drawable.btn_white_pill);
                                        }
                                        selectedOption = btn;
                                        btn.setBackgroundResource(R.drawable.btn_white_pill_selected);
                                    }
                                }
                            }
                        }
                    });
        }

        View.OnClickListener optionClickListener = v -> {
            // Reset previous selection
            if (selectedOption != null) {
                selectedOption.setBackgroundResource(R.drawable.btn_white_pill);
            }
            // Highlight new selection
            selectedOption = (Button) v;
            selectedOption.setBackgroundResource(R.drawable.btn_white_pill_selected);

            String goal = selectedOption.getText().toString();
            prefs.edit().putString(KEY_GOAL, goal).apply();

            // Save immediately to Firestore (merge) so Settings can show it
            if (user != null) {
                Map<String, Object> data = new HashMap<>();
                data.put(KEY_GOAL, goal);
                db.collection("users").document(user.getUid())
                        .set(data, SetOptions.merge());
            }
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

            String goal = selectedOption.getText().toString();
            prefs.edit().putString(KEY_GOAL, goal).apply();

            if (user != null) {
                Map<String, Object> data = new HashMap<>();
                data.put(KEY_GOAL, goal);
                db.collection("users").document(user.getUid())
                        .set(data, SetOptions.merge());
            }

            if (fromWelcome) {
                navC.navigate(R.id.action_questionnaireFragment_to_thanksFragment);
            } else {
                navC.navigate(R.id.action_questionnaireFragment_to_settingsFragment);
            }
        });
    }

    public static QuestionnaireFragment newInstance(boolean fromWelcome) {
        QuestionnaireFragment fragment = new QuestionnaireFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FROM_WELCOME, fromWelcome);
        fragment.setArguments(args);
        return fragment;
    }
}
