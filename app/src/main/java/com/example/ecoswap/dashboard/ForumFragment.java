package com.example.ecoswap.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.adapters.PostAdapter;
import com.example.ecoswap.models.Post;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ForumFragment extends Fragment {
    
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private FloatingActionButton fabCreatePost;
    private List<Post> postList;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forum, container, false);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadPosts();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        postList = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(postList, post -> {
            Intent intent = new Intent(getActivity(), PostDetailActivity.class);
            intent.putExtra("post_id", post.getId());
            startActivity(intent);
        });
        recyclerViewPosts.setAdapter(postAdapter);
    }
    
    private void setupListeners() {
        fabCreatePost.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Create post functionality to be implemented", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadPosts() {
        // TODO: Load posts from Supabase
        Toast.makeText(getContext(), "Loading posts...", Toast.LENGTH_SHORT).show();
    }
}
