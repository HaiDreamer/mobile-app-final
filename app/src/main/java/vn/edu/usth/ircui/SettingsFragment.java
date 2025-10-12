package vn.edu.usth.ircui;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView tvUser = root.findViewById(R.id.tvUser);
        Button btnLogout = root.findViewById(R.id.btnLogout);

        // Replace with your real auth repo if available. For now, a placeholder.
        tvUser.setText("Signed in as User");

        btnLogout.setOnClickListener(v -> {
            // TODO: AuthRepository.get(requireContext()).logout();
            // TODO: startActivity(new Intent(requireContext(), AuthActivity.class));
            // For demo, just go back to LoginFragment:
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .commit();
        });

        return root;
    }
}
