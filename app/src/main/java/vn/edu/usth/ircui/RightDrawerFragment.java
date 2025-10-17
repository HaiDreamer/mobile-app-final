package vn.edu.usth.ircui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RightDrawerFragment extends Fragment {

    private RecyclerView userListRecyclerView;
    private UserListAdapter userListAdapter;
    private List<String> onlineUsers;
    private String currentUsername;
    private String serverHost;
    
    // Server dropdown components
    private LinearLayout serverInfoBar;
    private LinearLayout serverDropdownList;
    private ImageView serverDropdownIcon;
    private boolean isDropdownOpen = false;
    
    // Available servers (same as ChooseServer)
    private String[] availableServers = {
        "irc.libera.chat",
        "irc.oftc.net",
        "irc.rizon.net"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_right_drawer, container, false);
        
        initializeData();
        setupUserList(view);
        setupMenuButtons(view);
        setupServerDropdown(view);
        
        return view;
    }
    
    private void initializeData() {
        // Get current user info
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
        currentUsername = prefs.getString("current_username", getString(R.string.guest));
        serverHost = prefs.getString("current_server", "irc.libera.chat");
        
        // Initialize online users list
        onlineUsers = new ArrayList<>();
        // Add some sample users with Vietnamese names for demonstration
        onlineUsers.add("Hải");
        onlineUsers.add("Hiếu");
        onlineUsers.add("Long");
        onlineUsers.add("Thành");
        onlineUsers.add("Minh");
        onlineUsers.add("Phương");
        onlineUsers.add("Giang");
        onlineUsers.add("Hương");
    }
    
    private void setupUserList(View view) {
        userListRecyclerView = view.findViewById(R.id.user_list_recycler);
        userListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        userListAdapter = new UserListAdapter(onlineUsers, selectedUser -> {
            // Show user info popup instead of direct navigation
            showUserInfoPopup(selectedUser);
        });
        
        userListRecyclerView.setAdapter(userListAdapter);
        
        // Update user count
        TextView userCountText = view.findViewById(R.id.user_count_text);
        userCountText.setText("Online Users (" + onlineUsers.size() + ")");
    }
    
    private void setupMenuButtons(View view) {
        // Settings button in header
        ImageButton settingsButtonHeader = view.findViewById(R.id.settings_button_header);
        settingsButtonHeader.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.closeRightDrawer();
                mainActivity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, SettingsFragment.newInstance())
                        .addToBackStack(null)
                        .commit();
            }
        });
        
        // Current user bar
        LinearLayout currentUserBar = view.findViewById(R.id.current_user_bar);
        currentUserBar.setOnClickListener(v -> {
            showCurrentUserInfoPopup();
        });
        
        // Close drawer button
        ImageButton closeDrawerButton = view.findViewById(R.id.close_drawer_button);
        closeDrawerButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.closeRightDrawer();
            }
        });
    }
    
    private void setupServerDropdown(View view) {
        serverInfoBar = view.findViewById(R.id.server_info_bar);
        serverDropdownList = view.findViewById(R.id.server_dropdown_list);
        serverDropdownIcon = view.findViewById(R.id.server_dropdown_icon);
        
        // Set current server name
        TextView serverNameText = view.findViewById(R.id.server_name_text);
        serverNameText.setText(serverHost);
        
        // Setup server info bar click listener
        serverInfoBar.setOnClickListener(v -> toggleServerDropdown());
        
        // Setup individual server item click listeners
        setupServerItemClickListeners(view);
    }
    
    private void setupServerItemClickListeners(View view) {
        // Server item 1
        LinearLayout serverItem1 = view.findViewById(R.id.server_item_1);
        serverItem1.setOnClickListener(v -> switchToServer(0));
        
        // Server item 2
        LinearLayout serverItem2 = view.findViewById(R.id.server_item_2);
        serverItem2.setOnClickListener(v -> switchToServer(1));
        
        // Server item 3
        LinearLayout serverItem3 = view.findViewById(R.id.server_item_3);
        serverItem3.setOnClickListener(v -> switchToServer(2));
    }
    
    private void toggleServerDropdown() {
        if (isDropdownOpen) {
            closeServerDropdown();
        } else {
            openServerDropdown();
        }
    }
    
    private void openServerDropdown() {
        serverDropdownList.setVisibility(View.VISIBLE);
        serverDropdownIcon.setRotation(180f); // Rotate arrow up
        isDropdownOpen = true;
    }
    
    private void closeServerDropdown() {
        serverDropdownList.setVisibility(View.GONE);
        serverDropdownIcon.setRotation(0f); // Rotate arrow down
        isDropdownOpen = false;
    }
    
    private void switchToServer(int serverIndex) {
        if (serverIndex >= 0 && serverIndex < availableServers.length) {
            String newServer = availableServers[serverIndex];
            
            // Update current server
            serverHost = newServer;
            
            // Save to SharedPreferences
            SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
            prefs.edit().putString("current_server", newServer).apply();
            
            // Update UI
            updateServerDisplay();
            updateServerSelection(serverIndex);
            
            // Close dropdown
            closeServerDropdown();
            
            // Show toast
            Toast.makeText(requireContext(), getString(R.string.switched_to_server, newServer), Toast.LENGTH_SHORT).show();
            
            // Notify MainActivity about server change
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // You can add a method in MainActivity to handle server switching
                // mainActivity.onServerChanged(newServer);
            }
        }
    }
    
    private void updateServerDisplay() {
        if (getView() != null) {
            TextView serverNameText = getView().findViewById(R.id.server_name_text);
            if (serverNameText != null) {
                serverNameText.setText(serverHost);
            }
        }
    }
    
    private void updateServerSelection(int selectedIndex) {
        if (getView() == null) return;
        
        // Hide all check marks
        for (int i = 0; i < availableServers.length; i++) {
            int itemId = getResources().getIdentifier("server_item_" + (i + 1), "id", requireContext().getPackageName());
            LinearLayout serverItem = getView().findViewById(itemId);
            if (serverItem != null) {
                ImageView checkIcon = (ImageView) serverItem.getChildAt(1); // Check icon is second child
                if (checkIcon != null) {
                    checkIcon.setVisibility(View.GONE);
                }
            }
        }
        
        // Show check mark for selected server
        int selectedItemId = getResources().getIdentifier("server_item_" + (selectedIndex + 1), "id", requireContext().getPackageName());
        LinearLayout selectedServerItem = getView().findViewById(selectedItemId);
        if (selectedServerItem != null) {
            ImageView checkIcon = (ImageView) selectedServerItem.getChildAt(1);
            if (checkIcon != null) {
                checkIcon.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void showUserInfoPopup(String username) {
        // Show fullscreen popup for all users
        UserInfoFullscreenDialog dialog = UserInfoFullscreenDialog.newInstance(
            username, currentUsername, serverHost, username.equals(currentUsername));
        dialog.show(getParentFragmentManager(), "user_info_fullscreen");
    }
    
    private void showCurrentUserInfoPopup() {
        // Show current user info in fullscreen popup
        UserInfoFullscreenDialog dialog = UserInfoFullscreenDialog.newInstance(
            currentUsername, currentUsername, serverHost, true);
        dialog.show(getParentFragmentManager(), "current_user_info_fullscreen");
    }
    
    public void updateUserList(List<String> users) {
        if (userListAdapter != null) {
            onlineUsers.clear();
            onlineUsers.addAll(users);
            userListAdapter.notifyDataSetChanged();
            
            // Update user count
            TextView userCountText = getView().findViewById(R.id.user_count_text);
            if (userCountText != null) {
                userCountText.setText("Online Users (" + users.size() + ")");
            }
        }
    }
    
    public void updateCurrentUserInfo(String username, String server) {
        currentUsername = username;
        serverHost = server;
        
        if (getView() != null) {
            TextView currentUserName = getView().findViewById(R.id.current_user_name);
            TextView serverNameText = getView().findViewById(R.id.server_name_text);
            
            if (currentUserName != null) {
                currentUserName.setText(username);
            }
            if (serverNameText != null) {
                serverNameText.setText(server);
            }
            
            // Update server selection in dropdown
            updateServerSelectionForCurrentServer();
        }
    }
    
    private void updateServerSelectionForCurrentServer() {
        // Find which server is currently selected
        for (int i = 0; i < availableServers.length; i++) {
            if (availableServers[i].equals(serverHost)) {
                updateServerSelection(i);
                break;
            }
        }
    }
}
