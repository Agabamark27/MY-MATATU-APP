package com.example.mymatauapp;

/**
 * A simple data class to hold a place result from the OpenCage Geocoding API.
 * This class replaces the Google-specific AutocompletePrediction.
 */
public class PlaceResult {
    private final String formattedAddress;
    private final double latitude;
    private final double longitude;

    public PlaceResult(String formattedAddress, double latitude, double longitude) {
        this.formattedAddress = formattedAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getFormattedAddress() {
        return formattedAddress;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
