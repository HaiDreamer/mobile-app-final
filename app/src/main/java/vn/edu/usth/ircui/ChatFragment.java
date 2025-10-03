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

        // Demo lines to show styles
        messages.add(new Message("system", "Welcome, " + username + "!", false));
        messages.add(new Message("ike",
                "Anyone have any lunch recommendations in Norwich?", false));
        messages.add(new Message("aimee",
                "Try the octopus lounge on Frith street.", false));
        messages.add(new Message("toby",
                "```java\nfor(int i=0;i<3;i++){\n  System.out.println(\"Hello IRC\");\n}\n```", false));
        messages.add(new Message(username,
                "Looks great! I’ll push UI first, backend later.", true));
        adapter.notifyDataSetChanged();

        btn.setOnClickListener(view -> {
            String text = et.getText() != null ? et.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(text)) {
                messages.add(new Message(username, text, true));
                adapter.notifyItemInserted(messages.size()-1);
                rv.scrollToPosition(messages.size()-1);
                et.getText().clear();
            }
        });

        return v;
    }

}
