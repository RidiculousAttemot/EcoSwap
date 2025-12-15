package com.example.ecoswap.tracker;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Locale;

public class EcoTrackerActivity extends AppCompatActivity {
    
    private TextView tvTotalCO2Saved, tvTotalWaterSaved, tvTotalWasteDiverted;
    private TextView tvItemsSwapped, tvItemsDonated, tvTreesEquivalent;
    private SessionManager sessionManager;
    private SupabaseClient supabaseClient;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eco_tracker);
        
        sessionManager = SessionManager.getInstance(this);
        supabaseClient = SupabaseClient.getInstance(this);
        initViews();
        loadEcoStats();
    }
    
    private void initViews() {
        tvTotalCO2Saved = findViewById(R.id.tvTotalCO2Saved);
        tvTotalWaterSaved = findViewById(R.id.tvTotalWaterSaved);
        tvTotalWasteDiverted = findViewById(R.id.tvTotalWasteDiverted);
        tvItemsSwapped = findViewById(R.id.tvItemsSwapped);
        tvItemsDonated = findViewById(R.id.tvItemsDonated);
        tvTreesEquivalent = findViewById(R.id.tvTreesEquivalent);
    }
    
    private void loadEcoStats() {
        String userId = sessionManager != null ? sessionManager.getUserId() : null;
        if (supabaseClient == null || userId == null) {
            Toast.makeText(this, "Login required to load eco stats", Toast.LENGTH_SHORT).show();
            return;
        }
        String endpoint = "/rest/v1/eco_savings?user_id=eq." + userId + "&select=*";
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JsonArray array = new com.google.gson.Gson().fromJson(data.toString(), JsonArray.class);
                    if (array == null || array.size() == 0) {
                        applyEcoStats(0, 0, 0, 0, 0);
                        return;
                    }
                    JsonObject stats = array.get(0).getAsJsonObject();
                    double co2 = stats.has("co2_saved") && !stats.get("co2_saved").isJsonNull() ? stats.get("co2_saved").getAsDouble() : 0;
                    double water = stats.has("water_saved") && !stats.get("water_saved").isJsonNull() ? stats.get("water_saved").getAsDouble() : 0;
                    double waste = stats.has("waste_diverted") && !stats.get("waste_diverted").isJsonNull() ? stats.get("waste_diverted").getAsDouble() : 0;
                    int swapped = stats.has("items_swapped") && !stats.get("items_swapped").isJsonNull() ? stats.get("items_swapped").getAsInt() : 0;
                    int donated = stats.has("items_donated") && !stats.get("items_donated").isJsonNull() ? stats.get("items_donated").getAsInt() : 0;
                    applyEcoStats(co2, water, waste, swapped, donated);
                } catch (Exception e) {
                    applyEcoStats(0, 0, 0, 0, 0);
                }
            }

            @Override
            public void onError(String error) {
                applyEcoStats(0, 0, 0, 0, 0);
            }
        });
    }

    private void applyEcoStats(double co2Kg, double waterLiters, double wasteKg, int swapped, int donated) {
        runOnUiThread(() -> {
            tvTotalCO2Saved.setText(String.format(Locale.US, "%.1f kg CO2", co2Kg));
            tvTotalWaterSaved.setText(String.format(Locale.US, "%.1f liters", waterLiters));
            tvTotalWasteDiverted.setText(String.format(Locale.US, "%.1f kg", wasteKg));
            tvItemsSwapped.setText(String.format(Locale.US, "%d items", swapped));
            tvItemsDonated.setText(String.format(Locale.US, "%d items", donated));
            double trees = co2Kg / 21.0; // rough annual CO2 absorbed per tree
            tvTreesEquivalent.setText(String.format(Locale.US, "~%.1f trees", trees));
        });
    }
}
