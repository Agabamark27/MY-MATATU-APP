package com.example.mymatauapp; // IMPORTANT: Replace with your app's actual package name

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

// This class handles the registration logic and fixes the keyboard issue.
public class Register extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    // Add ScrollView to your class members
    private ScrollView scrollView;

    EditText editTextEmail, editTextPassword, editTextFullName, editTextPhoneNumber, editTextAddress, editTextConfirmPassword;
    Button registerButton;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    Spinner genderSpinner; // Removed the roleSpinner

    // Add a Firestore instance
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // We will use the XML layout without the roleSpinner
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        // Initialize the Firestore instance
        db = FirebaseFirestore.getInstance();

        // Initialize ScrollView
        scrollView = findViewById(R.id.main);

        // Initialize all UI elements
        editTextEmail = findViewById(R.id.input_userEmail);
        editTextPassword = findViewById(R.id.input_password);
        editTextFullName = findViewById(R.id.input_fullName);
        editTextPhoneNumber = findViewById(R.id.inputMobile);
        editTextAddress = findViewById(R.id.inputAddress);
        editTextConfirmPassword = findViewById(R.id.confirm_password);
        registerButton = findViewById(R.id.register_button);
        progressBar = findViewById(R.id.progressBar);
        genderSpinner = findViewById(R.id.gender);

        // Setup back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Setup the Gender Spinner with an ArrayAdapter
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender, // You need to define this array in strings.xml
                android.R.layout.simple_spinner_item
        );
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        // Set the OnFocusChange listeners for each EditText to handle scrolling
        setKeyboardScrollListener(editTextFullName);
        setKeyboardScrollListener(editTextEmail);
        setKeyboardScrollListener(editTextPhoneNumber);
        setKeyboardScrollListener(editTextAddress);
        setKeyboardScrollListener(editTextPassword);
        setKeyboardScrollListener(editTextConfirmPassword);

        // Set the OnClickListener for the register button
        registerButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);

            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();
            String fullName = editTextFullName.getText().toString().trim();
            String phoneNumber = editTextPhoneNumber.getText().toString().trim();
            String address = editTextAddress.getText().toString().trim();
            String gender = genderSpinner.getSelectedItem().toString();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Register.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Register.this, "Please enter a password", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(Register.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(address) || gender.equals("Select Gender")) {
                Toast.makeText(Register.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    String userId = user.getUid();

                                    // Create a new map to store the user data
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("fullName", fullName);
                                    userData.put("email", email);
                                    userData.put("phoneNumber", phoneNumber);
                                    userData.put("address", address);
                                    userData.put("gender", gender);
                                    userData.put("role", "passenger"); // DEFAULT role is 'passenger'

                                    // Save the user data to Firestore using the user's UID as the document ID
                                    db.collection("users").document(userId)
                                            .set(userData)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "DocumentSnapshot successfully written!");
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(Register.this, "Registration successful. You are registered as a passenger.", Toast.LENGTH_SHORT).show();
                                                // Navigate to MainActivity only after successful registration and data save
                                                Intent intent = new Intent(Register.this, MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Error writing document", e);
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(Register.this, "Registration successful, but failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                }
                            } else {
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(Register.this, "Registration failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    /**
     * Sets a focus change listener on an EditText to programmatically scroll
     * the ScrollView and keep the focused EditText in view.
     *
     * @param editText The EditText to attach the listener to.
     */
    private void setKeyboardScrollListener(final EditText editText) {
        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                // We use view.post() to ensure the scroll happens after the keyboard
                // has been drawn and the layout has been resized.
                view.post(() -> {
                    // Calculate the position to scroll to. We add a small offset
                    // to keep the view slightly above the keyboard.
                    int[] location = new int[2];
                    editText.getLocationInWindow(location);
                    int y = location[1];
                    scrollView.smoothScrollTo(0, y);
                });
            }
        });
    }
}
