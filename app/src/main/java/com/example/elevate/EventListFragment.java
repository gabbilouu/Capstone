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
import android.widget.ImageButton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EventListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EventListFragment extends Fragment implements View.OnClickListener{

    NavController navC = null;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EventListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EventListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EventListFragment newInstance(String param1, String param2) {
        EventListFragment fragment = new EventListFragment();
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
        return inflater.inflate(R.layout.fragment_event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navC = Navigation.findNavController(view);
        ImageButton homeButton = view.findViewById(R.id.HomeButton);
        ImageButton checkButton = view.findViewById(R.id.CheckButton);
        ImageButton profileButton = view.findViewById(R.id.ProfileButton);
        if (homeButton != null) {
            homeButton.setOnClickListener(this);
        } else {
            Log.e("EventListFragment", "HomeButton not found!");
        }

        if (checkButton != null) {
            checkButton.setOnClickListener(this);
        } else {
            Log.e("EventListFragment", "CheckButton not found!");
        }

        if (profileButton != null) {
            profileButton.setOnClickListener(this);
        } else {
            Log.e("EventListFragment", "ProfileButton not found!");
        }
    }

    @Override
    public void onClick(View v) {
        if (navC != null) {
            if (v.getId() == R.id.HomeButton) {
                navC.navigate(R.id.action_eventListFragment_to_mainPageFragment);
            } else if (v.getId() == R.id.CheckButton) {
                navC.navigate(R.id.action_eventListFragment_to_taskListFragment);
            } else if (v.getId() == R.id.ProfileButton) {
                navC.navigate(R.id.action_eventListFragment_to_settingsFragment);
            }
        }
    }
}