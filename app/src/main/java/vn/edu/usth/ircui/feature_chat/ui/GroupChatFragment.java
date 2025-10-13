package vn.edu.usth.ircui.feature_chat.ui;

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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.R;
import vn.edu.usth.ircui.MessageAdapter;
import vn.edu.usth.ircui.feature_chat.data.Message;

/**
 * Simple group chat screen backed by fragment_group_chat.xml.
 * Factory: newInstance(String channelId, String channelName)
 *
 * sending/adding messages locally so the UI works without crashes.
 * Hook your IrcClientManager in later if needed.
 */
public class GroupChatFragment extends Fragment {

    private static final String ARG_CHANNEL_ID   = "arg_channel_id";
    private static final String ARG_CHANNEL_NAME = "arg_channel_name";

    public static GroupChatFragment newInstance(@NonNull String channelId,
                                                @NonNull String channelName) {
        GroupChatFragment f = new GroupChatFragment();
        Bundle b = new Bundle();
        b.putString(ARG_CHANNEL_ID, channelId);
        b.putString(ARG_CHANNEL_NAME, channelName);
        f.setArguments(b);
        return f;
    }

    private String channelId  = "chat";
    private String channelName = "#Chat";

    // very lightweight local message list just to render the screen
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;

    private RecyclerView rv;
    private EditText     etInput;
    private ImageButton  btnSend, btnPickImage, btnPickFile;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String id = args.getString(ARG_CHANNEL_ID);
            String name = args.getString(ARG_CHANNEL_NAME);
            if (!TextUtils.isEmpty(id))   channelId = id;
            if (!TextUtils.isEmpty(name)) channelName = name;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_group_chat, container, false);

        rv          = root.findViewById(R.id.groupRecycler);
        etInput     = root.findViewById(R.id.etGroupInput);
        btnSend     = root.findViewById(R.id.btnGroupSend);
        btnPickImage= root.findViewById(R.id.btnPickImage);
        btnPickFile = root.findViewById(R.id.btnPickFile);

        // Use your existing MessageAdapter (expects a "me" username)
        adapter = new MessageAdapter(messages, "Me");
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendCurrentInput());
        btnPickImage.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Pick image (not implemented)", Toast.LENGTH_SHORT).show());
        btnPickFile.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Pick file (not implemented)", Toast.LENGTH_SHORT).show());

        return root;
    }

    private void sendCurrentInput() {
        if (etInput == null) return;
        String txt = etInput.getText() == null ? "" : etInput.getText().toString().trim();
        if (txt.isEmpty()) return;

        // locally add a message as "mine"
        Message m = new Message("Me", txt, true);
        messages.add(m);
        adapter.notifyItemInserted(messages.size() - 1);
        rv.scrollToPosition(messages.size() - 1);
        etInput.getText().clear();

        // TODO: integrate real sending via your IrcClientManager if needed
    }
}
