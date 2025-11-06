package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class AboutFragment extends Fragment {

    public AboutFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        ImageView back   = root.findViewById(R.id.backButton);
        View rowPrivacy  = root.findViewById(R.id.rowPrivacy);
        View rowTerms    = root.findViewById(R.id.rowTerms);

        back.setOnClickListener(v -> {
            NavController nav = Navigation.findNavController(v);

            // Try to pop back to Settings if it's already in the stack
            boolean popped = nav.popBackStack(R.id.settingsFragment, false);

            // If Settings isn't in the back stack, navigate there explicitly
            if (!popped) {
                try {
                    // Preferred: use the explicit action from About -> Settings
                    nav.navigate(R.id.action_aboutFragment_to_settingsFragment);
                } catch (IllegalArgumentException actionMissing) {
                    // Fallbacks in case the action ID isn't available at runtime:
                    try {
                        // If you have a global destination or graph can resolve dest id
                        nav.navigate(R.id.settingsFragment);
                    } catch (Exception ignore) {
                        // Last resort: just go up
                        nav.navigateUp();
                    }
                }
            }
        });

        rowPrivacy.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_aboutFragment_to_privacyFragment)
        );

        rowTerms.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_aboutFragment_to_termsOfUseFragment)
        );
    }
}
