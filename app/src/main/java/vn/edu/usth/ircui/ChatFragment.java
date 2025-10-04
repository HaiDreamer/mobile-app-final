package vn.edu.usth.ircui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.ircui.model.Message;
import vn.edu.usth.ircui.network.IrcClientManager;

public class ChatFragment extends Fragment {
    private static final String ARG_USERNAME = "username";
    public static ChatFragment newInstance(String username) {
        ChatFragment f = new ChatFragment();
        Bundle b = new Bundle(); b.putString(ARG_USERNAME, username); f.setArguments(b);
        return f;
    }

    private String username = "Guest";
    private final List<Message> messages = new ArrayList<>();
    private MessageAdapter adapter;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments()!=null) username = getArguments().getString(ARG_USERNAME,"Guest");
    }

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                       @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        RecyclerView rv = v.findViewById(R.id.rvMessages);
        EditText et = v.findViewById(R.id.etMessage);
        ImageButton btn = v.findViewById(R.id.btnSend);

        adapter = new MessageAdapter(messages, username);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        IrcClientManager irc = getIrcClientManager(rv);

        btn.setOnClickListener(view -> {
            String text = et.getText() != null ? et.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(text)) {
                irc.sendMessage(text);
                et.getText().clear();
            }
        });

        return v;
    }

    @NonNull
    private IrcClientManager getIrcClientManager(RecyclerView rv) {
        IrcClientManager irc = new IrcClientManager();
        irc.setCallback(new IrcClientManager.MessageCallback() {
            @Override public void onMessage(String u, String t, long ts, boolean mine) {
                messages.add(new Message(u, t, mine));
                adapter.notifyItemInserted(messages.size()-1);
                rv.scrollToPosition(messages.size()-1);
            }
            @Override public void onSystem(String t) {
                messages.add(new Message("system", t, false));
                adapter.notifyItemInserted(messages.size()-1);
                rv.scrollToPosition(messages.size()-1);
            }
        });

        // Connect (Libera by default; you can expose a server picker later)
        irc.connect(username, "#usth-ircui");
        return irc;
    }

}
