package com.example.elevate;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment implements View.OnClickListener{

    NavController navC = null;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);
        ImageButton checkButton = view.findViewById(R.id.CheckButton);
        ImageButton listButton = view.findViewById(R.id.ListButton);
        ImageButton homeButton = view.findViewById(R.id.HomeButton);
        Button logoutButton = view.findViewById(R.id.LogOutButton);
        if (checkButton != null) {
            checkButton.setOnClickListener(this);
        } else {
            Log.e("SettingsFragment", "CheckButton not found!");
        }

        if (listButton != null) {
            listButton.setOnClickListener(this);
        } else {
            Log.e("SettingsFragment", "ListButton not found!");
        }

        if (homeButton != null) {
            homeButton.setOnClickListener(this);
        } else {
            Log.e("SettingsFragment", "HomeButton not found!");
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(this);
        } else {
            Log.e("SettingsFragment", "LogOutButton not found!");
        }
    }

    @Override
    public void onClick(View v) {
        if (navC != null) {
            if (v.getId() == R.id.CheckButton) {
                navC.navigate(R.id.action_settingsFragment_to_taskListFragment);
            } else if (v.getId() == R.id.ListButton) {
                navC.navigate(R.id.action_settingsFragment_to_eventListFragment);
            } else if (v.getId() == R.id.HomeButton) {
                navC.navigate(R.id.action_settingsFragment_to_mainPageFragment);
            } else if (v.getId() == R.id.LogOutButton) {
                navC.navigate(R.id.action_settingsFragment_to_loginFragment);
            }
        }
    }
}