package vn.edu.usth.ircui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.edu.usth.ircui.feature_user.NickUtils;

/**
 * Auth-only login screen that matches the UPDATED fragment_login.xml (username/password).
 * All legacy IRC-connection widgets and flows have been removed.
 */
public class LoginFragment extends Fragment {

    // New (auth) UI
    @Nullable private EditText etUserAuth, etPassAuth;
    @Nullable private Button   btnLoginAuth;
    @Nullable private TextView tvToRegister;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        // Bind new-auth UI only
        etUserAuth   = v.findViewById(R.id.etUser);
        etPassAuth   = v.findViewById(R.id.etPass);
        btnLoginAuth = v.findViewById(R.id.btnLogin);
        tvToRegister = v.findViewById(R.id.tvToRegister);

        setupAuthMode();
        return v;
    }

    private void setupAuthMode() {
        if (btnLoginAuth != null) {
            btnLoginAuth.setOnClickListener(v -> {
                String u = safeText(etUserAuth);
                String p = safeText(etPassAuth);

                if (TextUtils.isEmpty(u) || TextUtils.isEmpty(p)) {
                    Toast.makeText(requireContext(),
                            "Please enter username and password",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Your auth goes here; for now, just proceed to chat with a sanitized nick.
                String nick = NickUtils.sanitize(u, 32);
                ChatFragment chat = ChatFragment.newInstance(nick);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, chat)
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (tvToRegister != null) {
            tvToRegister.setOnClickListener(v ->
                    Toast.makeText(requireContext(),
                            "Registration screen not implemented yet.",
                            Toast.LENGTH_SHORT).show());
        }
    }

    private static String safeText(@Nullable EditText et) {
        if (et == null || et.getText() == null) return "";
        return et.getText().toString().trim();
    }
}
