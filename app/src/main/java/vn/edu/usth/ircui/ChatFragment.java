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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.feature_chat.MessageCooldownManager;
import vn.edu.usth.ircui.feature_chat.data.Message;
import vn.edu.usth.ircui.network.IrcClientManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ChatFragment extends Fragment {

    private static final String ARG_USERNAME = "username";

    public static ChatFragment newInstance(String username) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USERNAME, username);
        f.setArguments(b);
        return f;
    }

    private String username = "Guest";
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;
    private IrcClientManager ircClient;
    private final MessageCooldownManager cooldownManager = MessageCooldownManager.getInstance();

    private RecyclerView rvMessages;
    private EditText etMessage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME, "Guest");
        }

        // Add welcome message
        messages.add(new Message("System", "Welcome to IRC Chat, " + username + "!", false));
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
        FloatingActionButton fabDm = v.findViewById(R.id.fab);

        adapter = new MessageAdapter(messages, username);
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMessages.setAdapter(adapter);

        // Initialize IRC client but don't connect immediately for testing
        ircClient = new IrcClientManager();
        ircClient.setCallback(new IrcClientManager.MessageCallback() {
            @Override
            public void onMessage(String u, String t, long ts, boolean mine) {
                requireActivity().runOnUiThread(() -> {
                    messages.add(new Message(u, t, mine));
                    adapter.notifyItemInserted(messages.size() - 1);
                    rvMessages.scrollToPosition(messages.size() - 1);
                });
            }

            @Override
            public void onSystem(String t) {
                requireActivity().runOnUiThread(() -> {
                    displaySystemMessage(t);
                });
            }
        });

        btnSend.setOnClickListener(view -> handleSendMessageClick());

        // FIXED: Simplified DM functionality for now
        fabDm.setOnClickListener(view -> {
            Toast.makeText(requireContext(), "Direct Message feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Add test message to verify chat is working
        displaySystemMessage("Chat loaded successfully. You can start messaging!");

        return v;
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

        if (cooldownManager.isCooldownActive()) {
            Toast.makeText(getContext(),
                    "You are sending messages too quickly!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // For testing: just display the message locally without IRC
        messages.add(new Message(username, text, true));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);

        // Simulate response
        requireActivity().runOnUiThread(() -> {
            try {
                Thread.sleep(1000);
                messages.add(new Message("Bot", "Echo: " + text, false));
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
            } catch (Exception e) {
                // Ignore
            }
        });

        cooldownManager.recordMessageSent();
        etMessage.getText().clear();
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
                    displaySystemMessage("Nickname changed from " + username + " to " + newNick);
                    username = newNick;
                    adapter = new MessageAdapter(messages, username);
                    rvMessages.setAdapter(adapter);
                } else {
                    displaySystemMessage("Usage: /nick <new_nickname>");
                }
                break;
            case "/connect":
                displaySystemMessage("Attempting to connect to IRC server...");
                try {
                    ircClient.connect(username, "#usth-ircui");
                    displaySystemMessage("Connected to IRC server!");
                } catch (Exception e) {
                    displaySystemMessage("Connection failed: " + e.getMessage());
                }
                break;
            default:
                displaySystemMessage("Unknown command: " + command);
                break;
        }
    }

    private void showHelpInfo() {
        String helpMessage = "--- User Guide ---\n"
                + "/help or /general - Shows this guide.\n"
                + "/nick <new_name> - Changes your nickname.\n"
                + "/connect - Connect to IRC server\n"
                + "--------------------";
        displaySystemMessage(helpMessage);
    }

    private void displaySystemMessage(String text) {
        requireActivity().runOnUiThread(() -> {
            messages.add(new Message("System", text, false));
            adapter.notifyItemInserted(messages.size() - 1);
            rvMessages.scrollToPosition(messages.size() - 1);
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
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