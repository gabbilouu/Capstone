package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

        ImageView back = root.findViewById(R.id.backButton);
        View rowPrivacy = root.findViewById(R.id.rowPrivacy);
        View rowTerms   = root.findViewById(R.id.rowTerms);

        back.setOnClickListener(v -> {
            androidx.navigation.NavController nav = androidx.navigation.Navigation.findNavController(v);
            // Try to pop back to Settings if it's already in the stack…
            boolean popped = nav.popBackStack(R.id.settingsFragment, false);
            // …otherwise navigate there explicitly (requires an action in your nav graph).
            if (!popped) {
                nav.navigate(R.id.action_aboutFragment_to_settingsFragment);
            }
        });

        rowPrivacy.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v)
                        .navigate(R.id.action_aboutFragment_to_privacyFragment)
        );

        rowTerms.setOnClickListener(v ->
                androidx.navigation.Navigation.findNavController(v)
                        .navigate(R.id.action_aboutFragment_to_termsOfUseFragment)
        );
    }

}
