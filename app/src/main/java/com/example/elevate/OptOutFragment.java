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

public class OptOutFragment extends Fragment {

    private NavController navC;

    public OptOutFragment() { }

    public static OptOutFragment newInstance(String param1, String param2) {
        OptOutFragment fragment = new OptOutFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_opt_out, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        Button nextButton = view.findViewById(R.id.btn_next);
        nextButton.setOnClickListener(v -> saveNoneAndGo());
    }

    private void saveNoneAndGo() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Persist 'none' + onboardingComplete
        Map<String, Object> data = new HashMap<>();
        data.put("notifPref", "none");
        data.put("onboardingComplete", true);

        // Disable any schedules locally
        new NotificationsManager(requireContext()).disableNotifications();

        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (navC != null) navC.navigate(R.id.action_optOutFragment_to_assessmentFragment);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
