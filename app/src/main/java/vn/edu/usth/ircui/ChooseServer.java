package vn.edu.usth.ircui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class ChooseServer extends Fragment {

    private AutoCompleteTextView serverInput;
    private static final String[] PRESET_SERVERS = new String[] {
                       "irc.libera.chat", // TLS 6697 (Libera)
                       "irc.oftc.net",    // TLS 6697 (OFTC)
                       "irc.rizon.net"    // TLS 6697 (Rizon)
    };
    private MaterialButton enterServerButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.choose_server, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        String username = getArguments() != null ? getArguments().getString("username") : "";

        super.onViewCreated(view, savedInstanceState);

        serverInput = view.findViewById(R.id.serverInput);
        enterServerButton = view.findViewById(R.id.enterServer);

        serverInput.setDropDownBackgroundResource(R.color.dropdown_bg);
        serverInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                serverInput.showDropDown();
            }
        });

        // server suggest
        String[] servers = {"irc.libera.chat", "irc.oftc.chat", "irc.rizon.chat"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, servers);

        serverInput.setAdapter(adapter);
        serverInput.setThreshold(0);    // show suggestion when press-in

        enterServerButton.setOnClickListener(v -> {
            String selectedServer = serverInput.getText().toString().trim();
            if (selectedServer.isEmpty()) {
                Toast.makeText(requireContext(), "Please select or enter a server", Toast.LENGTH_SHORT).show();
                return;
            }

            // Call MainActivity for next fragment
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToChatFragment(selectedServer);
            }
        });
    }

    public static ChooseServer newInstance(String username) {
        ChooseServer fragment = new ChooseServer();
        Bundle args = new Bundle();
        args.putString("username", username);
        fragment.setArguments(args);
        return fragment;
    }

}
