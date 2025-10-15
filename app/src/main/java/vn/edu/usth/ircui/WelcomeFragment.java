package vn.edu.usth.ircui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        Button btnGoToLogin = view.findViewById(R.id.btn_go_to_login);
        Button btnGoToRegister = view.findViewById(R.id.btn_go_to_register);

        btnGoToLogin.setOnClickListener(v -> {
            // Switch to LoginFragment
            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.replace(R.id.container, new LoginFragment());
            ft.addToBackStack(null); // allow come back to welcome screen
            ft.commit();
        });

        btnGoToRegister.setOnClickListener(v -> {
            // Switch to RegisterFragment
            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.replace(R.id.container, new RegisterFragment());
            ft.addToBackStack(null); // allow come back to welcome screen
            ft.commit();
        });

        return view;
    }
}