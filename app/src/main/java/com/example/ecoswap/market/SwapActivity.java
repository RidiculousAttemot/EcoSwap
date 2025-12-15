package com.example.ecoswap.market;

import android.os.Bundle;
import android.widget.Button;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.adapters.ItemAdapter;
import com.example.ecoswap.models.Item;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SwapActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewSwapItems;
    private ItemAdapter itemAdapter;
    private FloatingActionButton fabAddItem;
    private List<Item> swapItems;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap);
        
        initViews();
        initDataProviders();
        setupRecyclerView();
        setupListeners();
        loadSwapItems();
    }
    
    private void initViews() {
        recyclerViewSwapItems = findViewById(R.id.recyclerViewSwapItems);
        fabAddItem = findViewById(R.id.fabAddItem);
        swapItems = new ArrayList<>();
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
        if (supabaseClient == null) {
            Toast.makeText(this, "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String endpoint = "/rest/v1/posts?" +
                "select=id,title,description,category,image_url,user_id,status,listing_type,current_bid&" +
                "listing_type=eq.swap&status=eq.available&order=created_at.desc&limit=30";

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    swapItems.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Item item = new Item();
                        item.setId(obj.optString("id"));
                        item.setName(obj.optString("title", "Swap item"));
                        item.setDescription(obj.optString("description", ""));
                        item.setCategory(obj.optString("category", "swap"));
                        item.setImageUrl(obj.optString("image_url", null));
                        item.setOwnerId(obj.optString("user_id", null));
                        item.setStatus(obj.optString("status", "available"));
                        item.setType(obj.optString("listing_type", "swap"));
                        if (!obj.isNull("current_bid")) {
                            item.setCurrentBid(obj.optDouble("current_bid", 0));
                        }
                        item.setCreatedAt(obj.optString("created_at", ""));

                        if (!TextUtils.isEmpty(item.getId())) {
                            swapItems.add(item);
                        }
                    }

                    itemAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(SwapActivity.this, "Failed to parse swaps", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SwapActivity.this, "Could not load swaps: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
