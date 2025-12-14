package com.example.ecoswap.dashboard;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.ecoswap.R;
import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private final Context context;
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

        bindAvatar(holder, message);

        // Set user name
        holder.tvUserName.setText(message.getUserName());

        // Set message preview
        holder.tvMessagePreview.setText(message.getLastMessage());
        
        // Set message preview style based on unread status
        if (message.getUnreadCount() > 0) {
            holder.tvMessagePreview.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary));
            holder.tvMessagePreview.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.tvMessagePreview.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary));
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
            bindListingImage(holder, message.getListingImageUrl());
        } else {
            holder.itemContextLayout.setVisibility(View.GONE);
            if (holder.imgItemContext != null) {
                holder.imgItemContext.setVisibility(View.GONE);
            }
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
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(this.messages, newMessages));
        messages.clear();
        messages.addAll(newMessages);
        rebuildFiltered();
        diffResult.dispatchUpdatesTo(this);
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

    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<Message> oldList;
        private final List<Message> newList;

        MessageDiffCallback(List<Message> oldList, List<Message> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Assumes each message has a unique ID. If not, another unique property should be used.
            return oldList.get(oldItemPosition).getUserId().equals(newList.get(newItemPosition).getUserId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
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

    private void bindAvatar(@NonNull MessageViewHolder holder, @NonNull Message message) {
        if (holder.imgAvatar != null && !TextUtils.isEmpty(message.getAvatarUrl())) {
            holder.imgAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setVisibility(View.GONE);
            Glide.with(holder.itemView)
                    .load(message.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.bg_circle_placeholder)
                    .error(R.drawable.bg_circle_placeholder)
                    .into(holder.imgAvatar);
        } else {
            if (holder.imgAvatar != null) {
                holder.imgAvatar.setVisibility(View.GONE);
            }
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText(getInitials(message.getUserName()));
        }
    }

    private void bindListingImage(@NonNull MessageViewHolder holder, String imageUrl) {
        if (holder.imgItemContext == null) {
            return;
        }
        if (!TextUtils.isEmpty(imageUrl)) {
            holder.imgItemContext.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgItemContext);
        } else {
            holder.imgItemContext.setVisibility(View.GONE);
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvUserName, tvMessagePreview, tvTimestamp, tvUnreadBadge, tvItemContext;
        View onlineIndicator;
        View itemContextLayout;
        ImageView imgAvatar;
        ImageView imgItemContext;

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
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            imgItemContext = itemView.findViewById(R.id.imgItemContext);
        }
    }

    // Message Model
    public static class Message {
        private final String userId;
        private String listingId;
        private String listingTitle;
        private String listingImageUrl;
        private String avatarUrl;
        private final String userName;
        private final String lastMessage;
        private final String timestamp;
        private int unreadCount;
        private final boolean isOnline;
        private String itemName;

        public Message(String userName, String lastMessage, String timestamp, int unreadCount, boolean isOnline, String itemName) {
            this(userName, lastMessage, timestamp, unreadCount, isOnline, itemName, null, null, null);
        }

        public Message(String userName, String lastMessage, String timestamp, int unreadCount, boolean isOnline, String itemName,
                        String userId, String listingId, String listingTitle) {
            this(userName, lastMessage, timestamp, unreadCount, isOnline, itemName, userId, listingId, listingTitle, null, null);
        }

        public Message(String userName, String lastMessage, String timestamp, int unreadCount, boolean isOnline, String itemName,
                        String userId, String listingId, String listingTitle, String listingImageUrl, String avatarUrl) {
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
            this.avatarUrl = avatarUrl;
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
        public String getAvatarUrl() { return avatarUrl; }

        public void setListingMetadata(String title, String imageUrl) {
            if (!TextUtils.isEmpty(title)) {
                this.listingTitle = title;
                if (TextUtils.isEmpty(this.itemName)) {
                    this.itemName = title;
                } else {
                    this.itemName = title;
                }
            }
            if (!TextUtils.isEmpty(imageUrl)) {
                this.listingImageUrl = imageUrl;
            }
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public void setListingId(String listingId) {
            if (!TextUtils.isEmpty(listingId)) {
                this.listingId = listingId;
            }
        }

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

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Message)) return false;
            Message other = (Message) obj;
            return userId.equals(other.userId) &&
                    listingId.equals(other.listingId) &&
                    listingTitle.equals(other.listingTitle) &&
                    listingImageUrl.equals(other.listingImageUrl) &&
                    avatarUrl.equals(other.avatarUrl) &&
                    userName.equals(other.userName) &&
                    lastMessage.equals(other.lastMessage) &&
                    timestamp.equals(other.timestamp) &&
                    unreadCount == other.unreadCount &&
                    isOnline == other.isOnline &&
                    itemName.equals(other.itemName);
        }

        @Override
        public int hashCode() {
            int result = userId.hashCode();
            result = 31 * result + listingId.hashCode();
            result = 31 * result + listingTitle.hashCode();
            result = 31 * result + listingImageUrl.hashCode();
            result = 31 * result + avatarUrl.hashCode();
            result = 31 * result + userName.hashCode();
            result = 31 * result + lastMessage.hashCode();
            result = 31 * result + timestamp.hashCode();
            result = 31 * result + unreadCount;
            result = 31 * result + (isOnline ? 1 : 0);
            result = 31 * result + itemName.hashCode();
            return result;
        }
    }
}
