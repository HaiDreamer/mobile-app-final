package vn.edu.usth.ircui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

public class ChatInfoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chat_info, container, false);

        RecyclerView rv = root.findViewById(R.id.rvMembers);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new MemberAdapter(Arrays.asList("Thanh", "Mobile team")));

        // (Optional) Wire toolbar back button if you want:
        // MaterialToolbar tb = root.findViewById(R.id.toolbar);
        // tb.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        return root;
    }
}
