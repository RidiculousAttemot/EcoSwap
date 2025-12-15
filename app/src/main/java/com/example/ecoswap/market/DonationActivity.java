package com.example.ecoswap.market;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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

public class DonationActivity extends AppCompatActivity {
    
    private RecyclerView recyclerViewDonations;
    private ItemAdapter itemAdapter;
    private FloatingActionButton fabAddDonation;
    private List<Item> donationItems;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        
        initViews();
        initDataProviders();
        setupRecyclerView();
        setupListeners();
        loadDonations();
    }
    
    private void initViews() {
        recyclerViewDonations = findViewById(R.id.recyclerViewDonations);
        fabAddDonation = findViewById(R.id.fabAddDonation);
        donationItems = new ArrayList<>();
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
        if (supabaseClient == null) {
            Toast.makeText(this, "Supabase not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String endpoint = "/rest/v1/posts?" +
                "select=id,title,description,category,image_url,user_id,status,listing_type,current_bid&" +
                "listing_type=eq.donation&status=eq.available&order=created_at.desc&limit=30";

        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    donationItems.clear();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Item item = new Item();
                        item.setId(obj.optString("id"));
                        item.setName(obj.optString("title", "Donation"));
                        item.setDescription(obj.optString("description", ""));
                        item.setCategory(obj.optString("category", "donation"));
                        item.setImageUrl(obj.optString("image_url", null));
                        item.setOwnerId(obj.optString("user_id", null));
                        item.setStatus(obj.optString("status", "available"));
                        item.setType(obj.optString("listing_type", "donation"));
                        item.setCreatedAt(obj.optString("created_at", ""));

                        if (!TextUtils.isEmpty(item.getId())) {
                            donationItems.add(item);
                        }
                    }

                    itemAdapter.notifyDataSetChanged();
                } catch (Exception e) {
                    Toast.makeText(DonationActivity.this, "Failed to parse donations", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(DonationActivity.this, "Could not load donations: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
