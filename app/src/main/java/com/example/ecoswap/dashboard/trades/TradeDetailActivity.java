package com.example.ecoswap.dashboard.trades;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.example.ecoswap.utils.TradeProofUploader;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TradeDetailActivity extends AppCompatActivity {

    private static final String EXTRA_TRADE_ID = "extra_trade_id";
    private static final String EXTRA_TRADE_TYPE = "extra_trade_type";
    private static final String EXTRA_PRIMARY_TITLE = "extra_primary_title";
    private static final String EXTRA_SECONDARY_TITLE = "extra_secondary_title";
    private static final String EXTRA_PRIMARY_IMAGE = "extra_primary_image";
    private static final String EXTRA_SECONDARY_IMAGE = "extra_secondary_image";
    private static final String EXTRA_COUNTERPARTY_ID = "extra_counterparty_id";
    private static final String EXTRA_COUNTERPARTY_NAME = "extra_counterparty_name";
    private static final String EXTRA_CREATED_AT = "extra_created_at";
    private static final String EXTRA_COMPLETED_AT = "extra_completed_at";
    private static final String EXTRA_PROOF_URL = "extra_proof_url";
    private static final String EXTRA_PICKUP = "extra_pickup";
    private static final String EXTRA_CAN_UPLOAD_PROOF = "extra_can_upload_proof";
    private static final String EXTRA_CURRENT_USER = "extra_current_user";

    public static void launch(@NonNull Context context, @NonNull TradeRecord record, @NonNull String currentUserId) {
        Intent intent = new Intent(context, TradeDetailActivity.class);
        intent.putExtra(EXTRA_TRADE_ID, record.getId());
        intent.putExtra(EXTRA_TRADE_TYPE, record.getType() == TradeRecord.TradeType.SWAP ? "swap" : "donation");
        intent.putExtra(EXTRA_PRIMARY_TITLE, record.getPrimaryItem() != null ? record.getPrimaryItem().getTitle() : null);
        intent.putExtra(EXTRA_SECONDARY_TITLE, record.getSecondaryItem() != null ? record.getSecondaryItem().getTitle() : null);
        intent.putExtra(EXTRA_PRIMARY_IMAGE, record.getPrimaryItem() != null ? record.getPrimaryItem().getImageUrl() : null);
        intent.putExtra(EXTRA_SECONDARY_IMAGE, record.getSecondaryItem() != null ? record.getSecondaryItem().getImageUrl() : null);
        intent.putExtra(EXTRA_COUNTERPARTY_ID, record.getCounterpartyId());
        intent.putExtra(EXTRA_COUNTERPARTY_NAME, record.getCounterpartyName());
        intent.putExtra(EXTRA_CREATED_AT, record.getCreatedAtEpochMs());
        intent.putExtra(EXTRA_COMPLETED_AT, record.getCompletedAtEpochMs() != null ? record.getCompletedAtEpochMs() : 0L);
        intent.putExtra(EXTRA_PROOF_URL, record.getProofPhotoUrl());
        intent.putExtra(EXTRA_PICKUP, record.getPickupLocation());
        intent.putExtra(EXTRA_CAN_UPLOAD_PROOF, record.canUploadProof());
        intent.putExtra(EXTRA_CURRENT_USER, currentUserId);
        context.startActivity(intent);
    }

    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;

    private RatingBar ratingBar;
    private MaterialButton btnSubmit;
    private TextView tvRatingState;
    private MaterialButton btnAddProof;
    private MaterialButton btnViewProof;
    private TextView tvProofStatus;
    private ImageView ivProofPreview;
    private ImageView ivPrimary;
    private ImageView ivSecondary;
    private ScrollView scrollView;

    private ActivityResultLauncher<String> proofPickerLauncher;
    private String tradeId;
    private String tradeType;
    private String counterpartyId;
    private String counterpartyName;
    private String currentUserId;
    private String proofUrl;
    private boolean canUploadProof;
    private boolean isCompleted;
    private boolean ratingAvailable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trade_detail);

        sessionManager = SessionManager.getInstance(this);
        supabaseClient = SupabaseClient.getInstance(this);
        supabaseClient.hydrateSession(
                sessionManager.getAccessToken(),
                sessionManager.getRefreshToken(),
                sessionManager.getAccessTokenExpiry(),
                sessionManager.getUserId());
        bindViews();
        bindArgs();
        bindUi();
        setupProofPicker();
        loadExistingReview();
    }

    private void bindViews() {
        scrollView = findViewById(R.id.tradeDetailScroll);
        ratingBar = findViewById(R.id.ratingBar);
        btnSubmit = findViewById(R.id.btnSubmitRating);
        tvRatingState = findViewById(R.id.tvRatingState);
        btnAddProof = findViewById(R.id.btnAddProof);
        btnViewProof = findViewById(R.id.btnViewProof);
        tvProofStatus = findViewById(R.id.tvProofStatus);
        ivProofPreview = findViewById(R.id.ivProofPreview);
        ivPrimary = findViewById(R.id.ivPrimary);
        ivSecondary = findViewById(R.id.ivSecondary);
    }

    private void bindArgs() {
        Intent intent = getIntent();
        tradeId = intent.getStringExtra(EXTRA_TRADE_ID);
        tradeType = intent.getStringExtra(EXTRA_TRADE_TYPE);
        counterpartyId = intent.getStringExtra(EXTRA_COUNTERPARTY_ID);
        counterpartyName = intent.getStringExtra(EXTRA_COUNTERPARTY_NAME);
        currentUserId = intent.getStringExtra(EXTRA_CURRENT_USER);
        proofUrl = intent.getStringExtra(EXTRA_PROOF_URL);
        canUploadProof = intent.getBooleanExtra(EXTRA_CAN_UPLOAD_PROOF, false);
        ratingAvailable = !TextUtils.isEmpty(tradeId) && !TextUtils.isEmpty(counterpartyId) && !TextUtils.isEmpty(currentUserId);
        if (TextUtils.isEmpty(counterpartyName) && !TextUtils.isEmpty(counterpartyId)) {
            fetchCounterpartyName();
        }
    }

    private void bindUi() {
        Intent intent = getIntent();
        String primaryTitle = intent.getStringExtra(EXTRA_PRIMARY_TITLE);
        String secondaryTitle = intent.getStringExtra(EXTRA_SECONDARY_TITLE);
        String primaryImage = intent.getStringExtra(EXTRA_PRIMARY_IMAGE);
        String secondaryImage = intent.getStringExtra(EXTRA_SECONDARY_IMAGE);
        long createdAt = intent.getLongExtra(EXTRA_CREATED_AT, 0L);
        long completedAt = intent.getLongExtra(EXTRA_COMPLETED_AT, 0L);
        String pickup = intent.getStringExtra(EXTRA_PICKUP);

        isCompleted = completedAt > 0;
        if (!isCompleted && !TextUtils.isEmpty(tradeType)) {
            isCompleted = true; // completed list implies completion; keeps proof enabled
        }
        canUploadProof = canUploadProof || isCompleted;

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvPartner = findViewById(R.id.tvPartner);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvPrimaryTitle = findViewById(R.id.tvPrimaryTitle);
        TextView tvSecondaryTitle = findViewById(R.id.tvSecondaryTitle);
        TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);
        TextView tvCompletedAt = findViewById(R.id.tvCompletedAt);
        TextView tvPickup = findViewById(R.id.tvPickup);

        tvTitle.setText(tradeType != null && tradeType.equals("donation")
                ? getString(R.string.listing_type_donation)
                : getString(R.string.listing_type_swap));
        if ("donation".equalsIgnoreCase(tradeType)) {
            String donationPartner = !TextUtils.isEmpty(counterpartyName)
                ? counterpartyName
                : getString(R.string.trade_history_receiver_placeholder);
            tvPartner.setText(getString(R.string.trade_history_donation_with, donationPartner));
        } else {
            tvPartner.setText(!TextUtils.isEmpty(counterpartyName)
                ? getString(R.string.trade_history_swap_with, counterpartyName)
                : getString(R.string.trade_history_partner_unknown));
        }
        tvStatus.setText(R.string.trade_history_confirmed_label);

        tvPrimaryTitle.setText(!TextUtils.isEmpty(primaryTitle) ? primaryTitle : getString(R.string.app_name));
        if (!TextUtils.isEmpty(secondaryTitle)) {
            tvSecondaryTitle.setText(secondaryTitle);
            tvSecondaryTitle.setVisibility(View.VISIBLE);
        } else {
            tvSecondaryTitle.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(primaryImage)) {
            Glide.with(this).load(primaryImage).centerCrop().placeholder(R.drawable.ic_launcher_background).into(ivPrimary);
        }
        if (!TextUtils.isEmpty(secondaryImage)) {
            ivSecondary.setVisibility(View.VISIBLE);
            Glide.with(this).load(secondaryImage).centerCrop().placeholder(R.drawable.ic_launcher_background).into(ivSecondary);
        } else {
            ivSecondary.setVisibility(View.GONE);
        }

        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        fmt.setTimeZone(TimeZone.getDefault());
        if (createdAt > 0) {
            tvCreatedAt.setText(getString(R.string.trade_detail_created, fmt.format(new Date(createdAt))));
        }
        if (completedAt > 0) {
            tvCompletedAt.setText(getString(R.string.trade_detail_completed, fmt.format(new Date(completedAt))));
        }
        boolean hasPickup = !TextUtils.isEmpty(pickup) && !"null".equalsIgnoreCase(pickup);
        if (hasPickup) {
            tvPickup.setVisibility(View.VISIBLE);
            tvPickup.setText(getString(R.string.pickup_location_label, pickup));
        } else {
            tvPickup.setVisibility(View.GONE);
        }

        if (!ratingAvailable) {
            ratingBar.setIsIndicator(true);
            btnSubmit.setEnabled(false);
            tvRatingState.setText(R.string.trade_detail_rating_unavailable);
        }

        renderProofSection();
        btnSubmit.setOnClickListener(v -> submitRating());
    }

    private void renderProofSection() {
        boolean hasProof = !TextUtils.isEmpty(proofUrl);
        tvProofStatus.setText(hasProof
            ? getString(R.string.trade_history_proof_uploaded)
            : (canUploadProof ? getString(R.string.trade_history_proof_missing) : getString(R.string.trade_history_proof_locked)));

        btnViewProof.setVisibility(hasProof ? View.VISIBLE : View.GONE);
        btnViewProof.setOnClickListener(hasProof ? v -> openProof() : null);

        if (ivProofPreview != null) {
            if (hasProof) {
                ivProofPreview.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(proofUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(ivProofPreview);
            } else {
                ivProofPreview.setVisibility(View.GONE);
            }
        }

        if (canUploadProof) {
            btnAddProof.setVisibility(View.VISIBLE);
            btnAddProof.setText(hasProof
                    ? getString(R.string.trade_history_replace_proof)
                    : getString(R.string.trade_history_add_proof));
            btnAddProof.setOnClickListener(v -> launchProofPicker());
            btnAddProof.setAlpha(1f);
            btnAddProof.setEnabled(true);
        } else {
            btnAddProof.setVisibility(View.GONE);
            btnAddProof.setOnClickListener(null);
        }
    }

    private void fetchCounterpartyName() {
        if (supabaseClient == null || TextUtils.isEmpty(counterpartyId)) return;
        String endpoint = "/rest/v1/profiles?select=name&id=eq." + counterpartyId;
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject profile = array.getJSONObject(0);
                        String name = profile.optString("name", null);
                        if (!TextUtils.isEmpty(name)) {
                            counterpartyName = name;
                            runOnUiThread(() -> {
                                TextView tvPartner = findViewById(R.id.tvPartner);
                                if (tvPartner != null) {
                                    if ("donation".equalsIgnoreCase(tradeType)) {
                                        tvPartner.setText(getString(R.string.trade_history_donation_with, name));
                                    } else {
                                        tvPartner.setText(getString(R.string.trade_history_swap_with, name));
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception ignored) { }
            }

            @Override
            public void onError(String error) {
                // best-effort
            }
        });
    }

    private void setupProofPicker() {
        proofPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                beginProofUpload(uri);
            }
        });
    }

    private void launchProofPicker() {
        if (!canUploadProof) return;
        proofPickerLauncher.launch("image/*");
    }

    private void beginProofUpload(@NonNull Uri uri) {
        if (TextUtils.isEmpty(tradeId) || supabaseClient == null || TextUtils.isEmpty(currentUserId)) {
            return;
        }
        btnAddProof.setEnabled(false);
        btnAddProof.setAlpha(0.6f);
        TradeProofUploader.upload(this, supabaseClient, currentUserId, tradeId, uri, new TradeProofUploader.Callback() {
            @Override
            public void onUploadSuccess(@NonNull String publicUrl) {
                proofUrl = publicUrl;
                canUploadProof = true;
                renderProofSection();
                Toast.makeText(TradeDetailActivity.this, R.string.trade_history_proof_upload_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUploadError(@NonNull String message) {
                btnAddProof.setEnabled(true);
                btnAddProof.setAlpha(1f);
                Toast.makeText(TradeDetailActivity.this, R.string.trade_history_proof_upload_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openProof() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(proofUrl)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.trade_history_proof_upload_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadExistingReview() {
        if (supabaseClient == null || !ratingAvailable) {
            return;
        }
        String endpoint = "/rest/v1/reviews?select=rating,comment&trade_id=eq." + tradeId + "&rater_id=eq." + currentUserId;
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject review = array.getJSONObject(0);
                        int rating = review.optInt("rating", 0);
                        if (rating > 0) {
                            ratingBar.setRating(rating);
                            ratingBar.setIsIndicator(true);
                            btnSubmit.setEnabled(false);
                            tvRatingState.setText(getString(R.string.trade_detail_already_rated, rating));
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onError(String error) {
                // no-op
            }
        });
    }

    private void submitRating() {
        if (supabaseClient == null || !ratingAvailable) {
            Toast.makeText(this, R.string.trade_detail_rating_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        int rating = Math.round(ratingBar.getRating());
        if (rating < 1) {
            Toast.makeText(this, R.string.trade_detail_pick_rating, Toast.LENGTH_SHORT).show();
            return;
        }
        btnSubmit.setEnabled(false);
        JsonObject payload = new JsonObject();
        payload.addProperty("trade_id", tradeId);
        payload.addProperty("trade_type", tradeType);
        payload.addProperty("rater_id", currentUserId);
        payload.addProperty("ratee_id", counterpartyId);
        payload.addProperty("rating", rating);

        supabaseClient.insert("reviews", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                updateProfileAggregate(rating);
            }

            @Override
            public void onError(String error) {
                btnSubmit.setEnabled(true);
                Toast.makeText(TradeDetailActivity.this, getString(R.string.trade_detail_submit_error, error), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateProfileAggregate(int newRating) {
        if (supabaseClient == null || TextUtils.isEmpty(counterpartyId)) {
            return;
        }
        String aggEndpoint = "/rest/v1/reviews?select=avg:avg(rating),count:count(id)&ratee_id=eq." + counterpartyId;
        supabaseClient.query(aggEndpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject row = array.getJSONObject(0);
                        double avg = newRating;
                        if (row.has("avg")) {
                            Object avgNode = row.get("avg");
                            if (avgNode instanceof JSONObject) {
                                avg = ((JSONObject) avgNode).optDouble("rating", newRating);
                            } else {
                                avg = row.optDouble("avg", newRating);
                            }
                        }

                        int count = 1;
                        if (row.has("count")) {
                            Object countNode = row.get("count");
                            if (countNode instanceof Number) {
                                count = ((Number) countNode).intValue();
                            }
                        }

                        JsonObject update = new JsonObject();
                        update.addProperty("rating", avg);
                        update.addProperty("review_count", count);
                        supabaseClient.update("profiles", counterpartyId, update, new SupabaseClient.OnDatabaseCallback() {
                            @Override
                            public void onSuccess(Object data) {
                                ratingBar.setIsIndicator(true);
                                tvRatingState.setText(getString(R.string.trade_detail_submitted, newRating));
                                Toast.makeText(TradeDetailActivity.this, R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                ratingBar.setIsIndicator(true);
                                tvRatingState.setText(getString(R.string.trade_detail_submitted, newRating));
                                Toast.makeText(TradeDetailActivity.this, R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception ignored) {
                    ratingBar.setIsIndicator(true);
                    tvRatingState.setText(getString(R.string.trade_detail_submitted, newRating));
                    Toast.makeText(TradeDetailActivity.this, R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                ratingBar.setIsIndicator(true);
                tvRatingState.setText(getString(R.string.trade_detail_submitted, newRating));
                Toast.makeText(TradeDetailActivity.this, R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
