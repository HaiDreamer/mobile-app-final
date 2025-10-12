package vn.edu.usth.irc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import vn.edu.usth.irc.databinding.FragmentMessageListBinding;

public class MessageListFragment extends Fragment {

    private FragmentMessageListBinding b;
    private ConversationAdapter conversationAdapter;
    private StoryAdapter storyAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentMessageListBinding.inflate(inflater, container, false);

        // toolbar left icon opens drawer
        b.toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        // stories (avatars)
        b.rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        storyAdapter = new StoryAdapter(dummyNames());
        b.rvStories.setAdapter(storyAdapter);

        // conversations
        b.rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationAdapter = new ConversationAdapter(dummyConversations(), conv -> {
            // open Chat
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, ChatFragment.newInstance(conv.title))
                    .addToBackStack(null)
                    .commit();
        });
        b.rvConversations.setAdapter(conversationAdapter);

        return b.getRoot();
    }

    private List<String> dummyNames() {
        List<String> list = new ArrayList<>();
        list.add("Bach"); list.add("Thanh"); list.add("Elsa"); list.add("Anna"); list.add("Jenny");
        return list;
    }

    private List<Conversation> dummyConversations() {
        List<Conversation> list = new ArrayList<>();
        list.add(new Conversation("Thanh", "I hate IRC client", "2m"));
        list.add(new Conversation("Bach", "IRC is so boring", "5h"));
        list.add(new Conversation("Elsa", "Why our team need to do this?", "8d"));
        list.add(new Conversation("Anna", "Help meeeee!!!!", "11mo"));
        list.add(new Conversation("Jenny", "You reply me, don't you?", "1y"));
        return list;
    }
}
