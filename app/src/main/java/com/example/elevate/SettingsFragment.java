package com.example.elevate;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private NavController navC;

    // Streak keys
    private static final String PREFS_STREAK = "LoginStreakPrefs";
    private static final String KEY_STREAK = "streak";
    private static final String KEY_LAST_LOGIN = "lastLogin";

    // Profile/user prefs (local)
    private static final String PREFS_USER = "UserPrefs";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PRONOUN = "user_pronoun";
    private static final String KEY_USER_UNI = "user_university";
    private static final String KEY_USER_MAJOR = "user_major";
    private static final String KEY_USER_PHOTO_URI = "user_photo_uri";
    private static final String KEY_USER_BIRTHDAY = "user_birthday";

    // Goal prefs (local)
    private static final String PREFS_GOAL = "UserChoicesPrefs";
    private static final String KEY_GOAL = "userGoal";

    // Firestore fields
    private static final String FS_FIELD_NAME  = "name";
    private static final String FS_FIELD_BDAY  = "birthday";
    private static final String FS_FIELD_GOAL  = "userGoal";

    // Daily gate key (assessment)
    private static final String GATE_KEY_ASSESSMENT = "assessment_done";

    private ImageView profileImage;
    private TextView profileName, profileDetails, userSince;

    // Media pickers
    private ActivityResultLauncher<String[]> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private Uri pendingPhotoUri = null;
    private Uri capturedPhotoUri = null;

    private AlertDialog editDialog = null;

    private FirebaseAuth auth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pick from gallery
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;
                    try {
                        requireContext().getContentResolver()
                                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    pendingPhotoUri = uri;
                    if (editDialog != null && editDialog.isShowing()) {
                        ImageView iv = editDialog.findViewById(R.id.ivProfilePreview);
                        if (iv != null) iv.setImageURI(pendingPhotoUri);
                    }
                }
        );

        // Take a photo with camera
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && capturedPhotoUri != null) {
                        pendingPhotoUri = capturedPhotoUri;
                        if (editDialog != null && editDialog.isShowing()) {
                            ImageView iv = editDialog.findViewById(R.id.ivProfilePreview);
                            if (iv != null) iv.setImageURI(pendingPhotoUri);
                        }
                        Toast.makeText(requireContext(), "Photo captured", Toast.LENGTH_SHORT).show();
                    } else if (capturedPhotoUri != null) {
                        try { requireContext().getContentResolver().delete(capturedPhotoUri, null, null); } catch (Exception ignored) {}
                    }
                    capturedPhotoUri = null;
                }
        );
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        profileImage   = view.findViewById(R.id.profileImage);
        profileName    = view.findViewById(R.id.profileName);
        profileDetails = view.findViewById(R.id.profileDetails);
        userSince      = view.findViewById(R.id.userSince);

        ensureDefaultNameOnce();
        applyProfileFromPrefs();
        fetchProfileFromCloudThenApply();   // name / birthday
        fetchGoalFromCloudThenCache();      // userGoal

        if (firebaseUser != null && firebaseUser.getMetadata() != null) {
            String signupDate = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                    .format(new Date(firebaseUser.getMetadata().getCreationTimestamp()));
            userSince.setText("User Since: " + signupDate);
        } else {
            userSince.setText("User Since: Unknown");
        }

        ImageButton editBtn = view.findViewById(R.id.btnEditProfile);
        editBtn.setOnClickListener(v -> openEditProfileDialog());

        updateAndDisplayStreak();
        setupPersonalityRadarChart();

        ImageButton taskButton = view.findViewById(R.id.TaskButton);
        ImageButton calendarButton = view.findViewById(R.id.CalendarButton);
        ImageButton homeButton = view.findViewById(R.id.HomeButton);
        taskButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_taskListFragment));
        calendarButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_eventFragment));
        homeButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_homePageFragment));

        Button myGoalsButton = view.findViewById(R.id.myGoalsButton);
        Button generalButton = view.findViewById(R.id.generalButton);
        Button aboutButton = view.findViewById(R.id.aboutButton);
        Button notificationsButton = view.findViewById(R.id.notificationsButton);
        Button faqButton = view.findViewById(R.id.faqButton);
        Button contactUsButton = view.findViewById(R.id.contactUsButton);
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton);
        Button logoutButton = view.findViewById(R.id.logoutButton);
        Button deleteAccountButton = view.findViewById(R.id.deleteAccountButton);
        Button moodReportButton = view.findViewById(R.id.moodReportButton);

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
        moodReportButton.setOnClickListener(v ->
                navC.navigate(R.id.action_settingsFragment_to_moodReportFragment)
        );

        generalButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_generalSettingsFragment));
        aboutButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_aboutFragment));
        notificationsButton.setOnClickListener(v -> navC.navigate(R.id.action_settingsFragment_to_notificationsFragment));
        faqButton.setOnClickListener(v -> Toast.makeText(requireContext(), "FAQ coming soon", Toast.LENGTH_SHORT).show());
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

        // NEW: Change Password click
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        logoutButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Log out?")
                    .setMessage("You'll need to sign in again to access your data.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Log out", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        clearLocalData();
                        DailyGate.clearKey(requireContext(), GATE_KEY_ASSESSMENT);
                        navigateToSplashClearBackStack();
                        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    /* ---------- Firestore helpers ---------- */

    private com.google.firebase.firestore.DocumentReference userDoc() {
        if (firebaseUser == null) return null;
        return db.collection("users").document(firebaseUser.getUid());
    }

    private void fetchProfileFromCloudThenApply() {
        if (firebaseUser == null) return;
        var doc = userDoc();
        if (doc == null) return;

        doc.get().addOnSuccessListener(snap -> {
            if (snap != null && snap.exists()) {
                String cloudName = snap.getString(FS_FIELD_NAME);
                String cloudBday = snap.getString(FS_FIELD_BDAY);

                SharedPreferences sp = requireContext().getSharedPreferences(PREFS_USER, 0);
                SharedPreferences.Editor ed = sp.edit();

                if (cloudName != null && !cloudName.trim().isEmpty()) ed.putString(KEY_USER_NAME, cloudName);
                if (cloudBday != null && !cloudBday.trim().isEmpty()) ed.putString(KEY_USER_BIRTHDAY, cloudBday);
                ed.apply();

                applyProfileFromPrefs();
            }
        });
    }

    private void fetchGoalFromCloudThenCache() {
        if (firebaseUser == null) return;
        var doc = userDoc();
        if (doc == null) return;

        doc.get().addOnSuccessListener(snap -> {
            if (snap != null && snap.exists()) {
                String cloudGoal = snap.getString(FS_FIELD_GOAL);
                if (cloudGoal != null && !cloudGoal.trim().isEmpty()) {
                    SharedPreferences gp = requireContext().getSharedPreferences(PREFS_GOAL, 0);
                    gp.edit().putString(KEY_GOAL, cloudGoal).apply();
                }
            }
        });
    }

    private void saveProfileToCloud(String name, String birthday) {
        if (firebaseUser == null) return;
        var doc = userDoc();
        if (doc == null) return;

        Map<String, Object> data = new HashMap<>();
        if (name != null)     data.put(FS_FIELD_NAME, name);
        if (birthday != null) data.put(FS_FIELD_BDAY, birthday);

        doc.set(data, SetOptions.merge())
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Couldn't update cloud profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /* ---------------- NEW: Change Password dialog ---------------- */

    private void showChangePasswordDialog() {
        if (firebaseUser == null || firebaseUser.getEmail() == null) {
            Toast.makeText(requireContext(), "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null, false);

        TextInputLayout tilCurrent = dialogView.findViewById(R.id.tilCurrentPassword);
        TextInputLayout tilNew     = dialogView.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirm = dialogView.findViewById(R.id.tilConfirmPassword);

        TextInputEditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew     = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.etConfirmPassword);

        // Set up eye toggles using your icons
        setupPasswordToggle(tilCurrent, etCurrent);
        setupPasswordToggle(tilNew, etNew);
        setupPasswordToggle(tilConfirm, etConfirm);

        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setTitle("Change password")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null) // we override later
                .create();

        dlg.setOnShowListener(d -> {
            Button saveBtn = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String currentPass = etCurrent.getText() != null ? etCurrent.getText().toString() : "";
                String newPass     = etNew.getText() != null ? etNew.getText().toString() : "";
                String confirmPass = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

                boolean hasError = false;

                if (currentPass.isEmpty()) {
                    tilCurrent.setError("Required");
                    hasError = true;
                } else {
                    tilCurrent.setError(null);
                }

                if (newPass.length() < 6) {
                    tilNew.setError("At least 6 characters");
                    hasError = true;
                } else {
                    tilNew.setError(null);
                }

                if (!newPass.equals(confirmPass)) {
                    tilConfirm.setError("Passwords do not match");
                    hasError = true;
                } else {
                    tilConfirm.setError(null);
                }

                if (hasError) return;

                String email = firebaseUser.getEmail();
                if (email == null || email.isEmpty()) {
                    Toast.makeText(requireContext(),
                            "Missing email for account.", Toast.LENGTH_LONG).show();
                    return;
                }

                // Re-authenticate with current password first
                var credential = EmailAuthProvider.getCredential(email, currentPass);
                firebaseUser.reauthenticate(credential)
                        .addOnSuccessListener(unused -> {
                            firebaseUser.updatePassword(newPass)
                                    .addOnSuccessListener(v1 -> {
                                        Toast.makeText(requireContext(),
                                                "Password updated.", Toast.LENGTH_LONG).show();
                                        dlg.dismiss();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                            "Couldn't update password: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                        })
                        .addOnFailureListener(e -> {
                            tilCurrent.setError("Incorrect password");
                        });
            });
        });

        dlg.show();
    }

    /** Helper: toggles show/hide password with ic_visibility/ic_visibility_off */
    private void setupPasswordToggle(TextInputLayout til, TextInputEditText et) {
        if (til == null || et == null) return;

        til.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        til.setEndIconDrawable(R.drawable.ic_visibility_off);
        til.setTag(Boolean.FALSE); // FALSE = currently hidden

        til.setEndIconOnClickListener(v -> {
            boolean showing = Boolean.TRUE.equals(til.getTag());
            int cursor = et.getText() != null ? et.getText().length() : 0;

            if (showing) {
                // Switch to hidden
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                til.setEndIconDrawable(R.drawable.ic_visibility_off);
                til.setTag(Boolean.FALSE);
            } else {
                // Switch to visible
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                til.setEndIconDrawable(R.drawable.ic_visibility);
                til.setTag(Boolean.TRUE);
            }
            et.setSelection(cursor);
        });
    }

    /* ---------------- Delete Account flow ---------------- */

    private void showDeleteAccountDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Type DELETE to confirm");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(true);

        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setTitle("Delete account?")
                .setMessage("This will permanently delete your account and remove your data. This cannot be undone.\n\nConfirmation required:")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, w) -> performFullAccountDeletion())
                .create();

        dlg.setOnShowListener(di -> {
            final Button positive = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setEnabled(false);
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                    positive.setEnabled(s != null && "delete".equalsIgnoreCase(s.toString().trim()));
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        });

        dlg.show();
    }

    private void performFullAccountDeletion() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "No signed-in user.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog progress = new AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setView(new android.widget.ProgressBar(requireContext()))
                .setMessage("Deleting account…")
                .create();
        progress.show();

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        deleteAllUserData(db, uid, () -> {
            user.delete()
                    .addOnSuccessListener(aVoid -> {
                        FirebaseAuth.getInstance().signOut();
                        clearLocalData();
                        DailyGate.clearKey(requireContext(), GATE_KEY_ASSESSMENT);
                        progress.dismiss();
                        navigateToSplashClearBackStack();
                        Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        progress.dismiss();
                        if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(requireContext(),
                                    "Deletion requires recent login. Please sign in again and retry.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Couldn't delete account: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }, e -> {
            progress.dismiss();
            Toast.makeText(requireContext(),
                    "Couldn't remove cloud data: " + (e != null ? e.getMessage() : "Unknown error"),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void deleteAllUserData(FirebaseFirestore db, String uid, Runnable onDone, OnError onError) {
        Query tasksQ  = db.collection("tasks").whereEqualTo("userId", uid);
        Query eventsQ = db.collection("events").whereEqualTo("userId", uid);

        deleteQueryInBatches(db, tasksQ, () ->
                        deleteQueryInBatches(db, eventsQ, () ->
                                        deleteSubcollectionInBatches(db, uid, "tasks", () ->
                                                        deleteSubcollectionInBatches(db, uid, "events", () ->
                                                                        db.collection("users").document(uid).delete()
                                                                                .addOnSuccessListener(v -> onDone.run())
                                                                                .addOnFailureListener(onError::onError)
                                                                , onError)
                                                , onError)
                                , onError)
                , onError);
    }

    private void deleteQueryInBatches(FirebaseFirestore db, Query query, Runnable onDone, OnError onError) {
        query.limit(500).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        onDone.run();
                        return;
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(v -> deleteQueryInBatches(db, query, onDone, onError))
                            .addOnFailureListener(onError::onError);
                })
                .addOnFailureListener(onError::onError);
    }

    private void deleteSubcollectionInBatches(FirebaseFirestore db, String uid, String subcol,
                                              Runnable onDone, OnError onError) {
        Query q = db.collection("users").document(uid).collection(subcol).limit(500);
        q.get().addOnSuccessListener(snap -> {
            if (snap.isEmpty()) {
                onDone.run();
                return;
            }
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : snap) {
                batch.delete(doc.getReference());
            }
            batch.commit()
                    .addOnSuccessListener(v -> deleteSubcollectionInBatches(db, uid, subcol, onDone, onError))
                    .addOnFailureListener(onError::onError);
        }).addOnFailureListener(onError::onError);
    }

    private interface OnError { void onError(Exception e); }

    /* ---------------- Navigation helpers ---------------- */

    private void navigateToSplashClearBackStack() {
        if (navC == null) return;
        NavOptions opts = new NavOptions.Builder()
                .setPopUpTo(navC.getGraph().getStartDestinationId(), true)
                .build();
        try {
            navC.navigate(R.id.splashFragment, null, opts);
        } catch (Exception e) {
            try { navC.navigate(R.id.splashFragment); } catch (Exception ignored) {}
        }
    }

    /* ---------------- Local cleanup ---------------- */

    private void clearLocalData() {
        try {
            requireContext().getSharedPreferences(PREFS_USER, 0).edit().clear().apply();
            requireContext().getSharedPreferences(PREFS_STREAK, 0).edit().clear().apply();
            requireContext().getSharedPreferences("MoodPrefs", 0).edit().clear().apply();
            requireContext().getSharedPreferences(PREFS_GOAL, 0).edit().clear().apply();
            requireContext().getSharedPreferences("AppGates", 0).edit().clear().apply();

            ContentResolver resolver = requireContext().getContentResolver();
            for (UriPermission perm : resolver.getPersistedUriPermissions()) {
                int flags = 0;
                if (perm.isReadPermission())  flags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;
                if (perm.isWritePermission()) flags |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                try { resolver.releasePersistableUriPermission(perm.getUri(), flags); } catch (Exception ignored) {}
            }

            deleteRecursive(requireContext().getCacheDir());
        } catch (Exception ignored) {}
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
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

        String name   = sp.getString(KEY_USER_NAME, "");
        String pronoun= sp.getString(KEY_USER_PRONOUN, "");
        String uni    = sp.getString(KEY_USER_UNI, "");
        String major  = sp.getString(KEY_USER_MAJOR, "");
        String photo  = sp.getString(KEY_USER_PHOTO_URI, null);
        String bday   = sp.getString(KEY_USER_BIRTHDAY, "");

        String title = name == null ? "" : name;
        if (pronoun != null && !pronoun.trim().isEmpty()) {
            title += " • " + pronoun.trim();
        }
        profileName.setText(title);

        StringBuilder details = new StringBuilder();
        if (bday != null && !bday.trim().isEmpty()) {
            details.append("🎂 ").append(bday.trim());
        }
        if (uni != null && !uni.trim().isEmpty()) {
            if (details.length() > 0) details.append("\n");
            details.append(uni.trim());
        }
        if (major != null && !major.trim().isEmpty()) {
            if (details.length() > 0) details.append("\n");
            details.append(major.trim());
        }
        profileDetails.setText(details.toString());

        if (photo != null) {
            try { profileImage.setImageURI(Uri.parse(photo)); } catch (Exception ignored) {}
        }
    }

    private void openEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null, false);

        ImageView iv = dialogView.findViewById(R.id.ivProfilePreview);
        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etPronouns = dialogView.findViewById(R.id.etPronouns);
        EditText etUniversity = dialogView.findViewById(R.id.etUniversity);
        EditText etMajor = dialogView.findViewById(R.id.etMajor);
        EditText etBirthday = dialogView.findViewById(R.id.etBirthday);
        Button btnChangePhoto = dialogView.findViewById(R.id.btnChangePhoto);
        Button btnTakePhoto   = dialogView.findViewById(R.id.btnTakePhoto);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        SharedPreferences sp = requireContext().getSharedPreferences(PREFS_USER, 0);
        String name = sp.getString(KEY_USER_NAME, "");
        String pronoun = sp.getString(KEY_USER_PRONOUN, "");
        String uni = sp.getString(KEY_USER_UNI, "");
        String major = sp.getString(KEY_USER_MAJOR, "");
        String photo = sp.getString(KEY_USER_PHOTO_URI, null);
        String bday = sp.getString(KEY_USER_BIRTHDAY, "");

        etName.setText(name);
        etPronouns.setText(pronoun);
        etUniversity.setText(uni);
        etMajor.setText(major);

        if (etBirthday != null) {
            etBirthday.setText(bday);
            etBirthday.setFocusable(false);
            etBirthday.setClickable(true);
            etBirthday.setOnClickListener(v ->
                    new android.app.DatePickerDialog(requireContext(),
                            (view, yy, mm, dd) ->
                                    etBirthday.setText(String.format(Locale.getDefault(),
                                            "%02d/%02d/%04d", mm + 1, dd, yy)),
                            Calendar.getInstance().get(Calendar.YEAR),
                            Calendar.getInstance().get(Calendar.MONTH),
                            Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                            .show()
            );
        }

        if (photo != null) {
            try { iv.setImageURI(Uri.parse(photo)); } catch (Exception ignored) {}
        }

        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> pickImageLauncher.launch(new String[]{"image/*"}));
        }

        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> {
                try {
                    capturedPhotoUri = createImageCaptureUri();
                    if (capturedPhotoUri != null) takePhotoLauncher.launch(capturedPhotoUri);
                    else Toast.makeText(requireContext(), "Couldn't create camera file", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Camera unavailable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    capturedPhotoUri = null;
                }
            });
        }

        AlertDialog.Builder b = new AlertDialog.Builder(requireContext());
        b.setView(dialogView);
        editDialog = b.create();
        editDialog.show();

        btnCancel.setOnClickListener(v -> {
            if (capturedPhotoUri != null && (pendingPhotoUri == null || !capturedPhotoUri.equals(pendingPhotoUri))) {
                try { requireContext().getContentResolver().delete(capturedPhotoUri, null, null); } catch (Exception ignored) {}
            }
            capturedPhotoUri = null;
            editDialog.dismiss();
        });

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPronoun = etPronouns.getText().toString().trim();
            String newUni = etUniversity.getText().toString().trim();
            String newMajor = etMajor.getText().toString().trim();
            String newBday = (etBirthday != null && etBirthday.getText() != null)
                    ? etBirthday.getText().toString().trim() : bday;

            SharedPreferences.Editor ed = sp.edit();
            ed.putString(KEY_USER_NAME, newName);
            ed.putString(KEY_USER_PRONOUN, newPronoun);
            ed.putString(KEY_USER_UNI, newUni);
            ed.putString(KEY_USER_MAJOR, newMajor);
            ed.putString(KEY_USER_BIRTHDAY, newBday);
            if (pendingPhotoUri != null) ed.putString(KEY_USER_PHOTO_URI, pendingPhotoUri.toString());
            ed.apply();

            // Push to Firestore
            saveProfileToCloud(newName, newBday);

            applyProfileFromPrefs();
            pendingPhotoUri = null;
            capturedPhotoUri = null;
            editDialog.dismiss();
        });
    }

    private Uri createImageCaptureUri() throws IOException {
        File picturesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (picturesDir == null) throw new IOException("No external files dir");
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File image = new File(picturesDir, "profile_" + time + ".jpg");
        return FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                image
        );
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

        String[] moodNames  = {"Very Happy", "Happy", "Neutral", "Sad", "Very Sad"};
        String[] moodEmojis = {"😄", "😊", "😐", "☹️", "😞"};
        int[] counts = new int[moodNames.length];

        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            String dayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.getTime());
            String mood = prefs.getString(dayKey, null);
            if (mood != null) {
                for (int j = 0; j < moodNames.length; j++) {
                    if (mood.equals(moodNames[j])) counts[j]++;
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        ArrayList<RadarEntry> entries = new ArrayList<>();
        int maxCount = 0;
        for (int cVal : counts) {
            entries.add(new RadarEntry(cVal));
            if (cVal > maxCount) maxCount = cVal;
        }

        RadarDataSet dataSet = new RadarDataSet(entries, null);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(180);
        dataSet.setLineWidth(2f);
        dataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setFillColor(getResources().getColor(android.R.color.holo_blue_light));
        dataSet.setDrawValues(false);
        dataSet.setDrawHighlightCircleEnabled(false);

        RadarData data = new RadarData(dataSet);
        radarChart.setData(data);

        XAxis xAxis = radarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(moodEmojis));
        xAxis.setTextSize(11f);
        xAxis.setXOffset(0f);
        xAxis.setYOffset(0f);

        YAxis yAxis = radarChart.getYAxis();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(Math.max(5f, maxCount));
        yAxis.setLabelCount(5, true);
        yAxis.setDrawLabels(false);

        radarChart.getLegend().setEnabled(false);
        radarChart.getDescription().setEnabled(false);
        radarChart.setRotationEnabled(false);
        radarChart.setExtraOffsets(0f, 24f, 0f, 8f);
        radarChart.setMinOffset(0f);
        radarChart.setPadding(0, 0, 0, 0);
        radarChart.setWebLineWidth(1f);
        radarChart.setWebColor(getResources().getColor(android.R.color.darker_gray));
        radarChart.setWebAlpha(180);

        radarChart.invalidate();
    }
}
