package com.example.mymatauapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, check their role and redirect
            checkUserRoleAndRedirect(currentUser.getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonLogin, buttonRegister, buttonForgotPassword;

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get references to UI elements
        editTextEmail = findViewById(R.id.input_userEmail);
        editTextPassword = findViewById(R.id.input_password);
        buttonLogin = findViewById(R.id.button_login);
        buttonRegister = findViewById(R.id.button_register);
        buttonForgotPassword = findViewById(R.id.button_forgot_password);

        // Set up the login button click listener
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(MainActivity.this, "Email is required.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(MainActivity.this, "Password is required.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, now get the user's role from Firestore
                                Toast.makeText(MainActivity.this, "Login successful.",
                                        Toast.LENGTH_SHORT).show();
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    checkUserRoleAndRedirect(user.getUid());
                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                Toast.makeText(MainActivity.this,
                                        "Authentication failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // Set up the Register button click listener
        buttonRegister.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Register.class);
            startActivity(intent);
            finish();
        });

        // Set up the Forgot Password button click listener
        buttonForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Fetches the user's role from Firestore and redirects to the appropriate activity.
     * @param uid The unique ID of the currently logged-in user.
     */
    private void checkUserRoleAndRedirect(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Get the 'role' field from the document
                            String userRole = document.getString("role");

                            if ("conductor".equals(userRole)) {
                                // User is a conductor, go to the conductor dashboard
                                Intent intent = new Intent(MainActivity.this, ConductorDashboardActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Default to landing page for all other roles (e.g., "passenger")
                                Intent intent = new Intent(MainActivity.this, LandingPage.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            // Document not found, redirect to landing page as a fallback
                            Intent intent = new Intent(MainActivity.this, LandingPage.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        // Handle the error and provide a fallback
                        Toast.makeText(MainActivity.this,
                                "Failed to get user data: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(MainActivity.this, LandingPage.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }
}
