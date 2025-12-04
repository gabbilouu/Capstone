package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class DailyWeeklyFragment extends Fragment {

    private NavController navC;
    private NotificationsManager notificationsManager; // Notification helper class

    public DailyWeeklyFragment() { }

    public static DailyWeeklyFragment newInstance(String param1, String param2) {
        DailyWeeklyFragment fragment = new DailyWeeklyFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_daily_weekly, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);
        notificationsManager = new NotificationsManager(requireContext()); // Initialize

        Button dailyButton = view.findViewById(R.id.dailyButton);
        Button weeklyButton = view.findViewById(R.id.weeklyButton);

        dailyButton.setOnClickListener(v -> savePrefAndContinue("daily"));
        weeklyButton.setOnClickListener(v -> savePrefAndContinue("weekly"));
    }

    private void savePrefAndContinue(String pref) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1) Persist onboarding completion + preference
        Map<String, Object> data = new HashMap<>();
        data.put("notifPref", pref);
        data.put("onboardingComplete", true);

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    // 2) Apply scheduling locally
                    notificationsManager.disableNotifications(); // clear any existing
                    if ("daily".equals(pref)) {
                        notificationsManager.enableDailyNotifications();
                        if (navC != null) navC.navigate(R.id.action_dailyWeeklyFragment_to_dailyFragment);
                    } else {
                        notificationsManager.enableWeeklyNotifications();
                        if (navC != null) navC.navigate(R.id.action_dailyWeeklyFragment_to_weeklyFragment);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
