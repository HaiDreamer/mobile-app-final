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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.feature_user.MessageCooldownManager;
import vn.edu.usth.ircui.feature_chat.data.Message;
import vn.edu.usth.ircui.network.IrcClientManager;
import vn.edu.usth.ircui.feature_chat.ui.DirectMessageFragment;

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

    public static ChatFragment newInstance(String username) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USERNAME, username);
        f.setArguments(b);
        return f;
    }

    private String currentUsername;
    private String currentNickname = "Guest";
    private FirebaseFirestore db;

    private final List<Message> messages = new ArrayList<>();
    private final List<String> currentUsers = new ArrayList<>();
    private MessageAdapter adapter;
    private IrcClientManager ircClient;
    private final MessageCooldownManager cooldownManager = MessageCooldownManager.getInstance();

    private RecyclerView rvMessages;
    private EditText etMessage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // initial Firestore
        db = FirebaseFirestore.getInstance();

        // get unique username
        if (getArguments() != null) {
            currentUsername = getArguments().getString(ARG_USERNAME, "Guest");
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
        FloatingActionButton fabDm = v.findViewById(R.id.fab);

        // pass nickname to adapter
        adapter = new MessageAdapter(messages, currentNickname);
        rvMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMessages.setAdapter(adapter);

        ircClient = getIrcClientManager();

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
                                .replace(R.id.container, DirectMessageFragment.newInstance(me, peer))
                                .addToBackStack(null)
                                .commit();
                    }
                })
                .show();
    }

    // =============================
    // 🔹 Connect to IRC Manager
    // =============================
    @NonNull
    private IrcClientManager getIrcClientManager() {
        IrcClientManager manager = new IrcClientManager();
        manager.setCallback(new IrcClientManager.MessageCallback() {
            @Override
            public void onMessage(String u, String t, long ts, boolean mine) {
                messages.add(new Message(u, t, mine));
                adapter.notifyItemInserted(messages.size() - 1);
                rvMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onSystem(String t) {
                displaySystemMessage(t);
            }
        });

        // Connect user to default channel
        manager.connect(currentUsername, "#usth-ircui");
        return manager;
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

                    // Update the adapter with the correct nickname for outgoing messages
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

        ircClient.sendMessage(text);
        cooldownManager.recordMessageSent();
        etMessage.getText().clear();
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
                    displaySystemMessage("Nickname changed from " + currentNickname + " to " + newNick);
                    currentNickname = newNick;
                    adapter = new MessageAdapter(messages, currentUsername);
                    rvMessages.setAdapter(adapter);
                } else {
                    displaySystemMessage("Usage: /nick <new_nickname>");
                }
                break;
            case "/connect":
                displaySystemMessage("Attempting to connect to IRC server...");
                try {
                    ircClient.connect(currentNickname, "#usth-ircui");
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

    // =============================
    // 🔹 Helper methods
    // =============================
    private void showHelpInfo() {
        String helpMessage = "--- User Guide ---\n"
                + "/help or /general - Shows this guide.\n"
                + "/nick <new_name> - Changes your nickname.\n"
                + "--------------------";
        displaySystemMessage(helpMessage);
    }

    private void displaySystemMessage(String text) {
        messages.add(new Message("System", text, false));
        adapter.notifyItemInserted(messages.size() - 1);
        rvMessages.scrollToPosition(messages.size() - 1);
    }

    private void fetchOnlineUsers() {
        db.collection("Users")
                .whereEqualTo("status", "online")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    currentUsers.clear();
                    querySnapshot.getDocuments().forEach(doc -> {
                        String name = doc.getString("nickname");
                        if (name != null && !name.equals(currentNickname)) {
                            currentUsers.add(name);
                        }
                    });

                    if (currentUsers.isEmpty()) {
                        Toast.makeText(getContext(), "No members online", Toast.LENGTH_SHORT).show();
                    } else {
                        showUserListDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load members", Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error loading users", e);
                });

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

        if (id == R.id.action_settings) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, SettingsFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
            return true;
        }

        if (id == R.id.action_members){
            fetchOnlineUsers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // === Update online status ===
    @Override
    public void onStart() {
        super.onStart();
        if (currentUsername != null && !currentUsername.equals("Guest")) {
            db.collection("Users").document(currentUsername)
                    .update("status", "online");
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if (currentUsername != null && !currentUsername.equals("Guest")) {
            db.collection("Users").document(currentUsername)
                    .update("status", "offline");
        }
    }
    // === Show online users ===
    private void showUserListDialog() {
        if (getContext() == null) return;

        if (currentUsers.isEmpty()) {
            Toast.makeText(getContext(), "No members online", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_user_list, null);

        RecyclerView rvUsers = dialogView.findViewById(R.id.rvUserList);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create the dialog first (so we can dismiss it later)
        AlertDialog dialog = builder
                .setTitle("Online Users (" + currentUsers.size() + ")")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        //  Adapter setup
        UserListAdapter userListAdapter = new UserListAdapter(currentUsers, selectedUser -> {
            // Prevent sending DM to yourself
            if (selectedUser.equals(currentUsername)) {
                Toast.makeText(getContext(), "You can't send a DM to yourself!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Dismiss the dialog
            dialog.dismiss();

            //  Navigate to DirectMessageFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.container,
                            vn.edu.usth.ircui.feature_chat.ui.DirectMessageFragment.newInstance(currentUsername, selectedUser)
                    )
                    .addToBackStack(null)
                    .commit();
        });

        rvUsers.setAdapter(userListAdapter);
        dialog.show();
    }

}
