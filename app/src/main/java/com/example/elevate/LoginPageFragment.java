package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class LoginPageFragment extends Fragment {

    private FirebaseAuth mAuth;
    private NavController navController;
    private static final String PREFS_NAME = "UserPrefs";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_page, container, false);

        mAuth = FirebaseAuth.getInstance();
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        EditText loginEmail = view.findViewById(R.id.email);
        EditText loginPassword = view.findViewById(R.id.password);
        Button loginButton = view.findViewById(R.id.loginButton);
        ImageView backArrow = view.findViewById(R.id.backArrow);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Auto-login for verified users
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            handleFirstLoginNavigation(currentUser, prefs);
        }

        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                loginEmail.setError("Email required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                loginPassword.setError("Password required");
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    handleFirstLoginNavigation(user, prefs);
                                } else {
                                    Toast.makeText(getActivity(),
                                            "Email not verified. Check your inbox.",
                                            Toast.LENGTH_LONG).show();

                                    user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Toast.makeText(getActivity(),
                                                    "Verification email resent to " + user.getEmail(),
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(getActivity(),
                                                    "Failed to resend verification email: " + emailTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    mAuth.signOut();
                                }
                            }
                        } else {
                            Toast.makeText(getActivity(),
                                    "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        backArrow.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private void handleFirstLoginNavigation(FirebaseUser user, SharedPreferences prefs) {
        // Keys for first login
        String firstLoginKey = "firstLoginDone_" + user.getUid();
        boolean firstLoginDone = prefs.getBoolean(firstLoginKey, false);

        // Key for today's assessment
        String todayKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        boolean assessmentDoneToday = prefs.getBoolean("assessmentDone_" + user.getUid() + "_" + todayKey, false);

        if (!firstLoginDone) {
            prefs.edit().putBoolean(firstLoginKey, true).apply();
            navController.navigate(R.id.action_LoginPageFragment_to_welcomeFragment);
        } else if (!assessmentDoneToday) {
            navController.navigate(R.id.action_LoginPageFragment_to_assessmentFragment);
        } else {
            navController.navigate(R.id.action_LoginPageFragment_to_homePageFragment);
        }
    }


}
