package com.example.ecoswap.dashboard;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.LocationUtils;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class MarketplaceAdapter extends RecyclerView.Adapter<MarketplaceAdapter.ViewHolder> {
    
    public interface OnListingClickListener {
        void onListingClicked(@NonNull MarketplaceFragment.MarketplaceItem item);
    }

    private final List<MarketplaceFragment.MarketplaceItem> items;
    private final Context context;
    private final OnListingClickListener clickListener;
    
    public MarketplaceAdapter(List<MarketplaceFragment.MarketplaceItem> items, Context context, OnListingClickListener clickListener) {
        this.items = items != null ? items : new ArrayList<>();
        this.context = context;
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_marketplace, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MarketplaceFragment.MarketplaceItem item = items.get(position);
        
        holder.tvItemTitle.setText(item.getTitle());
        holder.tvLocation.setText(buildLocationLabel(item));
        holder.tvPostedBy.setText(item.getPostedBy());
        holder.tvCondition.setText(item.getDisplayCondition());
        holder.tvCategory.setText(item.getDisplayCategory());
        
        // Set listing type badge
        String listingType = item.getListingType();
        if ("donation".equalsIgnoreCase(listingType)) {
            holder.tvListingType.setText("Donation");
            holder.tvListingType.setBackgroundResource(R.drawable.bg_chip_green); // Reuse green for donation
        } else {
            holder.tvListingType.setText("Swap");
            holder.tvListingType.setBackgroundResource(R.drawable.bg_chip_blue);
        }

        // Set avatar
        if (holder.ivAvatar != null && !TextUtils.isEmpty(item.getOwnerProfileImageUrl())) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
            Glide.with(context)
                .load(item.getOwnerProfileImageUrl())
                .circleCrop()
                .placeholder(R.drawable.bg_circle_placeholder_small)
                .into(holder.ivAvatar);
        } else {
            if (holder.ivAvatar != null) holder.ivAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText(getInitials(item.getPostedBy()));
        }

        // Set category text color based on category
        int textColor = R.color.primary_blue;
        String categoryKey = item.getRawCategory() != null ? item.getRawCategory().toLowerCase() : "";

        switch (categoryKey) {
            case "electronics":
                textColor = R.color.primary_blue;
                break;
            case "clothing":
                textColor = R.color.error_red;
                break;
            case "books":
                textColor = R.color.warning_orange;
                break;
            case "furniture":
                textColor = R.color.success_green;
                break;
            default:
                textColor = R.color.primary_green;
                break;
        }
        
        holder.tvCategory.setTextColor(context.getResources().getColor(textColor, null));
        
        if (!TextUtils.isEmpty(item.getDisplayImageUrl())) {
            Glide.with(context)
                    .load(item.getDisplayImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.ivItemImage);
        } else {
            holder.ivItemImage.setImageResource(R.drawable.ic_launcher_background);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onListingClicked(item);
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<MarketplaceFragment.MarketplaceItem> updatedItems) {
        items.clear();
        if (updatedItems != null) {
            items.addAll(updatedItems);
        }
        notifyDataSetChanged();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage, ivAvatar;
        TextView tvItemTitle, tvLocation, tvPostedBy, tvCondition, tvCategory, tvListingType, tvAvatar;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvListingType = itemView.findViewById(R.id.tvListingType);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
        }
    }

    private String buildLocationLabel(MarketplaceFragment.MarketplaceItem item) {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(item.getLocation())) {
            builder.append(item.getLocation());
        } else {
            builder.append(context.getString(R.string.location_unknown));
        }

        if (item.getDistanceKm() != null) {
            String distanceLabel = LocationUtils.formatDistanceLabel(item.getDistanceKm());
            builder.append(" • ").append(distanceLabel);
        } else if (item.isNearUser()) {
            builder.append(" • ").append(context.getString(R.string.location_near_you));
        }
        return builder.toString();
    }
}
