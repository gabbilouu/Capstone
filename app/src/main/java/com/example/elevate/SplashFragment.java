package com.example.elevate;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashFragment extends Fragment {

    private static final String TAG = "SplashFragment";
    private static final String GATE_KEY_ASSESSMENT = "assessment_done";
    private static final long SPLASH_DELAY_MS = 1000L;

    private NavController navC;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated = false;
    private FirebaseAuth.AuthStateListener authListener;

    public SplashFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);

        // Optional: auto sign-out if 2 weeks elapsed (no-op if you don't use it)
        try { AuthTwoWeekLogout.signOutIfExpired(); } catch (Throwable ignored) {}

        // React immediately if auth state flips while we're on splash (e.g., after delete)
        authListener = fbAuth -> {
            handler.removeCallbacksAndMessages(null);
            safeNavigate(resolveDestination());
        };
        FirebaseAuth.getInstance().addAuthStateListener(authListener);

        // Normal splash delay
        handler.postDelayed(() -> safeNavigate(resolveDestination()), SPLASH_DELAY_MS);
    }

    private int resolveDestination() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        boolean signedIn = (u != null && !u.isAnonymous());

        Log.d(TAG, "resolveDestination: user=" + (u == null ? "null" : u.getUid()) +
                " anon=" + (u != null && u.isAnonymous()));

        if (!signedIn) {
            // Not logged in → start/sign-in flow
            return R.id.startingFragment;
        }

        // Signed in → if first open today, go to assessment; otherwise home
        boolean doneToday = DailyGate.isDoneToday(requireContext(), GATE_KEY_ASSESSMENT);
        return doneToday ? R.id.homePageFragment : R.id.assessmentFragment;
    }

    private void safeNavigate(@IdRes int destinationId) {
        if (hasNavigated || navC == null) return;
        if (navC.getCurrentDestination() != null
                && navC.getCurrentDestination().getId() == R.id.splashFragment) {
            hasNavigated = true;

            // Pop splash off the back stack so back doesn't return here
            NavOptions opts = new NavOptions.Builder()
                    .setPopUpTo(R.id.splashFragment, true)
                    .build();

            try {
                navC.navigate(destinationId, null, opts);
            } catch (Exception e) {
                Log.w(TAG, "navigate failed, trying plain navigate", e);
                try { navC.navigate(destinationId); } catch (Exception ignored) {}
            }
        } else {
            Log.w(TAG, "Skipping navigate; destination already changed.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (authListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener);
            authListener = null;
        }
        navC = null;
        hasNavigated = false;
    }
}
