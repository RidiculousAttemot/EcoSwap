package com.example.ecoswap.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.chat.ChatFragment;
import com.example.ecoswap.utils.LocationUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

public class ListingPreviewBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_LISTING_ID = "arg_listing_id";
    private static final String ARG_OWNER_ID = "arg_owner_id";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESCRIPTION = "arg_description";
    private static final String ARG_IMAGE_URL = "arg_image_url";
    private static final String ARG_LOCATION = "arg_location";
    private static final String ARG_POSTED_BY = "arg_posted_by";
    private static final String ARG_CATEGORY = "arg_category";
    private static final String ARG_CATEGORY_LABEL = "arg_category_label";
    private static final String ARG_CONDITION_LABEL = "arg_condition_label";
    private static final String ARG_DISTANCE_KM = "arg_distance_km";
    private static final String ARG_HAS_DISTANCE = "arg_has_distance";
    private static final String ARG_NEAR_USER = "arg_near_user";

    private String listingId;
    private String ownerId;
    private String title;
    private String description;
    private String imageUrl;
    private String location;
    private String postedBy;
    private String rawCategory;
    private String displayCategory;
    private String displayCondition;
    private Double distanceKm;
    private boolean isNearUser;

    public static ListingPreviewBottomSheet newInstance(@NonNull MarketplaceFragment.MarketplaceItem item) {
        ListingPreviewBottomSheet sheet = new ListingPreviewBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_LISTING_ID, item.getId());
        args.putString(ARG_OWNER_ID, item.getOwnerId());
        args.putString(ARG_TITLE, item.getTitle());
        args.putString(ARG_DESCRIPTION, item.getDescription());
        args.putString(ARG_IMAGE_URL, item.getImageUrl());
        args.putString(ARG_LOCATION, item.getLocation());
        args.putString(ARG_POSTED_BY, item.getPostedBy());
        args.putString(ARG_CATEGORY, item.getRawCategory());
        args.putString(ARG_CATEGORY_LABEL, item.getDisplayCategory());
        args.putString(ARG_CONDITION_LABEL, item.getDisplayCondition());
        if (item.getDistanceKm() != null) {
            args.putBoolean(ARG_HAS_DISTANCE, true);
            args.putDouble(ARG_DISTANCE_KM, item.getDistanceKm());
        }
        args.putBoolean(ARG_NEAR_USER, item.isNearUser());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_listing_preview, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            listingId = args.getString(ARG_LISTING_ID);
            ownerId = args.getString(ARG_OWNER_ID);
            title = args.getString(ARG_TITLE);
            description = args.getString(ARG_DESCRIPTION);
            imageUrl = args.getString(ARG_IMAGE_URL);
            location = args.getString(ARG_LOCATION);
            postedBy = args.getString(ARG_POSTED_BY);
            rawCategory = args.getString(ARG_CATEGORY);
            displayCategory = args.getString(ARG_CATEGORY_LABEL);
            displayCondition = args.getString(ARG_CONDITION_LABEL);
            if (args.getBoolean(ARG_HAS_DISTANCE, false)) {
                distanceKm = args.getDouble(ARG_DISTANCE_KM);
            }
            isNearUser = args.getBoolean(ARG_NEAR_USER, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView ivPreviewImage = view.findViewById(R.id.ivPreviewImage);
        TextView tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewPostedBy = view.findViewById(R.id.tvPreviewPostedBy);
        TextView tvPreviewLocation = view.findViewById(R.id.tvPreviewLocation);
        TextView tvPreviewDistance = view.findViewById(R.id.tvPreviewDistance);
        TextView tvPreviewDescription = view.findViewById(R.id.tvPreviewDescription);
        Chip chipPreviewCategory = view.findViewById(R.id.chipPreviewCategory);
        Chip chipPreviewCondition = view.findViewById(R.id.chipPreviewCondition);
        MaterialButton btnStartChat = view.findViewById(R.id.btnStartChat);

        String resolvedTitle = !TextUtils.isEmpty(title) ? title : getString(R.string.app_name);
        String resolvedPoster = !TextUtils.isEmpty(postedBy) ? postedBy : getString(R.string.app_name);
        String resolvedCategory = !TextUtils.isEmpty(displayCategory) ? displayCategory : getString(R.string.category_other);
        String resolvedCondition = !TextUtils.isEmpty(displayCondition) ? displayCondition : getString(R.string.condition_good);

        tvPreviewTitle.setText(resolvedTitle);
        tvPreviewPostedBy.setText(getString(R.string.posted_by_format, resolvedPoster));
        tvPreviewLocation.setText(!TextUtils.isEmpty(location) ? location : getString(R.string.location_unknown));

        if (distanceKm != null) {
            tvPreviewDistance.setText(LocationUtils.formatDistanceLabel(distanceKm));
            tvPreviewDistance.setVisibility(View.VISIBLE);
        } else if (isNearUser) {
            tvPreviewDistance.setText(getString(R.string.listing_preview_nearby));
            tvPreviewDistance.setVisibility(View.VISIBLE);
        } else {
            tvPreviewDistance.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(description)) {
            tvPreviewDescription.setText(description);
        } else {
            tvPreviewDescription.setText(R.string.listing_preview_description_empty);
        }

        chipPreviewCategory.setText(resolvedCategory);
        applyCategoryStyle(chipPreviewCategory, rawCategory);
        chipPreviewCondition.setText(resolvedCondition);

        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(ivPreviewImage);
        } else {
            ivPreviewImage.setImageResource(R.drawable.ic_launcher_background);
        }

        btnStartChat.setOnClickListener(v -> {
            openChat();
            dismissAllowingStateLoss();
        });
    }

    private void openChat() {
        if (getParentFragmentManager() == null || getActivity() == null) {
            return;
        }
        ChatFragment fragment = ChatFragment.newInstance(ownerId, listingId, title, imageUrl);
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_up,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out_down)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("chat_fragment")
                .commit();
    }

    private void applyCategoryStyle(@NonNull Chip chip, @Nullable String rawCategory) {
        if (getContext() == null) {
            return;
        }
        int chipColor = R.color.chip_electronics;
        int textColor = R.color.primary_blue;
        String categoryKey = rawCategory != null ? rawCategory.toLowerCase() : "";
        switch (categoryKey) {
            case "electronics":
                chipColor = R.color.chip_electronics;
                textColor = R.color.primary_blue;
                break;
            case "clothing":
                chipColor = R.color.chip_clothing;
                textColor = R.color.error_red;
                break;
            case "books":
                chipColor = R.color.chip_books;
                textColor = R.color.warning_orange;
                break;
            case "furniture":
                chipColor = R.color.chip_furniture;
                textColor = R.color.success_green;
                break;
            case "donation":
                chipColor = R.color.chip_books;
                textColor = R.color.primary_green;
                break;
            case "swap":
                chipColor = R.color.chip_clothing;
                textColor = R.color.primary_blue;
                break;
        }
        Context context = getContext();
        if (context == null) {
            return;
        }
        chip.setChipBackgroundColorResource(chipColor);
        chip.setTextColor(ContextCompat.getColor(context, textColor));
    }
}
