package com.example.elevate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class StartingFragment extends Fragment {

    private NavController navC;

    public StartingFragment() {
        // Required empty public constructor
    }

    public static StartingFragment newInstance(String param1, String param2) {
        StartingFragment fragment = new StartingFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // handle parameters if needed
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_starting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get NavController from this fragment's view
        navC = Navigation.findNavController(view);

        // Find buttons in the layout
        Button signUpButton = view.findViewById(R.id.btn_sign_up);
        Button loginButton = view.findViewById(R.id.btn_login);

        // Set click listeners
        signUpButton.setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_startingFragment_to_SignUpFragment);
            } else {
                Log.e("StartingFragment", "NavController is null!");
            }
        });

        loginButton.setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_startingFragment_to_loginpageFragment);
            } else {
                Log.e("StartingFragment", "NavController is null!");
            }
        });
    }
}
