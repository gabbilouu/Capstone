package com.example.elevate;

import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomePageFragment extends Fragment implements View.OnClickListener {

    private NavController navC = null;

    // Declare the ProgressBar to update dynamically
    private ProgressBar progressBar;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public HomePageFragment() {
        // Required empty public constructor
    }

    public static HomePageFragment newInstance(String param1, String param2) {
        HomePageFragment fragment = new HomePageFragment();
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
        return inflater.inflate(R.layout.fragment_home_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            navC.navigate(R.id.action_homePageFragment_to_LoginPageFragment);
            return;
        }
        // Find the ProgressBar by ID
        progressBar = view.findViewById(R.id.progressLevel);

        // Set initial progress (optional, already set in XML)
        progressBar.setProgress(40);

        // You can also initialize the progress bar as needed (for example, starting from 0)
        // progressBar.setProgress(0);

        // Start a background task to update the progress dynamically
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 40; i <= 100; i++) {  // Start from 40 (as defined in XML)
                    try {
                        Thread.sleep(100);  // Simulate some work (like loading)
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // Update the progress bar in the main thread
                    final int progress = i;
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(progress);
                        }
                    });
                }
            }
        }).start();

        // Set up buttons to handle navigation (already present in your code)
        navC = Navigation.findNavController(view);
        ImageButton checkButton = view.findViewById(R.id.TaskButton);
        ImageButton listButton = view.findViewById(R.id.CalendarButton);
        ImageButton profileButton = view.findViewById(R.id.SettingsButton);

        if (checkButton != null) {
            checkButton.setOnClickListener(this);
        } else {
            Log.e("HomePageFragment", "CheckButton not found!");
        }

        if (listButton != null) {
            listButton.setOnClickListener(this);
        } else {
            Log.e("HomePageFragment", "ListButton not found!");
        }

        if (profileButton != null) {
            profileButton.setOnClickListener(this);
        } else {
            Log.e("HomePageFragment", "ProfileButton not found!");
        }
    }

    @Override
    public void onClick(View v) {
        if (navC != null) {
            if (v.getId() == R.id.TaskButton) {
                navC.navigate(R.id.action_homePageFragment_to_taskListFragment);
            } else if (v.getId() == R.id.CalendarButton) {
                navC.navigate(R.id.action_homePageFragment_to_eventFragment);
            } else if (v.getId() == R.id.SettingsButton) {
                navC.navigate(R.id.action_homePageFragment_to_settingsFragment);
            }
        }
    }
}
