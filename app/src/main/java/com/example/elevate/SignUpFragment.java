package com.example.elevate;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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

import java.util.regex.Pattern;

public class SignUpFragment extends Fragment {

    private NavController navC;
    private EditText emailInput, passwordInput;
    private Button signUpButton;
    private FirebaseAuth mAuth;

    // Password pattern: 8+ chars, 1 uppercase, 1 number, 1 special char
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[A-Z])" +         // at least 1 uppercase
                    "(?=.*[@#$%^&+=!])" +   // at least 1 special char
                    ".{8,}" +               // at least 8 characters
                    "$");

    public SignUpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);
        mAuth = FirebaseAuth.getInstance();

        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        signUpButton = view.findViewById(R.id.sign_up_button);

        signUpButton.setOnClickListener(v -> validateAndSignUp());
    }

    private void validateAndSignUp() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (!isValidUniversityEmail(email)) {
            emailInput.setError("Enter a valid university email");
            emailInput.requestFocus();
            return;
        }

        if (!isValidPassword(password)) {
            passwordInput.setError("Password must be 8+ chars, include 1 uppercase, 1 number, 1 special char");
            passwordInput.requestFocus();
            return;
        }

        // ✅ Create Firebase user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(getContext(),
                                    "Signup successful! Welcome " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        }
                        navC.navigate(R.id.action_SignUpFragment_to_welcomeFragment);
                    } else {
                        Toast.makeText(getContext(),
                                "Signup failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidUniversityEmail(String email) {
        return !TextUtils.isEmpty(email) &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                email.endsWith(".edu");
    }

    private boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
