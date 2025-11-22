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

    // 3 second splash delay
    private static final long SPLASH_DELAY_MS = 3000L;

    private NavController navC;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean hasNavigated = false;

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
        try {
            AuthTwoWeekLogout.signOutIfExpired();
        } catch (Throwable ignored) {}

        // Single delayed navigation after the splash time
        handler.postDelayed(() -> {
            int dest = resolveDestination();
            safeNavigate(dest);
        }, SPLASH_DELAY_MS);
    }

    /**
     * Decide where to go after the splash delay:
     *  - Not signed in                       -> startingFragment
     *  - Signed in, gate not done today      -> assessmentFragment
     *  - Signed in, gate done today          -> homePageFragment
     *
     * FirebaseAuth.getInstance().getCurrentUser() will return the
     * currently authenticated user if they were logged in before.
     */
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

            NavOptions opts = new NavOptions.Builder()
                    // Pop splash off the back stack so back doesn't return here
                    .setPopUpTo(R.id.splashFragment, true)
                    .build();

            try {
                navC.navigate(destinationId, null, opts);
            } catch (Exception e) {
                Log.w(TAG, "navigate failed, trying plain navigate", e);
                try {
                    navC.navigate(destinationId);
                } catch (Exception ignored) {}
            }
        } else {
            Log.w(TAG, "Skipping navigate; destination already changed.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        navC = null;
        hasNavigated = false;
    }
}
