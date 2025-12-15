package com.example.ecoswap.market;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.Item;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BiddingActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewBiddingItems;
    private FloatingActionButton fabAddBiddingItem;
    private List<Item> biddingItems;
    private BiddingItemAdapter biddingItemAdapter;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bidding);
        
        initViews();
        initDataProviders();
        setupRecyclerView();
        setupListeners();
        loadBiddingItems();
    }
    
    private void initViews() {
        recyclerViewBiddingItems = findViewById(R.id.recyclerViewBiddingItems);
        fabAddBiddingItem = findViewById(R.id.fabAddBiddingItem);
        biddingItems = new ArrayList<>();
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
        recyclerViewBiddingItems.setLayoutManager(new LinearLayoutManager(this));
        biddingItemAdapter = new BiddingItemAdapter(biddingItems, item -> {
            Intent intent = new Intent(this, BidDetailActivity.class);
            intent.putExtra("ITEM_ID", item.getId());
            intent.putExtra("ITEM_TITLE", item.getName());
            intent.putExtra("ITEM_IMAGE", item.getImageUrl());
            intent.putExtra("CURRENT_BID", item.getCurrentBid());
            intent.putExtra("BID_END_DATE", item.getCreatedAt());
            startActivity(intent);
        });
        recyclerViewBiddingItems.setAdapter(biddingItemAdapter);
    }
    
    private void setupListeners() {
        fabAddBiddingItem.setOnClickListener(v -> {
            Toast.makeText(this, "Use the Post tab to add a bidding listing", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadBiddingItems() {
        if (supabaseClient == null) {
            Toast.makeText(this, "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String endpoint = "/rest/v1/posts?" +
                "select=id,title,description,category,image_url,current_bid,starting_bid,bid_end_date&" +
                "category=eq.bidding&order=created_at.desc&limit=30";

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    biddingItems.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Item item = new Item();
                        item.setId(obj.optString("id"));
                        item.setName(obj.optString("title", "Bidding item"));
                        item.setDescription(obj.optString("description", ""));
                        item.setCategory("bidding");
                        item.setImageUrl(obj.optString("image_url", null));
                        item.setType("bidding");
                        item.setCurrentBid(obj.optDouble("current_bid", obj.optDouble("starting_bid", 0.0)));
                        item.setCreatedAt(obj.optString("bid_end_date", ""));

                        if (!TextUtils.isEmpty(item.getId())) {
                            biddingItems.add(item);
                        }
                    }

                    biddingItemAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(BiddingActivity.this, "Failed to parse bidding items", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(BiddingActivity.this, "Could not load bidding items: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class BiddingItemAdapter extends RecyclerView.Adapter<BiddingItemAdapter.BidItemViewHolder> {

        interface OnBidItemClickListener {
            void onBidClick(Item item);
        }

        private final List<Item> items;
        private final OnBidItemClickListener listener;

        BiddingItemAdapter(List<Item> items, OnBidItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public BidItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_item, parent, false);
            return new BidItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BidItemViewHolder holder, int position) {
            Item item = items.get(position);
            holder.bind(item, listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class BidItemViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivItemImage;
            private final TextView tvItemName;
            private final TextView tvItemDescription;
            private final TextView tvItemCategory;

            BidItemViewHolder(View itemView) {
                super(itemView);
                ivItemImage = itemView.findViewById(R.id.ivItemImage);
                tvItemName = itemView.findViewById(R.id.tvItemName);
                tvItemDescription = itemView.findViewById(R.id.tvItemDescription);
                tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            }

            void bind(Item item, OnBidItemClickListener listener) {
                tvItemName.setText(item.getName());
                tvItemDescription.setText(item.getDescription());
                String categoryText = "Current bid: $" + String.format(java.util.Locale.US, "%.2f", item.getCurrentBid());
                if (!TextUtils.isEmpty(item.getCreatedAt())) {
                    categoryText += " • Ends " + item.getCreatedAt();
                }
                tvItemCategory.setText(categoryText);

                if (!TextUtils.isEmpty(item.getImageUrl())) {
                    Glide.with(itemView)
                            .load(item.getImageUrl())
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(ivItemImage);
                } else {
                    ivItemImage.setImageResource(R.drawable.ic_launcher_background);
                }

                itemView.setOnClickListener(v -> listener.onBidClick(item));
            }
        }
    }
}
