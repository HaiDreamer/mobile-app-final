/**
 * A UI fragment that hosts a simple IRC-like group chat.
 *
 * <p>Shows a scrolling message list, a text input, and buttons to send text or
 * pick an image/file. It connects via {@link IrcClientManager} and appends
 * inbound/outbound messages to a {@link RecyclerView} using
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Lifecycle-aware activity result APIs for picking images/files.</li>
 *   <li>Runtime permission request for reading images on Android 13+.</li>
 *   <li>Auto-scroll to the latest message on receive/send.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In your Activity/host, add the fragment:
 * getSupportFragmentManager()
 *     .beginTransaction()
 *     .replace(R.id.container, new GroupChatFragment())
 *     .commit();
 * }</pre>
 *
 * <h2>Permissions</h2>
 * <ul>
 *   <li>Android 13+ (API 33+): {@code android.permission.READ_MEDIA_IMAGES} for image picking.</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 *   <li>Replace the temporary URI-send in {@code handlePickedUri(...)} with your upload flow.</li>
 *   <li>{@link IrcClientManager} is created here for demo purposes; inject it if you use DI.</li>
 * </ul>
 *
 * @author aaa
 * @since 1.0
 */

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

public class GroupChatFragment extends Fragment {

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
        irc.connect("Guest", "#usth-ircui");

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
        // Like direct send: tạm thời gửi đường dẫn (nếu bạn đã có upload thực, thay thế chỗ này)
        String tag = image ? "[image]" : "[file]";
        irc.sendMessage(tag + " " + uri.toString());
        toast(image ? "Picked image" : "Picked file");
    }

    private void toast(String s) {
        Toast.makeText(requireContext(), s, Toast.LENGTH_SHORT).show();
    }
}
