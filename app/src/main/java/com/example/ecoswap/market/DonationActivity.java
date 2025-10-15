package com.example.ecoswap.market;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.adapters.ItemAdapter;
import com.example.ecoswap.models.Item;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class DonationActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewDonations;
    private ItemAdapter itemAdapter;
    private FloatingActionButton fabAddDonation;
    private List<Item> donationItems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        loadDonations();
    }
    
    private void initViews() {
        recyclerViewDonations = findViewById(R.id.recyclerViewDonations);
        fabAddDonation = findViewById(R.id.fabAddDonation);
        donationItems = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewDonations.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(donationItems, item -> {
            Toast.makeText(this, "Donation: " + item.getName(), Toast.LENGTH_SHORT).show();
        });
        recyclerViewDonations.setAdapter(itemAdapter);
    }
    
    private void setupListeners() {
        fabAddDonation.setOnClickListener(v -> {
            Toast.makeText(this, "Add donation functionality to be implemented", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadDonations() {
        // TODO: Load donations from Supabase
        Toast.makeText(this, "Loading donations...", Toast.LENGTH_SHORT).show();
    }
}
