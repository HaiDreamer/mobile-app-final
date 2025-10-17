package vn.edu.usth.ircui.feature_chat.ui;

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

import vn.edu.usth.ircui.R;
import vn.edu.usth.ircui.feature_chat.data.Attachment;
import vn.edu.usth.ircui.network.IrcClientManager;
import vn.edu.usth.ircui.network.SharedIrcClient;

/**
 * render simple "direct message" chat UI
 *
 * **/

public class DirectMessageFragment extends Fragment {

    private static final String ARG_PEER = "arg_peer";
    private static final String ARG_ME   = "arg_me";
    private static final String ARG_SERVER_HOST = "arg_server_host";

    public static DirectMessageFragment newInstance(String me, String peer) {
        Bundle b = new Bundle();
        b.putString(ARG_ME, me);
        b.putString(ARG_PEER, peer);
        DirectMessageFragment f = new DirectMessageFragment();
        f.setArguments(b);
        return f;
    }

    public static DirectMessageFragment newInstance(String me, String peer, String serverHost) {
        Bundle b = new Bundle();
        b.putString(ARG_ME, me);
        b.putString(ARG_PEER, peer);
        b.putString(ARG_SERVER_HOST, serverHost);
        DirectMessageFragment f = new DirectMessageFragment();
        f.setArguments(b);
        return f;
    }

    private RecyclerView recycler;
    private DirectMessageAdapter adapter;
    private EditText input;
    private ImageButton btnSend, btnAttach, btnImage;

    private String me, peer;
    private SharedIrcClient sharedIrcClient;
    private SharedIrcClient.MessageCallback messageCallback;

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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister callback when fragment is destroyed
        if (sharedIrcClient != null && messageCallback != null) {
            sharedIrcClient.unregisterCallback(messageCallback);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_direct_message, container, false);

        me   = getArguments() != null ? getArguments().getString(ARG_ME, "me") : "me";
        peer = getArguments() != null ? getArguments().getString(ARG_PEER, "peer") : "peer";

        recycler   = v.findViewById(R.id.Recycler);
        input      = v.findViewById(R.id.Input);
        btnSend    = v.findViewById(R.id.BtnSend);
        btnAttach  = v.findViewById(R.id.BtnAttach);
        btnImage   = v.findViewById(R.id.BtnImage);

        adapter = new DirectMessageAdapter(me);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

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

        // Initialize shared IRC client
        initializeSharedIrcClient();

        // Seed message
        adapter.addText(false, peer, "Direct messages with " + peer + " — images & files supported.");
        
        // Add debug info
        adapter.addText(false, "Debug", "👤 Me: " + me + ", Peer: " + peer);
        adapter.addText(false, "Debug", "🔗 Connected: " + (sharedIrcClient != null && sharedIrcClient.isConnected()));

        // --- Keyboard / Insets handling (no extra IDs required) ---
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

        // Animate footer with the IME and add extra space for the list bottom.
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

    private void sendText() {
        String t = input.getText().toString().trim();
        if (TextUtils.isEmpty(t)) return;

        // Check if IRC client is connected before sending
        if (sharedIrcClient == null) {
            // Don't show system messages in direct message fragment
            // Error will be handled by ChatFragment's system callback
            return;
        }

        if (!sharedIrcClient.isConnected()) {
            // Don't show system messages in direct message fragment
            // Error will be handled by ChatFragment's system callback
            return;
        }

        try {
            // Add message to UI immediately (local echo)
            adapter.addText(true, me, t);
            recycler.scrollToPosition(adapter.getItemCount() - 1);
            
            // Send private message via IRC server
            sharedIrcClient.sendPrivateMessage(peer, t);
            input.setText("");
            
            // Add debug message to show that send was attempted
            adapter.addText(false, "Debug", "📤 Sent to: " + peer);
            recycler.scrollToPosition(adapter.getItemCount() - 1);
        } catch (Exception e) {
            // Remove the message from UI if send failed
            if (adapter.getItemCount() > 0) {
                adapter.removeLastMessage();
            }
            // Show error message for debugging
            adapter.addText(false, "Error", "❌ Send failed: " + e.getMessage());
            recycler.scrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void addAttachmentMessage(Attachment.Type type, Uri uri) {
        String name = queryDisplayName(requireContext(), uri);
        long size   = querySize(requireContext(), uri);

        // TODO: hook your file/image send here (upload or send-by-URI)
        adapter.addAttachment(type, true, me, uri, name, size);
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

    private void initializeSharedIrcClient() {
        try {
            sharedIrcClient = SharedIrcClient.getInstance();
            
            // Create callback for regular messages only (no system messages)
            messageCallback = new SharedIrcClient.MessageCallback() {
                @Override
                public void onMessage(String username, String text, long ts, boolean mine) {
                    // Show all private messages (not just from peer) for debugging
                    // TODO: Filter to only show messages from peer user once working
                    if (!mine) { // Only show messages from other users
                        adapter.addText(false, username, text);
                        recycler.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            };
            
            // Register this fragment as a callback
            sharedIrcClient.registerCallback(messageCallback);

            // Get server host from arguments if available
            String serverHost = getArguments() != null ? getArguments().getString(ARG_SERVER_HOST) : "irc.libera.chat";
            
            // Connect to IRC server using shared client
            sharedIrcClient.connect(serverHost, me, "#usth-ircui", requireContext());
            
        } catch (Exception e) {
            // Don't show system messages in direct message fragment
            // Error will be handled by ChatFragment's system callback
        }
    }

    // Small helper
    private int dp(int v) {
        float d = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }
}
