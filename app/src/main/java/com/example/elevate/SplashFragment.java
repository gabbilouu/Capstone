package com.example.elevate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class SplashFragment extends Fragment {

    private NavController navC;

    public SplashFragment() {
        // Required empty public constructor
    }

    public static SplashFragment newInstance(String param1, String param2) {
        SplashFragment fragment = new SplashFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get NavController from this fragment's view
        navC = Navigation.findNavController(view);

        // Set click listener on the entire root view
        view.setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_splashFragment_to_startingFragment);
            } else {
                Log.e("SplashFragment", "NavController is null!");
            }
        });
    }
}
