package com.example.elevate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginPageFragment extends Fragment {

    private FirebaseAuth mAuth;
    private NavController navController;

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_ONBOARDING_COMPLETE_PREFIX = "onboarding_complete_"; // + uid

    // Assessment gating (per-user) via DailyGate
    private static final String GATE_KEY_ASSESSMENT = "assessment_done";

    // General settings prefs + keys (must match GeneralSettingsFragment)
    private static final String GS_PREFS        = "general_settings";
    private static final String KEY_DAILY_MOOD  = "daily_mood_enabled";
    private static final String KEY_WEEKLY_MOOD = "weekly_mood_enabled";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_page, container, false);

        mAuth = FirebaseAuth.getInstance();
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        EditText loginEmail = view.findViewById(R.id.email);
        EditText loginPassword = view.findViewById(R.id.password);
        Button loginButton = view.findViewById(R.id.loginButton);
        ImageView backArrow = view.findViewById(R.id.back_button);
        TextView tvForgotPassword = view.findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setPaintFlags(
                tvForgotPassword.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG
        );

        Button resendVerificationButton = view.findViewById(R.id.resendVerificationButton); // still there if you want it later

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Auto-login for verified users
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            checkOnboardingAndNavigate(currentUser, prefs);
        }

        // Login click
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
                                    checkOnboardingAndNavigate(user, prefs);
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
                                                    "Failed to resend verification email: " +
                                                            (emailTask.getException() != null
                                                                    ? emailTask.getException().getMessage()
                                                                    : "unknown error"),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    mAuth.signOut();
                                }
                            }
                        } else {
                            Toast.makeText(getActivity(),
                                    "Login failed: " +
                                            (task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "unknown error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Forgot password click
        tvForgotPassword.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                loginEmail.setError("Enter your email to reset password");
                loginEmail.requestFocus();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(),
                                    "If an account exists for " + email +
                                            ", you'll receive an email with password reset instructions.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getActivity(),
                                    "Failed to send reset email: " +
                                            (task.getException() != null
                                                    ? task.getException().getMessage()
                                                    : "unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        backArrow.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    /**
     * Central gate:
     *  If onboardingComplete == false/missing  -> Welcome
     *  Else if assessment due (daily or weekly) -> Assessment
     *  Else                                    -> Home
     */
    private void checkOnboardingAndNavigate(@NonNull FirebaseUser user,
                                            @NonNull SharedPreferences prefs) {
        String uid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener((DocumentSnapshot snap) -> {
                    boolean onboardingComplete = snap != null && snap.exists()
                            && Boolean.TRUE.equals(snap.getBoolean("onboardingComplete"));

                    // Cache for offline fallback
                    prefs.edit()
                            .putBoolean(KEY_ONBOARDING_COMPLETE_PREFIX + uid, onboardingComplete)
                            .apply();

                    boolean shouldShowAssessment = onboardingComplete && shouldShowAssessment(user);

                    if (!onboardingComplete) {
                        navController.navigate(R.id.action_LoginPageFragment_to_welcomeFragment);
                    } else if (shouldShowAssessment) {
                        navController.navigate(R.id.action_LoginPageFragment_to_assessmentFragment);
                    } else {
                        navController.navigate(R.id.action_LoginPageFragment_to_homePageFragment);
                    }
                })
                .addOnFailureListener(e -> {
                    // Offline / error -> fallback to cached value (default false)
                    boolean onboardingComplete = prefs.getBoolean(
                            KEY_ONBOARDING_COMPLETE_PREFIX + uid, false);

                    boolean shouldShowAssessment = onboardingComplete && shouldShowAssessment(user);

                    if (!onboardingComplete) {
                        navController.navigate(R.id.action_LoginPageFragment_to_welcomeFragment);
                    } else if (shouldShowAssessment) {
                        navController.navigate(R.id.action_LoginPageFragment_to_assessmentFragment);
                    } else {
                        navController.navigate(R.id.action_LoginPageFragment_to_homePageFragment);
                    }
                });
    }

    /**
     * Decide whether the assessment is due, based on general settings + DailyGate.
     * - Daily mode: once per *day*
     * - Weekly mode: once per *week*
     * - If both off: never require the assessment
     */
    private boolean shouldShowAssessment(@NonNull FirebaseUser user) {
        Context ctx = requireContext();

        SharedPreferences gs =
                ctx.getSharedPreferences(GS_PREFS, Context.MODE_PRIVATE);
        boolean weekly = gs.getBoolean(KEY_WEEKLY_MOOD, false);
        boolean daily  = gs.getBoolean(KEY_DAILY_MOOD, true);

        // Per-user gate key
        String gateKey = GATE_KEY_ASSESSMENT + "_" + user.getUid();

        if (weekly && !daily) {
            // Weekly mode: show if not done this week
            return !DailyGate.isDoneThisWeek(ctx, gateKey);
        } else if (daily) {
            // Daily mode (or both: we treat as daily)
            return !DailyGate.isDoneToday(ctx, gateKey);
        } else {
            // Both off → no required assessment
            return false;
        }
    }
}
