package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecoswap.R;
import com.example.ecoswap.models.User;
import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {
    
    private RecyclerView recyclerViewCommunity;
    private List<User> communityMembers;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);
        
        initViews(view);
        setupRecyclerView();
        loadCommunityMembers();
        
        return view;
    }
    
    private void initViews(View view) {
        recyclerViewCommunity = view.findViewById(R.id.recyclerViewCommunity);
        communityMembers = new ArrayList<>();
    }
    
    private void setupRecyclerView() {
        recyclerViewCommunity.setLayoutManager(new GridLayoutManager(getContext(), 2));
        // TODO: Create and set community adapter
    }
    
    private void loadCommunityMembers() {
        // TODO: Load community members from Supabase
        Toast.makeText(getContext(), "Loading community members...", Toast.LENGTH_SHORT).show();
    }
}
