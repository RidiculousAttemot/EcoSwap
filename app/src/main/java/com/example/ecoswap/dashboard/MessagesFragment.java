package com.example.ecoswap.dashboard;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.ecoswap.R;
import com.example.ecoswap.chat.ChatFragment;
import com.example.ecoswap.utils.ChatFeatureCompat;
import com.example.ecoswap.utils.ConversationMetadataStore;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.tabs.TabLayout;
import okhttp3.HttpUrl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

public class MessagesFragment extends Fragment {

    private static final String TAG = "MessagesFragment";

    private static final String[] ISO_PATTERNS = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
    };

    private RecyclerView rvMessages;
    private MessagesAdapter adapter;
    private EditText etSearchMessages;
    private ImageView btnClearSearch, btnFilter;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmptyMessages;
    private final List<MessagesAdapter.Message> messagesList = new ArrayList<>();
    private final Map<String, ListingMetadata> listingMetadataCache = new HashMap<>();
    private final Set<String> pendingListingLookups = new HashSet<>();
    private SessionManager sessionManager;
    private SupabaseClient supabaseClient;
    private ConversationMetadataStore conversationMetadataStore;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        
        initDataProviders();
        // Initialize views
        rvMessages = view.findViewById(R.id.rvMessages);
        etSearchMessages = view.findViewById(R.id.etSearchMessages);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnFilter = view.findViewById(R.id.btnFilter);
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tvEmptyMessages = view.findViewById(R.id.tvEmptyMessages);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup search
        setupSearch();
        
        // Setup tabs
        setupTabs();
        
        // Setup swipe refresh
        setupSwipeRefresh();
        
        // Setup filter button
        btnFilter.setOnClickListener(v -> showFilterDialog());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rvMessages != null) {
            loadMessages();
        }
    }

    private void initDataProviders() {
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
        conversationMetadataStore = new ConversationMetadataStore(requireContext());

        getParentFragmentManager().setFragmentResultListener("messages_refresh", this, (requestKey, bundle) -> {
            loadMessages();
        });
    }
    
    private void setupRecyclerView() {
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessagesAdapter(requireContext(), messagesList, this::openConversation);
        rvMessages.setAdapter(adapter);
        adapter.filterByTab(0);
    }
    
    private void setupSearch() {
        etSearchMessages.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                adapter.filter(query);
                
                // Show/hide clear button
                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        btnClearSearch.setOnClickListener(v -> {
            etSearchMessages.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });
    }
    
    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                adapter.filterByTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary_green);
        swipeRefresh.setOnRefreshListener(this::loadMessages);
    }

    private void showFilterDialog() {
        String[] options = new String[]{"All", "Unread", "Archived"};
        int current = tabLayout != null ? tabLayout.getSelectedTabPosition() : 0;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Show conversations")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    if (tabLayout != null && tabLayout.getTabAt(which) != null) {
                        tabLayout.getTabAt(which).select();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadMessages() {
        if (!isAdded() || rvMessages == null) {
            return;
        }
        if (swipeRefresh != null && !swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(true);
        }

        if (sessionManager == null || supabaseClient == null) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            return;
        }

        String currentUserId = sessionManager.getUserId();
        if (TextUtils.isEmpty(currentUserId)) {
            if (swipeRefresh != null) {
                swipeRefresh.setRefreshing(false);
            }
            messagesList.clear();
            adapter.setMessages(messagesList);
            updateEmptyState();
            return;
        }

        boolean includeListingMetadata = ChatFeatureCompat.isListingMetadataSupported();
        String endpoint = buildChatsEndpoint(currentUserId, includeListingMetadata);
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                if (!isAdded()) {
                    return;
                }
                swipeRefresh.setRefreshing(false);
                parseConversations(data, currentUserId);
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) {
                    return;
                }
                swipeRefresh.setRefreshing(false);
                if (ChatFeatureCompat.isListingMetadataSupported() && ChatFeatureCompat.isListingMetadataError(error)) {
                    Log.w(TAG, "Supabase missing listing metadata columns; falling back to base conversations.");
                    ChatFeatureCompat.disableListingMetadata();
                    loadMessages();
                    return;
                }
                Log.e(TAG, "Failed to load conversations: " + error);
                Toast.makeText(requireContext(), R.string.messages_error_generic, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private String buildChatsEndpoint(String userId, boolean includeListingMetadata) {
        if (supabaseClient == null || TextUtils.isEmpty(supabaseClient.getSupabaseUrl())) {
            return "/rest/v1/chats";
        }
        HttpUrl baseUrl = HttpUrl.parse(supabaseClient.getSupabaseUrl());
        if (baseUrl == null) {
            return "/rest/v1/chats";
        }

        StringBuilder selectFields = new StringBuilder("id,message,created_at,is_read,sender_id,receiver_id");
        if (includeListingMetadata) {
            selectFields.append(",listing_id,listing_title_snapshot,listing_image_url_snapshot,listing:posts!chats_listing_id_fkey(id,title,image_url)");
        }
        selectFields.append(",sender:profiles!chats_sender_id_fkey(id,name,profile_image_url),receiver:profiles!chats_receiver_id_fkey(id,name,profile_image_url)");
        String orFilter = String.format(Locale.US, "(sender_id.eq.%s,receiver_id.eq.%s)", userId, userId);

        HttpUrl url = baseUrl.newBuilder()
                .addPathSegments("rest/v1/chats")
                .addQueryParameter("select", selectFields.toString())
                .addQueryParameter("or", orFilter)
                .addQueryParameter("order", "created_at.desc")
                .build();

        return url.toString().replace(supabaseClient.getSupabaseUrl(), "");
    }

    private void parseConversations(Object data, String currentUserId) {
        try {
            JSONArray array = new JSONArray(data.toString());
            Map<String, MessagesAdapter.Message> conversations = new LinkedHashMap<>();

            for (int i = 0; i < array.length(); i++) {
                JSONObject chat = array.getJSONObject(i);
                String senderId = chat.optString("sender_id");
                String receiverId = chat.optString("receiver_id");
                String listingId = chat.has("listing_id") && !chat.isNull("listing_id") ? chat.optString("listing_id", null) : null;
                listingId = sanitizeMetadata(listingId);
                String messageBody = chat.optString("message", "");
                boolean isRead = chat.optBoolean("is_read", false);
                String createdAt = chat.optString("created_at");

                boolean fromCurrentUser = currentUserId.equals(senderId);
                String counterpartId = fromCurrentUser ? receiverId : senderId;
                if (TextUtils.isEmpty(counterpartId)) {
                    continue;
                }

                JSONObject senderProfile = chat.optJSONObject("sender");
                JSONObject receiverProfile = chat.optJSONObject("receiver");
                JSONObject listingObject = ChatFeatureCompat.isListingMetadataSupported() ? chat.optJSONObject("listing") : null;
                String listingTitleSnapshot = ChatFeatureCompat.isListingMetadataSupported()
                    ? chat.optString("listing_title_snapshot", null)
                    : null;
                String listingImageSnapshot = ChatFeatureCompat.isListingMetadataSupported()
                    ? chat.optString("listing_image_url_snapshot", null)
                    : null;
                listingTitleSnapshot = sanitizeMetadata(listingTitleSnapshot);
                listingImageSnapshot = sanitizeMetadata(listingImageSnapshot);

                JSONObject counterpartProfile = fromCurrentUser ? receiverProfile : senderProfile;
                String counterpartName = counterpartProfile != null
                        ? counterpartProfile.optString("name", getString(R.string.messages_unknown_user))
                        : getString(R.string.messages_unknown_user);
                String avatarUrl = counterpartProfile != null ? counterpartProfile.optString("profile_image_url", null) : null;

                String listingTitle = listingObject != null ? listingObject.optString("title", null) : null;
                String listingImage = listingObject != null ? listingObject.optString("image_url", null) : null;
                listingTitle = sanitizeMetadata(listingTitle);
                listingImage = sanitizeMetadata(listingImage);
                if (TextUtils.isEmpty(listingTitle) && !TextUtils.isEmpty(listingTitleSnapshot)) {
                    listingTitle = listingTitleSnapshot;
                }
                if (TextUtils.isEmpty(listingImage) && !TextUtils.isEmpty(listingImageSnapshot)) {
                    listingImage = listingImageSnapshot;
                }
                if (!TextUtils.isEmpty(listingId) && (!TextUtils.isEmpty(listingTitle) || !TextUtils.isEmpty(listingImage))) {
                    listingMetadataCache.put(listingId, new ListingMetadata(listingTitle, listingImage));
                }
                ConversationMetadataStore.ListingContext storedContext = conversationMetadataStore != null
                        ? conversationMetadataStore.getListingContext(counterpartId, listingId)
                        : null;
                if (storedContext != null) {
                    if (TextUtils.isEmpty(listingId) && !TextUtils.isEmpty(storedContext.listingId)) {
                        listingId = storedContext.listingId;
                    }
                    if (TextUtils.isEmpty(listingTitle) && !TextUtils.isEmpty(storedContext.title)) {
                        listingTitle = storedContext.title;
                    }
                    if (TextUtils.isEmpty(listingImage) && !TextUtils.isEmpty(storedContext.imageUrl)) {
                        listingImage = storedContext.imageUrl;
                    }
                }

                ListingMetadata cachedMetadata = !TextUtils.isEmpty(listingId) ? listingMetadataCache.get(listingId) : null;
                if (cachedMetadata != null) {
                    if (TextUtils.isEmpty(listingTitle)) {
                        listingTitle = cachedMetadata.title;
                    }
                    if (TextUtils.isEmpty(listingImage)) {
                        listingImage = cachedMetadata.imageUrl;
                    }
                }
                boolean archived = conversationMetadataStore != null && conversationMetadataStore.isArchived(counterpartId, listingId);
                if (conversationMetadataStore != null && conversationMetadataStore.isBlocked(counterpartId, listingId)) {
                    continue;
                }
                String key = counterpartId + "|" + (listingId != null ? listingId : "direct");

                MessagesAdapter.Message summary = conversations.get(key);
                if (summary == null) {
                    int unreadCount = (!fromCurrentUser && !isRead) ? 1 : 0;
                        summary = new MessagesAdapter.Message(
                            counterpartName,
                            messageBody,
                            formatTimestamp(createdAt),
                            unreadCount,
                            false,
                            listingTitle,
                            counterpartId,
                            listingId,
                            listingTitle,
                            listingImage,
                            avatarUrl,
                            archived
                    );
                    conversations.put(key, summary);
                } else if (!fromCurrentUser && !isRead) {
                    summary.incrementUnreadCount();
                }
                if (storedContext != null && summary != null) {
                    summary.setListingId(storedContext.listingId);
                    summary.setListingMetadata(storedContext.title, storedContext.imageUrl);
                }
                if (!TextUtils.isEmpty(avatarUrl)) {
                    summary.setAvatarUrl(avatarUrl);
                }
                if (!TextUtils.isEmpty(listingId) && TextUtils.isEmpty(summary.getItemName())) {
                    summary.setListingMetadata(listingTitle, listingImage);
                }
                if (!TextUtils.isEmpty(listingId) && TextUtils.isEmpty(summary.getListingTitle())) {
                    if (!listingMetadataCache.containsKey(listingId)) {
                        pendingListingLookups.add(listingId);
                    }
                }
            }

            messagesList.clear();
            messagesList.addAll(conversations.values());
            adapter.setMessages(messagesList);
            if (conversationMetadataStore != null) {
                for (MessagesAdapter.Message message : messagesList) {
                    if (message == null || TextUtils.isEmpty(message.getUserId())) {
                        continue;
                    }
                    if (TextUtils.isEmpty(message.getListingId())
                            && TextUtils.isEmpty(message.getListingTitle())
                            && TextUtils.isEmpty(message.getListingImageUrl())) {
                        continue;
                    }
                    conversationMetadataStore.saveListingContext(
                            message.getUserId(),
                            sanitizeMetadata(message.getListingId()),
                            sanitizeMetadata(message.getListingTitle()),
                            sanitizeMetadata(message.getListingImageUrl())
                    );
                }
            }
            if (!pendingListingLookups.isEmpty()) {
                fetchMissingListingMetadata();
            }
            updateEmptyState();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse conversations", e);
            Toast.makeText(requireContext(), R.string.messages_error_generic, Toast.LENGTH_SHORT).show();
            updateEmptyState();
        }
    }

    private void fetchMissingListingMetadata() {
        if (supabaseClient == null || pendingListingLookups.isEmpty()) {
            return;
        }
        List<String> ids = new ArrayList<>(pendingListingLookups);
        pendingListingLookups.clear();
        StringBuilder builder = new StringBuilder("(");
        for (int i = 0; i < ids.size(); i++) {
            builder.append("\"").append(ids.get(i)).append("\"");
            if (i < ids.size() - 1) {
                builder.append(",");
            }
        }
        builder.append(")");
        String endpoint = "/rest/v1/posts?select=id,title,image_url&id=in." + Uri.encode(builder.toString());
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject post = array.getJSONObject(i);
                        String id = post.optString("id");
                        if (TextUtils.isEmpty(id)) {
                            continue;
                        }
                        String title = sanitizeMetadata(post.optString("title", null));
                        String image = sanitizeMetadata(post.optString("image_url", null));
                        listingMetadataCache.put(id, new ListingMetadata(title, image));
                    }
                    for (MessagesAdapter.Message message : messagesList) {
                        if (!TextUtils.isEmpty(message.getListingId())) {
                            ListingMetadata metadata = listingMetadataCache.get(message.getListingId());
                            if (metadata != null) {
                                message.setListingMetadata(metadata.title, metadata.imageUrl);
                            }
                        }
                    }
                    adapter.setMessages(messagesList);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse listing metadata", e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to fetch listing metadata: " + error);
            }
        });
    }

    private void updateEmptyState() {
        boolean hasMessages = !messagesList.isEmpty();
        rvMessages.setVisibility(hasMessages ? View.VISIBLE : View.GONE);
        if (!hasMessages && tvEmptyMessages != null) {
            tvEmptyMessages.setText("No conversations yet. Open a listing to start chatting or check archived chats.");
        }
        tvEmptyMessages.setVisibility(hasMessages ? View.GONE : View.VISIBLE);
    }

    private String formatTimestamp(String isoDate) {
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
        for (String pattern : ISO_PATTERNS) {
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
    private static String sanitizeMetadata(@Nullable String value) {
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

    private static class ListingMetadata {
        final String title;
        final String imageUrl;

        ListingMetadata(String title, String imageUrl) {
            this.title = title;
            this.imageUrl = imageUrl;
        }
    }

    private void openConversation(MessagesAdapter.Message message) {
        if (message == null || getActivity() == null) {
            return;
        }
        String counterpartId = message.getUserId();
        if (TextUtils.isEmpty(counterpartId)) {
            Toast.makeText(requireContext(), R.string.messages_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }
        String listingId = sanitizeMetadata(message.getListingId());
        String listingTitle = sanitizeMetadata(!TextUtils.isEmpty(message.getListingTitle())
                ? message.getListingTitle()
                : message.getItemName());
        String listingImage = sanitizeMetadata(message.getListingImageUrl());
        if (conversationMetadataStore != null) {
            ConversationMetadataStore.ListingContext cached = conversationMetadataStore.getListingContext(counterpartId, listingId);
            if (cached != null) {
                if (TextUtils.isEmpty(listingId)) {
                    listingId = cached.listingId;
                }
                if (TextUtils.isEmpty(listingTitle)) {
                    listingTitle = cached.title;
                }
                if (TextUtils.isEmpty(listingImage)) {
                    listingImage = cached.imageUrl;
                }
            }
        }
        ChatFragment fragment = ChatFragment.newInstance(
                counterpartId,
                listingId,
                listingTitle,
            listingImage,
            null,
            null
        );

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
}
