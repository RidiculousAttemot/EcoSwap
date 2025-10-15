package com.example.ecoswap.market;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.Bid;
import java.util.ArrayList;
import java.util.List;

public class BidDetailActivity extends AppCompatActivity {
    
    private ImageView ivItemImage;
    private TextView tvItemName, tvItemDescription, tvCurrentBid, tvBidEndDate;
    private EditText etBidAmount;
    private Button btnPlaceBid;
    private RecyclerView recyclerViewBids;
    private List<Bid> bidList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bid_detail);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        loadItemDetails();
        loadBids();
    }
    
    private void initViews() {
        ivItemImage = findViewById(R.id.ivItemImage);
        tvItemName = findViewById(R.id.tvItemName);
        tvItemDescription = findViewById(R.id.tvItemDescription);
        tvCurrentBid = findViewById(R.id.tvCurrentBid);
        tvBidEndDate = findViewById(R.id.tvBidEndDate);
        etBidAmount = findViewById(R.id.etBidAmount);
        btnPlaceBid = findViewById(R.id.btnPlaceBid);
        recyclerViewBids = findViewById(R.id.recyclerViewBids);
        bidList = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewBids.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Setup bid adapter
    }
    
    private void setupListeners() {
        btnPlaceBid.setOnClickListener(v -> placeBid());
    }
    
    private void loadItemDetails() {
        // TODO: Load item details from Supabase
        tvItemName.setText("Item Name");
        tvItemDescription.setText("Item description goes here...");
        tvCurrentBid.setText("Current Bid: $50");
        tvBidEndDate.setText("Ends: October 20, 2025");
    }
    
    private void loadBids() {
        // TODO: Load bids from Supabase
        Toast.makeText(this, "Loading bids...", Toast.LENGTH_SHORT).show();
    }
    
    private void placeBid() {
        String bidAmount = etBidAmount.getText().toString().trim();
        
        if (bidAmount.isEmpty()) {
            Toast.makeText(this, "Please enter bid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: Place bid via Supabase
        Toast.makeText(this, "Bid placement functionality to be implemented", Toast.LENGTH_SHORT).show();
    }
}
