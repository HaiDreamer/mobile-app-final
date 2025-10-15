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

import java.util.Random;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        // Existing buttons (keep same IDs)
        Button btnGoToLogin = view.findViewById(R.id.btn_go_to_login);
        Button btnGoToRegister = view.findViewById(R.id.btn_go_to_register);

        // ✅ NEW: "Use as Guest" button
        Button btnUseGuest = view.findViewById(R.id.btn_use_guest);

        // Navigate to LoginFragment (existing behavior)
        btnGoToLogin.setOnClickListener(v -> {
            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.replace(R.id.container, new LoginFragment());
            ft.addToBackStack(null); // allow coming back to welcome screen
            ft.commit();
        });

        // Navigate to RegisterFragment (existing behavior)
        btnGoToRegister.setOnClickListener(v -> {
            FragmentTransaction ft = getParentFragmentManager().beginTransaction();
            ft.replace(R.id.container, new RegisterFragment());
            ft.addToBackStack(null); // allow coming back to welcome screen
            ft.commit();
        });

        // ✅ Guest flow: generate a random username and jump straight to Chat
        btnUseGuest.setOnClickListener(v -> {
            // Generate a readable random guest name (no DB save)
            String guestName = "Guest" + (new Random().nextInt(9000) + 1000); // e.g., Guest3478

            // Use the public API in MainActivity to open Chat
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToChatFragment(guestName);
            }
        });

        return view;
    }
}
