package vn.edu.usth.ircui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import vn.edu.usth.ircui.feature_chat.data.Attachment;
import vn.edu.usth.ircui.feature_chat.ui.DirectMessageAdapter;
import vn.edu.usth.ircui.network.IrcClientManager;
import vn.edu.usth.ircui.feature_user.MessageCooldownManager;

/**
 * Channel message fragment for IRC channel chat
 * Similar to DirectMessageFragment but for channel communication
 */
public class ChannelMessageFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_SERVER = "server";
    private static final String ARG_CHANNEL = "channel";

    public static ChannelMessageFragment newInstance(String username, String server, String channel) {
        Bundle b = new Bundle();
        b.putString(ARG_USERNAME, username);
        b.putString(ARG_SERVER, server);
        b.putString(ARG_CHANNEL, channel);
        ChannelMessageFragment f = new ChannelMessageFragment();
        f.setArguments(b);
        return f;
    }

    private RecyclerView recycler;
    private DirectMessageAdapter adapter;
    private EditText input;
    private ImageButton btnSend, btnAttach, btnImage, btnBack;
    private TextView header;

    private String username, serverHost, channel;
    private IrcClientManager ircClient;
    private final MessageCooldownManager cooldownManager = MessageCooldownManager.getInstance();

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris == null || uris.isEmpty()) return;
                for (Uri uri : uris) addAttachmentMessage(Attachment.Type.FILE, uri);
            });

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) addAttachmentMessage(Attachment.Type.IMAGE, uri);
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure the window resizes when the IME shows.
        requireActivity().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

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
        View v = inflater.inflate(R.layout.fragment_channel_message, container, false);

        recycler = v.findViewById(R.id.Recycler);
        input = v.findViewById(R.id.Input);
        btnSend = v.findViewById(R.id.BtnSend);
        btnAttach = v.findViewById(R.id.BtnAttach);
        btnImage = v.findViewById(R.id.BtnImage);
        btnBack = v.findViewById(R.id.BtnBack);
        header = v.findViewById(R.id.Header);
        
        // Set channel name in header
        header.setText(channel);
        
        // Handle back button
        btnBack.setOnClickListener(view -> {
            // Navigate back to main chat fragment
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                String currentUsername = getCurrentUsername();
                String currentServer = getCurrentServer();
                mainActivity.navigateToChatFragment(currentUsername, currentServer, "#usth-ircui");
            }
        });

        adapter = new DirectMessageAdapter(username);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        // Initialize IRC client
        initializeIrcClient();

        btnSend.setOnClickListener(view -> sendText());
        btnImage.setOnClickListener(view -> imagePicker.launch("image/*"));
        btnAttach.setOnClickListener(view -> filePicker.launch(new String[]{"*/*"}));

        input.setOnEditorActionListener((tv, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendText();
                return true;
            }
            return false;
        });

        // No need for channel join notification since we already show server connection status

        // --- Keyboard / Insets handling ---
        final View footer = (View) input.getParent(); // the bottom bar container

        // 1) Apply system bar paddings once and keep bottom space equal to nav bar.
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Keep footer above navigation bar (gesture area)
            footer.setPadding(
                    footer.getPaddingLeft(),
                    footer.getPaddingTop(),
                    footer.getPaddingRight(),
                    sys.bottom
            );
            // Make list respect status/nav bars
            recycler.setPadding(
                    recycler.getPaddingLeft(),
                    sys.top,
                    recycler.getPaddingRight(),
                    sys.bottom
            );
            return insets; // don't consume; let resize work
        });

        // 2) Animate footer with the IME and add extra space for the list bottom.
        ViewCompat.setWindowInsetsAnimationCallback(v,
                new WindowInsetsAnimationCompat.Callback(
                        WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    @NonNull
                    @Override
                    public WindowInsetsCompat onProgress(@NonNull WindowInsetsCompat insets,
                                                         @NonNull java.util.List<WindowInsetsAnimationCompat> runningAnimations) {
                        Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                        Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                        // Move footer up by the IME height
                        footer.setTranslationY(-ime.bottom);

                        // Ensure last message stays above the keyboard
                        int extraBottom = sys.bottom + ime.bottom + dp(8);
                        recycler.setPadding(
                                recycler.getPaddingLeft(),
                                recycler.getPaddingTop(),
                                recycler.getPaddingRight(),
                                extraBottom
                        );
                        recycler.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                        return insets;
                    }
                });

        return v;
    }

    private void initializeIrcClient() {
        try {
            // Check if we already have a connected IRC client
            if (ircClient != null && ircClient.isConnected()) {
                // Just join the new channel without reconnecting
                adapter.addText(false, "System", "📺 Joining channel: " + channel);
                ircClient.joinChannel(channel);
                return;
            }
            
            // Create new IRC client only if not connected
            ircClient = new IrcClientManager();
            ircClient.setContext(requireContext());
            ircClient.setCallback(new IrcClientManager.MessageCallback() {
                @Override
                public void onMessage(String user, String text, long timestamp, boolean isMine) {
                    // Add all messages from IRC server (real-time chat)
                    adapter.addText(isMine, user, text);
                    recycler.scrollToPosition(adapter.getItemCount() - 1);
                }

                @Override
                public void onSystem(String text) {
                    // Show all system messages with server info
                    adapter.addText(false, "System", text);
                    recycler.scrollToPosition(adapter.getItemCount() - 1);
                }
            });

            // Configure server list with popular IRC servers
            java.util.List<vn.edu.usth.ircui.network.IrcClientManager.Server> servers = new java.util.ArrayList<>();
            servers.add(new vn.edu.usth.ircui.network.IrcClientManager.Server(serverHost, 6697, true));
            servers.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.libera.chat", 6697, true));
            servers.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.oftc.net", 6697, true));
            servers.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.rizon.net", 6697, true));
            servers.add(new vn.edu.usth.ircui.network.IrcClientManager.Server("irc.freenode.net", 6697, true));
            
            ircClient.setServers(servers);
            ircClient.connect(username, channel);
            
            // Show minimal connection status
            adapter.addText(false, "System", "🔄 Connecting to " + serverHost + "...");
        } catch (Exception e) {
            adapter.addText(false, "System", "❌ Failed to initialize IRC client to " + serverHost + ": " + e.getMessage());
        }
    }

    private void sendText() {
        String text = input.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (isCommand(text)) {
            handleCommand(text);
            input.setText("");
            return;
        }

        if (cooldownManager.isCooldownActive()) {
            Toast.makeText(getContext(),
                    "You are sending messages too quickly!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if IRC client is connected before sending
        if (ircClient == null) {
            adapter.addText(false, "System", "❌ IRC client not initialized. Try reconnecting.");
            return;
        }

        if (!ircClient.isActive()) {
            adapter.addText(false, "System", "❌ Not connected to IRC server. Use /reconnect to try again.");
            return;
        }

        // Send message via IRC and show it immediately in UI
        try {
            // Add message to UI immediately (local echo)
            adapter.addText(true, username, text);
            recycler.scrollToPosition(adapter.getItemCount() - 1);
            
            // Send to IRC server
            ircClient.sendMessage(text);
            cooldownManager.recordMessageSent();
            input.setText("");
        } catch (Exception e) {
            // Remove the message from UI if send failed
            if (adapter.getItemCount() > 0) {
                adapter.removeLastMessage();
            }
            adapter.addText(false, "System", "❌ Send failed: " + e.getMessage());
            // If send fails, we might be disconnected
            if (e.getMessage() != null && 
                (e.getMessage().contains("disconnected") || 
                 e.getMessage().contains("connection"))) {
                adapter.addText(false, "System", "💡 Try reconnecting with /reconnect");
            }
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
                showHelpInfo();
                break;
            case "/nick":
                if (parts.length > 1) {
                    String newNick = parts[1];
                    username = newNick;
                    adapter = new DirectMessageAdapter(username);
                    recycler.setAdapter(adapter);
                }
                break;
            case "/join":
                if (parts.length > 1) {
                    String newChannel = parts[1];
                    if (!newChannel.startsWith("#")) {
                        newChannel = "#" + newChannel;
                    }
                    
                    if (ircClient != null && ircClient.isConnected()) {
                        // Leave current channel and join new one
                        ircClient.partChannel(channel);
                        channel = newChannel;
                        ircClient.joinChannel(newChannel);
                        adapter.addText(false, "System", "📺 Switched to channel: " + newChannel);
                    } else {
                        adapter.addText(false, "System", "❌ Cannot join channel: Not connected to server");
                    }
                } else {
                    adapter.addText(false, "System", "❌ Usage: /join <channel>");
                }
                break;
            case "/part":
                if (ircClient != null && ircClient.isConnected()) {
                    ircClient.partChannel(channel);
                    adapter.addText(false, "System", "👋 Left channel: " + channel);
                    // Navigate back to channel list or main chat
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.navigateToChatFragment(username, serverHost, "#usth-ircui");
                    }
                } else {
                    adapter.addText(false, "System", "❌ Cannot leave channel: Not connected to server");
                }
                break;
            case "/status":
                showConnectionStatus();
                break;
            case "/who":
                adapter.addText(false, "System", "You are: " + username + " in " + channel);
                break;
            case "/reconnect":
                if (ircClient != null) {
                    ircClient.resetConnection();
                    ircClient.connect(username, channel);
                }
                break;
            default:
                adapter.addText(false, "System", "Unknown command: " + command);
                break;
        }
    }

    private void showHelpInfo() {
        adapter.addText(false, "System", "📋 Available commands:");
        adapter.addText(false, "System", "  /help - Show this help");
        adapter.addText(false, "System", "  /status - Check connection status");
        adapter.addText(false, "System", "  /reconnect - Reconnect to server");
        adapter.addText(false, "System", "  /nick <name> - Change nickname");
        adapter.addText(false, "System", "  /who - Show user info");
        adapter.addText(false, "System", "  /join <channel> - Join channel");
        adapter.addText(false, "System", "  /part - Leave current channel");
    }

    private void addAttachmentMessage(Attachment.Type type, Uri uri) {
        String name = queryDisplayName(requireContext(), uri);
        long size = querySize(requireContext(), uri);

        // TODO: hook your file/image send here (upload or send-by-URI)
        adapter.addAttachment(type, true, username, uri, name, size);
        recycler.scrollToPosition(adapter.getItemCount() - 1);
    }

    private static String queryDisplayName(Context ctx, Uri uri) {
        try (android.database.Cursor c = ctx.getContentResolver()
                .query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return "attachment";
    }

    private static long querySize(Context ctx, Uri uri) {
        try (android.database.Cursor c = ctx.getContentResolver()
                .query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getLong(0);
        } catch (Exception ignored) {}
        return -1L;
    }

    // Small helper
    private int dp(int v) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }
    
    private String getCurrentUsername() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
        return prefs.getString("current_username", username);
    }
    
    private String getCurrentServer() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
        return prefs.getString("current_server", serverHost);
    }
    
    public boolean isConnected() {
        return ircClient != null && ircClient.isActive();
    }
    
    public void showConnectionStatus() {
        if (ircClient == null) {
            adapter.addText(false, "System", "❌ IRC client not initialized");
            return;
        }
        
        if (isConnected()) {
            adapter.addText(false, "System", "✅ Connected to IRC server");
            adapter.addText(false, "System", "👤 User: " + username);
            adapter.addText(false, "System", "📺 Channel: " + channel);
            adapter.addText(false, "System", "🌐 Server: " + serverHost);
        } else {
            adapter.addText(false, "System", "❌ Not connected to IRC server");
            adapter.addText(false, "System", "💡 Try: /reconnect");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ircClient != null) {
            ircClient.disconnect();
        }
    }
}
