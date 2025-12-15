package com.example.ecoswap.market;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.gson.JsonObject;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BidDetailActivity extends AppCompatActivity {
    
    private ImageView ivItemImage;
    private TextView tvItemName, tvItemDescription, tvCurrentBid, tvBidEndDate;
    private EditText etBidAmount;
    private Button btnPlaceBid;
    private RecyclerView recyclerViewBids;
    private List<Bid> bidList;
    private BidListAdapter bidListAdapter;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private String itemId;
    private double currentBidValue = 0.0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bid_detail);
        
        itemId = getIntent().getStringExtra("ITEM_ID");
        if (TextUtils.isEmpty(itemId)) {
            Toast.makeText(this, "Missing bidding item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initDataProviders();
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

    private void initDataProviders() {
        sessionManager = SessionManager.getInstance(this);
        supabaseClient = SupabaseClient.getInstance(this);
        if (supabaseClient != null) {
            supabaseClient.hydrateSession(
                sessionManager.getAccessToken(),
                sessionManager.getRefreshToken(),
                sessionManager.getAccessTokenExpiry(),
                sessionManager.getUserId()
            );
        }
    }
    
    private void setupRecyclerView() {
        recyclerViewBids.setLayoutManager(new LinearLayoutManager(this));
        bidListAdapter = new BidListAdapter(bidList);
        recyclerViewBids.setAdapter(bidListAdapter);
    }
    
    private void setupListeners() {
        btnPlaceBid.setOnClickListener(v -> placeBid());
    }
    
    private void loadItemDetails() {
        if (supabaseClient == null) {
            Toast.makeText(this, "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String endpoint = String.format(Locale.US,
                "/rest/v1/posts?id=eq.%s&select=title,description,image_url,current_bid,starting_bid,bid_end_date",
                itemId);

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) {
                        Toast.makeText(BidDetailActivity.this, "Item not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    JSONObject obj = array.getJSONObject(0);
                    String title = obj.optString("title", "Bidding item");
                    String description = obj.optString("description", "");
                    String imageUrl = obj.optString("image_url", null);
                    double startingBid = obj.optDouble("starting_bid", 0.0);
                    currentBidValue = obj.optDouble("current_bid", startingBid);
                    String bidEnd = obj.optString("bid_end_date", "");

                    tvItemName.setText(title);
                    tvItemDescription.setText(description.isEmpty() ? "No description provided." : description);
                    tvCurrentBid.setText("Current Bid: " + formatCurrency(currentBidValue));
                    tvBidEndDate.setText(!TextUtils.isEmpty(bidEnd) ? "Ends: " + bidEnd : "No end date set");

                    if (!TextUtils.isEmpty(imageUrl)) {
                        Glide.with(BidDetailActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(ivItemImage);
                    }
                } catch (Exception e) {
                    Toast.makeText(BidDetailActivity.this, "Failed to load item", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BidDetailActivity.this, "Could not load item: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadBids() {
        if (supabaseClient == null) {
            return;
        }

        String endpoint = String.format(Locale.US,
                "/rest/v1/bids?select=*,profiles(name)&post_id=eq.%s&order=created_at.desc",
                itemId);

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    bidList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String bidderId = obj.optString("bidder_id", "");
                        double amount = obj.optDouble("amount", 0.0);
                        String createdAt = obj.optString("created_at", "");
                        JSONObject profile = obj.optJSONObject("profiles");
                        String bidderName = profile != null ? profile.optString("name", "Bidder") : "Bidder";

                        Bid bid = new Bid(id, itemId, bidderId, bidderName, amount, createdAt);
                        bidList.add(bid);
                    }
                    bidListAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(BidDetailActivity.this, "Failed to parse bids", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BidDetailActivity.this, "Could not load bids: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void placeBid() {
        String bidAmount = etBidAmount.getText().toString().trim();
        
        if (bidAmount.isEmpty()) {
            Toast.makeText(this, "Please enter bid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (supabaseClient == null) {
            Toast.makeText(this, "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = sessionManager != null ? sessionManager.getUserId() : null;
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Please login to place a bid", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(bidAmount);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= currentBidValue) {
            Toast.makeText(this, "Bid must exceed current bid", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceBid.setEnabled(false);

        JsonObject payload = new JsonObject();
        payload.addProperty("post_id", itemId);
        payload.addProperty("bidder_id", userId);
        payload.addProperty("amount", amount);
        payload.addProperty("status", "active");

        supabaseClient.insert("bids", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                currentBidValue = amount;
                tvCurrentBid.setText("Current Bid: " + formatCurrency(currentBidValue));
                etBidAmount.setText("");
                btnPlaceBid.setEnabled(true);
                updateCurrentBidOnPost(amount);
                loadBids();
                Toast.makeText(BidDetailActivity.this, "Bid placed", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                btnPlaceBid.setEnabled(true);
                Toast.makeText(BidDetailActivity.this, "Failed to place bid: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCurrentBidOnPost(double amount) {
        if (supabaseClient == null) {
            return;
        }
        JsonObject update = new JsonObject();
        update.addProperty("current_bid", amount);
        String endpoint = String.format(Locale.US, "/rest/v1/posts?id=eq.%s", itemId);
        supabaseClient.updateRecord(endpoint, update, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                // No-op; UI already updated
            }

            @Override
            public void onError(String error) {
                // Silent; bid already recorded
            }
        });
    }

    private String formatCurrency(double value) {
        return String.format(Locale.US, "$%.2f", value);
    }

    private String formatRelativeTime(String iso) {
        if (TextUtils.isEmpty(iso)) {
            return getString(R.string.just_now);
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(iso);
            if (date == null) return iso;
            long delta = System.currentTimeMillis() - date.getTime();
            long minutes = Math.max(1, delta / 60000);
            if (minutes < 60) return minutes + "m ago";
            long hours = minutes / 60;
            if (hours < 24) return hours + "h ago";
            long days = hours / 24;
            return days + "d ago";
        } catch (Exception e) {
            return iso;
        }
    }

    private class BidListAdapter extends RecyclerView.Adapter<BidListAdapter.BidViewHolder> {

        private final List<Bid> bids;

        BidListAdapter(List<Bid> bids) {
            this.bids = bids;
        }

        @Override
        public BidViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bid, parent, false);
            return new BidViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BidViewHolder holder, int position) {
            Bid bid = bids.get(position);
            holder.tvBidderName.setText(bid.getBidderName());
            holder.tvBidAmount.setText(formatCurrency(bid.getAmount()));
            holder.tvBidTime.setText(formatRelativeTime(bid.getTimestamp()));
            holder.tvBidderAvatar.setText(getInitials(bid.getBidderName()));
        }

        @Override
        public int getItemCount() {
            return bids.size();
        }

        class BidViewHolder extends RecyclerView.ViewHolder {
            final TextView tvBidderName;
            final TextView tvBidAmount;
            final TextView tvBidTime;
            final TextView tvBidderAvatar;

            BidViewHolder(View itemView) {
                super(itemView);
                tvBidderName = itemView.findViewById(R.id.tvBidderName);
                tvBidAmount = itemView.findViewById(R.id.tvBidAmount);
                tvBidTime = itemView.findViewById(R.id.tvBidTime);
                tvBidderAvatar = itemView.findViewById(R.id.tvBidderAvatar);
            }
        }
    }

    private String getInitials(String name) {
        if (TextUtils.isEmpty(name)) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.US);
    }
}
