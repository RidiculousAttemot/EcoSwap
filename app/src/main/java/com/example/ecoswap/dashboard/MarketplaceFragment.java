package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class MarketplaceFragment extends Fragment {
    
    private EditText etSearch;
    private Chip chipAll, chipElectronics, chipClothing, chipBooks, chipFurniture;
    private RecyclerView rvItems;
    private MarketplaceAdapter adapter;
    private List<MarketplaceItem> itemList;
    private String selectedCategory = "All";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_marketplace, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupCategoryFilters();
        loadItems();
        
        return view;
    }
    
    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        chipAll = view.findViewById(R.id.chipAll);
        chipElectronics = view.findViewById(R.id.chipElectronics);
        chipClothing = view.findViewById(R.id.chipClothing);
        chipBooks = view.findViewById(R.id.chipBooks);
        chipFurniture = view.findViewById(R.id.chipFurniture);
        rvItems = view.findViewById(R.id.rvItems);
    }
    
    private void setupRecyclerView() {
        itemList = new ArrayList<>();
        adapter = new MarketplaceAdapter(itemList, getContext());
        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvItems.setAdapter(adapter);
    }
    
    private void setupCategoryFilters() {
        View.OnClickListener categoryListener = v -> {
            // Reset all chips to default style
            resetChipStyles();
            
            // Set clicked chip as selected
            Chip clickedChip = (Chip) v;
            clickedChip.setChipBackgroundColorResource(R.color.primary_green);
            clickedChip.setTextColor(getResources().getColor(R.color.text_white, null));
            
            // Update selected category
            selectedCategory = clickedChip.getText().toString();
            
            // Filter items
            filterItems();
        };
        
        chipAll.setOnClickListener(categoryListener);
        chipElectronics.setOnClickListener(categoryListener);
        chipClothing.setOnClickListener(categoryListener);
        chipBooks.setOnClickListener(categoryListener);
        chipFurniture.setOnClickListener(categoryListener);
    }
    
    private void resetChipStyles() {
        Chip[] chips = {chipAll, chipElectronics, chipClothing, chipBooks, chipFurniture};
        for (Chip chip : chips) {
            chip.setChipBackgroundColorResource(R.color.background_white);
            chip.setTextColor(getResources().getColor(R.color.text_primary, null));
        }
    }
    
    private void filterItems() {
        // TODO: Implement filtering logic
        // For now, just reload all items
        loadItems();
    }
    
    private void loadItems() {
        // Sample data - Replace with actual Supabase data
        itemList.clear();
        
        // Add sample item (Vintage Desk Lamp from screenshot)
        MarketplaceItem sampleItem = new MarketplaceItem();
        sampleItem.setTitle("Vintage Desk Lamp");
        sampleItem.setLocation("2.3 km away");
        sampleItem.setPostedBy("Sarah");
        sampleItem.setCategory("Electronics");
        sampleItem.setCondition("Good");
        sampleItem.setImageUrl(null); // Will show placeholder
        
        itemList.add(sampleItem);
        
        // Add more sample items
        MarketplaceItem item2 = new MarketplaceItem();
        item2.setTitle("Leather Jacket");
        item2.setLocation("5.1 km away");
        item2.setPostedBy("Mike");
        item2.setCategory("Clothing");
        item2.setCondition("Excellent");
        itemList.add(item2);
        
        MarketplaceItem item3 = new MarketplaceItem();
        item3.setTitle("Harry Potter Collection");
        item3.setLocation("1.8 km away");
        item3.setPostedBy("Emma");
        item3.setCategory("Books");
        item3.setCondition("Good");
        itemList.add(item3);
        
        adapter.notifyDataSetChanged();
    }
    
    // Inner class for marketplace items
    public static class MarketplaceItem {
        private String title;
        private String location;
        private String postedBy;
        private String category;
        private String condition;
        private String imageUrl;
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getPostedBy() { return postedBy; }
        public void setPostedBy(String postedBy) { this.postedBy = postedBy; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }
}
