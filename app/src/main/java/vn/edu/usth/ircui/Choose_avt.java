package vn.edu.usth.ircui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Choose_avt extends Fragment {

    private ImageView avatarLogo;
    private int selectedAvatarResId = R.drawable.logo; // avt mặc định

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_avt, container, false);

        avatarLogo = view.findViewById(R.id.avatarLogo);

        // Gắn sự kiện chọn avatar
        setupAvatarOption(view, R.id.avatarOption1, R.drawable.avt1);
        setupAvatarOption(view, R.id.avatarOption2, R.drawable.avt2);
        setupAvatarOption(view, R.id.avatarOption3, R.drawable.avt3);
        setupAvatarOption(view, R.id.avatarOption4, R.drawable.avt4);
        setupAvatarOption(view, R.id.avatarOption5, R.drawable.avt5);

        // Nút Next
        Button nextButton = view.findViewById(R.id.next);
        nextButton.setOnClickListener(v -> {
            saveAvatarChoice();
            goToServerSelection();
        });

        return view;
    }

    private void setupAvatarOption(View root, int viewId, int resId) {
        ImageView avatar = root.findViewById(viewId);
        avatar.setOnClickListener(v -> {
            selectedAvatarResId = resId;
            avatarLogo.setImageResource(resId);
        });
    }

    private void saveAvatarChoice() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("profile", Context.MODE_PRIVATE);
        prefs.edit().putInt("avatarResId", selectedAvatarResId).apply();
    }

    private void goToServerSelection() {
        String username = getArguments() != null ? getArguments().getString("username") : "Guest";

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToChooseServer(username);
        }
    }
}
