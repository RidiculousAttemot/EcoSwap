package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.Post;
import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {
    
    private ImageView ivPostImage;
    private TextView tvPostTitle, tvPostAuthor, tvPostDate, tvPostContent;
    private RecyclerView recyclerViewComments;
    private String postId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        
        postId = getIntent().getStringExtra("post_id");
        
        initViews();
        loadPostDetail();
        loadComments();
    }
    
    private void initViews() {
        ivPostImage = findViewById(R.id.ivPostImage);
        tvPostTitle = findViewById(R.id.tvPostTitle);
        tvPostAuthor = findViewById(R.id.tvPostAuthor);
        tvPostDate = findViewById(R.id.tvPostDate);
        tvPostContent = findViewById(R.id.tvPostContent);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void loadPostDetail() {
        // TODO: Load post details from Supabase
        tvPostTitle.setText("Post Title");
        tvPostAuthor.setText("Author Name");
        tvPostDate.setText("October 15, 2025");
        tvPostContent.setText("Post content goes here...");
    }
    
    private void loadComments() {
        // TODO: Load comments from Supabase
        Toast.makeText(this, "Loading comments...", Toast.LENGTH_SHORT).show();
    }
}
