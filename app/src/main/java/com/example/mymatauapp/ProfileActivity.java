package com.example.mymatauapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Objects;

/**
 * An activity to display user profile information and provide a logout option.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivityDebug";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView textViewName;
    private TextView textViewEmail;
    private TextView textViewUid;
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase Auth and Firestore instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        textViewName = findViewById(R.id.textViewName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewUid = findViewById(R.id.textViewUid);
        btnLogout = findViewById(R.id.btnLogout);

        // Display user information
        displayUserProfile();

        // Set up the logout button click listener
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Log.d(TAG, "User signed out.");
            // After signing out, navigate back to the MainActivity
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Toast.makeText(ProfileActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
            finish(); // Finish the current activity so the user cannot navigate back to it
        });
    }

    /**
     * Retrieves and displays the current user's email, UID, and name from Firestore.
     */
    private void displayUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String email = user.getEmail();

            // Set the email and UID immediately
            textViewEmail.setText("Email: " + (email != null ? email : "Not available"));
            textViewUid.setText("User ID: " + uid);
            Log.d(TAG, "Displaying profile for user: " + uid);

            // Get user's name from Firestore
            DocumentReference docRef = db.collection("users").document(uid);
            docRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Correctly fetch the "fullName" field from the document
                        String name = document.getString("fullName");
                        textViewName.setText("Name: " + (name != null ? name : "Not set"));
                        Log.d(TAG, "Fetched user name from Firestore: " + name);
                    } else {
                        Log.d(TAG, "No such document for user: " + uid);
                        textViewName.setText("Name: Not set");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch user document.", task.getException());
                    textViewName.setText("Name: Error loading");
                }
            });

        } else {
            // No user is logged in
            textViewName.setText("Name: No user logged in");
            textViewEmail.setText("Email: No user logged in");
            textViewUid.setText("User ID: Not available");
            Log.w(TAG, "No authenticated user found.");
        }
    }
}
