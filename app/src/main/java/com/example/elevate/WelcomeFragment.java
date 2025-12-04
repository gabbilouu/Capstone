package com.example.elevate;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WelcomeFragment extends Fragment {

    private EditText inputName, inputBirthday;
    private Button nextButton;
    private NavController navC;

    // Local prefs
    private static final String PREFS_USER = "UserPrefs";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_BIRTHDAY = "user_birthday";

    // Firestore fields
    private static final String FS_FIELD_NAME = "name";
    private static final String FS_FIELD_BDAY = "birthday";

    public WelcomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        inputName     = view.findViewById(R.id.inputName);
        inputBirthday = view.findViewById(R.id.inputBirthday);
        nextButton    = view.findViewById(R.id.nextButton);

        // Birthday opens a DatePickerDialog
        inputBirthday.setFocusable(false);
        inputBirthday.setClickable(true);
        inputBirthday.setOnClickListener(v -> showDatePickerDialog());

        nextButton.setOnClickListener(v -> {
            String name = textOf(inputName);
            String birthday = textOf(inputBirthday);

            // Require first + last name
            if (TextUtils.isEmpty(name) || !name.trim().contains(" ")) {
                inputName.setError("Please enter both first and last name");
                inputName.requestFocus();
                return;
            }

            // Capitalize each part
            String[] parts = name.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) {
                    sb.append(Character.toUpperCase(p.charAt(0)))
                            .append(p.length() > 1 ? p.substring(1).toLowerCase() : "")
                            .append(" ");
                }
            }
            String finalName = sb.toString().trim();

            // Persist locally (nice immediate UX and fallback)
            requireContext().getSharedPreferences(PREFS_USER, 0).edit()
                    .putString(KEY_USER_NAME, finalName)
                    .putString(KEY_USER_BIRTHDAY, birthday)
                    .apply();

            // Persist to Firestore so it survives logout/login on any device
            FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
            if (fu != null) {
                Map<String, Object> data = new HashMap<>();
                data.put(FS_FIELD_NAME, finalName);
                data.put(FS_FIELD_BDAY, birthday); // format MM/dd/yyyy as entered

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(fu.getUid())
                        .set(data, SetOptions.merge()) // don't clobber other fields (e.g., plantName)
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(),
                                        "Saved locally; cloud sync pending: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
            }

            // Pass along to next screen (optional)
            Bundle bundle = new Bundle();
            bundle.putString(KEY_USER_NAME, finalName);
            bundle.putString(KEY_USER_BIRTHDAY, birthday);
            bundle.putBoolean("from_welcome", true);

            navC.navigate(R.id.action_welcomeFragment_to_questionnaireFragment, bundle);
            Toast.makeText(getContext(), "Welcome, " + finalName + "!", Toast.LENGTH_SHORT).show();
        });
    }

    private String textOf(EditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);
        new DatePickerDialog(requireContext(),
                (view, yy, mm, dd) ->
                        inputBirthday.setText(String.format(Locale.getDefault(),
                                "%02d/%02d/%04d", mm + 1, dd, yy)),
                y, m, d).show();
    }
}
