package com.example.ecoswap.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.Post;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    
    private List<Post> postList;
    private OnPostClickListener listener;
    
    public interface OnPostClickListener {
        void onPostClick(Post post);
    }
    
    public PostAdapter(List<Post> postList, OnPostClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.bind(post, listener);
    }
    
    @Override
    public int getItemCount() {
        return postList.size();
    }
    
    static class PostViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPostImage;
        private TextView tvPostTitle, tvPostAuthor, tvPostDate, tvPostPreview;
        
        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostAuthor = itemView.findViewById(R.id.tvPostAuthor);
            tvPostDate = itemView.findViewById(R.id.tvPostDate);
            tvPostPreview = itemView.findViewById(R.id.tvPostPreview);
        }
        
        public void bind(Post post, OnPostClickListener listener) {
            tvPostTitle.setText(post.getTitle());
            tvPostAuthor.setText(post.getAuthor());
            tvPostDate.setText(post.getDate());
            tvPostPreview.setText(post.getContent());
            
            itemView.setOnClickListener(v -> listener.onPostClick(post));
        }
    }
}
