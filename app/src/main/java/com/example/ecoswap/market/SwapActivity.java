package com.example.ecoswap.market;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.adapters.ItemAdapter;
import com.example.ecoswap.models.Item;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class SwapActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewSwapItems;
    private ItemAdapter itemAdapter;
    private FloatingActionButton fabAddItem;
    private List<Item> swapItems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        loadSwapItems();
    }
    
    private void initViews() {
        recyclerViewSwapItems = findViewById(R.id.recyclerViewSwapItems);
        fabAddItem = findViewById(R.id.fabAddItem);
        swapItems = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewSwapItems.setLayoutManager(new GridLayoutManager(this, 2));
        itemAdapter = new ItemAdapter(swapItems, item -> {
            Toast.makeText(this, "Item: " + item.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerViewSwapItems.setAdapter(itemAdapter);
    }
    
    private void setupListeners() {
        fabAddItem.setOnClickListener(v -> {
            Toast.makeText(this, "Add item functionality to be implemented", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadSwapItems() {
        // TODO: Load swap items from Supabase
        Toast.makeText(this, "Loading swap items...", Toast.LENGTH_SHORT).show();
    }
}
