package com.example.ecoswap.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.Item;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    
    private List<Item> itemList;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(Item item);
    }
    
    public ItemAdapter(List<Item> itemList, OnItemClickListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_item, parent, false);
        return new ItemViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.bind(item, listener);
    }
    
    @Override
    public int getItemCount() {
        return itemList.size();
    }
    
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivItemImage;
        private TextView tvItemName, tvItemDescription, tvItemCategory;
        
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemDescription = itemView.findViewById(R.id.tvItemDescription);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
        }
        
        public void bind(Item item, OnItemClickListener listener) {
            tvItemName.setText(item.getName());
            tvItemDescription.setText(item.getDescription());
            tvItemCategory.setText(item.getCategory());
            
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
