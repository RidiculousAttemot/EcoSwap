package com.example.ecoswap.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import java.util.ArrayList;
import java.util.List;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_OUTGOING = 1;
    private static final int VIEW_INCOMING = 2;

    private final String currentUserId;
    private final List<ChatMessage> messages = new ArrayList<>();

    public ChatMessagesAdapter(@NonNull String currentUserId) {
        this.currentUserId = currentUserId;
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
        ((MessageViewHolder) holder).bind(message);
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

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTimestamp;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvChatMessage);
            tvTimestamp = itemView.findViewById(R.id.tvChatTimestamp);
        }

        void bind(ChatMessage message) {
            tvMessage.setText(message.getMessage());
            tvTimestamp.setText(message.getDisplayTimestamp());
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
