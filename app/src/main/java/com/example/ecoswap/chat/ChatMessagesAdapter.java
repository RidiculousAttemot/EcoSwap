package com.example.ecoswap.chat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_OUTGOING = 1;
    private static final int VIEW_INCOMING = 2;

    private final String currentUserId;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<String, ParticipantProfile> participantProfiles = new HashMap<>();
    private final OnAvatarClickListener avatarClickListener;

    public interface OnAvatarClickListener {
        void onAvatarClick(String userId);
    }

    public ChatMessagesAdapter(@NonNull String currentUserId, OnAvatarClickListener listener) {
        this.currentUserId = currentUserId;
        this.avatarClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_OUTGOING;
        }
        return VIEW_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_OUTGOING ? R.layout.item_chat_message_outgoing : R.layout.item_chat_message_incoming;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        ParticipantProfile profile = participantProfiles.get(message.getSenderId());
        ((MessageViewHolder) holder).bind(message, profile, avatarClickListener);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(@NonNull List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void appendMessage(@NonNull ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void setParticipantProfile(@NonNull String userId, String displayName, String avatarUrl) {
        participantProfiles.put(userId, new ParticipantProfile(displayName, avatarUrl));
        notifyDataSetChanged();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTimestamp;
        private final TextView tvAvatarFallback;
        private final ImageView imgAvatar;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvChatMessage);
            tvTimestamp = itemView.findViewById(R.id.tvChatTimestamp);
            tvAvatarFallback = itemView.findViewById(R.id.tvAvatarFallback);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
        }

        void bind(ChatMessage message, ParticipantProfile profile, OnAvatarClickListener listener) {
            tvMessage.setText(message.getMessage());
            tvTimestamp.setText(message.getDisplayTimestamp());
            bindAvatar(profile, message.getSenderId(), listener);
        }

        private void bindAvatar(ParticipantProfile profile, String senderId, OnAvatarClickListener listener) {
            if (imgAvatar == null || tvAvatarFallback == null) {
                return;
            }
            
            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onAvatarClick(senderId);
                }
            };
            
            imgAvatar.setOnClickListener(clickListener);
            tvAvatarFallback.setOnClickListener(clickListener);

            if (profile != null && !TextUtils.isEmpty(profile.avatarUrl)) {
                imgAvatar.setVisibility(View.VISIBLE);
                tvAvatarFallback.setVisibility(View.GONE);
                Glide.with(itemView)
                        .load(profile.avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_launcher_background) // Changed from bg_circle_white to see if it's loading
                        .error(R.drawable.ic_launcher_background)
                        .into(imgAvatar);
            } else {
                imgAvatar.setVisibility(View.GONE);
                tvAvatarFallback.setVisibility(View.VISIBLE);
                tvAvatarFallback.setText(extractInitials(profile != null ? profile.displayName : null, senderId));
            }
        }

        private String extractInitials(String name, String fallback) {
            String source = !TextUtils.isEmpty(name) ? name : fallback;
            if (TextUtils.isEmpty(source)) {
                return "?";
            }
            String[] parts = source.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.US);
            }
            return source.substring(0, Math.min(2, source.length())).toUpperCase(Locale.US);
        }
    }

    private static class ParticipantProfile {
        final String displayName;
        final String avatarUrl;

        ParticipantProfile(String displayName, String avatarUrl) {
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
        }
    }

    public static class ChatMessage {
        private final String id;
        private final String senderId;
        private final String message;
        private final String displayTimestamp;

        public ChatMessage(String id, String senderId, String message, String displayTimestamp) {
            this.id = id;
            this.senderId = senderId;
            this.message = message;
            this.displayTimestamp = displayTimestamp;
        }

        public String getId() {
            return id;
        }

        public String getSenderId() {
            return senderId;
        }

        public String getMessage() {
            return message;
        }

        public String getDisplayTimestamp() {
            return displayTimestamp;
        }
    }
}
