package com.example.ecoswap.dashboard;

import android.content.Context;
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
    private List<Message> messages;
    private List<Message> messagesFiltered;
    private OnMessageClickListener listener;

    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    public MessagesAdapter(Context context, List<Message> messages, OnMessageClickListener listener) {
        this.context = context;
        this.messages = messages;
        this.messagesFiltered = new ArrayList<>(messages);
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
        messagesFiltered.clear();
        if (query.isEmpty()) {
            messagesFiltered.addAll(messages);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Message message : messages) {
                if (message.getUserName().toLowerCase().contains(lowerCaseQuery) ||
                    message.getLastMessage().toLowerCase().contains(lowerCaseQuery) ||
                    (message.getItemName() != null && message.getItemName().toLowerCase().contains(lowerCaseQuery))) {
                    messagesFiltered.add(message);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void filterByTab(int position) {
        messagesFiltered.clear();
        switch (position) {
            case 0: // All
                messagesFiltered.addAll(messages);
                break;
            case 1: // Unread
                for (Message message : messages) {
                    if (message.getUnreadCount() > 0) {
                        messagesFiltered.add(message);
                    }
                }
                break;
            case 2: // Archived
                // TODO: Add archived filter when implemented
                break;
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
        private String userName;
        private String lastMessage;
        private String timestamp;
        private int unreadCount;
        private boolean isOnline;
        private String itemName;

        public Message(String userName, String lastMessage, String timestamp, int unreadCount, boolean isOnline, String itemName) {
            this.userName = userName;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
            this.unreadCount = unreadCount;
            this.isOnline = isOnline;
            this.itemName = itemName;
        }

        public String getUserName() { return userName; }
        public String getLastMessage() { return lastMessage; }
        public String getTimestamp() { return timestamp; }
        public int getUnreadCount() { return unreadCount; }
        public boolean isOnline() { return isOnline; }
        public String getItemName() { return itemName; }
    }
}
