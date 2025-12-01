package com.example.ecoswap.chat;

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
import com.example.ecoswap.utils.ChatFeatureCompat;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.appbar.MaterialToolbar;
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

        initViews(view);
        setupToolbar(view);
        setupRecyclerView();
        setupSendButton();
        loadConversationHistory();
        return view;
    }

    private void initViews(View root) {
        rvMessages = root.findViewById(R.id.rvChatMessages);
        btnSend = root.findViewById(R.id.btnSend);
        etMessage = root.findViewById(R.id.etMessage);
        progressSending = root.findViewById(R.id.progressSending);
        tvListingTitle = root.findViewById(R.id.tvListingTitle);
        ivListing = root.findViewById(R.id.ivListingPreview);

        tvListingTitle.setText(getArguments() != null ? getArguments().getString(ARG_LISTING_TITLE, "") : "");
        String imageUrl = getArguments() != null ? getArguments().getString(ARG_IMAGE_URL) : null;
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(ivListing);
        }
    }

    private void setupToolbar(View root) {
        MaterialToolbar toolbar = root.findViewById(R.id.chatToolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void setupRecyclerView() {
        messagesLayoutManager = new LinearLayoutManager(requireContext());
        messagesLayoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(messagesLayoutManager);
        String currentUserId = sessionManager.getUserId();
        chatAdapter = new ChatMessagesAdapter(currentUserId != null ? currentUserId : "");
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

    private void sendMessage(String content) {
        String currentUserId = sessionManager.getUserId();
        String ownerId = getArguments() != null ? getArguments().getString(ARG_OWNER_ID) : null;
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(ownerId)) {
            return;
        }

        btnSend.setEnabled(false);
        progressSending.setVisibility(View.VISIBLE);

        JsonObject payload = new JsonObject();
        payload.addProperty("sender_id", currentUserId);
        payload.addProperty("receiver_id", ownerId);
        payload.addProperty("message", content);
        String listingId = getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : null;
        if (ChatFeatureCompat.isListingMetadataSupported() && !TextUtils.isEmpty(listingId)) {
            payload.addProperty("listing_id", listingId);
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
        String otherUserId = getArguments() != null ? getArguments().getString(ARG_OWNER_ID) : null;
        if (TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(otherUserId)) {
            return;
        }

        String endpoint = buildThreadEndpoint(currentUserId, otherUserId, getArguments() != null ? getArguments().getString(ARG_LISTING_ID) : null);
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
                        ? "id,message,created_at,sender_id,receiver_id,listing_id"
                        : "id,message,created_at,sender_id,receiver_id")
                .addQueryParameter("or", participantsFilter)
                .addQueryParameter("order", "created_at.asc");
        if (ChatFeatureCompat.isListingMetadataSupported() && !TextUtils.isEmpty(listingId)) {
            builder.addQueryParameter("listing_id", "eq." + listingId);
        }
        return builder.build().toString().replace(supabaseClient.getSupabaseUrl(), "");
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
}
