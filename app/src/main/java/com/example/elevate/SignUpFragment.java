package com.example.elevate;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Pattern;

public class SignUpFragment extends Fragment {

    private NavController navC;
    private EditText emailInput; // email field can stay as EditText
    private AppCompatEditText passwordInput, confirmPasswordInput;
    private AppCompatImageButton btnTogglePassword, btnToggleConfirm;
    private ImageButton backButton;
    private Button signUpButton;
    private FirebaseAuth mAuth;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +        // at least one digit
                    "(?=.*[A-Z])" +        // at least one uppercase
                    "(?=.*[@#$%^&+=!])" +  // at least one special char
                    ".{8,}" +              // min length 8
                    "$");

    public SignUpFragment() {}

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

        backButton = view.findViewById(R.id.back_button);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        btnTogglePassword = view.findViewById(R.id.btn_toggle_password);
        btnToggleConfirm = view.findViewById(R.id.btn_toggle_confirm);
        signUpButton = view.findViewById(R.id.sign_up_button);

        // Start with passwords hidden
        passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
        confirmPasswordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

        wirePasswordToggle(passwordInput, btnTogglePassword,
                R.drawable.ic_visibility, R.drawable.ic_visibility_off);

        wirePasswordToggle(confirmPasswordInput, btnToggleConfirm,
                R.drawable.ic_visibility, R.drawable.ic_visibility_off);

        signUpButton.setOnClickListener(v -> validateAndSignUp());
        backButton.setOnClickListener(v -> navC.navigateUp());
    }

    private void wirePasswordToggle(AppCompatEditText et,
                                    AppCompatImageButton btn,
                                    int iconVisibleRes,
                                    int iconHiddenRes) {
        // Initial icon: hidden state
        btn.setImageResource(iconHiddenRes);
        btn.setContentDescription("Show password");

        btn.setOnClickListener(v -> {
            boolean isShowing = et.getTransformationMethod() instanceof HideReturnsTransformationMethod;
            if (isShowing) {
                // Hide
                et.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btn.setImageResource(iconHiddenRes);
                btn.setContentDescription("Show password");
            } else {
                // Show
                et.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btn.setImageResource(iconVisibleRes);
                btn.setContentDescription("Hide password");
            }
            // Keep cursor at end
            if (et.getText() != null) et.setSelection(et.getText().length());
        });
    }

    private void validateAndSignUp() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";
        String confirmPassword = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString() : "";

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

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                if (emailTask.isSuccessful()) {
                                    Toast.makeText(requireContext(),
                                            "Signup successful! Verification email sent to " + user.getEmail(),
                                            Toast.LENGTH_LONG).show();
                                    mAuth.signOut(); // sign out until verified
                                    navC.navigate(R.id.action_SignUpFragment_to_startingFragment);
                                } else {
                                    Toast.makeText(requireContext(),
                                            "Failed to send verification email: " + emailTask.getException().getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(requireContext(),
                                "Signup failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidUniversityEmail(String email) {
        return !TextUtils.isEmpty(email)
                && Patterns.EMAIL_ADDRESS.matcher(email).matches()
                && email.endsWith(".com"); // If you want .edu: change to .endsWith(".edu") or restrict to your domain
    }

    private boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
