package com.example.mymatauapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main activity for the MyMatauApp.
 * This activity handles the Google Map, user location, and place search functionality
 * using the OpenCage Geocoding API. It also implements a more intelligent
 * multi-modal journey planner and a real-time matatu tracking dashboard.
 */
public class LandingPage extends AppCompatActivity implements OnMapReadyCallback, OnPlaceClickListener {

    private static final String TAG = "LandingPageDebug";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String OPENCAGE_API_KEY = "209a7174081e413aab7025cb858d2883";
    private static final double AVERAGE_MATATU_SPEED_MPS = 5.55; // 20 km/h in meters per second

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextInputEditText whereToInput;
    private RecyclerView placesRecyclerView;
    private PlacesAdapter placesAdapter;
    private TextView distanceTextView;
    private MaterialButton startNavigationButton;
    private FloatingActionButton btnProfile;
    private LatLng destinationLatLng;
    private LatLng startLatLng;
    private MatatuRouteManager matatuRouteManager;

    private TextView dashboardStatusTextView;
    private View loadingIndicator;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        // Initialize UI components and services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        whereToInput = findViewById(R.id.edit_text_where_to);
        placesRecyclerView = findViewById(R.id.recycler_view_places);
        distanceTextView = findViewById(R.id.distance_text_view);
        startNavigationButton = findViewById(R.id.start_navigation_button);
        btnProfile = findViewById(R.id.btnProfile); // Initialize the profile button
        matatuRouteManager = new MatatuRouteManager();

        // New UI component references
        dashboardStatusTextView = findViewById(R.id.statusTextView);
        loadingIndicator = findViewById(R.id.loadingIndicator);

        // Hide the dashboard card view initially
        findViewById(R.id.dashboardCard).setVisibility(View.GONE);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        startNavigationButton.setOnClickListener(v -> startGoogleMapsNavigation());

        // Set the click listener for the profile button
        btnProfile.setOnClickListener(v -> {
            // Start the new ProfileActivity
            Intent intent = new Intent(LandingPage.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Setup the Google Map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup the RecyclerView for displaying search results
        placesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        placesAdapter = new PlacesAdapter(new ArrayList<>(), this);
        placesRecyclerView.setAdapter(placesAdapter);

        // Add a listener to the text input for real-time place predictions
        whereToInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 2) {
                    fetchOpenCagePredictions(charSequence.toString());
                } else {
                    placesRecyclerView.setVisibility(View.GONE);
                    distanceTextView.setText("");
                    startNavigationButton.setVisibility(View.GONE);
                    // Hide the dashboard if the user clears the search
                    findViewById(R.id.dashboardCard).setVisibility(View.GONE);
                    if (mMap != null) {
                        mMap.clear(); // Clear the map when a new search starts
                        getDeviceLocation(); // Reset camera to user's location
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    /**
     * Called when the Google Map is ready to be used.
     * @param googleMap The GoogleMap object.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready.");
        requestLocationPermissions();
    }

    /**
     * Requests location permission from the user.
     */
    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            getDeviceLocation();
        } else {
            Log.d(TAG, "Requesting location permission.");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Handles the result of the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted by the user.");
                requestLocationPermissions();
            } else {
                Log.w(TAG, "Location permission denied by the user.");
                Toast.makeText(this, "Location permission is required for this app to function.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Fetches the last known location of the device and moves the map camera there.
     */
    private void getDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        startLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f));
                        Log.d(TAG, "Device location found: " + startLatLng.latitude + ", " + startLatLng.longitude);
                    } else {
                        Log.w(TAG, "Last known location is null. Using default location for Nairobi.");
                        LatLng defaultLocation = new LatLng(-1.286389, 36.817223);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception during location retrieval: " + e.getMessage());
        }
    }

    /**
     * This method fetches place predictions from the OpenCage Geocoding API.
     * It performs the network request on a background thread.
     */
    private void fetchOpenCagePredictions(String query) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<PlaceResult> results = new ArrayList<>();
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlString = "https://api.opencagedata.com/geocode/v1/json?q=" + encodedQuery + "&key=" + OPENCAGE_API_KEY;
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray resultsArray = jsonResponse.getJSONArray("results");
                    for (int i = 0; i < resultsArray.length(); i++) {
                        JSONObject resultObject = resultsArray.getJSONObject(i);
                        String formattedAddress = resultObject.getString("formatted");
                        JSONObject geometry = resultObject.getJSONObject("geometry");
                        double lat = geometry.getDouble("lat");
                        double lng = geometry.getDouble("lng");
                        results.add(new PlaceResult(formattedAddress, lat, lng));
                    }
                } else {
                    Log.e(TAG, "API call failed with response code: " + responseCode);
                }

                connection.disconnect();
            } catch (IOException e) {
                Log.e(TAG, "Network error fetching OpenCage data", e);
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error on OpenCage data", e);
            }

            handler.post(() -> {
                if (!results.isEmpty()) {
                    placesAdapter.setResults(results);
                    placesRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    placesAdapter.setResults(new ArrayList<>());
                    placesRecyclerView.setVisibility(View.GONE);
                }
            });
        });
    }

    /**
     * Implementation of the OnPlaceClickListener interface.
     * This method is called when a place in the RecyclerView is clicked.
     * @param result The PlaceResult object that was clicked.
     */
    @Override
    public void onPlaceClicked(PlaceResult result) {
        try {
            Log.d(TAG, "Making dashboard visible.");
            findViewById(R.id.dashboardCard).setVisibility(View.VISIBLE);

            whereToInput.setText(result.getFormattedAddress());
            whereToInput.setSelection(whereToInput.getText().length());
            placesRecyclerView.setVisibility(View.GONE);

            destinationLatLng = new LatLng(result.getLatitude(), result.getLongitude());

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission is required to calculate the route.", Toast.LENGTH_SHORT).show();
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, startLocation -> {
                if (startLocation != null) {
                    // Start tracking ONLY after a destination is selected
                    startMatatuTracking(startLocation);
                    drawMultiModalRoute(startLocation, destinationLatLng);
                } else {
                    Toast.makeText(this, "Could not find your current location. Cannot start tracking.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Current location is null, cannot start tracking.");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "An unexpected error occurred in onPlaceClicked: " + e.getMessage(), e);
            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Draws a multi-modal route on the map with distinct polylines for each segment.
     * @param startLocation The user's starting location.
     * @param destinationLatLng The final destination's LatLng coordinates.
     */
    private void drawMultiModalRoute(Location startLocation, LatLng destinationLatLng) {
        mMap.clear();
        startLatLng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());

        LatLng nearestStartStage = matatuRouteManager.findNearestStop(startLatLng);
        LatLng nearestEndStage = matatuRouteManager.findNearestStop(destinationLatLng);

        if (nearestStartStage == null || nearestEndStage == null) {
            Toast.makeText(this, "Could not find a valid matatu route.", Toast.LENGTH_SHORT).show();
            // Fallback to a simple route if matatu route can't be found
            mMap.addMarker(new MarkerOptions().position(startLatLng).title("Your Location"));
            mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(startLatLng);
            builder.include(destinationLatLng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            startNavigationButton.setVisibility(View.GONE);
            return;
        }

        // Get waypoints from the MatatuRouteManager
        List<LatLng> waypoints = matatuRouteManager.getWaypointsBetween(nearestStartStage, nearestEndStage);

        // Check if waypoints are returned correctly
        if (waypoints == null || waypoints.isEmpty()) {
            Log.w(TAG, "MatatuRouteManager returned an empty or null list of waypoints. Falling back to simple route.");
            Toast.makeText(this, "No matatu route waypoints found. Displaying direct path.", Toast.LENGTH_SHORT).show();

            // Draw a direct path if no matatu route is found
            mMap.addPolyline(new PolylineOptions()
                    .add(startLatLng, destinationLatLng)
                    .width(10f)
                    .color(Color.GRAY)
                    .geodesic(true));

            // Add markers for start and destination
            mMap.addMarker(new MarkerOptions().position(startLatLng).title("Your Location"));
            mMap.addMarker(new MarkerOptions().position(destinationLatLng).title(whereToInput.getText().toString()));

            // Zoom to fit the two points
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(startLatLng);
            builder.include(destinationLatLng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            startNavigationButton.setVisibility(View.GONE);
            return;
        }

        // 1. Draw walking line to the matatu stage (Blue)
        mMap.addPolyline(new PolylineOptions()
                .add(startLatLng, nearestStartStage)
                .width(10f)
                .color(Color.BLUE)
                .geodesic(true));

        // 2. Draw the matatu route line (Green)
        List<LatLng> matatuRoutePoints = new ArrayList<>();
        matatuRoutePoints.add(nearestStartStage);
        matatuRoutePoints.addAll(waypoints);
        matatuRoutePoints.add(nearestEndStage);

        mMap.addPolyline(new PolylineOptions()
                .addAll(matatuRoutePoints)
                .width(10f)
                .color(Color.GREEN)
                .geodesic(true));

        // 3. Draw walking line from the last stage to the destination (Blue)
        mMap.addPolyline(new PolylineOptions()
                .add(nearestEndStage, destinationLatLng)
                .width(10f)
                .color(Color.BLUE)
                .geodesic(true));

        // Add markers for key points
        mMap.addMarker(new MarkerOptions()
                .position(startLatLng)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mMap.addMarker(new MarkerOptions()
                .position(nearestStartStage)
                .title("Matatu Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        mMap.addMarker(new MarkerOptions()
                .position(nearestEndStage)
                .title("Matatu Drop-off")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
        mMap.addMarker(new MarkerOptions()
                .position(destinationLatLng)
                .title(whereToInput.getText().toString())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // Zoom the camera to fit the entire route
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(startLatLng);
        builder.include(destinationLatLng);
        for (LatLng waypoint : matatuRoutePoints) {
            builder.include(waypoint);
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

        startNavigationButton.setVisibility(View.VISIBLE);
        distanceTextView.setText("Route calculated: Walking (blue) to matatu (green) and final walk (blue).");
    }

    /**
     * A method to start the Google Maps navigation app via an Intent,
     * including a multi-modal journey with only the relevant matatu stops.
     */
    private void startGoogleMapsNavigation() {
        if (startLatLng == null || destinationLatLng == null) {
            Toast.makeText(this, "Please select a starting point and destination first.", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLng nearestStartStage = matatuRouteManager.findNearestStop(startLatLng);
        LatLng nearestEndStage = matatuRouteManager.findNearestStop(destinationLatLng);

        if (nearestStartStage == null || nearestEndStage == null) {
            Toast.makeText(this, "Could not find a valid matatu route.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build the URI with only the key points: start, pickup stage, drop-off stage, destination
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destinationLatLng.latitude + "," + destinationLatLng.longitude +
                "&mode=walking" +
                "&waypoints=" +
                startLatLng.latitude + "," + startLatLng.longitude + "|" +
                nearestStartStage.latitude + "," + nearestStartStage.longitude + "|" +
                nearestEndStage.latitude + "," + nearestEndStage.longitude);

        Log.d(TAG, "Google Maps URI: " + gmmIntentUri.toString());

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps app is not installed.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets up a real-time Firestore listener to find available matatus.
     * This method queries for trips with a status of 'available' on a specific route.
     *
     * @param userLocation The user's current location, required for ETA calculation. Can be null if not available.
     */
    private void startMatatuTracking(Location userLocation) {
        // The destination route the user is interested in. This would be chosen by the user in a real app.
        String destinationRoute = "Roysambu to Nairobi Town";

        loadingIndicator.setVisibility(View.VISIBLE);
        dashboardStatusTextView.setText("Searching for Matatus...");

        CollectionReference tripsCollection = db.collection("trips");
        Query query = tripsCollection
                .whereEqualTo("status", "available")
                .whereEqualTo("routeId", destinationRoute);

        query.addSnapshotListener((querySnapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Matatu tracking listener failed.", e);
                dashboardStatusTextView.setText("Error fetching matatus. Please try again later.");
                loadingIndicator.setVisibility(View.GONE);
                return;
            }

            loadingIndicator.setVisibility(View.GONE);
            StringBuilder matatuInfo = new StringBuilder();

            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                // We found at least one available matatu.
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    // Check for valid location data and user location
                    if (doc.contains("latitude") && doc.contains("longitude") && userLocation != null) {
                        Double matatuLat = doc.getDouble("latitude");
                        Double matatuLon = doc.getDouble("longitude");
                        String matatuId = doc.getString("matatuId");

                        if (matatuLat != null && matatuLon != null && matatuId != null) {
                            LatLng matatuLatLng = new LatLng(matatuLat, matatuLon);
                            LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                            // Calculate distance in meters
                            double distanceInMeters = SphericalUtil.computeDistanceBetween(userLatLng, matatuLatLng);

                            // Calculate ETA in minutes (Distance / Speed)
                            int etaInMinutes = (int) Math.ceil((distanceInMeters / AVERAGE_MATATU_SPEED_MPS) / 60);

                            matatuInfo.append("Matatu: ").append(matatuId).append("\n");
                            matatuInfo.append("ETA: ").append(etaInMinutes).append(" mins\n\n");
                        }
                    }
                }
                if (matatuInfo.length() > 0) {
                    dashboardStatusTextView.setText(matatuInfo.toString());
                } else {
                    // This case handles if a matatu is found, but its location data is missing.
                    dashboardStatusTextView.setText("No location data for available matatus.");
                }

            } else {
                // No documents matched the query. No available matatus.
                dashboardStatusTextView.setText("NO AVAILABLE MATATU TO YOUR DESTINATION AT THE MOMENT");
            }
        });
    }
}
