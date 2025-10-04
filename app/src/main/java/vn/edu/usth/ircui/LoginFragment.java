package vn.edu.usth.ircui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.edu.usth.ircui.network.IrcClientManager;
import vn.edu.usth.ircui.service.IrcForegroundService;
import vn.edu.usth.ircui.utils.NickUtils;

public class LoginFragment extends Fragment {

    private Spinner  spServer;
    private EditText etNick, etChannel, etSaslUser, etSaslPass;
    private CheckBox cbSaslPlain, cbSaslExternal;
    private Button   btnStart, btnChatOnly;

    // Android 13+ runtime notification permission (required so FGS notif is visible) :contentReference[oaicite:0]{index=0}
    private final ActivityResultLauncher<String> notifPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { /* no-op */ });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        spServer      = v.findViewById(R.id.spServer);
        etNick        = v.findViewById(R.id.etUsername);
        etChannel     = v.findViewById(R.id.etChannel);
        etSaslUser    = v.findViewById(R.id.etSaslUser);
        etSaslPass    = v.findViewById(R.id.etSaslPass);
        cbSaslPlain   = v.findViewById(R.id.cbSaslPlain);
        cbSaslExternal= v.findViewById(R.id.cbSaslExternal);
        btnStart      = v.findViewById(R.id.btnStartService);
        btnChatOnly   = v.findViewById(R.id.btnLogin);

        // Simple server list (all TLS 6697). Libera explicitly documents 6697 for TLS. :contentReference[oaicite:1]{index=1}
        ArrayAdapter<String> aa = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Libera.Chat (irc.libera.chat:6697)",
                        "OFTC (irc.oftc.net:6697)",
                        "Rizon (irc.rizon.net:6697)"});
        spServer.setAdapter(aa);

        // Toggle SASL fields
        cbSaslPlain.setOnCheckedChangeListener((btn, checked) -> {
            etSaslUser.setEnabled(checked);
            etSaslPass.setEnabled(checked);
            if (checked) cbSaslExternal.setChecked(false);
        });
        cbSaslExternal.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                cbSaslPlain.setChecked(false);
                etSaslUser.setEnabled(false);
                etSaslPass.setEnabled(false);
            }
        });

        // Default channel
        if (TextUtils.isEmpty(etChannel.getText())) etChannel.setText("#usth-ircui");

        // Start ForegroundService (recommended for stable background connection) :contentReference[oaicite:2]{index=2}
        btnStart.setOnClickListener(vw -> {
            String rawNick = getTextSafe(etNick);
            String nick = vn.edu.usth.ircui.utils.NickUtils.sanitize(rawNick, 32); // RFC-style; no spaces
            String channel = normalizeChannel(getTextSafe(etChannel));
            ServerChoice sc = readServerChoice();

            // Ask for POST_NOTIFICATIONS on API 33+ so FGS notif shows. :contentReference[oaicite:3]{index=3}
            if (Build.VERSION.SDK_INT >= 33) {
                notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS);
            }

            Intent svc = new Intent(requireContext(), IrcForegroundService.class);
            svc.putExtra(IrcForegroundService.EXTRA_NICK, nick);
            svc.putExtra(IrcForegroundService.EXTRA_CHANNEL, channel);

            boolean useExternal = cbSaslExternal.isChecked();
            boolean usePlain    = cbSaslPlain.isChecked();
            String saslUser = usePlain ? getTextSafe(etSaslUser) : null;
            String saslPass = usePlain ? getTextSafe(etSaslPass) : null;

            svc.putExtra(IrcForegroundService.EXTRA_SASL_EXTERNAL, useExternal);
            svc.putExtra(IrcForegroundService.EXTRA_SASL_USER, saslUser);
            svc.putExtra(IrcForegroundService.EXTRA_SASL_PASS, saslPass);

            // Also tell IrcClientManager which server to try first (optional: you can add setServers later)
            // For now, the manager has sensible defaults; you could extend it to accept host/port extras.

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                requireContext().startForegroundService(svc);  // O+ path
            } else {
                requireContext().startService(svc);            // pre-O path
            }

            Toast.makeText(requireContext(), "Starting background IRC as " + nick + " → " + channel, Toast.LENGTH_SHORT).show();
        });

        // “Chat only” button: go straight to ChatFragment without service (lifecycle-tied connection)
        btnChatOnly.setOnClickListener(vw -> {
            String rawNick = getTextSafe(etNick);
            String nick = NickUtils.sanitize(rawNick, 32);
            ChatFragment chat = ChatFragment.newInstance(nick);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, chat)
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }

    private static class ServerChoice {
        final String host; final int port; final boolean tls;
        ServerChoice(String h, int p, boolean t){ host=h; port=p; tls=t; }
    }

    private ServerChoice readServerChoice() {
        int pos = spServer.getSelectedItemPosition();
        switch (pos) {
            case 1: return new ServerChoice("irc.oftc.net", 6697, true);
            case 2: return new ServerChoice("irc.rizon.net", 6697, true);
            case 0:
            default: return new ServerChoice("irc.libera.chat", 6697, true);
        }
    }

    private static String getTextSafe(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static String normalizeChannel(String s) {
        if (TextUtils.isEmpty(s)) return "#usth-ircui";
        if (!s.startsWith("#")) return "#" + s;
        return s;
    }
}
