package vn.edu.usth.ircui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        EditText etUsername = v.findViewById(R.id.etUsername);
        Button btnLogin = v.findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(view -> {
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            if (TextUtils.isEmpty(username)) username = "Guest";

            ChatFragment chat = ChatFragment.newInstance(username);
            FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, chat).addToBackStack(null).commit();
        });

        return v;
    }
}
