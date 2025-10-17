package vn.edu.usth.ircui.feature_chat.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.R;
import vn.edu.usth.ircui.feature_chat.data.Message;
import vn.edu.usth.ircui.network.IrcClientManager;

/**
 * A UI fragment that hosts a simple IRC-like group chat.
 */
public class GroupChatFragment extends Fragment {

    private static final String ARG_CHANNEL = "arg_channel";

    /** Factory: creates fragment with the given channel name. */
    public static GroupChatFragment newInstance(@NonNull String channel) {
        GroupChatFragment f = new GroupChatFragment();
        Bundle b = new Bundle();
        b.putString(ARG_CHANNEL, channel);
        f.setArguments(b);
        return f;
    }

    private String channelName; // current channel (read form args)

    private final List<Message> messages = new ArrayList<>();
    private vn.edu.usth.ircui.MessageAdapter adapter;
    private IrcClientManager irc;

    private EditText etInput;

    // pickers
    private ActivityResultLauncher<String> pickImage;
    private ActivityResultLauncher<String> pickFile;
    private ActivityResultLauncher<String> requestReadImages;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // read channel parameter (default if not init)
        channelName = getArguments() != null
                ? getArguments().getString(ARG_CHANNEL)
                : "#usth-ircui";
        if (channelName == null || channelName.trim().isEmpty()) {
            channelName = "#usth-ircui";
        }

        pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) handlePickedUri(uri, true); });

        pickFile  = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) handlePickedUri(uri, false); });

        requestReadImages = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> { if (granted) pickImage.launch("image/*"); else toast("Permission denied"); }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group_chat, container, false);

        // RecyclerView
        RecyclerView rv = v.findViewById(R.id.groupRecycler);
        adapter = new vn.edu.usth.ircui.MessageAdapter(messages, "me");
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // IRC
        irc = new IrcClientManager();
        irc.setCallback(new IrcClientManager.MessageCallback() {
            @Override public void onMessage(String u, String t, long ts, boolean mine) {
                messages.add(new Message(u, t, mine));
                adapter.notifyItemInserted(messages.size() - 1);
                rv.scrollToPosition(messages.size() - 1);
            }
            @Override public void onSystem(String t) {
                messages.add(new Message("system", t, false));
                adapter.notifyItemInserted(messages.size() - 1);
                rv.scrollToPosition(messages.size() - 1);
            }
        });

        // set title by channel (depending on UI u set)
        requireActivity().setTitle(channelName);

        // Channel connection (user demo "Guest")
        irc.connect("Guest", channelName);

        // Input + buttons
        etInput = v.findViewById(R.id.etGroupInput);
        ImageButton btnSend = v.findViewById(R.id.btnGroupSend);
        ImageButton btnImg  = v.findViewById(R.id.btnPickImage);
        ImageButton btnFile = v.findViewById(R.id.btnPickFile);

        // cursor + keyboard
        etInput.requestFocus();
        v.post(() -> {
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT);
        });

        btnSend.setOnClickListener(vw -> {
            String msg = etInput.getText() == null ? "" : etInput.getText().toString().trim();
            if (TextUtils.isEmpty(msg)) return;
            irc.sendMessage(msg);
            etInput.setText("");
        });

        btnImg.setOnClickListener(vw -> {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    requestReadImages.launch(Manifest.permission.READ_MEDIA_IMAGES);
                } else {
                    pickImage.launch("image/*");
                }
            } else {
                pickImage.launch("image/*");
            }
        });

        btnFile.setOnClickListener(vw -> pickFile.launch("*/*"));

        return v;
    }

    private void handlePickedUri(@NonNull Uri uri, boolean image) {
        // Demo: send temporary link. When upload file, replace this stream.
        String tag = image ? "[image]" : "[file]";
        irc.sendMessage(tag + " " + uri.toString());
        toast(image ? "Picked image" : "Picked file");
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // avoid memory leak callback
        if (irc != null) {
            irc.setCallback(null);
            // if IrcClientManager has disconnect(), can be call this: irc.disconnect();
        }
    }
}
