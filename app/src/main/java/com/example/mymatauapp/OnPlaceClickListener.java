package com.example.mymatauapp;

/**
 * Interface to handle click events on a place item in the RecyclerView.
 */
public interface OnPlaceClickListener {
    /**
     * Called when a place item is clicked.
     * @param result The PlaceResult object that was clicked.
     */
    void onPlaceClicked(PlaceResult result);
}
