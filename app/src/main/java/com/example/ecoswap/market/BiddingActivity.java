package com.example.ecoswap.market;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.Item;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class BiddingActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewBiddingItems;
    private FloatingActionButton fabAddBiddingItem;
    private List<Item> biddingItems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bidding);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        loadBiddingItems();
    }
    
    private void initViews() {
        recyclerViewBiddingItems = findViewById(R.id.recyclerViewBiddingItems);
        fabAddBiddingItem = findViewById(R.id.fabAddBiddingItem);
        biddingItems = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewBiddingItems.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Setup bidding items adapter with click listener
    }
    
    private void setupListeners() {
        fabAddBiddingItem.setOnClickListener(v -> {
            Toast.makeText(this, "Add bidding item functionality to be implemented", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadBiddingItems() {
        // TODO: Load bidding items from Supabase
        Toast.makeText(this, "Loading bidding items...", Toast.LENGTH_SHORT).show();
    }
}
