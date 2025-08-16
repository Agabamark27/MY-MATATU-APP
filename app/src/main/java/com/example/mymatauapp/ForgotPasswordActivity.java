package com.example.mymatauapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText inputEmail;
    Button buttonResetPassword;
    TextView backToLogin;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get references to UI elements from the XML layout
        inputEmail = findViewById(R.id.input_email);
        buttonResetPassword = findViewById(R.id.button_reset_password);
        backToLogin = findViewById(R.id.back_to_login);

        // Set up the click listener for the reset password button
        buttonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = inputEmail.getText().toString().trim();

                // Validate that the email field is not empty
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(ForgotPasswordActivity.this, "Please enter your registered email address.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Send a password reset email to the user
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            // Regardless of success or failure, we give a generic message to prevent user enumeration.
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "If that email is registered, a password reset link has been sent. Please check your inbox.",
                                    Toast.LENGTH_LONG).show();
                        });
            }
        });

        // Set up the click listener for the "Back to login" text
        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the MainActivity
                Intent intent = new Intent(ForgotPasswordActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close the current activity
            }
        });
    }
}
