package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class thanksFragment extends Fragment {

    private NavController navC;

    public thanksFragment() { }

    public static thanksFragment newInstance(String param1, String param2) {
        thanksFragment fragment = new thanksFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_thanks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navC = Navigation.findNavController(view);

        Button nextButton = view.findViewById(R.id.nextButtonT);
        nextButton.setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_thanksFragment_to_beginFragment);
            }
        });
    }
}
