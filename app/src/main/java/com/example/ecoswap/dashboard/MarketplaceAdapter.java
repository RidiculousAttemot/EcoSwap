package com.example.ecoswap.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.google.android.material.chip.Chip;
import java.util.List;

public class MarketplaceAdapter extends RecyclerView.Adapter<MarketplaceAdapter.ViewHolder> {
    
    private List<MarketplaceFragment.MarketplaceItem> items;
    private Context context;
    
    public MarketplaceAdapter(List<MarketplaceFragment.MarketplaceItem> items, Context context) {
        this.items = items;
        this.context = context;
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
        holder.tvLocation.setText(item.getLocation());
        holder.tvPostedBy.setText("Posted by " + item.getPostedBy());
        holder.tvCondition.setText(item.getCondition());
        holder.chipCategory.setText(item.getCategory());
        
        // Set category chip color based on category
        int chipColor = R.color.chip_electronics;
        int textColor = R.color.primary_blue;
        
        switch (item.getCategory()) {
            case "Electronics":
                chipColor = R.color.chip_electronics;
                textColor = R.color.primary_blue;
                break;
            case "Clothing":
                chipColor = R.color.chip_clothing;
                textColor = R.color.error_red;
                break;
            case "Books":
                chipColor = R.color.chip_books;
                textColor = R.color.warning_orange;
                break;
            case "Furniture":
                chipColor = R.color.chip_furniture;
                textColor = R.color.success_green;
                break;
        }
        
        holder.chipCategory.setChipBackgroundColorResource(chipColor);
        holder.chipCategory.setTextColor(context.getResources().getColor(textColor, null));
        
        // TODO: Load image with Glide or Picasso if imageUrl is not null
        holder.ivItemImage.setImageResource(R.drawable.ic_launcher_background); // Placeholder
        
        // Favorite button click
        holder.ivFavorite.setOnClickListener(v -> {
            // TODO: Toggle favorite status
            holder.ivFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        });
    }
    
    @Override
    public int getItemCount() {
        return items.size();
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
}
