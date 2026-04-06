package com.example.roamio.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.roamio.BuildConfig;
import com.example.roamio.R;
import com.example.roamio.models.NearbyPlace;

import java.util.List;

public class NearbyPlaceAdapter extends RecyclerView.Adapter<NearbyPlaceAdapter.ViewHolder> {

    private final Context           context;
    private final List<NearbyPlace> places;
    private       OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onPlaceClick(NearbyPlace place);
        void onFavouriteClick(NearbyPlace place, int position);
    }

    public NearbyPlaceAdapter(Context context, List<NearbyPlace> places) {
        this.context = context;
        this.places  = places;
    }

    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_nearby_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyPlace place = places.get(position);

        holder.tvPlaceName.setText(place.getName());

        // Show rating + review count in one chip
        if (place.getRating() > 0) {
            String ratingText;
            int reviews = place.getUserRatingsTotal();
            if (reviews >= 1000) {
                ratingText = String.format("\u2605 %.1f \u00b7 %.1fk",
                        place.getRating(), reviews / 1000.0);
            } else if (reviews > 0) {
                ratingText = String.format("\u2605 %.1f \u00b7 %d",
                        place.getRating(), reviews);
            } else {
                ratingText = String.format("\u2605 %.1f", place.getRating());
            }
            holder.tvRating.setText(ratingText);
            holder.ratingChip.setVisibility(View.VISIBLE);
        } else {
            holder.ratingChip.setVisibility(View.GONE);
        }

        // Load image from Google Places Photo API (legacy)
        // place.getPhotoReference() holds the photo_reference hash from the
        // legacy Nearby Search API used in NearbyActivity and MainActivity.
        if (place.hasPhoto()) {
            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                    + "?maxwidth=400"
                    + "&photo_reference=" + place.getPhotoReference()
                    + "&key=" + BuildConfig.MAPS_API_KEY;

            Glide.with(context)
                    .load(photoUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(holder.ivPlaceImage);
        } else {
            holder.ivPlaceImage.setBackgroundColor(getCategoryColor(place.getCategory()));
            holder.ivPlaceImage.setImageDrawable(null);
        }

        // Heart button state
        holder.ivHeart.setColorFilter(
                place.isFavourite() ? Color.parseColor("#FF4444") : Color.parseColor("#888888")
        );

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPlaceClick(place);
        });

        holder.btnFavourite.setOnClickListener(v -> {
            place.setFavourite(!place.isFavourite());
            notifyItemChanged(position);
            if (listener != null) listener.onFavouriteClick(place, position);
        });
    }

    @Override
    public int getItemCount() { return places.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView    ivPlaceImage;
        TextView     tvPlaceName;
        TextView     tvRating;
        LinearLayout ratingChip;   // chip container — hide this, not just the text
        View         btnFavourite;
        ImageView    ivHeart;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlaceImage = itemView.findViewById(R.id.ivPlaceImage);
            tvPlaceName  = itemView.findViewById(R.id.tvPlaceName);
            tvRating     = itemView.findViewById(R.id.tvRating);
            ratingChip   = itemView.findViewById(R.id.ratingChip);
            btnFavourite = itemView.findViewById(R.id.btnFavourite);
            ivHeart      = itemView.findViewById(R.id.ivHeart);
        }
    }

    private int getCategoryColor(String category) {
        if (category == null) return Color.parseColor("#1A2533");
        switch (category) {
            case "restaurant":        return Color.parseColor("#2C1A0E");
            case "hindu_temple":
            case "place_of_worship":  return Color.parseColor("#1A1A2C");
            case "tourist_attraction":
            case "museum":            return Color.parseColor("#0A1F2E");
            case "amusement_park":    return Color.parseColor("#1A2C0A");
            case "shopping_mall":     return Color.parseColor("#2C2C0A");
            case "spa":               return Color.parseColor("#0A2C2C");
            case "lodging":           return Color.parseColor("#1A1A1A");
            default:                  return Color.parseColor("#1A2533");
        }
    }
}