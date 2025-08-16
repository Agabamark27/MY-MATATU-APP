package com.example.mymatauapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    private EditText newPassword, confirmPassword;
    Button buttonResetPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get references to UI elements from the XML layout
        // CORRECTED: Assuming a separate ID for the new password field.
        // Please check your activity_reset_password.xml file for the correct ID.
        newPassword = findViewById(R.id.input_new_password);
        confirmPassword = findViewById(R.id.input_confirm_new_password);
        buttonResetPassword = findViewById(R.id.button_reset_password);

        // Set up the click listener for the reset password button
        buttonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the current user
                FirebaseUser user = mAuth.getCurrentUser();

                if (user != null) {
                    String newPass = newPassword.getText().toString().trim();
                    String confirmPass = confirmPassword.getText().toString().trim();

                    // Validate that the password fields are not empty
                    if (TextUtils.isEmpty(newPass)) {
                        Toast.makeText(ResetPasswordActivity.this, "Please enter a new password.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(confirmPass)) {
                        Toast.makeText(ResetPasswordActivity.this, "Please confirm your new password.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Validate that the passwords match
                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(ResetPasswordActivity.this, "Passwords do not match.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // You can add more validation here, like minimum password length
                    if (newPass.length() < 6) {
                        Toast.makeText(ResetPasswordActivity.this, "Password must be at least 6 characters long.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update the user's password in Firebase
                    user.updatePassword(newPass)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ResetPasswordActivity.this,
                                            "Your password has been reset successfully.",
                                            Toast.LENGTH_LONG).show();
                                    // Navigate back to the login screen after a successful reset
                                    Intent intent = new Intent(ResetPasswordActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish(); // Close the current activity
                                } else {
                                    // Log the error message to Logcat
                                    Log.e(TAG, "Password reset failed: " + task.getException().getMessage(), task.getException());
                                    Toast.makeText(ResetPasswordActivity.this,
                                            "Password reset failed. Please try again.",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    // This case should ideally not happen if the user is authenticated, but it's good to handle
                    Toast.makeText(ResetPasswordActivity.this, "User not logged in.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


    }
}
