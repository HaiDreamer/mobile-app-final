package vn.edu.usth.ircui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
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


import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.feature_chat.data.Message;
import vn.edu.usth.ircui.network.IrcClientManager;
import vn.edu.usth.ircui.feature_chat.ui.DirectMessageFragment;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ChatFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_SERVER   = "server";
    private static final String ARG_CHANNEL  = "channel";

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
    private String serverHost = "irc.libera.chat";
    private String channel = "#usth-ircui";
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private IrcClientManager ircClient;

    private RecyclerView rvMessages;
    private EditText etMessage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME, "Guest");
            serverHost = getArguments().getString(ARG_SERVER, "irc.libera.chat");
            channel = getArguments().getString(ARG_CHANNEL, "#usth-ircui");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        rvMessages = v.findViewById(R.id.rvMessages);
        etMessage  = v.findViewById(R.id.etMessage);
        ImageButton btnSend = v.findViewById(R.id.btnSend);

        adapter = new MessageAdapter(messages, username);
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMessages.setAdapter(adapter);

        ircClient = getIrcClientManager();

        btnSend.setOnClickListener(view -> handleSendMessageClick());

        return v;
    }

    private void openDirectMessageDialog() {
        final EditText inputUser = new EditText(requireContext());
        inputUser.setHint("Nhập username (ví dụ: bob)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Direct message")
                .setView(inputUser)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Mở", (d, w) -> {
                    String peer = inputUser.getText() != null
                            ? inputUser.getText().toString().trim()
                            : "";
                    if (!peer.isEmpty()) {
                        String me = username;
                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container,
                                        DirectMessageFragment.newInstance(me, peer))
                                .addToBackStack(null)
                                .commit();
                    }
                })
                .show();
    }

    @NonNull
    private IrcClientManager getIrcClientManager() {
        IrcClientManager manager = new IrcClientManager();
        manager.setContext(requireContext());
        manager.setCallback(new IrcClientManager.MessageCallback() {
            @Override
            public void onMessage(String u, String t, long ts, boolean mine) {
                messages.add(new Message(u, t, mine));
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onSystem(String t) {
                // Show all system messages with server info
                displaySystemMessage(t);
            }
        });

        // Configure server list: prefer chosen server first, then fallbacks
        java.util.List<vn.edu.usth.ircui.network.IrcClientManager.Server> list = new java.util.ArrayList<>();
        list.add(new vn.edu.usth.ircui.network.IrcClientManager.Server(serverHost, 6697, true));
        list.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.libera.chat", 6697, true));
        list.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.oftc.net", 6697, true));
        list.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.rizon.net", 6697, true));
        list.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.freenode.net", 6697, true));
        manager.setServers(list);
        manager.connect(username, channel);
        
        // Show minimal connection info
        displaySystemMessage("🔄 Connecting to " + serverHost + "...");
        return manager;
    }

    private void handleSendMessageClick() {
        String text = etMessage.getText() != null
                ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        if (isCommand(text)) {
            handleCommand(text);
            etMessage.getText().clear();
            return;
        }


        // Check if IRC client is connected before sending
        if (ircClient == null) {
            displaySystemMessage("❌ IRC client not initialized. Try reconnecting.");
            return;
        }

        if (!ircClient.isConnected()) {
            displaySystemMessage("❌ Not connected to IRC server. Use /reconnect to try again.");
            return;
        }

        try {
            // Add message to UI immediately (local echo)
            messages.add(new Message(username, text, true));
            adapter.notifyItemInserted(messages.size() - 1);
            rvMessages.scrollToPosition(messages.size() - 1);
            
            // Send to IRC server
            ircClient.sendMessage(text);
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
                    adapter = new MessageAdapter(messages, username);
                    rvMessages.setAdapter(adapter);
                    displaySystemMessage("✅ Nickname changed to: " + newNick);
                } else {
                    displaySystemMessage("❌ Usage: /nick <new_nickname>");
                }
                break;
            case "/connect":
                try {
                    ircClient.connect(username, "#usth-ircui");
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
                    if (ircClient != null) {
                        ircClient.resetConnection();
                        ircClient.connect(username, channel);
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
        if (ircClient == null) {
            displaySystemMessage("❌ IRC client not initialized");
            return;
        }
        
        if (ircClient.isActive()) {
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
        }
        return super.onOptionsItemSelected(item);
    }
}
