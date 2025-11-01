package com.example.ecoswap.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.google.android.material.chip.Chip;
import java.util.List;

public class CommunityPostAdapter extends RecyclerView.Adapter<CommunityPostAdapter.PostViewHolder> {

    private List<CommunityFragment.CommunityPost> posts;
    private Context context;

    public CommunityPostAdapter(List<CommunityFragment.CommunityPost> posts, Context context) {
        this.posts = posts;
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        CommunityFragment.CommunityPost post = posts.get(position);

        holder.tvUserAvatar.setText(post.getUserAvatar());
        holder.tvUserName.setText(post.getUserName());
        holder.tvPostTime.setText(post.getPostTime());
        holder.tvPostContent.setText(post.getContent());
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));
        holder.chipCategory.setText(post.getCategory());

        // Set chip color based on category
        int chipColorRes;
        switch (post.getCategory()) {
            case "Swap Story":
                chipColorRes = R.color.chip_electronics;
                break;
            case "Donations":
                chipColorRes = R.color.chip_clothing;
                break;
            case "Eco Tips":
                chipColorRes = R.color.chip_furniture;
                break;
            case "Questions":
                chipColorRes = R.color.chip_books;
                break;
            default:
                chipColorRes = R.color.chip_electronics;
        }
        holder.chipCategory.setChipBackgroundColorResource(chipColorRes);

        // Like button click
        holder.layoutLike.setOnClickListener(v -> {
            post.setLikeCount(post.getLikeCount() + 1);
            holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            Toast.makeText(context, "Liked! ❤️", Toast.LENGTH_SHORT).show();
        });

        // Comment button click
        holder.layoutComment.setOnClickListener(v -> {
            Toast.makeText(context, "Comments coming soon! 💬", Toast.LENGTH_SHORT).show();
        });

        // Share button click
        holder.layoutShare.setOnClickListener(v -> {
            Toast.makeText(context, "Share coming soon! 🔗", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserAvatar, tvUserName, tvPostTime, tvPostContent;
        TextView tvLikeCount, tvCommentCount;
        Chip chipCategory;
        LinearLayout layoutLike, layoutComment, layoutShare;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserAvatar = itemView.findViewById(R.id.tvUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            layoutLike = itemView.findViewById(R.id.layoutLike);
            layoutComment = itemView.findViewById(R.id.layoutComment);
            layoutShare = itemView.findViewById(R.id.layoutShare);
        }
    }
}
