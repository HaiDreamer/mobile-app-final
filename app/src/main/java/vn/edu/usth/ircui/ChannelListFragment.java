package vn.edu.usth.ircui;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class ChannelListFragment extends Fragment {

    private ListView channelListView;
    private ArrayAdapter<String> channelAdapter;
    private List<String> channels;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_list, container, false);
        
        // Initialize channels list
        initializeChannels();
        
        // Setup ListView
        channelListView = view.findViewById(R.id.channel_list_view);
        channelAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, channels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                // Use theme-aware text color for better contrast
                textView.setTextColor(getResources().getColor(R.color.text_primary, requireContext().getTheme()));
                textView.setTextSize(16);
                textView.setPadding(32, 24, 32, 24);
                return view;
            }
        };
        channelListView.setAdapter(channelAdapter);
        
        // Handle add channel button
        ImageButton btnAddChannel = view.findViewById(R.id.btn_add_channel);
        btnAddChannel.setOnClickListener(v -> showAddChannelDialog());
        
        // Handle channel selection
        channelListView.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedChannel = channels.get(position);
            
            // Navigate to selected channel using the same method as direct chat
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                
                // Get current username and server from current chat session
                String username = getCurrentUsername();
                String serverHost = getCurrentServer();
                
                // Show feedback to user
                Toast.makeText(requireContext(), 
                        getString(R.string.switching_to_channel, selectedChannel), 
                        Toast.LENGTH_SHORT).show();
                
                // Navigate to channel message fragment with selected channel
                mainActivity.navigateToChannelMessageFragment(username, serverHost, selectedChannel);
                
                // Close the drawer
                mainActivity.closeDrawer();
            }
        });
        
        return view;
    }
    
    private void initializeChannels() {
        channels = new ArrayList<>();
        // Popular IRC channels where people actually chat
        channels.add("#general");
        channels.add("#usth-ircui");
        channels.add("#random");
        channels.add("#help");
        channels.add("#chat");
        channels.add("#lobby");
        channels.add("#main");
        channels.add("#welcome");
        channels.add("#newbies");
        channels.add("#support");
        channels.add("#tech");
        channels.add("#programming");
        channels.add("#android");
        channels.add("#java");
        channels.add("#linux");
    }
    
    public void addChannel(String channelName) {
        if (!channels.contains(channelName)) {
            channels.add(channelName);
            channelAdapter.notifyDataSetChanged();
        }
    }
    
    public void removeChannel(String channelName) {
        channels.remove(channelName);
        channelAdapter.notifyDataSetChanged();
    }
    
    public void setCurrentChannel(String channelName) {
        // This method can be called to highlight the current channel
        // For now, we'll just ensure the channel is in the list
        if (!channels.contains(channelName)) {
            addChannel(channelName);
        }
    }
    
    private String getCurrentUsername() {
        // Get username from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
        String savedUsername = prefs.getString("current_username", "Guest");
        
        return savedUsername;
    }
    
    private String getCurrentServer() {
        // Try to get server from SharedPreferences first
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
        String savedServer = prefs.getString("current_server", null);
        
        if (savedServer != null && !savedServer.isEmpty()) {
            return savedServer;
        }
        
        // Default server fallback
        return "irc.libera.chat";
    }
    
    private void showAddChannelDialog() {
        final EditText inputChannel = new EditText(requireContext());
        inputChannel.setHint(getString(R.string.add_channel_hint));

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.add_channel_dialog_title))
                .setView(inputChannel)
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton(getString(R.string.add_channel), (dialog, which) -> {
                    String channelName = inputChannel.getText() != null
                            ? inputChannel.getText().toString().trim()
                            : "";
                    
                    if (!TextUtils.isEmpty(channelName)) {
                        // Ensure channel name starts with #
                        if (!channelName.startsWith("#")) {
                            channelName = "#" + channelName;
                        }
                        
                        // Add channel to list
                        addChannel(channelName);
                        Toast.makeText(requireContext(), 
                                getString(R.string.channel_added, channelName), 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), 
                                getString(R.string.enter_channel_name), 
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}