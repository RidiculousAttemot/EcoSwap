package com.example.ecoswap.dashboard.trades;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TradeDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TRADE_ID = "arg_trade_id";
    private static final String ARG_TRADE_TYPE = "arg_trade_type";
    private static final String ARG_PRIMARY_TITLE = "arg_primary_title";
    private static final String ARG_SECONDARY_TITLE = "arg_secondary_title";
    private static final String ARG_PRIMARY_IMAGE = "arg_primary_image";
    private static final String ARG_SECONDARY_IMAGE = "arg_secondary_image";
    private static final String ARG_COUNTERPARTY_ID = "arg_counterparty_id";
    private static final String ARG_COUNTERPARTY_NAME = "arg_counterparty_name";
    private static final String ARG_CREATED_AT = "arg_created_at";
    private static final String ARG_COMPLETED_AT = "arg_completed_at";
    private static final String ARG_PROOF_URL = "arg_proof_url";
    private static final String ARG_CURRENT_USER = "arg_current_user";

    private SupabaseClient supabaseClient;
    private RatingBar ratingBar;
    private MaterialButton btnSubmit;
    private TextView tvRatingState;

    private String tradeId;
    private String tradeType;
    private String counterpartyId;
    private String counterpartyName;
    private String currentUserId;

    public static TradeDetailBottomSheet newInstance(@NonNull TradeRecord record, @NonNull String currentUserId) {
        Bundle args = new Bundle();
        args.putString(ARG_TRADE_ID, record.getId());
        args.putString(ARG_TRADE_TYPE, record.getType() == TradeRecord.TradeType.SWAP ? "swap" : "donation");
        args.putString(ARG_PRIMARY_TITLE, record.getPrimaryItem() != null ? record.getPrimaryItem().getTitle() : null);
        args.putString(ARG_SECONDARY_TITLE, record.getSecondaryItem() != null ? record.getSecondaryItem().getTitle() : null);
        args.putString(ARG_PRIMARY_IMAGE, record.getPrimaryItem() != null ? record.getPrimaryItem().getImageUrl() : null);
        args.putString(ARG_SECONDARY_IMAGE, record.getSecondaryItem() != null ? record.getSecondaryItem().getImageUrl() : null);
        args.putString(ARG_COUNTERPARTY_ID, record.getCounterpartyId());
        args.putString(ARG_COUNTERPARTY_NAME, record.getCounterpartyName());
        args.putLong(ARG_CREATED_AT, record.getCreatedAtEpochMs());
        args.putLong(ARG_COMPLETED_AT, record.getCompletedAtEpochMs() != null ? record.getCompletedAtEpochMs() : 0L);
        args.putString(ARG_PROOF_URL, record.getProofPhotoUrl());
        args.putString(ARG_CURRENT_USER, currentUserId);
        TradeDetailBottomSheet sheet = new TradeDetailBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_trade_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        supabaseClient = SupabaseClient.getInstance(requireContext());
        bindArgs();
        bindUi(view);
        loadExistingReview();
    }

    private void bindArgs() {
        Bundle args = getArguments();
        if (args == null) return;
        tradeId = args.getString(ARG_TRADE_ID);
        tradeType = args.getString(ARG_TRADE_TYPE);
        counterpartyId = args.getString(ARG_COUNTERPARTY_ID);
        counterpartyName = args.getString(ARG_COUNTERPARTY_NAME);
        currentUserId = args.getString(ARG_CURRENT_USER);
    }

    private void bindUi(@NonNull View root) {
        TextView tvTitle = root.findViewById(R.id.tvTitle);
        TextView tvPartner = root.findViewById(R.id.tvPartner);
        TextView tvStatus = root.findViewById(R.id.tvStatus);
        TextView tvPrimaryTitle = root.findViewById(R.id.tvPrimaryTitle);
        TextView tvSecondaryTitle = root.findViewById(R.id.tvSecondaryTitle);
        TextView tvCreatedAt = root.findViewById(R.id.tvCreatedAt);
        TextView tvCompletedAt = root.findViewById(R.id.tvCompletedAt);
        TextView tvProof = root.findViewById(R.id.tvProof);
        ImageView ivPrimary = root.findViewById(R.id.ivPrimary);
        ImageView ivSecondary = root.findViewById(R.id.ivSecondary);
        ratingBar = root.findViewById(R.id.ratingBar);
        btnSubmit = root.findViewById(R.id.btnSubmitRating);
        tvRatingState = root.findViewById(R.id.tvRatingState);

        Bundle args = getArguments();
        if (args == null) return;

        String primaryTitle = args.getString(ARG_PRIMARY_TITLE);
        String secondaryTitle = args.getString(ARG_SECONDARY_TITLE);
        String primaryImage = args.getString(ARG_PRIMARY_IMAGE);
        String secondaryImage = args.getString(ARG_SECONDARY_IMAGE);
        long createdAt = args.getLong(ARG_CREATED_AT, 0L);
        long completedAt = args.getLong(ARG_COMPLETED_AT, 0L);
        String proofUrl = args.getString(ARG_PROOF_URL);

        tvTitle.setText(tradeType != null && tradeType.equals("donation") ? getString(R.string.listing_type_donation) : getString(R.string.listing_type_swap));
        tvPartner.setText(!TextUtils.isEmpty(counterpartyName)
                ? getString(R.string.trade_history_swap_with, counterpartyName)
                : getString(R.string.trade_history_partner_unknown));
        tvStatus.setText(R.string.trade_history_confirmed_label);

        tvPrimaryTitle.setText(!TextUtils.isEmpty(primaryTitle) ? primaryTitle : getString(R.string.app_name));
        if (!TextUtils.isEmpty(secondaryTitle)) {
            tvSecondaryTitle.setText(secondaryTitle);
            tvSecondaryTitle.setVisibility(View.VISIBLE);
        } else {
            tvSecondaryTitle.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(primaryImage)) {
            Glide.with(requireContext()).load(primaryImage).centerCrop().placeholder(R.drawable.ic_launcher_background).into(ivPrimary);
        }
        if (!TextUtils.isEmpty(secondaryImage)) {
            ivSecondary.setVisibility(View.VISIBLE);
            Glide.with(requireContext()).load(secondaryImage).centerCrop().placeholder(R.drawable.ic_launcher_background).into(ivSecondary);
        } else {
            ivSecondary.setVisibility(View.GONE);
        }

        SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());
        fmt.setTimeZone(TimeZone.getDefault());
        if (createdAt > 0) {
            tvCreatedAt.setText(getString(R.string.trade_detail_created, fmt.format(createdAt)));
        }
        if (completedAt > 0) {
            tvCompletedAt.setText(getString(R.string.trade_detail_completed, fmt.format(completedAt)));
        }

        if (!TextUtils.isEmpty(proofUrl)) {
            tvProof.setText(getString(R.string.trade_detail_proof_link, proofUrl));
        } else {
            tvProof.setText(R.string.trade_detail_no_proof);
        }

        btnSubmit.setOnClickListener(v -> submitRating());
    }

    private void loadExistingReview() {
        if (supabaseClient == null || TextUtils.isEmpty(tradeId) || TextUtils.isEmpty(currentUserId)) {
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
                // ignore
            }
        });
    }

    private void submitRating() {
        if (supabaseClient == null || TextUtils.isEmpty(tradeId) || TextUtils.isEmpty(counterpartyId) || TextUtils.isEmpty(currentUserId)) {
            return;
        }
        int rating = Math.round(ratingBar.getRating());
        if (rating < 1) {
            Toast.makeText(requireContext(), R.string.trade_detail_pick_rating, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(requireContext(), getString(R.string.trade_detail_submit_error, error), Toast.LENGTH_LONG).show();
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
                                Toast.makeText(requireContext(), R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                ratingBar.setIsIndicator(true);
                                tvRatingState.setText(getString(R.string.trade_detail_submitted, newRating));
                                Toast.makeText(requireContext(), R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception ignored) {
                    ratingBar.setIsIndicator(true);
                    tvRatingState.setText(getString(R.string.trade_detail_submitted, newRating));
                    Toast.makeText(requireContext(), R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                ratingBar.setIsIndicator(true);
                tvRatingState.setText(getString(R.string.trade_detail_submitted, newRating));
                Toast.makeText(requireContext(), R.string.trade_detail_thanks, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
