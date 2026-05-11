package com.example.roamio.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roamio.R;
import com.example.roamio.models.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    public interface OnItemLongClickListener {
        void onLongClick(Review review);
    }

    private OnItemLongClickListener longClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    private final Context       context;
    private final List<Review>  reviews;

    private static final String[] AVATAR_COLORS = {
            "#00C9B1", "#F4B942", "#7A9BB0", "#2E6B2E", "#E67E22", "#4A7C59"
    };

    public ReviewAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Review r = reviews.get(position);

        h.tvAvatar.setText(r.getInitial());
        String color = AVATAR_COLORS[position % AVATAR_COLORS.length];
        ViewCompat.setBackgroundTintList(h.tvAvatar, ColorStateList.valueOf(Color.parseColor(color)));

        h.tvName.setText(r.getUserName() != null ? r.getUserName() : "Anonymous");
        h.tvTime.setText(r.getTimeAgo());
        h.tvPlace.setText(String.format("📍 %s", r.getPlaceName() != null ? r.getPlaceName() : ""));

        buildStars(h.llStars, r.getRating());

        h.tvReviewText.setText(r.getReviewText());

        h.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(reviews.get(position));
            }
            return true;
        });
    }

    private void buildStars(LinearLayout container, float rating) {
        container.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            TextView star = new TextView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginEnd(2);
            star.setLayoutParams(lp);
            star.setTextSize(14f);
            if (i <= (int) rating) {
                star.setText("★");
                star.setTextColor(Color.parseColor("#F4B942"));
            } else if (i - rating < 1 && i - rating > 0) {
                star.setText("½");
                star.setTextColor(Color.parseColor("#F4B942"));
            } else {
                star.setText("★");
                star.setTextColor(Color.parseColor("#333333"));
            }
            container.addView(star);
        }
    }

    @Override
    public int getItemCount() { return reviews.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    tvAvatar, tvName, tvTime, tvPlace, tvReviewText;
        LinearLayout llStars;

        ViewHolder(@NonNull View v) {
            super(v);
            tvAvatar     = v.findViewById(R.id.tvReviewAvatar);
            tvName       = v.findViewById(R.id.tvReviewerName);
            tvTime       = v.findViewById(R.id.tvReviewTime);
            tvPlace      = v.findViewById(R.id.tvReviewPlace);
            llStars      = v.findViewById(R.id.llReviewStars);
            tvReviewText = v.findViewById(R.id.tvReviewText);
        }
    }
}
