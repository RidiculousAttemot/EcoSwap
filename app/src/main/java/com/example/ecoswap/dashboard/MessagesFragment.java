package com.example.ecoswap.dashboard;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.ecoswap.R;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {
    
    private RecyclerView rvMessages;
    private MessagesAdapter adapter;
    private EditText etSearchMessages;
    private ImageView btnClearSearch, btnFilter;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private List<MessagesAdapter.Message> messagesList;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        
        // Initialize views
        rvMessages = view.findViewById(R.id.rvMessages);
        etSearchMessages = view.findViewById(R.id.etSearchMessages);
        btnClearSearch = view.findViewById(R.id.btnClearSearch);
        btnFilter = view.findViewById(R.id.btnFilter);
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Setup search
        setupSearch();
        
        // Setup tabs
        setupTabs();
        
        // Setup swipe refresh
        setupSwipeRefresh();
        
        // Setup filter button
        btnFilter.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Filter options coming soon", Toast.LENGTH_SHORT).show();
        });
        
        return view;
    }
    
    private void setupRecyclerView() {
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Sample data
        messagesList = new ArrayList<>();
        messagesList.add(new MessagesAdapter.Message(
            "Sarah Martinez",
            "Hey! Is the vintage desk lamp still available?",
            "2m ago",
            3,
            true,
            "Vintage Desk Lamp"
        ));
        messagesList.add(new MessagesAdapter.Message(
            "Mike Thompson",
            "Thanks for the swap! The jacket is perfect 👍",
            "1h ago",
            0,
            true,
            "Leather Jacket"
        ));
        messagesList.add(new MessagesAdapter.Message(
            "Emma Wilson",
            "I'd love to donate some books too. Where should I drop them?",
            "3h ago",
            1,
            false,
            null
        ));
        messagesList.add(new MessagesAdapter.Message(
            "David Kim",
            "Can we meet at the community center tomorrow?",
            "5h ago",
            0,
            false,
            null
        ));
        messagesList.add(new MessagesAdapter.Message(
            "Lisa Anderson",
            "Just saw your post! I have similar items to swap 🌱",
            "1d ago",
            0,
            false,
            null
        ));
        messagesList.add(new MessagesAdapter.Message(
            "James Brown",
            "The Harry Potter collection - is it the complete series?",
            "2d ago",
            0,
            false,
            "Harry Potter Collection"
        ));
        messagesList.add(new MessagesAdapter.Message(
            "Jennifer Lee",
            "Great doing business with you! 5 stars ⭐",
            "3d ago",
            0,
            false,
            null
        ));
        
        adapter = new MessagesAdapter(requireContext(), messagesList, message -> {
            // Handle message click - navigate to chat detail
            Toast.makeText(requireContext(), "Opening chat with " + message.getUserName(), Toast.LENGTH_SHORT).show();
            // TODO: Navigate to ChatActivity with message details
        });
        
        rvMessages.setAdapter(adapter);
    }
    
    private void setupSearch() {
        etSearchMessages.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                adapter.filter(query);
                
                // Show/hide clear button
                if (query.isEmpty()) {
                    btnClearSearch.setVisibility(View.GONE);
                } else {
                    btnClearSearch.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        btnClearSearch.setOnClickListener(v -> {
            etSearchMessages.setText("");
            btnClearSearch.setVisibility(View.GONE);
        });
    }
    
    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                adapter.filterByTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary_green);
        swipeRefresh.setOnRefreshListener(() -> {
            // TODO: Refresh messages from server
            swipeRefresh.postDelayed(() -> {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Messages refreshed", Toast.LENGTH_SHORT).show();
            }, 1000);
        });
    }
}
