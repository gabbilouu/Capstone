package com.example.elevate;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.widget.EditText;
import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private NavController navC;

    // Streak keys
    private static final String PREFS_STREAK = "LoginStreakPrefs";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_LOGIN = "lastLogin";

    // Profile/user prefs
    private static final String PREFS_USER = "UserPrefs";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PRONOUN = "user_pronoun";
    private static final String KEY_USER_UNI = "user_university";
    private static final String KEY_USER_MAJOR = "user_major";
    private static final String KEY_USER_PHOTO_URI = "user_photo_uri";

    // Goals
    private static final String PREFS_GOAL = "LoginStreakPrefs";
    private static final String KEY_GOAL = "userGoal";

    private ImageView profileImage;
    private TextView profileName, profileDetails, userSince;

    private ActivityResultLauncher<String[]> pickImageLauncher;

    private Uri pendingPhotoUri = null;
    private AlertDialog editDialog = null;

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Persistable image picker
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    try {
                        requireContext().getContentResolver()
                                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    pendingPhotoUri = uri;
                    // If dialog is open, reflect preview immediately
                    if (editDialog != null && editDialog.isShowing()) {
                        ImageView iv = editDialog.findViewById(R.id.ivProfilePreview);
                        if (iv != null) iv.setImageURI(pendingPhotoUri);
                    }
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        profileImage = view.findViewById(R.id.profileImage);
        profileName  = view.findViewById(R.id.profileName);
        profileDetails = view.findViewById(R.id.profileDetails);
        userSince   = view.findViewById(R.id.userSince);

        // Default name setup (only if not stored)
        ensureDefaultNameOnce();

        // Apply UI from prefs
        applyProfileFromPrefs();

        // User since
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getMetadata() != null) {
            String signupDate = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(new Date(user.getMetadata().getCreationTimestamp()));
            userSince.setText("User Since: " + signupDate);
        } else {
            userSince.setText("User Since: Unknown");
        }

        // Pencil → edit dialog
        ImageButton editBtn = view.findViewById(R.id.btnEditProfile);
        editBtn.setOnClickListener(v -> openEditProfileDialog());

        // Streak + radar
        updateAndDisplayStreak();
        setupPersonalityRadarChart();

        // Bottom nav
        ImageButton taskButton = view.findViewById(R.id.TaskButton);
        ImageButton calendarButton = view.findViewById(R.id.CalendarButton);
        ImageButton homeButton = view.findViewById(R.id.HomeButton);
        taskButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_taskListFragment));
        calendarButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_eventFragment));
        homeButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_homePageFragment));

        // Settings buttons
        Button myGoalsButton = view.findViewById(R.id.myGoalsButton);
        Button moodReportButton = view.findViewById(R.id.moodReportButton);
        Button generalButton = view.findViewById(R.id.generalButton);
        Button aboutButton = view.findViewById(R.id.aboutButton);
        Button notificationsButton = view.findViewById(R.id.notificationsButton);
        Button faqButton = view.findViewById(R.id.faqButton);
        Button contactUsButton = view.findViewById(R.id.contactUsButton);

        myGoalsButton.setOnClickListener(v -> {
            SharedPreferences goalPrefs = requireContext().getSharedPreferences(PREFS_GOAL, 0);
            String currentGoal = goalPrefs.getString(KEY_GOAL, "No goal set");

            new AlertDialog.Builder(requireContext())
                    .setTitle("Your Goal")
                    .setMessage("You selected:\n\n" + currentGoal)
                    .setPositiveButton("Change Goal", (dialog, which) -> {
                        Bundle args = new Bundle();
                        args.putBoolean("from_welcome", false);
                        navC.navigate(R.id.action_settingsFragment_to_questionnaireFragment, args);
                    })
                    .setNegativeButton("Close", null)
                    .show();
        });

        generalButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_generalSettingsFragment));
        aboutButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_aboutFragment));
        notificationsButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_notificationsFragment));
        faqButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_FAQFragment));
        contactUsButton.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:elevatehealthapp6@gmail.com"));
            email.putExtra(Intent.EXTRA_SUBJECT, "Elevate App — Support");
            email.putExtra(Intent.EXTRA_TEXT, "Hi Elevate team,\n\n");
            try {
                startActivity(Intent.createChooser(email, "Send email"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "No email app found on this device.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* ---------------- Profile helpers ---------------- */

    private void ensureDefaultNameOnce() {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS_USER, 0);
        String stored = sp.getString(KEY_USER_NAME, null);
        if (stored != null && !stored.trim().isEmpty()) return;

        String fallback = "User";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                fallback = user.getDisplayName().trim();
            } else if (user.getEmail() != null) {
                String email = user.getEmail();
                int at = email.indexOf('@');
                fallback = (at > 0) ? email.substring(0, at) : email;
            }
        }
        sp.edit().putString(KEY_USER_NAME, fallback).apply();
    }

    private void applyProfileFromPrefs() {
        SharedPreferences sp = requireContext().getSharedPreferences(PREFS_USER, 0);

        String name = sp.getString(KEY_USER_NAME, "");
        String pronoun = sp.getString(KEY_USER_PRONOUN, "");
        String uni = sp.getString(KEY_USER_UNI, "");
        String major = sp.getString(KEY_USER_MAJOR, "");
        String photo = sp.getString(KEY_USER_PHOTO_URI, null);

        // Title line: "Name • Pronouns" (pronouns optional)
        String title = name == null ? "" : name;
        if (pronoun != null && !pronoun.trim().isEmpty()) {
            title += " • " + pronoun.trim();
        }
        profileName.setText(title);

        // Details lines: show whichever exist
        StringBuilder details = new StringBuilder();
        if (uni != null && !uni.trim().isEmpty()) details.append(uni.trim());
        if (major != null && !major.trim().isEmpty()) {
            if (details.length() > 0) details.append("\n");
            details.append(major.trim());
        }
        profileDetails.setText(details.toString());

        if (photo != null) {
            try {
                profileImage.setImageURI(Uri.parse(photo));
            } catch (Exception ignored) {}
        }
    }

    private void openEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null, false);

        ImageView iv = dialogView.findViewById(R.id.ivProfilePreview);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etPronouns = dialogView.findViewById(R.id.etPronouns);
        EditText etUniversity = dialogView.findViewById(R.id.etUniversity);
        EditText etMajor = dialogView.findViewById(R.id.etMajor);
        Button btnChangePhoto = dialogView.findViewById(R.id.btnChangePhoto);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        SharedPreferences sp = requireContext().getSharedPreferences(PREFS_USER, 0);
        String name = sp.getString(KEY_USER_NAME, "");
        String pronoun = sp.getString(KEY_USER_PRONOUN, "");
        String uni = sp.getString(KEY_USER_UNI, "");
        String major = sp.getString(KEY_USER_MAJOR, "");
        String photo = sp.getString(KEY_USER_PHOTO_URI, null);

        etName.setText(name);
        etPronouns.setText(pronoun);
        etUniversity.setText(uni);
        etMajor.setText(major);
        if (photo != null) {
            try { iv.setImageURI(Uri.parse(photo)); } catch (Exception ignored) {}
        }

        btnChangePhoto.setOnClickListener(v ->
                pickImageLauncher.launch(new String[]{"image/*"})
        );

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setView(dialogView);
        editDialog = b.create();
        editDialog.show();

        btnCancel.setOnClickListener(v -> editDialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPronoun = etPronouns.getText().toString().trim();
            String newUni = etUniversity.getText().toString().trim();
            String newMajor = etMajor.getText().toString().trim();

            SharedPreferences.Editor ed = sp.edit();
            ed.putString(KEY_USER_NAME, newName);
            ed.putString(KEY_USER_PRONOUN, newPronoun);
            ed.putString(KEY_USER_UNI, newUni);
            ed.putString(KEY_USER_MAJOR, newMajor);
            if (pendingPhotoUri != null) {
                ed.putString(KEY_USER_PHOTO_URI, pendingPhotoUri.toString());
            }
            ed.apply();

            // Reflect immediately
            applyProfileFromPrefs();
            pendingPhotoUri = null;
            editDialog.dismiss();
        });
    }

    /* ---------------- Streak + Chart ---------------- */

    private void updateAndDisplayStreak() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_STREAK, 0);
        int streak = prefs.getInt(KEY_STREAK, 0);
        String lastLoginStr = prefs.getString(KEY_LAST_LOGIN, "");

        Calendar today = Calendar.getInstance();
        Calendar lastLogin = Calendar.getInstance();

        if (!lastLoginStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                lastLogin.setTime(sdf.parse(lastLoginStr));
                long diffDays = (today.getTimeInMillis() - lastLogin.getTimeInMillis()) / (1000 * 60 * 60 * 24);
                if (diffDays > 1) streak = 0;
                else if (diffDays == 1) streak++;
            } catch (Exception ignored) {}
        } else {
            streak = 1;
        }

        prefs.edit()
                .putInt(KEY_STREAK, streak)
                .putString(KEY_LAST_LOGIN, new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(today.getTime()))
                .apply();

        TextView streakTextView = requireView().findViewById(R.id.streakText);
        streakTextView.setText(streak > 0 ? "🌱 " + streak + " day streak!" : "🌱 No streak yet");
    }

    private void setupPersonalityRadarChart() {
        RadarChart radarChart = requireView().findViewById(R.id.personalityRadarChart);
        SharedPreferences prefs = requireContext().getSharedPreferences("MoodPrefs", 0);

        String[] moods = {"Very Happy", "Happy", "Neutral", "Sad", "Very Sad"};
        int[] counts = new int[moods.length];

        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            String dayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.getTime());
            String mood = prefs.getString(dayKey, null);
            if (mood != null) {
                for (int j = 0; j < moods.length; j++) {
                    if (mood.equals(moods[j])) counts[j]++;
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        ArrayList<RadarEntry> entries = new ArrayList<>();
        for (int c : counts) entries.add(new RadarEntry(c));

        RadarDataSet dataSet = new RadarDataSet(entries, "Past 30 Days Mood");
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setFillColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(2f);

        RadarData data = new RadarData(dataSet);
        radarChart.setData(data);

        XAxis xAxis = radarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(moods));
        YAxis yAxis = radarChart.getYAxis();
        yAxis.setAxisMinimum(0f);

        radarChart.getDescription().setEnabled(false);
        radarChart.invalidate();
    }
}
