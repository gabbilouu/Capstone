package com.example.elevate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class OptOutFragment extends Fragment {

    private NavController navC;

    public OptOutFragment() {
        // Required empty public constructor
    }

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
        nextButton.setOnClickListener(v -> {
            if (navC != null) {
                navC.navigate(R.id.action_optOutFragment_to_assessmentFragment);
            }
        });
    }
}
