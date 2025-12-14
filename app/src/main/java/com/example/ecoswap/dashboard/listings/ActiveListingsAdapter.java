package com.example.ecoswap.dashboard.listings;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ActiveListingsAdapter extends RecyclerView.Adapter<ActiveListingsAdapter.ActiveListingViewHolder> {

    public interface OnListingClickListener {
        void onListingClicked(@NonNull ActiveListing listing);
        void onEditClicked(@NonNull ActiveListing listing);
        void onMarkCompleteClicked(@NonNull ActiveListing listing);
    }

    private final List<ActiveListing> listings = new ArrayList<>();
    private final LayoutInflater inflater;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private final OnListingClickListener listener;

    public ActiveListingsAdapter(@NonNull Context context, @NonNull OnListingClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActiveListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_my_listing_active, parent, false);
        return new ActiveListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveListingViewHolder holder, int position) {
        ActiveListing listing = listings.get(position);
        holder.bind(listing, listener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    public void replaceData(@NonNull List<ActiveListing> data) {
        listings.clear();
        listings.addAll(data);
        notifyDataSetChanged();
    }

    static class ActiveListingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final Chip chipType;
        private final TextView tvStatus;
        private final TextView tvLocation;
        private final TextView tvUpdatedAt;
        private final android.widget.ImageView ivCover;
        private final com.google.android.material.button.MaterialButton btnEdit;
        private final com.google.android.material.button.MaterialButton btnMarkComplete;

        ActiveListingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivListingCover);
            tvTitle = itemView.findViewById(R.id.tvListingTitle);
            chipType = itemView.findViewById(R.id.chipListingType);
            tvStatus = itemView.findViewById(R.id.tvListingStatus);
            tvLocation = itemView.findViewById(R.id.tvListingLocation);
            tvUpdatedAt = itemView.findViewById(R.id.tvListingUpdatedAt);
            btnEdit = itemView.findViewById(R.id.btnEditListing);
            btnMarkComplete = itemView.findViewById(R.id.btnMarkComplete);
        }

        void bind(ActiveListing listing,
                  OnListingClickListener listener,
                  SimpleDateFormat dateFormat) {
            tvTitle.setText(listing.getTitle());
            chipType.setText(listing.getDisplayCategory());
            tvStatus.setText(listing.getDisplayStatus());
            tvLocation.setText(!TextUtils.isEmpty(listing.getLocation())
                    ? listing.getLocation()
                    : itemView.getContext().getString(R.string.location_unknown));
            String updatedLabel = itemView.getContext().getString(
                    R.string.my_listings_updated_at,
                    dateFormat.format(new Date(listing.getUpdatedAtEpochMs())));
            tvUpdatedAt.setText(updatedLabel);

            if (!TextUtils.isEmpty(listing.getImageUrl())) {
                Glide.with(itemView.getContext())
                        .load(listing.getImageUrl())
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(ivCover);
            } else {
                ivCover.setImageResource(R.drawable.ic_launcher_background);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onListingClicked(listing);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClicked(listing);
                }
            });

            btnMarkComplete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkCompleteClicked(listing);
                }
            });
        }
    }

    public static class ActiveListing {
        private final String id;
        private final String title;
        private final String description;
        private final String imageUrl;
        private final String location;
        private final String rawCategory;
        private final String displayCategory;
        private final String listingType;
        private final String condition;
        private final String rawStatus;
        private final String displayStatus;
        private final long updatedAtEpochMs;

        public ActiveListing(String id,
                             String title,
                             String description,
                             String imageUrl,
                             String location,
                             String rawCategory,
                             String displayCategory,
                             String listingType,
                             String condition,
                             String rawStatus,
                             String displayStatus,
                             long updatedAtEpochMs) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.location = location;
            this.rawCategory = rawCategory;
            this.displayCategory = displayCategory;
            this.listingType = listingType;
            this.condition = condition;
            this.rawStatus = rawStatus;
            this.displayStatus = displayStatus;
            this.updatedAtEpochMs = updatedAtEpochMs;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
        public String getLocation() { return location; }
        public String getRawCategory() { return rawCategory; }
        public String getDisplayCategory() { return displayCategory; }
        public String getListingType() { return listingType; }
        public String getCondition() { return condition; }
        public String getRawStatus() { return rawStatus; }
        public String getDisplayStatus() { return displayStatus; }
        public long getUpdatedAtEpochMs() { return updatedAtEpochMs; }
    }
}
