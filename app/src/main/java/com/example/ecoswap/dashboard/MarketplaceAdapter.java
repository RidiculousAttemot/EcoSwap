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
        holder.tvPostedBy.setText(context.getString(R.string.posted_by_format, item.getPostedBy()));
        holder.tvCondition.setText(item.getDisplayCondition());
        holder.chipCategory.setText(item.getDisplayCategory());
        
        // Set category chip color based on category
        int chipColor = R.color.chip_electronics;
        int textColor = R.color.primary_blue;
        String categoryKey = item.getRawCategory() != null ? item.getRawCategory().toLowerCase() : "";

        switch (categoryKey) {
            case "electronics":
                chipColor = R.color.chip_electronics;
                textColor = R.color.primary_blue;
                break;
            case "clothing":
                chipColor = R.color.chip_clothing;
                textColor = R.color.error_red;
                break;
            case "books":
                chipColor = R.color.chip_books;
                textColor = R.color.warning_orange;
                break;
            case "furniture":
                chipColor = R.color.chip_furniture;
                textColor = R.color.success_green;
                break;
            case "donation":
                chipColor = R.color.chip_books;
                textColor = R.color.primary_green;
                break;
            case "swap":
                chipColor = R.color.chip_clothing;
                textColor = R.color.primary_blue;
                break;
            default:
                chipColor = R.color.chip_electronics;
                textColor = R.color.primary_blue;
                break;
        }
        
        holder.chipCategory.setChipBackgroundColorResource(chipColor);
        holder.chipCategory.setTextColor(context.getResources().getColor(textColor, null));
        
        if (!TextUtils.isEmpty(item.getImageUrl())) {
            Glide.with(context)
                    .load(item.getImageUrl())
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
        ImageView ivItemImage, ivFavorite;
        TextView tvItemTitle, tvLocation, tvPostedBy, tvCondition;
        Chip chipCategory;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            tvCondition = itemView.findViewById(R.id.tvCondition);
            chipCategory = itemView.findViewById(R.id.chipCategory);
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
