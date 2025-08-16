package com.example.mymatauapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Activity for the conductor's dashboard.
 * This activity handles the UI, manages location permissions,
 * and controls the lifecycle of the LocationTrackingService.
 */
public class ConductorDashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "ConductorDashboard";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 102;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private Button startTripButton;
    private Button statusButton;
    private FloatingActionButton profileButton;
    private boolean isTripActive = false;
    private String currentTripId = null;
    private String currentStatus = "available";

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference tripsCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conductor_dashboard);

        // Initialize Firebase Authentication and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tripsCollection = db.collection("trips");

        // Initialize FusedLocationProviderClient for location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get references to UI elements
        startTripButton = findViewById(R.id.button_start_trip);
        statusButton = findViewById(R.id.button_status);
        profileButton = findViewById(R.id.button_profile); // Initialize the new button

        // Get the map fragment and set the OnMapReadyCallback
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set up the click listener for the Start/End Trip button
        startTripButton.setOnClickListener(v -> {
            if (isTripActive) {
                endTrip();
            } else {
                startTrip();
            }
        });

        // Set up the click listener for the Status button
        statusButton.setOnClickListener(v -> {
            toggleStatus();
        });

        // Set up the click listener for the Profile button
        profileButton.setOnClickListener(v -> {
            // Start the ProfileActivity when the button is clicked
            Intent profileIntent = new Intent(ConductorDashboardActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
        });

        // Check for location permissions on start
        if (checkLocationPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, check for background location permission
                if (!checkBackgroundLocationPermission()) {
                    requestBackgroundLocationPermission();
                }
            }
        } else {
            // Request permissions if not granted
            requestLocationPermission();
        }
    }

    /**
     * This method is called when the map is ready to be used.
     * It enables the my-location layer and moves the camera to the last known location.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Check for permissions again to enable my-location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            // Get last known location and move the camera
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                    Log.d(TAG, "Map initialized to last known location: " + currentLatLng.toString());
                }
            });
        }
    }

    /**
     * Handles the logic for starting a new trip.
     * This method creates a new trip document in Firestore and starts the
     * LocationTrackingService to begin sending location updates.
     */
    private void startTrip() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check both foreground and background location permissions before starting the trip
        if (checkLocationPermission() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || checkBackgroundLocationPermission())) {
            // Generate a unique trip ID
            currentTripId = UUID.randomUUID().toString();
            isTripActive = true;
            startTripButton.setText("End Trip");
            statusButton.setVisibility(View.VISIBLE); // Show the status button

            // Create a new trip entry in Firestore
            String conductorId = user.getUid();
            long startTime = System.currentTimeMillis();

            // *** UPDATED: Added real route and matatu IDs for the user app to query ***
            // NOTE: In a full app, these values would be selected by the conductor from a list
            // or automatically retrieved.
            Map<String, Object> tripData = new HashMap<>();
            tripData.put("conductorId", conductorId);
            tripData.put("startTime", startTime);
            tripData.put("status", currentStatus);
            // Replacing placeholder with a real route ID from our MatatuRouteManager.
            tripData.put("routeId", "Roysambu to Nairobi Town");
            // Replacing placeholder with a sample matatu registration number.
            tripData.put("matatuId", "KCA 123A");

            // Set the initial trip data in Firestore
            tripsCollection.document(currentTripId).set(tripData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ConductorDashboardActivity.this, "Trip started successfully!", Toast.LENGTH_SHORT).show();
                        // START THE LOCATION TRACKING SERVICE
                        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
                        serviceIntent.putExtra("tripId", currentTripId);
                        serviceIntent.putExtra("conductorId", conductorId);
                        ContextCompat.startForegroundService(this, serviceIntent);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ConductorDashboardActivity.this, "Failed to start trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error starting trip", e);
                        // Reset state on failure
                        isTripActive = false;
                        startTripButton.setText("Start Trip");
                        statusButton.setVisibility(View.GONE);
                    });
        } else {
            Toast.makeText(this, "Location permissions are required to start a trip.", Toast.LENGTH_LONG).show();
            // Request permissions again if they were denied
            requestLocationPermission();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestBackgroundLocationPermission();
            }
        }
    }

    /**
     * Handles the logic for ending a trip.
     * This method stops the LocationTrackingService and updates the trip
     * document in Firestore with a 'complete' status and end time.
     */
    private void endTrip() {
        if (currentTripId == null) {
            // Prevent attempting to end a trip that was never started or has no ID
            isTripActive = false;
            startTripButton.setText("Start Trip");
            statusButton.setVisibility(View.GONE);
            return;
        }

        // STOP THE LOCATION TRACKING SERVICE
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);

        // Update the trip status to 'complete' in the database
        long endTime = System.currentTimeMillis();
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "complete");
        updates.put("endTime", endTime);

        tripsCollection.document(currentTripId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ConductorDashboardActivity.this, "Trip ended successfully!", Toast.LENGTH_SHORT).show();
                    // Reset the UI and state
                    isTripActive = false;
                    currentTripId = null;
                    startTripButton.setText("Start Trip");
                    statusButton.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ConductorDashboardActivity.this, "Failed to end trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error ending trip", e);
                });
    }

    /**
     * Handles the status toggle logic (available/full).
     * This method updates the status in the Firestore trip document.
     */
    private void toggleStatus() {
        if (!isTripActive || currentTripId == null) {
            Toast.makeText(this, "Please start a trip first.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentStatus = currentStatus.equals("available") ? "full" : "available";
        statusButton.setText(currentStatus.equals("available") ? "Mark as Full" : "Mark as Available");

        // Update the status in Firestore
        tripsCollection.document(currentTripId).update("status", currentStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ConductorDashboardActivity.this, "Status updated to " + currentStatus, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update status", e);
                });
    }

    // Helper method to check foreground location permissions
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Helper method to check background location permissions (for Android 10+)
    private boolean checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Background location permission not needed on older devices
    }

    // Helper method to request foreground location permissions
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    // Helper method to request background location permissions (for Android 10+)
    private void requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Foreground location permission granted.");
                // Check for background permission if on a newer device
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkBackgroundLocationPermission()) {
                    requestBackgroundLocationPermission();
                } else {
                    // All necessary permissions are granted, re-initialize the map
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    if (mapFragment != null) {
                        mapFragment.getMapAsync(this);
                    }
                }
            } else {
                Log.d(TAG, "Foreground location permission denied.");
                Toast.makeText(this, "Location permission is required to track trips.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Background location permission granted.");
                // All necessary permissions are granted, re-initialize the map
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this);
                }
            } else {
                Log.d(TAG, "Background location permission denied.");
                Toast.makeText(this, "Background location is required to track trips when the app is in the background.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This method is called when the activity is about to be destroyed.
     * It serves as a failsafe to stop the location service if the activity
     * is closed without the trip being ended.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Failsafe to stop the service if the activity is destroyed
        if (isTripActive) {
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
            stopService(serviceIntent);
        }
    }
}
