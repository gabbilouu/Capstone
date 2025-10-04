package com.example.elevate;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class loginpage extends Fragment {

    private FirebaseAuth mAuth;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loginpage, container, false);

        mAuth = FirebaseAuth.getInstance();
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

        // UI elements
        EditText LoginUsernameField = view.findViewById(R.id.email);
        EditText LoginPasswordField = view.findViewById(R.id.password);
        Button buttonLogin = view.findViewById(R.id.loginButton);
        ImageView backArrow = view.findViewById(R.id.backArrow);

        // 🔹 Auto-login if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navController.navigate(R.id.action_loginFragment_to_startingFragment);
        }

        // 🔹 Login button
        buttonLogin.setOnClickListener(v -> {
            String email = LoginUsernameField.getText().toString().trim();
            String password = LoginPasswordField.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                LoginUsernameField.setError("Email required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                LoginPasswordField.setError("Password required");
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Login successful!", Toast.LENGTH_SHORT).show();
                            navController.navigate(R.id.action_loginpageFragment_to_assessmentFragment);
                        } else {
                            Toast.makeText(getActivity(), "Login failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // 🔹 Back arrow navigation
        backArrow.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }
}
