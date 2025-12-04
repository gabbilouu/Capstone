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
import androidx.core.content.ContextCompat;

import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

// Firebase AI Logic SDK
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;

// Guava for ListenableFuture callbacks
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class QuestionnaireFragment extends Fragment {

    private NavController navC;
    private Button option1, option2, option3, option4, option5, nextButton;
    private Button selectedOption = null;

    private static final String PREFS_NAME = "UserChoicesPrefs";
    private static final String KEY_GOAL = "userGoal";
    private static final String KEY_AI_TASKS_CREATED = "ai_tasks_created";
    private static final String ARG_FROM_WELCOME = "from_welcome";

    private boolean fromWelcome = false;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    // Firebase AI model + executor for callbacks
    private GenerativeModelFutures aiModel;
    private Executor mainExecutor;

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

        // Executor for running callbacks on main thread
        mainExecutor = ContextCompat.getMainExecutor(requireContext());

        // Init Firebase AI Gemini model
        initAiModel();

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

            // Only auto-create starter tasks the first time
            if (!prefs.getBoolean(KEY_AI_TASKS_CREATED, false)) {
                nextButton.setEnabled(false);
                nextButton.setText("Creating your plan…");
                generateTasksForGoal(goal, prefs);
            } else {
                navigateAfterQuestionnaire();
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

    // -------------------- Firebase AI helpers --------------------

    private void initAiModel() {
        if (aiModel != null) return;

        // Initialize the Gemini Developer API backend service (Firebase AI Logic)
        // and create a GenerativeModel instance.
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash");

        // Java compatibility layer (ListenableFuture API)
        aiModel = GenerativeModelFutures.from(ai);
    }

    private void generateTasksForGoal(String goal, SharedPreferences prefs) {
        if (aiModel == null || user == null) {
            // Fallback: if AI not available, just continue
            nextButton.setEnabled(true);
            nextButton.setText("Next");
            navigateAfterQuestionnaire();
            return;
        }

        String promptText =
                "You are helping a user in a self-improvement & mental health app called Elevate.\n" +
                        "The user's main goal is:\n\"" + goal + "\"\n\n" +
                        "Create EXACTLY 5 small, concrete, healthy tasks that support this goal.\n" +
                        "Each task should be realistic for a busy college student.\n\n" +
                        "Return ONLY valid JSON (no markdown, no backticks, no explanation text).\n" +
                        "Format:\n" +
                        "[\n" +
                        "  {\n" +
                        "    \"name\": \"Short task name\",\n" +
                        "    \"emoji\": \"emoji character like 🧘 or 😌\",\n" +
                        "    \"repeatType\": \"Daily\" or \"Weekly\",\n" +
                        "    \"taskType\": \"short category like Sleep, Exercise, Stress, Mood\",\n" +
                        "    \"notes\": \"1–2 short sentences describing what to do\"\n" +
                        "  },\n" +
                        "  ...(total 5 objects)\n" +
                        "]";

        Content prompt = new Content.Builder()
                .addText(promptText)
                .build();

        ListenableFuture<GenerateContentResponse> future =
                aiModel.generateContent(prompt);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String text = result.getText();
                boolean ok = parseAndSaveTasksFromJson(text);

                if (ok) {
                    prefs.edit().putBoolean(KEY_AI_TASKS_CREATED, true).apply();
                }

                nextButton.setEnabled(true);
                nextButton.setText("Next");
                navigateAfterQuestionnaire();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                t.printStackTrace();
                Toast.makeText(requireContext(),
                        "Couldn't auto-create starter tasks, but you can add your own later.",
                        Toast.LENGTH_LONG).show();

                nextButton.setEnabled(true);
                nextButton.setText("Next");
                navigateAfterQuestionnaire();
            }
        }, mainExecutor);
    }

    private boolean parseAndSaveTasksFromJson(String jsonText) {
        if (user == null || jsonText == null) return false;

        try {
            // Try to isolate JSON array if extra text slips through
            String trimmed = jsonText.trim();
            int firstBracket = trimmed.indexOf('[');
            int lastBracket = trimmed.lastIndexOf(']');
            if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                trimmed = trimmed.substring(firstBracket, lastBracket + 1);
            }

            JSONArray array = new JSONArray(trimmed);
            if (array.length() == 0) return false;

            // Basic "today" defaults for dates/times
            Calendar now = Calendar.getInstance();
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat =
                    new SimpleDateFormat("hh:mma", Locale.getDefault());
            String todayDate = dateFormat.format(now.getTime());
            String nowTime = timeFormat.format(now.getTime());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                String name = obj.optString("name", "").trim();
                if (name.isEmpty()) continue;

                String emoji = obj.optString("emoji", "");
                String repeatType = obj.optString("repeatType", "Daily");
                String taskType = obj.optString("taskType", "General");
                String notes = obj.optString("notes", "");

                Task task = new Task();
                task.setName(name);
                task.setEmoji(emoji);
                task.setRepeatType(repeatType);
                task.setTaskType(taskType);
                task.setNotes(notes);
                task.setUserId(user.getUid());
                task.setCompleted(false);

                // Simple defaults; adjust if your TaskListFragment expects something else
                task.setStartDate(todayDate);
                task.setStartTime(nowTime);
                task.setEndDate(null);
                task.setEndTime(null);

                // Save to Firestore. Change path if you use users/{uid}/tasks instead.
                db.collection("tasks").add(task);
            }

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void navigateAfterQuestionnaire() {
        if (fromWelcome) {
            navC.navigate(R.id.action_questionnaireFragment_to_thanksFragment);
        } else {
            navC.navigate(R.id.action_questionnaireFragment_to_settingsFragment);
        }
    }
}
