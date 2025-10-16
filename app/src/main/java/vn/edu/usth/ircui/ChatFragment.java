package vn.edu.usth.ircui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.feature_chat.data.Message;
import vn.edu.usth.ircui.feature_chat.ui.DirectMessageFragment;
import vn.edu.usth.ircui.feature_user.MessageCooldownManager;
import vn.edu.usth.ircui.network.IrcClientManager;
import vn.edu.usth.ircui.network.SharedIrcClient;

/**
 * ChatFragment
 * ------------
 * Handles main chat functionality:
 *  - Display messages
 *  - Send messages to server
 *  - Handle guest/registered users
 *  - Support /commands (help, nick, connect)
 */
public class ChatFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_SERVER   = "server";
    private static final String ARG_CHANNEL  = "channel";

    // Factory method to create a new instance with username
    public static ChatFragment newInstance(String username) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USERNAME, username);
        f.setArguments(b);
        return f;
    }

    public static ChatFragment newInstance(String username, String server, String channel) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USERNAME, username);
        b.putString(ARG_SERVER, server);
        b.putString(ARG_CHANNEL, channel);
        f.setArguments(b);
        return f;
    }

    private String username = "Guest";
    private String currentUsername;
    private String currentNickname = "Guest";
    private String serverHost = "irc.libera.chat";
    private String channel = "#usth-ircui";
    private FirebaseFirestore db;
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private SharedIrcClient sharedIrcClient;
    private SharedIrcClient.MessageCallback messageCallback;
    private SharedIrcClient.SystemMessageCallback systemCallback;
    private MessageCooldownManager cooldownManager;

    private RecyclerView rvMessages;
    private EditText etMessage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        db = FirebaseFirestore.getInstance();
        cooldownManager = MessageCooldownManager.getInstance();

        // Retrieve username passed from MainActivity
        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME, "Guest");
            currentUsername = getArguments().getString(ARG_USERNAME, "Guest");
            serverHost = getArguments().getString(ARG_SERVER, "irc.libera.chat");
            channel = getArguments().getString(ARG_CHANNEL, "#usth-ircui");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister callbacks when fragment is destroyed
        if (sharedIrcClient != null) {
            if (messageCallback != null) {
                sharedIrcClient.unregisterCallback(messageCallback);
            }
            if (systemCallback != null) {
                sharedIrcClient.unregisterSystemCallback(systemCallback);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        rvMessages = v.findViewById(R.id.rvMessages);
        etMessage = v.findViewById(R.id.etMessage);
        ImageButton btnSend = v.findViewById(R.id.btnSend);
        FloatingActionButton fabDm = v.findViewById(R.id.fab);

        // Setup adapter with nickname
        adapter = new MessageAdapter(messages, currentNickname);
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMessages.setAdapter(adapter);

        initializeSharedIrcClient();

        btnSend.setOnClickListener(view -> handleSendMessageClick());
        
        // Open Direct Message via FAB
        fabDm.setOnClickListener(view -> openDirectMessageDialog());
        
        // Load nickname info from Firestore or Guest welcome
        fetchUserData();

        return v;
    }

    // =============================
    // 🔹 Direct Message dialog
    // =============================
    private void openDirectMessageDialog() {
        final EditText inputUser = new EditText(requireContext());
        inputUser.setHint("Enter username (e.g., bob)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Direct Message")
                .setView(inputUser)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Open", (d, w) -> {
                    String peer = inputUser.getText() != null
                            ? inputUser.getText().toString().trim()
                            : "";
                    if (!peer.isEmpty()) {
                        String me = currentUsername;
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container,
                                        DirectMessageFragment.newInstance(me, peer, serverHost))
                                .addToBackStack(null)
                                .commit();
                    }
                })
                .show();
    }

    private void initializeSharedIrcClient() {
        sharedIrcClient = SharedIrcClient.getInstance();
        
        // Create callback for regular messages
        messageCallback = new SharedIrcClient.MessageCallback() {
            @Override
            public void onMessage(String u, String t, long ts, boolean mine) {
                messages.add(new Message(u, t, mine));
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
            }
        };
        
        // Create callback for system messages
        systemCallback = new SharedIrcClient.SystemMessageCallback() {
            @Override
            public void onSystem(String t) {
                // Show all system messages with server info
                displaySystemMessage(t);
            }
        };
        
        // Register this fragment as a callback for both messages and system messages
        sharedIrcClient.registerCallback(messageCallback);
        sharedIrcClient.registerSystemCallback(systemCallback);

        // Connect to IRC server using shared client
        sharedIrcClient.connect(serverHost, username, channel, requireContext());
    }

    // =============================
    // 🔹 Load nickname info
    // =============================
    private void fetchUserData() {
        // Guests are detected by "Guest" prefix
        if (currentUsername == null
                || currentUsername.equals("Guest")
                || currentUsername.startsWith("Guest")) {

            currentNickname = "Guest";
            displaySystemMessage("Welcome to IRC Chat, " + currentNickname + "!");
            displaySystemMessage("Chat loaded successfully. You can start messaging!");
            adapter.setCurrentUser(currentNickname);
            return;
        }

        // Registered users: fetch nickname from Firestore
        db.collection("Users").document(currentUsername).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentNickname = documentSnapshot.getString("nickname");
                    } else {
                        Log.d("Firestore", "User not found: " + currentUsername);
                        currentNickname = currentUsername;
                    }

                    displaySystemMessage("Welcome to IRC Chat, " + currentNickname + "!");
                    displaySystemMessage("Chat loaded successfully. You can start messaging!");
                    adapter.setCurrentUser(currentNickname);
                });
    }

    // =============================
    // 🔹 Handle Send button
    // =============================
    private void handleSendMessageClick() {
        String text = etMessage.getText() != null
                ? etMessage.getText().toString().trim()
                : "";
        if (TextUtils.isEmpty(text)) return;

        // Handle IRC commands
        if (isCommand(text)) {
            handleCommand(text);
            etMessage.getText().clear();
            return;
        }

        // Prevent spamming
        if (cooldownManager.isCooldownActive()) {
            Toast.makeText(getContext(), "You are sending messages too quickly!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if IRC client is connected before sending
        if (sharedIrcClient == null) {
            displaySystemMessage("❌ IRC client not initialized. Try reconnecting.");
            return;
        }

        if (!sharedIrcClient.isConnected()) {
            displaySystemMessage("❌ Not connected to IRC server. Use /reconnect to try again.");
            return;
        }

        try {
            // Add message to UI immediately (local echo)
            messages.add(new Message(username, text, true));
            adapter.notifyItemInserted(messages.size() - 1);
            rvMessages.scrollToPosition(messages.size() - 1);
            
            // Send to IRC server
            sharedIrcClient.sendMessage(text);
            etMessage.getText().clear();
        } catch (Exception e) {
            // Remove the message from UI if send failed
            if (!messages.isEmpty()) {
                messages.remove(messages.size() - 1);
                adapter.notifyItemRemoved(messages.size());
            }
            displaySystemMessage("❌ Send failed: " + e.getMessage());
        }
    }

    // =============================
    // 🔹 Commands (/nick, /help, etc.)
    // =============================
    private boolean isCommand(String text) {
        return text.startsWith("/");
    }

    private void handleCommand(String commandText) {
        String[] parts = commandText.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/help":
            case "/general":
                showHelpInfo();
                break;
            case "/nick":
                if (parts.length > 1) {
                    String newNick = parts[1];
                    username = newNick;
                    currentNickname = newNick;
                    adapter = new MessageAdapter(messages, username);
                    rvMessages.setAdapter(adapter);
                    displaySystemMessage("✅ Nickname changed to: " + newNick);
                } else {
                    displaySystemMessage("❌ Usage: /nick <new_nickname>");
                }
                break;
            case "/connect":
                try {
                    sharedIrcClient.connect(serverHost, username, "#usth-ircui", requireContext());
                    displaySystemMessage("🔄 Attempting to connect...");
                } catch (Exception e) {
                    displaySystemMessage("❌ Connection failed: " + e.getMessage());
                }
                break;
            case "/status":
                showConnectionStatus();
                break;
            case "/reconnect":
                try {
                    if (sharedIrcClient != null) {
                        sharedIrcClient.resetConnection();
                        sharedIrcClient.connect(serverHost, username, channel, requireContext());
                        displaySystemMessage("🔄 Reconnecting...");
                    } else {
                        displaySystemMessage("❌ IRC client not initialized");
                    }
                } catch (Exception e) {
                    displaySystemMessage("❌ Reconnect failed: " + e.getMessage());
                }
                break;
            case "/who":
                displaySystemMessage("👤 Current user: " + username);
                displaySystemMessage("🌐 Server: " + serverHost);
                displaySystemMessage("📺 Channel: " + channel);
                break;
            default:
                displaySystemMessage("❌ Unknown command: " + command);
                break;
        }
    }

    // =============================
    // 🔹 Helper methods
    // =============================
    private void showHelpInfo() {
        displaySystemMessage("📋 Available commands:");
        displaySystemMessage("  /help - Show this help");
        displaySystemMessage("  /status - Check connection status");
        displaySystemMessage("  /reconnect - Reconnect to server");
        displaySystemMessage("  /nick <name> - Change nickname");
        displaySystemMessage("  /connect - Connect to server");
        displaySystemMessage("  /who - Show user info");
    }

    private void showConnectionStatus() {
        if (sharedIrcClient == null) {
            displaySystemMessage("❌ IRC client not initialized");
            return;
        }
        
        if (sharedIrcClient.isConnected()) {
            displaySystemMessage("✅ Connected to IRC server");
            displaySystemMessage("👤 User: " + username);
            displaySystemMessage("📺 Channel: " + channel);
        } else {
            displaySystemMessage("❌ Not connected to IRC server");
            displaySystemMessage("💡 Try: /reconnect");
        }
    }

    private void displaySystemMessage(String text) {
        messages.add(new Message("System", text, false));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    // =============================
    // 🔹 Options menu
    // =============================
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_direct_message) {
            openDirectMessageDialog();
            return true;
        } else if (id == R.id.action_settings) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, SettingsFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
            return true;
        } else if (id == R.id.action_members) {
            fetchOnlineUsers();
            return true;
        } else if (id == R.id.action_refresh) {
            // Refresh connection
            if (sharedIrcClient != null) {
                displaySystemMessage("Refreshing connection...");
                // Add refresh logic here if needed
            }
            return true;
        } else if (id == R.id.action_clear_history) {
            clearChatHistory();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // =============================
    // 🔹 Menu action methods
    // =============================
    private void fetchOnlineUsers() {
        // For now, just show a placeholder message
        displaySystemMessage("Fetching online users...");
        Toast.makeText(getContext(), "Online users feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    private void clearChatHistory() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Chat History")
                .setMessage("Are you sure you want to clear all chat history?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    messages.clear();
                    adapter.notifyDataSetChanged();
                    displaySystemMessage("Chat history cleared.");
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("About IRC UI")
                .setMessage("IRC UI v1.0\nDeveloped for USTH\nA modern IRC client for Android")
                .setPositiveButton("OK", null)
                .show();
    }
}
