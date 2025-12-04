package com.example.elevate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;

import java.util.concurrent.TimeUnit;

public final class AuthTwoWeekLogout {

    private static final long PERIOD_MS = TimeUnit.DAYS.toMillis(14);

    private AuthTwoWeekLogout() {}

    /** Signs out if the last sign-in was >= 14 days ago. Returns true if a sign-out occurred. */
    public static boolean signOutIfExpired() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return false;

        FirebaseUserMetadata md = user.getMetadata();
        if (md == null) return false;

        long anchor = Math.max(md.getLastSignInTimestamp(), md.getCreationTimestamp());
        long now = System.currentTimeMillis();

        if (now - anchor >= PERIOD_MS) {
            auth.signOut();
            return true;
        }
        return false;
    }
}
