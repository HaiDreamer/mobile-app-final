package vn.edu.usth.irc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Arrays;

import vn.edu.usth.irc.databinding.FragmentChatInfoBinding;

public class ChatInfoFragment extends Fragment {

    private FragmentChatInfoBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentChatInfoBinding.inflate(inflater, container, false);
        b.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        b.rvMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        b.rvMembers.setAdapter(new MemberAdapter(Arrays.asList("Thanh", "Mobile team")));
        return b.getRoot();
    }
}
