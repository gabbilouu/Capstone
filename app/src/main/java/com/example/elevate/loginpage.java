package com.example.elevate;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class loginpage extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_loginpage, container, false);

        // Initialize UI elements
        EditText LoginUsernameField = view.findViewById(R.id.email);
        EditText LoginPasswordField = view.findViewById(R.id.password);
        Button buttonLogin = view.findViewById(R.id.loginButton);

        // Set login button click listener
        buttonLogin.setOnClickListener(view1 -> {
            String username = LoginUsernameField.getText().toString();
            String password = LoginPasswordField.getText().toString();

            // Dummy check for login credentials
            if (username.equals("user") && password.equals("password123")) {
                // If credentials are correct
                Toast.makeText(getActivity(), "Login successful!", Toast.LENGTH_SHORT).show();
                // Proceed to another fragment or activity
                // For example, you can load another fragment here.
            } else {
                // If credentials are incorrect
                Toast.makeText(getActivity(), "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }


}