package com.example.ecoswap.dashboard;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {
    
    private RecyclerView rvCommunityPosts;
    private FloatingActionButton fabCreatePost;
    private EditText etSearchPosts;
    private Chip chipAllPosts, chipSwapStories, chipDonations, chipTips, chipQuestions;
    
    private CommunityPostAdapter postAdapter;
    private List<CommunityPost> postList;
    private String currentFilter = "All Posts";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);
        
        initViews(view);
        setupCategoryFilters();
        setupRecyclerView();
        loadPosts();
        setupFAB();
        
        return view;
    }
    
    private void initViews(View view) {
        rvCommunityPosts = view.findViewById(R.id.rvCommunityPosts);
        fabCreatePost = view.findViewById(R.id.fabCreatePost);
        etSearchPosts = view.findViewById(R.id.etSearchPosts);
        
        chipAllPosts = view.findViewById(R.id.chipAllPosts);
        chipSwapStories = view.findViewById(R.id.chipSwapStories);
        chipDonations = view.findViewById(R.id.chipDonations);
        chipTips = view.findViewById(R.id.chipTips);
        chipQuestions = view.findViewById(R.id.chipQuestions);
    }
    
    private void setupCategoryFilters() {
        View.OnClickListener chipClickListener = v -> {
            // Uncheck all chips
            chipAllPosts.setChecked(false);
            chipSwapStories.setChecked(false);
            chipDonations.setChecked(false);
            chipTips.setChecked(false);
            chipQuestions.setChecked(false);
            
            // Check the clicked chip
            Chip clickedChip = (Chip) v;
            clickedChip.setChecked(true);
            currentFilter = clickedChip.getText().toString();
            
            // Filter posts
            filterPosts(currentFilter);
        };
        
        chipAllPosts.setOnClickListener(chipClickListener);
        chipSwapStories.setOnClickListener(chipClickListener);
        chipDonations.setOnClickListener(chipClickListener);
        chipTips.setOnClickListener(chipClickListener);
        chipQuestions.setOnClickListener(chipClickListener);
    }
    
    private void setupRecyclerView() {
        postList = new ArrayList<>();
        postAdapter = new CommunityPostAdapter(postList, getContext());
        rvCommunityPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCommunityPosts.setAdapter(postAdapter);
    }
    
    private void loadPosts() {
        // Sample posts data
        postList.add(new CommunityPost(
            "John Doe", "JD", "2 hours ago", "Swap Story",
            "Just completed my first swap! Traded my old bicycle for a laptop. Amazing experience! 🚲💻 #EcoSwap",
            24, 8
        ));
        
        postList.add(new CommunityPost(
            "Sarah M", "SM", "5 hours ago", "Eco Tips",
            "Pro tip: Before buying something new, check EcoSwap first! I've saved so much money and helped the environment. 🌱",
            42, 15
        ));
        
        postList.add(new CommunityPost(
            "Mike T", "MT", "1 day ago", "Donations",
            "Donated 5 boxes of books to the local library through EcoSwap. Feels amazing to give back! 📚❤️",
            67, 23
        ));
        
        postList.add(new CommunityPost(
            "Emma L", "EL", "2 days ago", "Questions",
            "How does the swap verification process work? New to EcoSwap and loving it so far! 🤔",
            18, 31
        ));
        
        postList.add(new CommunityPost(
            "David K", "DK", "3 days ago", "Swap Story",
            "Swapped my old guitar for a gaming console. The other person was so happy! This platform is incredible! 🎸🎮",
            89, 19
        ));
        
        postAdapter.notifyDataSetChanged();
    }
    
    private void filterPosts(String category) {
        // TODO: Implement filtering logic
        Toast.makeText(getContext(), "Filtering by: " + category, Toast.LENGTH_SHORT).show();
    }
    
    private void setupFAB() {
        fabCreatePost.setOnClickListener(v -> showCreatePostDialog());
    }
    
    private void showCreatePostDialog() {
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_post);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Initialize dialog views
        Spinner spinnerCategory = dialog.findViewById(R.id.spinnerPostCategory);
        EditText etPostContent = dialog.findViewById(R.id.etPostContent);
        LinearLayout layoutAddPhoto = dialog.findViewById(R.id.layoutAddPhoto);
        Button btnCancel = dialog.findViewById(R.id.btnCancelPost);
        Button btnPublish = dialog.findViewById(R.id.btnPublishPost);
        
        // Setup category spinner
        String[] categories = {"Swap Story", "Donations", "Eco Tips", "Questions"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        
        // Add photo click listener
        layoutAddPhoto.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Photo picker coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Publish button
        btnPublish.setOnClickListener(v -> {
            String content = etPostContent.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            
            if (content.isEmpty()) {
                etPostContent.setError("Please write something");
                return;
            }
            
            // TODO: Save post to Supabase
            Toast.makeText(getContext(), "Post published! 🎉", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            
            // Add post to list (temporary, until Supabase integration)
            postList.add(0, new CommunityPost(
                "You", "YO", "Just now", category, content, 0, 0
            ));
            postAdapter.notifyItemInserted(0);
            rvCommunityPosts.smoothScrollToPosition(0);
        });
        
        dialog.show();
    }
    
    // Inner class for Community Post model
    public static class CommunityPost {
        private String userName;
        private String userAvatar;
        private String postTime;
        private String category;
        private String content;
        private int likeCount;
        private int commentCount;
        
        public CommunityPost(String userName, String userAvatar, String postTime, 
                           String category, String content, int likeCount, int commentCount) {
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.postTime = postTime;
            this.category = category;
            this.content = content;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
        }
        
        public String getUserName() { return userName; }
        public String getUserAvatar() { return userAvatar; }
        public String getPostTime() { return postTime; }
        public String getCategory() { return category; }
        public String getContent() { return content; }
        public int getLikeCount() { return likeCount; }
        public int getCommentCount() { return commentCount; }
        
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    }
}
