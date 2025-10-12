package vn.edu.usth.irc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.irc.Adapter.MessageAdapter;
import vn.edu.usth.irc.databinding.FragmentChatBinding;
import vn.edu.usth.irc.model.Message;

public class ChatFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";

    public static ChatFragment newInstance(String title) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }

    private FragmentChatBinding b;
    private MessageAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentChatBinding.inflate(inflater, container, false);

        String title = (getArguments() != null)
                ? getArguments().getString(ARG_TITLE, "#Chat")
                : "#Chat";
        b.toolbar.setTitle(title);
        b.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        b.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_info) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new ChatInfoFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
            return false;
        });

        // RecyclerView
        b.rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MessageAdapter(dummyMessages());
        b.rvMessages.setAdapter(adapter);

        // Gửi bằng nút
        b.btnSend.setOnClickListener(v -> sendCurrentText());

        // Gửi bằng nút "Send" trên bàn phím
        b.etMessage.setImeOptions(EditorInfo.IME_ACTION_SEND);
        b.etMessage.setOnEditorActionListener((TextView tv, int actionId, android.view.KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentText();
                return true;
            }
            return false;
        });

        return b.getRoot();
    }

    private void sendCurrentText() {
        String text = b.etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        adapter.addRightMessage(text);
        b.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        b.etMessage.setText("");
    }

    private List<Message> dummyMessages() {
        List<Message> list = new ArrayList<>();
        list.add(Message.left("Welcome to #Chat"));
        list.add(Message.right("Hello everyone!"));
        return list;
    }
}
