package com.example.ecoswap.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments;

    public CommentsAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.tvAuthor.setText(comment.authorName);
        holder.tvContent.setText(comment.content);
        holder.tvTime.setText(comment.timeAgo);
        holder.tvAvatar.setText(getInitials(comment.authorName));
    }

    @Override
    public int getItemCount() {
        return comments.size();
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

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvContent, tvTime, tvAvatar;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvAvatar = itemView.findViewById(R.id.tvCommentAvatar);
        }
    }

    public static class Comment {
        String id;
        String authorName;
        String content;
        String timeAgo;

        public Comment(String id, String authorName, String content, String timeAgo) {
            this.id = id;
            this.authorName = authorName;
            this.content = content;
            this.timeAgo = timeAgo;
        }
    }
}