package com.example.ecoswap.dashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.ListingImageUploader;
import com.example.ecoswap.utils.LocationFeatureCompat;
import com.example.ecoswap.utils.LocationUtils;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateListingFragment extends Fragment {

    private static final String TAG = "CreateListingFragment";
    private static final String ARG_EDIT_LISTING_ID = "arg_edit_listing_id";

    private EditText etItemTitle;
    private EditText etDescription;
    private EditText etLocation;
    private RadioGroup rgListingType;
    private RadioButton rbSwap;
    private RadioButton rbDonation;
    private Spinner spinnerCategory;
    private Spinner spinnerCondition;
    private Button btnPostItem;
    private ProgressBar progressPost;
    private View addPhotoContainer;
    private RecyclerView rvSelectedImages;
    private SelectedImagesAdapter imagesAdapter;

    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;

    private String editListingId;
    private boolean isEditMode = false;

    private String[] categoryLabels;
    private String[] categoryValues;
    private String[] conditionLabels;
    private String[] conditionValues;
    private FusedLocationProviderClient fusedLocationClient;
    private CancellationTokenSource locationTokenSource;
    private String autoFilledLocationLabel;
    private Double autoFilledLatitude;
    private Double autoFilledLongitude;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static CreateListingFragment newInstanceForEdit(String listingId) {
        CreateListingFragment fragment = new CreateListingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EDIT_LISTING_ID, listingId);
        fragment.setArguments(args);
        return fragment;
    }

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null || uris.isEmpty()) {
                    return;
                }
                if (isAdded() && imagesAdapter != null) {
                    for (Uri uri : uris) {
                        imagesAdapter.addImage(uri);
                    }
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fine != null && fine) || (coarse != null && coarse)) {
                    fetchDeviceLocation();
                } else {
                    Log.w(TAG, "Location permission denied; keeping profile location fallback.");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_listing, container, false);
        
        if (getArguments() != null) {
            editListingId = getArguments().getString(ARG_EDIT_LISTING_ID);
            isEditMode = !TextUtils.isEmpty(editListingId);
        }

        initViews(view);
        initSupabase();
        setupSpinners();
        setupButton();
        
        if (isEditMode) {
            btnPostItem.setText("Update Listing");
            loadListingForEdit();
        } else {
            prefillLocation();
            requestDeviceLocationPermission();
        }
        
        return view;
    }

    private void loadListingForEdit() {
        if (supabaseClient == null || TextUtils.isEmpty(editListingId)) return;
        
        setLoading(true);
        String endpoint = "/rest/v1/posts?id=eq." + editListingId + "&select=*";
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) return;
                setLoading(false);
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject post = array.getJSONObject(0);
                        populateForm(post);
                    }
                } catch (JSONException e) {
                    Toast.makeText(getContext(), "Failed to load listing details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(getContext(), "Failed to load listing: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateForm(JSONObject post) {
        etItemTitle.setText(post.optString("title", ""));
        etDescription.setText(post.optString("description", ""));
        etLocation.setText(post.optString("location", ""));
        
        String type = post.optString("listing_type", "swap");
        if ("donation".equalsIgnoreCase(type)) {
            rbDonation.setChecked(true);
        } else {
            rbSwap.setChecked(true);
        }
        
        String category = post.optString("category", "other");
        setSpinnerSelection(spinnerCategory, categoryValues, category);
        
        String condition = post.optString("condition", "good");
        setSpinnerSelection(spinnerCondition, conditionValues, condition);
        
        // Note: Handling existing images is complex with the current adapter which expects Uris.
        // For now, we won't pre-populate images in the adapter, but we will preserve them if not changed.
        // A full implementation would require modifying SelectedImagesAdapter to handle URLs.
    }

    private void setSpinnerSelection(Spinner spinner, String[] values, String value) {
        if (values == null || value == null) return;
        for (int i = 0; i < values.length; i++) {
            if (value.equalsIgnoreCase(values[i])) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void initViews(View view) {
        etItemTitle = view.findViewById(R.id.etItemTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etLocation = view.findViewById(R.id.etLocation);
        rgListingType = view.findViewById(R.id.rgListingType);
        rbSwap = view.findViewById(R.id.rbSwap);
        rbDonation = view.findViewById(R.id.rbDonation);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerCondition = view.findViewById(R.id.spinnerCondition);
        btnPostItem = view.findViewById(R.id.btnPostItem);
        progressPost = view.findViewById(R.id.progressPostListing);
        addPhotoContainer = view.findViewById(R.id.cardAddPhoto);
        rvSelectedImages = view.findViewById(R.id.rvSelectedImages);

        imagesAdapter = new SelectedImagesAdapter(requireContext(), position -> {
            if (imagesAdapter != null) {
                imagesAdapter.removeImage(position);
            }
        });
        rvSelectedImages.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSelectedImages.setAdapter(imagesAdapter);
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
        String listingType = rbSwap.isChecked() ? "swap" : "donation";

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
        String listingId = isEditMode ? editListingId : UUID.randomUUID().toString();
        if (!isEditMode) {
            payload.addProperty("id", listingId);
            payload.addProperty("user_id", userId);
            payload.addProperty("status", "available");
        }
        
        payload.addProperty("title", title);
        payload.addProperty("description", description);
        payload.addProperty("location", location);
        payload.addProperty("category", categoryValue);
        payload.addProperty("listing_type", listingType);
        payload.addProperty("condition", conditionValue);

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

        boolean matchesAutoFill = !TextUtils.isEmpty(locationValue)
                && !TextUtils.isEmpty(autoFilledLocationLabel)
                && locationValue.equals(autoFilledLocationLabel)
                && autoFilledLatitude != null
                && autoFilledLongitude != null;

        if (matchesAutoFill) {
            if (LocationFeatureCompat.areCoordinatesSupported()) {
                payload.addProperty("latitude", autoFilledLatitude);
                payload.addProperty("longitude", autoFilledLongitude);
            }
            mainHandler.post(() -> dispatchListingSubmission(ownerId, listingId, payload));
            return;
        }

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
        List<Uri> images = imagesAdapter != null ? imagesAdapter.getImages() : new ArrayList<>();
        if (!images.isEmpty()) {
            uploadPhotosAndCreateListing(ownerId, listingId, payload, images, new ArrayList<>(), 0);
        } else {
            if (isEditMode) {
                updateListing(payload);
            } else {
                createListing(payload);
            }
        }
    }

    private void uploadPhotosAndCreateListing(String ownerId, String listingId, JsonObject payload, List<Uri> images, List<String> uploadedUrls, int index) {
        if (!isAdded()) {
            return;
        }
        if (index >= images.size()) {
            // All images uploaded
            String joinedUrls = TextUtils.join(",", uploadedUrls);
            // If editing and we have new images, we might want to append or replace. 
            // For simplicity in this version, we replace if new images are selected.
            payload.addProperty("image_url", joinedUrls);
            
            if (isEditMode) {
                updateListing(payload);
            } else {
                createListing(payload);
            }
            return;
        }

        Uri currentUri = images.get(index);
        ListingImageUploader.upload(requireContext(), supabaseClient, ownerId, listingId, currentUri,
                new ListingImageUploader.Callback() {
                    @Override
                    public void onUploadSuccess(@NonNull String publicUrl) {
                        uploadedUrls.add(publicUrl);
                        uploadPhotosAndCreateListing(ownerId, listingId, payload, images, uploadedUrls, index + 1);
                    }

                    @Override
                    public void onUploadError(@NonNull String message) {
                        if (!isAdded()) {
                            return;
                        }
                        // If one fails, we stop and show error, or we could continue?
                        // For now, fail fast.
                        setLoading(false);
                        Toast.makeText(getContext(), "Failed to upload image " + (index + 1) + ": " + message, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Image upload failed: " + message);
                    }
                });
    }

    private void updateListing(JsonObject payload) {
        supabaseClient.update("posts", editListingId, payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(getContext(), "Listing updated successfully", Toast.LENGTH_SHORT).show();
                navigateBackToExplore();
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(getContext(), "Failed to update listing: " + error, Toast.LENGTH_SHORT).show();
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
                if (isMissingCoordinateColumnError(error)) {
                    Log.w(TAG, "Supabase rejected latitude/longitude columns; retrying without them.");
                    LocationFeatureCompat.markCoordinatesUnsupported();
                    if (payload.has("latitude")) {
                        payload.remove("latitude");
                    }
                    if (payload.has("longitude")) {
                        payload.remove("longitude");
                    }
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
        if (imagesAdapter != null) {
            // Create new adapter or clear list
            imagesAdapter = new SelectedImagesAdapter(requireContext(), position -> {
                if (imagesAdapter != null) {
                    imagesAdapter.removeImage(position);
                }
            });
            rvSelectedImages.setAdapter(imagesAdapter);
        }
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

    private void requestDeviceLocationPermission() {
        if (!isAdded()) {
            return;
        }
        if (hasLocationPermission()) {
            fetchDeviceLocation();
        } else {
            locationPermissionLauncher.launch(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private boolean hasLocationPermission() {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void fetchDeviceLocation() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (!hasLocationPermission()) {
            return;
        }
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        }
        final Context appContext = context.getApplicationContext();
        cancelLocationRequest();
        locationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, locationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        Log.w(TAG, "Unable to determine current device location.");
                        return;
                    }
                    geocodeExecutor.execute(() -> {
                        String resolvedAddress = LocationUtils.reverseGeocodeCoordinates(appContext, location.getLatitude(), location.getLongitude());
                        if (TextUtils.isEmpty(resolvedAddress)) {
                            resolvedAddress = fallbackCoordinateLabel(location.getLatitude(), location.getLongitude());
                        }
                        final String finalAddress = resolvedAddress;
                        final double lat = location.getLatitude();
                        final double lon = location.getLongitude();
                        mainHandler.post(() -> applyAutoLocation(finalAddress, lat, lon));
                    });
                })
                .addOnFailureListener(error -> Log.e(TAG, "Failed to fetch device location", error));
    }

    private void applyAutoLocation(@Nullable String locationLabel, double latitude, double longitude) {
        if (!isAdded()) {
            return;
        }
        String resolvedLabel = !TextUtils.isEmpty(locationLabel) ? locationLabel.trim() : fallbackCoordinateLabel(latitude, longitude);
        autoFilledLocationLabel = resolvedLabel;
        autoFilledLatitude = latitude;
        autoFilledLongitude = longitude;
        etLocation.setText(resolvedLabel);
        etLocation.setError(null);
    }

    private String fallbackCoordinateLabel(double latitude, double longitude) {
        return String.format(Locale.getDefault(), "%.5f, %.5f", latitude, longitude);
    }

    private void cancelLocationRequest() {
        if (locationTokenSource != null) {
            locationTokenSource.cancel();
            locationTokenSource = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelLocationRequest();
        geocodeExecutor.shutdownNow();
    }
}
