package com.example.ecoswap.dashboard;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private Context context;
    private final List<Message> messages;
    private final List<Message> messagesFiltered;
    private final OnMessageClickListener listener;
    private String currentSearchQuery = "";
    private int currentTabPosition = 0;

    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    public MessagesAdapter(Context context, List<Message> messages, OnMessageClickListener listener) {
        this.context = context;
        this.messages = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
        this.messagesFiltered = new ArrayList<>(this.messages);
        this.listener = listener;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messagesFiltered.get(position);

        // Set avatar initials
        holder.tvAvatar.setText(getInitials(message.getUserName()));

        // Set user name
        holder.tvUserName.setText(message.getUserName());

        // Set message preview
        holder.tvMessagePreview.setText(message.getLastMessage());
        
        // Set message preview style based on unread status
        if (message.getUnreadCount() > 0) {
            holder.tvMessagePreview.setTextColor(context.getResources().getColor(R.color.text_primary));
            holder.tvMessagePreview.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.tvMessagePreview.setTextColor(context.getResources().getColor(R.color.text_secondary));
            holder.tvMessagePreview.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Set timestamp
        holder.tvTimestamp.setText(message.getTimestamp());

        // Show/hide unread badge
        if (message.getUnreadCount() > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(message.getUnreadCount()));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        // Show/hide online indicator
        if (message.isOnline()) {
            holder.onlineIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.onlineIndicator.setVisibility(View.GONE);
        }

        // Show/hide item context
        if (message.getItemName() != null && !message.getItemName().isEmpty()) {
            holder.itemContextLayout.setVisibility(View.VISIBLE);
            holder.tvItemContext.setText(message.getItemName());
        } else {
            holder.itemContextLayout.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messagesFiltered.size();
    }

    public void filter(String query) {
        currentSearchQuery = query != null ? query.toLowerCase() : "";
        rebuildFiltered();
    }

    public void filterByTab(int position) {
        currentTabPosition = position;
        rebuildFiltered();
    }

    public void setMessages(@NonNull List<Message> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        rebuildFiltered();
    }

    private void rebuildFiltered() {
        List<Message> base = new ArrayList<>();
        switch (currentTabPosition) {
            case 1:
                for (Message message : messages) {
                    if (message.getUnreadCount() > 0) {
                        base.add(message);
                    }
                }
                break;
            case 2:
                // Archived conversations not yet implemented
                break;
            default:
                base.addAll(messages);
                break;
        }

        messagesFiltered.clear();
        if (currentSearchQuery.isEmpty()) {
            messagesFiltered.addAll(base);
        } else {
            for (Message message : base) {
                if (message.matchesQuery(currentSearchQuery)) {
                    messagesFiltered.add(message);
                }
            }
        }
        notifyDataSetChanged();
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        } else {
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvUserName, tvMessagePreview, tvTimestamp, tvUnreadBadge, tvItemContext;
        View onlineIndicator;
        LinearLayout itemContextLayout;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvMessagePreview = itemView.findViewById(R.id.tvMessagePreview);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            tvItemContext = itemView.findViewById(R.id.tvItemContext);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
            itemContextLayout = itemView.findViewById(R.id.itemContextLayout);
        }
    }

    // Message Model
    public static class Message {
        private final String userId;
        private final String listingId;
        private final String listingTitle;
        private final String listingImageUrl;
        private String userName;
        private String lastMessage;
        private String timestamp;
        private int unreadCount;
        private boolean isOnline;
        private String itemName;

        public Message(String userName, String lastMessage, String timestamp, int unreadCount, boolean isOnline, String itemName) {
            this(userName, lastMessage, timestamp, unreadCount, isOnline, itemName, null, null, null, null);
        }

        public Message(String userName, String lastMessage, String timestamp, int unreadCount, boolean isOnline, String itemName,
                        String userId, String listingId, String listingTitle) {
            this(userName, lastMessage, timestamp, unreadCount, isOnline, itemName, userId, listingId, listingTitle, null);
        }

        public Message(String userName, String lastMessage, String timestamp, int unreadCount, boolean isOnline, String itemName,
                        String userId, String listingId, String listingTitle, String listingImageUrl) {
            this.userName = userName;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
            this.unreadCount = unreadCount;
            this.isOnline = isOnline;
            this.itemName = itemName != null ? itemName : listingTitle;
            this.userId = userId;
            this.listingId = listingId;
            this.listingTitle = listingTitle;
            this.listingImageUrl = listingImageUrl;
        }

        public String getUserName() { return userName; }
        public String getLastMessage() { return lastMessage; }
        public String getTimestamp() { return timestamp; }
        public int getUnreadCount() { return unreadCount; }
        public boolean isOnline() { return isOnline; }
        public String getItemName() { return itemName; }

        public String getUserId() { return userId; }
        public String getListingId() { return listingId; }
        public String getListingTitle() { return listingTitle; }
        public String getListingImageUrl() { return listingImageUrl; }

        public void incrementUnreadCount() {
            unreadCount++;
        }

        public boolean matchesQuery(String query) {
            if (TextUtils.isEmpty(query)) {
                return true;
            }
            String lowerQuery = query.toLowerCase();
            return (userName != null && userName.toLowerCase().contains(lowerQuery))
                    || (lastMessage != null && lastMessage.toLowerCase().contains(lowerQuery))
                    || (itemName != null && itemName.toLowerCase().contains(lowerQuery));
        }
    }
}
