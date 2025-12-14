package com.example.ecoswap.chat;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import com.example.ecoswap.chat.ChatMessagesAdapter.ChatMessage;
import com.example.ecoswap.dashboard.UserProfileBottomSheet;
import com.example.ecoswap.dashboard.listings.MyListingsFragment;
import com.example.ecoswap.utils.ChatFeatureCompat;
import com.example.ecoswap.utils.ConversationMetadataStore;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.gson.JsonObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import okhttp3.HttpUrl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Basic chat view that lets two users exchange messages for a listing.
 */
public class ChatFragment extends Fragment {

    private static final String ARG_OWNER_ID = "owner_id";
    private static final String ARG_LISTING_ID = "listing_id";
    private static final String ARG_LISTING_TITLE = "listing_title";
    private static final String ARG_IMAGE_URL = "image_url";
    private static final String STATE_LISTING_DETAILS_EXPANDED = "state_listing_details_expanded";

    public static ChatFragment newInstance(String ownerId, String listingId, String listingTitle, String imageUrl) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OWNER_ID, ownerId);
        args.putString(ARG_LISTING_ID, listingId);
        args.putString(ARG_LISTING_TITLE, listingTitle);
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    private RecyclerView rvMessages;
    private ChatMessagesAdapter chatAdapter;
    private LinearLayoutManager messagesLayoutManager;
    private SessionManager sessionManager;
    private SupabaseClient supabaseClient;
    private ImageButton btnSend;
    private EditText etMessage;
    private ProgressBar progressSending;
    private TextView tvListingTitle;
    private ImageView ivListing;
    private MaterialCardView cardListingContext;
    private View layoutListingDetails;
    private TextView tvListingCategoryLabel;
    private TextView tvListingStatusLabel;
    private MaterialButton btnListingMarkComplete;
    private MaterialButton btnToggleListingCard;
    private ExtendedFloatingActionButton fabMarkListingComplete;
    private MaterialCardView cardPeerCompletion;
    private MaterialButton btnPeerAccept;
    private MaterialButton btnPeerDismiss;
    private ConversationMetadataStore conversationMetadataStore;

    private String listingOwnerId;
    private boolean isListingDetailsExpanded = false;
    private boolean hasListingContext = false;
    private String resolvedListingId;
    private String resolvedListingTitle;
    private String resolvedListingImageUrl;
    private boolean listingLookupInFlight = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            resolvedListingId = sanitizeMetadataValue(getArguments().getString(ARG_LISTING_ID));
            resolvedListingTitle = sanitizeMetadataValue(getArguments().getString(ARG_LISTING_TITLE));
            resolvedListingImageUrl = sanitizeMetadataValue(getArguments().getString(ARG_IMAGE_URL));
            listingOwnerId = sanitizeMetadataValue(getArguments().getString(ARG_OWNER_ID));
        }
        if (savedInstanceState != null) {
            isListingDetailsExpanded = savedInstanceState.getBoolean(STATE_LISTING_DETAILS_EXPANDED, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        supabaseClient.hydrateSession(
                sessionManager.getAccessToken(),
                sessionManager.getRefreshToken(),
                sessionManager.getAccessTokenExpiry(),
                sessionManager.getUserId()
        );
        conversationMetadataStore = new ConversationMetadataStore(requireContext());
        hydrateListingPreviewFromCache();

        initViews(view);
        setupToolbar(view);
        setupRecyclerView();
        setupSendButton();
        loadParticipantProfiles();
        loadConversationHistory();
        loadListingContext();
        return view;
    }

    private void initViews(View root) {
        rvMessages = root.findViewById(R.id.rvChatMessages);
        btnSend = root.findViewById(R.id.btnSend);
        etMessage = root.findViewById(R.id.etMessage);
        progressSending = root.findViewById(R.id.progressSending);
        tvListingTitle = root.findViewById(R.id.tvListingTitle);
        ivListing = root.findViewById(R.id.ivListingPreview);
        cardListingContext = root.findViewById(R.id.cardListingContext);
        layoutListingDetails = root.findViewById(R.id.layoutListingDetails);
        tvListingCategoryLabel = root.findViewById(R.id.tvListingCategoryLabel);
        tvListingStatusLabel = root.findViewById(R.id.tvListingStatusLabel);
        btnListingMarkComplete = root.findViewById(R.id.btnListingMarkComplete);
        btnToggleListingCard = root.findViewById(R.id.btnToggleListingCard);
        fabMarkListingComplete = root.findViewById(R.id.fabMarkListingComplete);
        cardPeerCompletion = root.findViewById(R.id.cardPeerCompletion);
        btnPeerAccept = root.findViewById(R.id.btnPeerAccept);
        btnPeerDismiss = root.findViewById(R.id.btnPeerDismiss);

        applyListingPreview();

        if (btnListingMarkComplete != null) {
            btnListingMarkComplete.setOnClickListener(v -> promptCompletion());
        }
        if (fabMarkListingComplete != null) {
            fabMarkListingComplete.setOnClickListener(v -> promptCompletion());
        }
        if (btnToggleListingCard != null) {
            btnToggleListingCard.setOnClickListener(v -> {
                isListingDetailsExpanded = !isListingDetailsExpanded;
                updateListingCardVisibility();
            });
        }
            if (btnPeerAccept != null) {
                btnPeerAccept.setOnClickListener(v -> acceptPeerCompletion());
            }
            if (btnPeerDismiss != null) {
                btnPeerDismiss.setOnClickListener(v -> hidePeerCompletionPrompt());
            }
    }

    private void setupToolbar(View root) {
        MaterialToolbar toolbar = root.findViewById(R.id.chatToolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        toolbar.inflateMenu(R.menu.chat_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_mark_completed) {
                promptCompletion();
                return true;
            } else if (id == R.id.action_archive) {
                Toast.makeText(requireContext(), "Chat archived", Toast.LENGTH_SHORT).show();
                // TODO: Implement archive logic in database
                return true;
            } else if (id == R.id.action_view_profile) {
                String otherUserId = getOwnerIdArg();
                if (!TextUtils.isEmpty(otherUserId)) {
                    UserProfileBottomSheet profileSheet = UserProfileBottomSheet.newInstance(otherUserId);
                    profileSheet.show(getParentFragmentManager(), "user_profile");
                } else {
                    Toast.makeText(requireContext(), "User info unavailable", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == R.id.action_report) {
                Toast.makeText(requireContext(), "Report User clicked", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        messagesLayoutManager = new LinearLayoutManager(requireContext());
        messagesLayoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(messagesLayoutManager);
        String currentUserId = sessionManager.getUserId();
        chatAdapter = new ChatMessagesAdapter(currentUserId != null ? currentUserId : "", userId -> {
            if (!TextUtils.isEmpty(userId)) {
                UserProfileBottomSheet profileSheet = UserProfileBottomSheet.newInstance(userId);
                profileSheet.show(getParentFragmentManager(), "user_profile");
            }
        });
        rvMessages.setAdapter(chatAdapter);
    }

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(messageText)) {
                return;
            }
            sendMessage(messageText);
        });
    }

    private void loadParticipantProfiles() {
        if (supabaseClient == null || chatAdapter == null) {
            return;
        }
        String currentUserId = sessionManager.getUserId();
        String otherUserId = getOwnerIdArg();
        List<String> ids = new ArrayList<>();
        if (!TextUtils.isEmpty(currentUserId)) {
            ids.add(currentUserId);
        }
        if (!TextUtils.isEmpty(otherUserId) && (ids.isEmpty() || !otherUserId.equals(ids.get(0)))) {
            ids.add(otherUserId);
        }
        if (ids.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < ids.size(); i++) {
            builder.append("\"").append(ids.get(i)).append("\"");
            if (i < ids.size() - 1) {
                builder.append(",");
            }
        }
        builder.append(")");
        String endpoint = "/rest/v1/profiles?select=id,name,profile_image_url&id=in." + Uri.encode(builder.toString());
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded() || chatAdapter == null) {
                    return;
                }
                try {
                    JSONArray array = new JSONArray(data.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject profile = array.getJSONObject(i);
                        String id = profile.optString("id");
                        if (TextUtils.isEmpty(id)) {
                            continue;
                        }
                        String name = profile.optString("name", null);
                        String avatar = profile.optString("profile_image_url", null);
                        chatAdapter.setParticipantProfile(id, name, avatar);
                    }
                } catch (JSONException e) {
                    // Ignore parse issues; avatars are optional.
                }
            }

            @Override
            public void onError(String error) {
                // Avatars are optional; no user notification needed.
            }
        });
    }

    private void sendMessage(String content) {
        String currentUserId = sessionManager.getUserId();
        String ownerId = getOwnerIdArg();
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(ownerId)) {
            return;
        }

        btnSend.setEnabled(false);
        progressSending.setVisibility(View.VISIBLE);

        JsonObject payload = new JsonObject();
        payload.addProperty("sender_id", currentUserId);
        payload.addProperty("receiver_id", ownerId);
        payload.addProperty("message", content);
        if (ChatFeatureCompat.isListingMetadataSupported()) {
            String listingId = getListingId();
            if (!TextUtils.isEmpty(listingId)) {
                payload.addProperty("listing_id", listingId);
            }
            if (hasMeaningfulValue(resolvedListingTitle)) {
                payload.addProperty("listing_title_snapshot", resolvedListingTitle);
            }
            if (hasMeaningfulValue(resolvedListingImageUrl)) {
                payload.addProperty("listing_image_url_snapshot", resolvedListingImageUrl);
            }
        }

        dispatchSendMessage(payload, currentUserId, content);
    }

    private void dispatchSendMessage(JsonObject payload, String senderId, String content) {
        supabaseClient.insert("chats", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                appendMessageFromResponse(senderId, content, data);
                etMessage.setText("");
                btnSend.setEnabled(true);
                progressSending.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                if (ChatFeatureCompat.isListingMetadataSupported() && ChatFeatureCompat.isListingMetadataError(error)) {
                    ChatFeatureCompat.disableListingMetadata();
                    payload.remove("listing_id");
                    payload.remove("listing_title_snapshot");
                    payload.remove("listing_image_url_snapshot");
                    dispatchSendMessage(payload, senderId, content);
                    return;
                }
                btnSend.setEnabled(true);
                progressSending.setVisibility(View.GONE);
            }
        });
    }

    private void appendMessageFromResponse(String senderId, String content, Object data) {
        String id = null;
        String createdAt = null;
        try {
            JSONArray array = new JSONArray(data.toString());
            if (array.length() > 0) {
                JSONObject message = array.getJSONObject(0);
                id = message.optString("id", null);
                createdAt = message.optString("created_at", null);
            }
        } catch (JSONException ignored) {
            // Fall back to local timestamp
        }
        appendLocalMessage(id, senderId, content, createdAt);
    }

    private void appendLocalMessage(String id, String senderId, String content, @Nullable String createdAt) {
        String displayTime = formatTimestamp(createdAt);
        chatAdapter.appendMessage(new ChatMessage(id, senderId, content, displayTime));
        rvMessages.scrollToPosition(Math.max(chatAdapter.getItemCount() - 1, 0));
    }

    private void loadConversationHistory() {
        if (supabaseClient == null) {
            return;
        }
        String currentUserId = sessionManager.getUserId();
        String otherUserId = getOwnerIdArg();
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(otherUserId)) {
            return;
        }

        // Mark messages as read
        markMessagesAsRead(currentUserId, otherUserId);

        String endpoint = buildThreadEndpoint(currentUserId, otherUserId, getListingId());
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                try {
                    JSONArray array = new JSONArray(data.toString());
                    List<ChatMessage> history = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject message = array.getJSONObject(i);
                        if (ChatFeatureCompat.isListingMetadataSupported() && TextUtils.isEmpty(resolvedListingId)) {
                            String potentialListingId = message.has("listing_id") && !message.isNull("listing_id")
                                    ? sanitizeMetadataValue(message.optString("listing_id", null))
                                    : null;
                            if (!TextUtils.isEmpty(potentialListingId)) {
                                resolvedListingId = potentialListingId;
                                loadListingContext();
                            }
                        }
                        if (ChatFeatureCompat.isListingMetadataSupported()) {
                            if (!hasMeaningfulValue(resolvedListingTitle)) {
                                String snapshotTitle = sanitizeMetadataValue(message.optString("listing_title_snapshot", null));
                                if (hasMeaningfulValue(snapshotTitle)) {
                                    resolvedListingTitle = snapshotTitle;
                                    applyListingPreview();
                                }
                            }
                            if (!hasMeaningfulValue(resolvedListingImageUrl)) {
                                String snapshotImage = sanitizeMetadataValue(message.optString("listing_image_url_snapshot", null));
                                if (hasMeaningfulValue(snapshotImage)) {
                                    resolvedListingImageUrl = snapshotImage;
                                    applyListingPreview();
                                }
                            }
                        }
                        history.add(new ChatMessage(
                                message.optString("id"),
                                message.optString("sender_id"),
                                message.optString("message"),
                                formatTimestamp(message.optString("created_at"))
                        ));
                    }
                    chatAdapter.setMessages(history);
                    rvMessages.scrollToPosition(Math.max(history.size() - 1, 0));
                } catch (JSONException e) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.chat_history_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.chat_history_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void markMessagesAsRead(String currentUserId, String otherUserId) {
        if (supabaseClient == null) return;
        
        JsonObject payload = new JsonObject();
        payload.addProperty("is_read", true);
        
        String endpoint = String.format(Locale.US, 
            "/rest/v1/chats?sender_id=eq.%s&receiver_id=eq.%s&is_read=eq.false", 
            otherUserId, currentUserId);
            
        supabaseClient.updateRecord(endpoint, payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                // Messages marked as read
            }

            @Override
            public void onError(String error) {
                // Silently fail
            }
        });
    }

    private String buildThreadEndpoint(String userId, String otherUserId, @Nullable String listingId) {
        if (supabaseClient == null || TextUtils.isEmpty(supabaseClient.getSupabaseUrl())) {
            return "/rest/v1/chats";
        }
        HttpUrl baseUrl = HttpUrl.parse(supabaseClient.getSupabaseUrl());
        if (baseUrl == null) {
            return "/rest/v1/chats";
        }
        String participantsFilter = String.format(Locale.US,
                "(and(sender_id.eq.%s,receiver_id.eq.%s),and(sender_id.eq.%s,receiver_id.eq.%s))",
                userId, otherUserId, otherUserId, userId);
        HttpUrl.Builder builder = baseUrl.newBuilder()
                .addPathSegments("rest/v1/chats")
                .addQueryParameter("select", ChatFeatureCompat.isListingMetadataSupported()
                ? "id,message,created_at,sender_id,receiver_id,listing_id,listing_title_snapshot,listing_image_url_snapshot"
                        : "id,message,created_at,sender_id,receiver_id")
                .addQueryParameter("or", participantsFilter)
                .addQueryParameter("order", "created_at.asc");
        // Removed strict listing_id filter to show full history
        // if (ChatFeatureCompat.isListingMetadataSupported() && !TextUtils.isEmpty(listingId)) {
        //    builder.addQueryParameter("listing_id", "eq." + listingId);
        // }
        return builder.build().toString().replace(supabaseClient.getSupabaseUrl(), "");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_LISTING_DETAILS_EXPANDED, isListingDetailsExpanded);
    }

    private void loadListingContext() {
        if (cardListingContext == null || supabaseClient == null) {
            return;
        }
        String listingId = getListingId();
        if (TextUtils.isEmpty(listingId)) {
            ensureListingPreviewFallback();
            tryResolveListingIdFromThread();
            return;
        }

        hasListingContext = true;
        // Keep expanded state if user toggled it, otherwise default to collapsed
        if (tvListingCategoryLabel != null) {
            tvListingCategoryLabel.setText(getString(R.string.chat_listing_category_label, getString(R.string.category_other)));
        }
        if (tvListingStatusLabel != null) {
            tvListingStatusLabel.setText(getString(R.string.chat_listing_status_label, getString(R.string.my_listings_status_pending)));
            tvListingStatusLabel.setVisibility(View.VISIBLE);
        }
        updateListingCardVisibility();
        updateCompletionControls(false, false);

        String endpoint = String.format(Locale.US,
            "/rest/v1/posts?select=id,user_id,category,status,title,image_url&id=eq.%s&limit=1",
            listingId);
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) {
                        hideListingContext();
                        return;
                    }
                    JSONObject listing = array.getJSONObject(0);
                    listingOwnerId = sanitizeMetadataValue(listing.optString("user_id", null));
                    if (TextUtils.isEmpty(listingOwnerId)) {
                        listingOwnerId = getOwnerIdArg();
                    }
                    String rawStatus = listing.optString("status", null);
                    String rawCategory = listing.optString("category", null);
                    String fetchedTitle = sanitizeMetadataValue(listing.optString("title", null));
                    if (hasMeaningfulValue(fetchedTitle)) {
                        resolvedListingTitle = fetchedTitle;
                    }
                    String fetchedImage = sanitizeMetadataValue(listing.optString("image_url", null));
                    if (hasMeaningfulValue(fetchedImage)) {
                        resolvedListingImageUrl = fetchedImage;
                    }
                    applyListingPreview();
                    bindListingContext(rawCategory, rawStatus);
                } catch (JSONException e) {
                    showListingContextFallback();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                showListingContextFallback();
            }
        });
    }

    private void bindListingContext(@Nullable String rawCategory, @Nullable String rawStatus) {
        if (cardListingContext == null || tvListingCategoryLabel == null || tvListingStatusLabel == null) {
            return;
        }
        hasListingContext = true;
        String categoryLabel = mapCategoryLabel(rawCategory);
        String statusLabel = mapStatusLabel(rawStatus);
        tvListingCategoryLabel.setText(getString(R.string.chat_listing_category_label, categoryLabel));
        tvListingStatusLabel.setText(statusLabel);
        tvListingStatusLabel.setVisibility(View.VISIBLE);
        updateListingCardVisibility();
        boolean allowMark = shouldAllowCompletionAction(rawStatus);
        boolean isOwner = isCurrentUserListingOwner();
        updateCompletionControls(allowMark, isOwner);
        maybeShowPeerCompletionPrompt(rawStatus);
    }

    private void hideListingContext() {
        hasListingContext = false;
        isListingDetailsExpanded = false;
        updateListingCardVisibility();
        updateCompletionControls(false, false);
    }

    private void showListingContextFallback() {
        ensureListingPreviewFallback();
        if (tvListingCategoryLabel != null) {
            tvListingCategoryLabel.setText(R.string.chat_listing_card_unavailable);
        }
        if (tvListingStatusLabel != null) {
            tvListingStatusLabel.setVisibility(View.GONE);
        }
        hasListingContext = true;
        updateListingCardVisibility();
        updateCompletionControls(false, false);
    }

    private void ensureListingPreviewFallback() {
        if (!hasListingContext) {
            hasListingContext = hasMeaningfulValue(resolvedListingTitle)
                    || hasMeaningfulValue(resolvedListingImageUrl)
                    || !TextUtils.isEmpty(getListingId());
        }
        if (hasListingContext) {
            updateListingCardVisibility();
        }
    }

    private void tryResolveListingIdFromThread() {
        if (listingLookupInFlight || supabaseClient == null || TextUtils.isEmpty(supabaseClient.getSupabaseUrl())) {
            showListingContextFallback();
            return;
        }
        if (!ChatFeatureCompat.isListingMetadataSupported()) {
            showListingContextFallback();
            return;
        }
        String currentUserId = sessionManager.getUserId();
        String otherUserId = getOwnerIdArg();
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(otherUserId)) {
            showListingContextFallback();
            return;
        }
        HttpUrl baseUrl = HttpUrl.parse(supabaseClient.getSupabaseUrl());
        if (baseUrl == null) {
            showListingContextFallback();
            return;
        }
        listingLookupInFlight = true;
        String participantsFilter = String.format(Locale.US,
                "(and(sender_id.eq.%s,receiver_id.eq.%s),and(sender_id.eq.%s,receiver_id.eq.%s))",
                currentUserId, otherUserId, otherUserId, currentUserId);
        HttpUrl.Builder builder = baseUrl.newBuilder()
                .addPathSegments("rest/v1/chats")
                .addQueryParameter("select", "listing_id")
                .addQueryParameter("or", participantsFilter)
                .addQueryParameter("listing_id", "is.not.null")
                .addQueryParameter("order", "created_at.desc")
                .addQueryParameter("limit", "1");
        String endpoint = builder.build().toString().replace(supabaseClient.getSupabaseUrl(), "");
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                listingLookupInFlight = false;
                if (!isAdded()) {
                    return;
                }
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() > 0) {
                        JSONObject message = array.getJSONObject(0);
                        String listingId = message.has("listing_id") && !message.isNull("listing_id")
                                ? message.optString("listing_id", null)
                                : null;
                        if (!TextUtils.isEmpty(listingId)) {
                            resolvedListingId = listingId;
                            loadListingContext();
                            return;
                        }
                    }
                } catch (JSONException ignored) {
                    // fall through to fallback
                }
                showListingContextFallback();
            }

            @Override
            public void onError(String error) {
                listingLookupInFlight = false;
                if (ChatFeatureCompat.isListingMetadataSupported() && ChatFeatureCompat.isListingMetadataError(error)) {
                    ChatFeatureCompat.disableListingMetadata();
                }
                if (isAdded()) {
                    showListingContextFallback();
                }
            }
        });
    }

    private void updateListingCardVisibility() {
        if (cardListingContext == null || btnToggleListingCard == null || layoutListingDetails == null) {
            return;
        }
        if (!hasListingContext) {
            cardListingContext.setVisibility(View.GONE);
            return;
        }
        cardListingContext.setVisibility(View.VISIBLE);
        layoutListingDetails.setVisibility(isListingDetailsExpanded ? View.VISIBLE : View.GONE);
        btnToggleListingCard.setText(isListingDetailsExpanded
                ? getString(R.string.chat_hide_listing_card)
                : getString(R.string.chat_show_listing_card));
    }

    private void updateCompletionControls(boolean allowMark, boolean isOwner) {
        // Only show completion controls to the owner of the listing
        if (btnListingMarkComplete != null) {
            btnListingMarkComplete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }
        if (fabMarkListingComplete != null) {
            fabMarkListingComplete.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }
    }

    private void maybeShowPeerCompletionPrompt(@Nullable String rawStatus) {
        if (cardPeerCompletion == null || TextUtils.isEmpty(rawStatus) || isCurrentUserListingOwner()) {
            hidePeerCompletionPrompt();
            return;
        }
        String normalized = rawStatus.toLowerCase(Locale.US);
        if (normalized.equals("swapped") || normalized.equals("donated") || normalized.equals("completed")) {
            cardPeerCompletion.setVisibility(View.VISIBLE);
        } else {
            hidePeerCompletionPrompt();
        }
    }

    private void hidePeerCompletionPrompt() {
        if (cardPeerCompletion != null) {
            cardPeerCompletion.setVisibility(View.GONE);
        }
    }

    private void acceptPeerCompletion() {
        hidePeerCompletionPrompt();
        String status = mapStatusForAcceptance();
        if (tvListingStatusLabel != null) {
            tvListingStatusLabel.setText(mapStatusLabel(status));
            tvListingStatusLabel.setVisibility(View.VISIBLE);
        }
        updateListingStatusAndPersist(status);
        Bundle result = new Bundle();
        result.putBoolean("refresh", true);
        result.putBoolean("openCompleted", true);
        getParentFragmentManager().setFragmentResult("listing_updates", result);
        Toast.makeText(requireContext(), R.string.chat_mark_complete_success, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    private String mapStatusForAcceptance() {
        if (tvListingStatusLabel != null) {
            CharSequence currentLabel = tvListingStatusLabel.getText();
            if (currentLabel != null && currentLabel.toString().toLowerCase(Locale.US).contains("donat")) {
                return "donated";
            }
        }
        return "swapped";
    }

    private void persistTradeRecord(String status) {
        if (supabaseClient == null || TextUtils.isEmpty(getListingId())) {
            updateImpactForInvolvedUsers(status);
            return;
        }
        boolean isDonation = "donated".equals(status);
        if (isDonation) {
            createDonationRecord(status);
        } else {
            createSwapRecord(status);
        }
    }

    private void createSwapRecord(String status) {
        String listingId = getListingId();
        String ownerId = listingOwnerId != null ? listingOwnerId : getOwnerIdArg();
        String currentUserId = sessionManager != null ? sessionManager.getUserId() : null;
        if (TextUtils.isEmpty(listingId) || TextUtils.isEmpty(ownerId) || TextUtils.isEmpty(currentUserId)) {
            updateImpactForInvolvedUsers(status);
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("post1_id", listingId);
        payload.addProperty("user1_id", ownerId);
        payload.addProperty("user2_id", currentUserId);
        payload.addProperty("status", "completed");
        payload.addProperty("completed_at", Instant.now().toString());
        supabaseClient.insert("swaps", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                updateImpactForInvolvedUsers(status);
            }

            @Override
            public void onError(String error) {
                updateImpactForInvolvedUsers(status);
            }
        });
    }

    private void createDonationRecord(String status) {
        String listingId = getListingId();
        String ownerId = listingOwnerId != null ? listingOwnerId : getOwnerIdArg();
        String currentUserId = sessionManager != null ? sessionManager.getUserId() : null;
        if (TextUtils.isEmpty(listingId) || TextUtils.isEmpty(ownerId) || TextUtils.isEmpty(currentUserId)) {
            updateImpactForInvolvedUsers(status);
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("post_id", listingId);
        payload.addProperty("donor_id", ownerId);
        payload.addProperty("receiver_name", "Peer");
        payload.addProperty("status", "completed");
        payload.addProperty("completed_at", Instant.now().toString());
        supabaseClient.insert("donations", payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                updateImpactForInvolvedUsers(status);
            }

            @Override
            public void onError(String error) {
                updateImpactForInvolvedUsers(status);
            }
        });
    }

    private void promptCompletion() {
        if (!isCurrentUserListingOwner() || TextUtils.isEmpty(getListingId())) {
            openMyListings(true);
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_mark_complete_title)
            .setMessage(R.string.chat_mark_complete_message)
            .setPositiveButton(R.string.chat_mark_complete_swapped, (d, which) -> markListingComplete("swap"))
            .setNeutralButton(R.string.chat_mark_complete_donated, (d, which) -> markListingComplete("donation"))
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void markListingComplete(String selection) {
        String listingId = getListingId();
        if (supabaseClient == null || TextUtils.isEmpty(listingId)) {
            return;
        }
        boolean isDonation = "donation".equals(selection) || "donated".equals(selection);
        String targetStatus = isDonation ? "donated" : "swapped"; // swaps now complete immediately
        JsonObject payload = new JsonObject();
        payload.addProperty("status", targetStatus);
        supabaseClient.update("posts", listingId, payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                persistTradeRecord(targetStatus);
                if (tvListingStatusLabel != null) {
                    tvListingStatusLabel.setText(mapStatusLabel(targetStatus));
                    tvListingStatusLabel.setVisibility(View.VISIBLE);
                }
                Bundle result = new Bundle();
                result.putBoolean("refresh", true);
                result.putBoolean("openCompleted", true);
                getParentFragmentManager().setFragmentResult("listing_updates", result);
                Toast.makeText(requireContext(), getString(R.string.chat_mark_complete_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), getString(R.string.chat_mark_complete_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListingStatusAndPersist(String status) {
        String listingId = getListingId();
        if (supabaseClient == null || TextUtils.isEmpty(listingId)) {
            persistTradeRecord(status);
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("status", status);
        payload.addProperty("completed_at", Instant.now().toString());
        supabaseClient.update("posts", listingId, payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                persistTradeRecord(status);
            }

            @Override
            public void onError(String error) {
                persistTradeRecord(status);
            }
        });
    }

    private void updateImpactForInvolvedUsers(String status) {
        String owner = listingOwnerId;
        if (TextUtils.isEmpty(owner)) {
            owner = getOwnerIdArg();
        }
        String currentUser = sessionManager != null ? sessionManager.getUserId() : null;
        List<String> targetUsers = new ArrayList<>();
        if (!TextUtils.isEmpty(owner)) {
            targetUsers.add(owner);
        }
        if (!TextUtils.isEmpty(currentUser) && !currentUser.equals(owner)) {
            targetUsers.add(currentUser);
        }
        for (String user : targetUsers) {
            updateImpactForUser(user, status);
        }
    }

    private void updateImpactForUser(String userId, String status) {
        if (supabaseClient == null || TextUtils.isEmpty(userId)) return;
        String profileQuery = "/rest/v1/profiles?id=eq." + userId + "&select=total_swaps,total_donations,total_purchases";
        supabaseClient.query(profileQuery, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) return;
                    JSONObject profile = array.getJSONObject(0);
                    int swaps = profile.optInt("total_swaps", 0);
                    int donations = profile.optInt("total_donations", 0);
                    int purchases = profile.optInt("total_purchases", 0);

                    if ("swapped".equals(status)) {
                        swaps++;
                    } else if ("donated".equals(status)) {
                        donations++;
                    }

                    int newScore = (swaps * 2) + (donations * 3) + (purchases * 1);
                    String newLevel = "Beginner EcoSaver";
                    String newIcon = "🌱";
                    if (newScore >= 100) {
                        newLevel = "Planet Pioneer";
                        newIcon = "🌞";
                    } else if (newScore >= 50) {
                        newLevel = "Eco Guardian";
                        newIcon = "🦋";
                    } else if (newScore >= 25) {
                        newLevel = "Sustainable Hero";
                        newIcon = "🌍";
                    } else if (newScore >= 10) {
                        newLevel = "Rising Recycler";
                        newIcon = "♻️";
                    }

                    JsonObject updatePayload = new JsonObject();
                    updatePayload.addProperty("total_swaps", swaps);
                    updatePayload.addProperty("total_donations", donations);
                    updatePayload.addProperty("impact_score", newScore);
                    updatePayload.addProperty("eco_level", newLevel);
                    updatePayload.addProperty("eco_icon", newIcon);

                    supabaseClient.update("profiles", userId, updatePayload, new SupabaseClient.OnDatabaseCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            updateEcoSavings(userId, status);
                        }

                        @Override
                        public void onError(String error) {
                            // ignore silently
                        }
                    });
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void onError(String error) {
                // ignore silently
            }
        });
    }

    private void updateEcoSavings(String userId, String status) {
        if (supabaseClient == null || TextUtils.isEmpty(userId)) return;
        String savingsQuery = "/rest/v1/eco_savings?user_id=eq." + userId + "&select=*";
        supabaseClient.query(savingsQuery, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    if (array.length() == 0) return;
                    JSONObject savings = array.getJSONObject(0);
                    double co2 = savings.optDouble("co2_saved", 0);
                    double water = savings.optDouble("water_saved", 0);
                    double waste = savings.optDouble("waste_diverted", 0);
                    double energy = savings.optDouble("energy_saved", 0);
                    int itemsSwapped = savings.optInt("items_swapped", 0);
                    int itemsDonated = savings.optInt("items_donated", 0);

                    if ("swapped".equals(status)) {
                        co2 += 5;
                        water += 100;
                        waste += 2;
                        energy += 10;
                        itemsSwapped++;
                    } else if ("donated".equals(status)) {
                        co2 += 7;
                        water += 150;
                        waste += 3;
                        energy += 15;
                        itemsDonated++;
                    }

                    JsonObject updatePayload = new JsonObject();
                    updatePayload.addProperty("co2_saved", co2);
                    updatePayload.addProperty("water_saved", water);
                    updatePayload.addProperty("waste_diverted", waste);
                    updatePayload.addProperty("energy_saved", energy);
                    updatePayload.addProperty("items_swapped", itemsSwapped);
                    updatePayload.addProperty("items_donated", itemsDonated);

                    supabaseClient.update("eco_savings", "user_id", userId, updatePayload, new SupabaseClient.OnDatabaseCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            // ok
                        }

                        @Override
                        public void onError(String error) {
                            // ignore
                        }
                    });
                } catch (JSONException ignored) {
                }
            }

            @Override
            public void onError(String error) {
                // ignore
            }
        });
    }

    private void applyListingPreview() {
        boolean hasPreviewTitle = hasMeaningfulValue(resolvedListingTitle);
        boolean hasPreviewImage = hasMeaningfulValue(resolvedListingImageUrl);
        if (tvListingTitle != null) {
            tvListingTitle.setText(hasPreviewTitle ? resolvedListingTitle : getString(R.string.app_name));
        }
        if (ivListing != null) {
            if (hasPreviewImage) {
                Glide.with(this)
                        .load(resolvedListingImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(ivListing);
            } else {
                ivListing.setImageResource(R.drawable.ic_launcher_background);
            }
        }
        if (hasPreviewTitle || hasPreviewImage || !TextUtils.isEmpty(resolvedListingId)) {
            hasListingContext = true;
            updateListingCardVisibility();
        }
        persistListingMetadata();
    }

    private void persistListingMetadata() {
        if (conversationMetadataStore == null) {
            return;
        }
        String counterpartId = getOwnerIdArg();
        if (TextUtils.isEmpty(counterpartId)) {
            return;
        }
        String normalizedTitle = sanitizeMetadataValue(resolvedListingTitle);
        String normalizedImage = sanitizeMetadataValue(resolvedListingImageUrl);
        String normalizedListingId = sanitizeMetadataValue(resolvedListingId);
        if (!hasMeaningfulValue(normalizedTitle)
                && !hasMeaningfulValue(normalizedImage)
                && TextUtils.isEmpty(normalizedListingId)) {
            return;
        }
        conversationMetadataStore.saveListingContext(
                counterpartId,
                normalizedListingId,
                normalizedTitle,
                normalizedImage
        );
    }

    private void hydrateListingPreviewFromCache() {
        if (conversationMetadataStore == null) {
            return;
        }
        String counterpartId = getOwnerIdArg();
        if (TextUtils.isEmpty(counterpartId)) {
            return;
        }
        ConversationMetadataStore.ListingContext cached = conversationMetadataStore.getListingContext(
                counterpartId,
                resolvedListingId
        );
        if (cached == null) {
            return;
        }
        if (TextUtils.isEmpty(resolvedListingId) && hasMeaningfulValue(cached.listingId)) {
            resolvedListingId = cached.listingId;
        }
        if (!hasMeaningfulValue(resolvedListingTitle) && hasMeaningfulValue(cached.title)) {
            resolvedListingTitle = cached.title;
        }
        if (!hasMeaningfulValue(resolvedListingImageUrl) && hasMeaningfulValue(cached.imageUrl)) {
            resolvedListingImageUrl = cached.imageUrl;
        }
    }

    private boolean shouldAllowCompletionAction(@Nullable String rawStatus) {
        if (TextUtils.isEmpty(rawStatus)) {
            return false;
        }
        String normalized = rawStatus.toLowerCase(Locale.US);
        return normalized.equals("pending");
    }

    private String mapCategoryLabel(@Nullable String rawCategory) {
        if (TextUtils.isEmpty(rawCategory)) {
            return getString(R.string.category_other);
        }
        switch (rawCategory.toLowerCase(Locale.US)) {
            case "electronics":
                return getString(R.string.category_electronics);
            case "clothing":
                return getString(R.string.category_clothing);
            case "books":
                return getString(R.string.category_books);
            case "furniture":
                return getString(R.string.category_furniture);
            case "donation":
                return getString(R.string.category_donation);
            case "swap":
                return getString(R.string.category_swap);
            default:
                return rawCategory.length() > 1
                        ? Character.toUpperCase(rawCategory.charAt(0)) + rawCategory.substring(1)
                        : rawCategory.toUpperCase(Locale.US);
        }
    }

    private String mapStatusLabel(@Nullable String rawStatus) {
        if (TextUtils.isEmpty(rawStatus)) {
            return getString(R.string.my_listings_status_available);
        }
        switch (rawStatus.toLowerCase(Locale.US)) {
            case "available":
                return getString(R.string.my_listings_status_available);
            case "pending":
                return getString(R.string.my_listings_status_pending);
            case "completed":
            case "swapped":
            case "donated":
                return getString(R.string.my_listings_status_completed);
            default:
                return rawStatus.length() > 1
                        ? Character.toUpperCase(rawStatus.charAt(0)) + rawStatus.substring(1)
                        : rawStatus.toUpperCase(Locale.US);
        }
    }

    private void openMyListings(boolean openCompletedTab) {
        if (!isAdded()) {
            return;
        }
        MyListingsFragment fragment = MyListingsFragment.newInstance(getListingId(), openCompletedTab);
        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_up,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.slide_out_down)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("my_listings_from_chat")
                .commit();
    }

    @Nullable
    private String getListingId() {
        if (!TextUtils.isEmpty(resolvedListingId)) {
            return resolvedListingId;
        }
        return null;
    }

    private String formatTimestamp(@Nullable String isoDate) {
        if (TextUtils.isEmpty(isoDate)) {
            return getString(R.string.just_now);
        }
        try {
            long then = parseIsoDate(isoDate);
            CharSequence relative = DateUtils.getRelativeTimeSpanString(then, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
            return relative.toString();
        } catch (ParseException e) {
            return getString(R.string.just_now);
        }
    }

    private long parseIsoDate(String isoDate) throws ParseException {
        ParseException lastException = null;
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                sdf.setLenient(false);
                return sdf.parse(isoDate).getTime();
            } catch (ParseException e) {
                lastException = e;
            }
        }
        throw lastException != null ? lastException : new ParseException("Unable to parse timestamp", 0);
    }

    @Nullable
    private String getOwnerIdArg() {
        return getArguments() != null ? getArguments().getString(ARG_OWNER_ID) : null;
    }

    private boolean isCurrentUserListingOwner() {
        String currentUserId = sessionManager.getUserId();
        return !TextUtils.isEmpty(currentUserId)
                && !TextUtils.isEmpty(listingOwnerId)
                && currentUserId.equals(listingOwnerId);
    }

    @Nullable
    private String sanitizeMetadataValue(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("null".equalsIgnoreCase(trimmed) || "undefined".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private boolean hasMeaningfulValue(@Nullable String value) {
        return !TextUtils.isEmpty(sanitizeMetadataValue(value));
    }
}
