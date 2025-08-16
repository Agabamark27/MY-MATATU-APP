package com.example.mymatauapp;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.example.mymatauapp.OnPlaceClickListener;

/**
 * A simple adapter for the RecyclerView to display PlaceResult items.
 * This adapter uses an interface to handle click events on its items.
 * This version includes basic styling to improve visibility.
 */
public class PlacesAdapter extends RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder> {

    private List<PlaceResult> results;
    private final OnPlaceClickListener listener;

    /**
     * Constructs a new PlacesAdapter.
     *
     * @param results The initial list of PlaceResult objects to display.
     * @param listener The OnPlaceClickListener to handle item clicks.
     */
    public PlacesAdapter(List<PlaceResult> results, OnPlaceClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    /**
     * Updates the list of predictions and refreshes the RecyclerView.
     *
     * @param newResults The new list of results.
     */
    public void setResults(List<PlaceResult> newResults) {
        this.results = newResults;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        // Use a LinearLayout to hold the TextView and a separator line.
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        // Create the TextView for the place's formatted address.
        TextView textView = new TextView(context);
        textView.setPadding(32, 24, 32, 24); // Added more padding for better spacing.
        textView.setTextSize(16);
        textView.setTextColor(Color.parseColor("#000000")); // Black text for good contrast.
        textView.setBackgroundColor(Color.parseColor("#FFFFFF")); // Slightly white background for the item.

        // Create a thin View to act as a separator line.
        View separator = new View(context);
        separator.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2 // Set separator height to 2 pixels.
        ));
        separator.setBackgroundColor(Color.parseColor("#E0E0E0")); // Light gray line color.

        // Add the TextView and separator to the container.
        container.addView(textView);
        container.addView(separator);

        return new PlaceViewHolder(container);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        PlaceResult result = results.get(position);
        holder.bind(result, listener);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            // The TextView is the first child of the LinearLayout container.
            this.textView = (TextView) ((ViewGroup) itemView).getChildAt(0);
        }

        public void bind(PlaceResult result, OnPlaceClickListener listener) {
            textView.setText(result.getFormattedAddress());
            itemView.setOnClickListener(v -> listener.onPlaceClicked(result));
        }
    }
}
