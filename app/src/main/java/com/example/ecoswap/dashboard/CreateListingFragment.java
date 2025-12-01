package com.example.ecoswap.dashboard;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.ListingImageUploader;
import com.example.ecoswap.utils.LocationFeatureCompat;
import com.example.ecoswap.utils.LocationUtils;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateListingFragment extends Fragment {

    private static final String TAG = "CreateListingFragment";

    private EditText etItemTitle;
    private EditText etDescription;
    private EditText etLocation;
    private Spinner spinnerCategory;
    private Spinner spinnerCondition;
    private Button btnPostItem;
    private ProgressBar progressPost;
    private View addPhotoContainer;
    private ImageView ivListingPreview;

    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;

    private String[] categoryLabels;
    private String[] categoryValues;
    private String[] conditionLabels;
    private String[] conditionValues;
    private Uri selectedImageUri;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                selectedImageUri = uri;
                if (isAdded()) {
                    ivListingPreview.setVisibility(View.VISIBLE);
                    Glide.with(requireContext())
                            .load(uri)
                            .centerCrop()
                            .into(ivListingPreview);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_listing, container, false);
        initViews(view);
        initSupabase();
        setupSpinners();
        setupButton();
        prefillLocation();
        return view;
    }

    private void initViews(View view) {
        etItemTitle = view.findViewById(R.id.etItemTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etLocation = view.findViewById(R.id.etLocation);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerCondition = view.findViewById(R.id.spinnerCondition);
        btnPostItem = view.findViewById(R.id.btnPostItem);
        progressPost = view.findViewById(R.id.progressPostListing);
        addPhotoContainer = view.findViewById(R.id.cardAddPhoto);
        ivListingPreview = view.findViewById(R.id.ivListingPreview);
    }

    private void initSupabase() {
        if (getContext() == null) {
            return;
        }
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        supabaseClient.hydrateSession(
            sessionManager.getAccessToken(),
            sessionManager.getRefreshToken(),
            sessionManager.getAccessTokenExpiry(),
            sessionManager.getUserId()
        );
    }

    private void setupSpinners() {
        categoryLabels = getResources().getStringArray(R.array.listing_category_labels);
        categoryValues = getResources().getStringArray(R.array.listing_category_values);
        conditionLabels = getResources().getStringArray(R.array.listing_condition_labels);
        conditionValues = getResources().getStringArray(R.array.listing_condition_values);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categoryLabels);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, conditionLabels);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCondition.setAdapter(conditionAdapter);
    }

    private void setupButton() {
        btnPostItem.setOnClickListener(v -> attemptPost());
        addPhotoContainer.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void attemptPost() {
        if (sessionManager == null || supabaseClient == null) {
            Toast.makeText(getContext(), R.string.error_loading_listings, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(getContext(), R.string.require_login_message, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etItemTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String categoryValue = getSelectedValue(categoryValues, spinnerCategory.getSelectedItemPosition());
        String conditionValue = getSelectedValue(conditionValues, spinnerCondition.getSelectedItemPosition());

        if (TextUtils.isEmpty(title)) {
            etItemTitle.setError(getString(R.string.validation_title_required));
            etItemTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError(getString(R.string.validation_description_required));
            etDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(location)) {
            etLocation.setError(getString(R.string.validation_location_required));
            etLocation.requestFocus();
            return;
        }

        JsonObject payload = new JsonObject();
        String listingId = UUID.randomUUID().toString();
        payload.addProperty("id", listingId);
        payload.addProperty("user_id", userId);
        payload.addProperty("title", title);
        payload.addProperty("description", description);
        payload.addProperty("location", location);
        payload.addProperty("category", categoryValue);
        payload.addProperty("condition", conditionValue);
        payload.addProperty("status", "available");

        setLoading(true);
        prepareLocationAndSubmit(payload, userId, listingId);
    }

    private void prepareLocationAndSubmit(JsonObject payload, String ownerId, String listingId) {
        Context context = getContext();
        if (context == null) {
            if (isAdded()) {
                setLoading(false);
            }
            return;
        }

        final Context appContext = context.getApplicationContext();
        final String locationValue = payload.has("location") ? payload.get("location").getAsString() : null;

        if (!LocationFeatureCompat.areCoordinatesSupported() || TextUtils.isEmpty(locationValue)) {
            mainHandler.post(() -> dispatchListingSubmission(ownerId, listingId, payload));
            return;
        }

        geocodeExecutor.execute(() -> {
            if (!TextUtils.isEmpty(locationValue)) {
                LocationUtils.Coordinates coordinates = LocationUtils.geocodeLocation(appContext, locationValue);
                if (coordinates != null) {
                    payload.addProperty("latitude", coordinates.getLatitude());
                    payload.addProperty("longitude", coordinates.getLongitude());
                }
            }

            mainHandler.post(() -> dispatchListingSubmission(ownerId, listingId, payload));
        });
    }

    private void dispatchListingSubmission(String ownerId, String listingId, JsonObject payload) {
        if (!isAdded()) {
            return;
        }
        if (selectedImageUri != null) {
            uploadPhotoAndCreateListing(ownerId, listingId, payload);
        } else {
            createListing(payload);
        }
    }

    private void uploadPhotoAndCreateListing(String ownerId, String listingId, JsonObject payload) {
        if (!isAdded()) {
            return;
        }
        ListingImageUploader.upload(requireContext(), supabaseClient, ownerId, listingId, selectedImageUri,
                new ListingImageUploader.Callback() {
                    @Override
                    public void onUploadSuccess(@NonNull String publicUrl) {
                        payload.addProperty("image_url", publicUrl);
                        createListing(payload);
                    }

                    @Override
                    public void onUploadError(@NonNull String message) {
                        if (!isAdded()) {
                            return;
                        }
                        setLoading(false);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Image upload failed: " + message);
                    }
                });
    }

    private void createListing(JsonObject payload) {
        supabaseClient.insert("posts", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                setLoading(false);
                clearForm();
                Toast.makeText(getContext(), R.string.listing_created_success, Toast.LENGTH_SHORT).show();
                navigateBackToExplore();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                if (LocationFeatureCompat.areCoordinatesSupported() && isMissingCoordinateColumnError(error)) {
                    Log.w(TAG, "Supabase rejected latitude/longitude columns; retrying without them.");
                    LocationFeatureCompat.markCoordinatesUnsupported();
                    payload.remove("latitude");
                    payload.remove("longitude");
                    createListing(payload);
                    return;
                }
                setLoading(false);
                Toast.makeText(getContext(), R.string.listing_created_error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Posting listing failed: " + error);
            }
        });
    }

    private boolean isMissingCoordinateColumnError(String error) {
        if (TextUtils.isEmpty(error)) {
            return false;
        }
        String lower = error.toLowerCase(Locale.US);
        return lower.contains("42703") || (lower.contains("column") && (lower.contains("latitude") || lower.contains("longitude")));
    }

    private void setLoading(boolean isLoading) {
        btnPostItem.setEnabled(!isLoading);
        progressPost.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void clearForm() {
        etItemTitle.setText("");
        etDescription.setText("");
        if (spinnerCategory.getAdapter() != null) {
            spinnerCategory.setSelection(0);
        }
        if (spinnerCondition.getAdapter() != null) {
            spinnerCondition.setSelection(0);
        }
        selectedImageUri = null;
        ivListingPreview.setImageDrawable(null);
        ivListingPreview.setVisibility(View.GONE);
    }

    private void navigateBackToExplore() {
        if (getActivity() == null) {
            return;
        }
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private String getSelectedValue(String[] values, int position) {
        if (values == null || values.length == 0 || position < 0 || position >= values.length) {
            return "other";
        }
        return values[position];
    }

    private void prefillLocation() {
        if (sessionManager == null || supabaseClient == null) {
            return;
        }
        String userId = sessionManager.getUserId();
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        String endpoint = "/rest/v1/profiles?select=location&id=eq." + userId + "&limit=1";
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject profile = array.getJSONObject(0);
                        String locationValue = profile.optString("location", "");
                        if (!TextUtils.isEmpty(locationValue)) {
                            etLocation.setText(locationValue);
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to prefill location", e);
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Unable to prefill location: " + error);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        geocodeExecutor.shutdownNow();
    }
}
