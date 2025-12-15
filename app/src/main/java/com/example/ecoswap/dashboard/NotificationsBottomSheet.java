package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.utils.SessionManager;
import com.example.ecoswap.utils.SupabaseClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private NotificationsAdapter adapter;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private Button btnMarkAllRead;
    private String userId;

    public static NotificationsBottomSheet newInstance() {
        return new NotificationsBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvNotifications = view.findViewById(R.id.rvNotifications);
        progressBar = view.findViewById(R.id.progressNotifications);
        tvEmpty = view.findViewById(R.id.tvEmptyNotifications);
        btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);
        adapter = new NotificationsAdapter(new ArrayList<>());
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNotifications.setAdapter(adapter);
        sessionManager = SessionManager.getInstance(requireContext());
        supabaseClient = SupabaseClient.getInstance(requireContext());
        userId = sessionManager != null ? sessionManager.getUserId() : null;
        btnMarkAllRead.setOnClickListener(v -> markAllRead());
        loadNotifications();
    }

    private void loadNotifications() {
        if (supabaseClient == null || TextUtils.isEmpty(userId)) {
            progressBar.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Login to view notifications");
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        String endpoint = "/rest/v1/notifications?select=*&user_id=eq." + userId + "&order=created_at.desc";
        supabaseClient.query(endpoint, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                try {
                    JSONArray array = new JSONArray(data.toString());
                    List<NotificationItem> items = new ArrayList<>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        String id = obj.optString("id");
                        String type = obj.optString("type", "");
                        String message = obj.optString("message", "");
                        boolean isRead = obj.optBoolean("is_read", false);
                        String createdAt = obj.optString("created_at", "");
                        items.add(new NotificationItem(id, type, message, isRead, createdAt));
                    }
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            adapter.replace(items);
                            progressBar.setVisibility(View.GONE);
                            boolean empty = items.isEmpty();
                            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                            btnMarkAllRead.setVisibility(empty ? View.GONE : View.VISIBLE);
                            markUnreadAsRead(items);
                        });
                    }
                } catch (JSONException e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                    }
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void markUnreadAsRead(List<NotificationItem> items) {
        if (supabaseClient == null || items == null) return;
        for (NotificationItem item : items) {
            if (item == null || item.isRead || TextUtils.isEmpty(item.id)) continue;
            com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
            payload.addProperty("is_read", true);
            supabaseClient.update("notifications", item.id, payload, new SupabaseClient.OnDatabaseCallback() {
                @Override
                public void onSuccess(Object data) { }

                @Override
                public void onError(String error) { }
            });
        }
    }

    private void markAllRead() {
        if (supabaseClient == null || TextUtils.isEmpty(userId)) {
            return;
        }
        com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
        payload.addProperty("is_read", true);
        supabaseClient.update("notifications", "user_id", userId, payload, new SupabaseClient.OnDatabaseCallback() {
            @Override
            public void onSuccess(Object data) {
                loadNotifications();
                notifyBadgeRefresh();
            }

            @Override
            public void onError(String error) { }
        });
    }

    private void notifyBadgeRefresh() {
        if (!isAdded()) return;
        getParentFragmentManager().setFragmentResult("notifications_refresh", new Bundle());
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        notifyBadgeRefresh();
    }

    static class NotificationItem {
        final String id;
        final String type;
        final String message;
        final boolean isRead;
        final String createdAt;

        NotificationItem(String id, String type, String message, boolean isRead, String createdAt) {
            this.id = id;
            this.type = type;
            this.message = message;
            this.isRead = isRead;
            this.createdAt = createdAt;
        }
    }

    static class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {
        private final List<NotificationItem> items;

        NotificationsAdapter(List<NotificationItem> items) {
            this.items = items;
        }

        void replace(List<NotificationItem> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            NotificationItem item = items.get(position);
            holder.tvMessage.setText(item.message.isEmpty() ? "Notification" : item.message);
            holder.tvType.setText(item.type.toUpperCase(Locale.US));
            holder.tvTime.setText(formatRelative(item.createdAt, holder.itemView.getContext()));
            holder.itemView.setAlpha(item.isRead ? 0.6f : 1f);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class NotificationViewHolder extends RecyclerView.ViewHolder {
            final TextView tvMessage;
            final TextView tvTime;
            final TextView tvType;

            NotificationViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
                tvTime = itemView.findViewById(R.id.tvNotificationTime);
                tvType = itemView.findViewById(R.id.tvNotificationType);
            }
        }

        private static String formatRelative(String iso, android.content.Context context) {
            if (TextUtils.isEmpty(iso)) {
                return context.getString(R.string.just_now);
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(iso);
                if (date == null) return iso;
                long now = System.currentTimeMillis();
                long delta = now - date.getTime();
                long minutes = Math.max(1, delta / 60000);
                if (minutes < 60) return minutes + "m ago";
                long hours = minutes / 60;
                if (hours < 24) return hours + "h ago";
                long days = hours / 24;
                return days + "d ago";
            } catch (ParseException e) {
                return iso;
            }
        }
    }
}
