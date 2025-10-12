package vn.edu.usth.irc;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import vn.edu.usth.irc.auth.AuthActivity;
import vn.edu.usth.irc.auth.AuthRepository;
import vn.edu.usth.irc.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        b = FragmentSettingsBinding.inflate(inflater, container, false);
        String u = AuthRepository.get(requireContext()).username();
        b.tvUser.setText("Signed in as " + u);
        b.btnLogout.setOnClickListener(v -> {
            AuthRepository.get(requireContext()).logout();
            startActivity(new Intent(requireContext(), AuthActivity.class));
            requireActivity().finish();
        });
        return b.getRoot();
    }
}
