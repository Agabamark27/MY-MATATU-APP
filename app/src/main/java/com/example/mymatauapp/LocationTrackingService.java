package com.example.mymatauapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

// This service runs in the background to track the user's location,
// specifically for a matatu trip started by a conductor.
public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "LocationServiceChannel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;

    // We'll use these to identify the specific trip document to update.
    private String tripId;
    private String conductorId;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase Firestore.
        db = FirebaseFirestore.getInstance();

        // Initialize the FusedLocationProviderClient.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // This is the callback that gets called every time a new location is available.
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "LocationResult is null.");
                    return;
                }

                // Loop through all new location data.
                for (android.location.Location location : locationResult.getLocations()) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        Log.d(TAG, "New location received - Lat: " + latitude + ", Lng: " + longitude);

                        // Call the method to update the trip's location in Firestore.
                        updateTripLocation(latitude, longitude);
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Retrieve the trip and conductor IDs from the Intent.
        if (intent != null) {
            tripId = intent.getStringExtra("tripId");
            conductorId = intent.getStringExtra("conductorId");
        }

        if (tripId == null || conductorId == null) {
            Log.e(TAG, "Trip ID or Conductor ID is null. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Create a notification channel for Android Oreo and above.
        createNotificationChannel();

        // Create and display a persistent notification to the user, as required for a foreground service.
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Matatu Trip Active")
                .setContentText("Your location is being updated for trip " + tripId)
                .setSmallIcon(R.mipmap.ic_launcher) // Use your app's launcher icon
                .build();
        startForeground(1, notification);

        // Start requesting location updates.
        startLocationUpdates();

        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(10000) // Update every 10 seconds
                .setMinUpdateIntervalMillis(5000) // Don't get updates faster than 5 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        try {
            // Request location updates from the FusedLocationProviderClient.
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
            Log.d(TAG, "Location updates requested.");
        } catch (SecurityException e) {
            // This catch block handles the case where location permission was denied.
            Log.e(TAG, "Location permission not granted.", e);
        }
    }

    private void updateTripLocation(double latitude, double longitude) {
        if (tripId != null && conductorId != null) {
            // Create a map to hold the new location data and a timestamp.
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("lastUpdated", System.currentTimeMillis());

            // Update the specific trip's document in Firestore.
            db.collection("trips").document(tripId)
                    .update(locationData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Trip location successfully updated!"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating trip location", e));
        } else {
            Log.e(TAG, "Trip ID is missing, cannot update location.");
        }
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Matatu Location Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop location updates when the service is destroyed.
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates removed.");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
