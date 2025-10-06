package vn.edu.usth.ircui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import vn.edu.usth.ircui.feature_user.NickAvailabilityChecker;
import vn.edu.usth.ircui.network.IrcForegroundService;
import vn.edu.usth.ircui.feature_user.NickUtils;

public class LoginFragment extends Fragment {

    private Spinner  spServer;
    private EditText etNick, etChannel, etSaslUser, etSaslPass;
    private CheckBox cbSaslPlain, cbSaslExternal;
    private Button btnStart, btnChatOnly;

    // Android 13+ runtime notification permission (để FGS hiện notif)
    private final ActivityResultLauncher<String> notifPerm =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { /* no-op */ });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_login, container, false);

        spServer       = v.findViewById(R.id.spServer);
        etNick         = v.findViewById(R.id.etUsername);
        etChannel      = v.findViewById(R.id.etChannel);
        etSaslUser     = v.findViewById(R.id.etSaslUser);
        etSaslPass     = v.findViewById(R.id.etSaslPass);
        cbSaslPlain    = v.findViewById(R.id.cbSaslPlain);
        cbSaslExternal = v.findViewById(R.id.cbSaslExternal);
        btnStart       = v.findViewById(R.id.btnStartService);
        btnChatOnly    = v.findViewById(R.id.btnLogin);

        // ===== Spinner server với layout tùy biến (selected trắng, dropdown đen)
        final String[] servers = new String[]{
                "Libera.Chat (irc.libera.chat:6697)",
                "OFTC (irc.oftc.net:6697)",
                "Rizon (irc.rizon.net:6697)"
        };

        ArrayAdapter<String> serverAdapter = new ArrayAdapter<String>(requireContext(), 0, servers) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = getLayoutInflater().inflate(R.layout.item_spinner_selected, parent, false);
                }
                TextView tv = row.findViewById(R.id.tv);
                tv.setText(getItem(position));
                return row;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = getLayoutInflater().inflate(R.layout.item_spinner_dropdown, parent, false);
                }
                TextView tv = row.findViewById(R.id.tv);
                tv.setText(getItem(position));
                return row;
            }
        };
        spServer.setAdapter(serverAdapter);
        // ===== end spinner

        // Bật/tắt ô SASL theo checkbox
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

        // Mặc định
        if (TextUtils.isEmpty(etChannel.getText())) etChannel.setText("#usth-ircui");
        cbSaslExternal.setChecked(true);

        // Start ForegroundService (kết nối chạy nền)
        btnStart.setOnClickListener(vw -> {
            String rawNick = getTextSafe(etNick);
            String nick    = NickUtils.sanitize(rawNick, 32);
            String channel = normalizeChannel(getTextSafe(etChannel));
            ServerChoice sc = readServerChoice();

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

            // Bạn có thể mở rộng service nhận host/port nếu muốn:
            // svc.putExtra("EXTRA_HOST", sc.host);
            // svc.putExtra("EXTRA_PORT", sc.port);
            // svc.putExtra("EXTRA_TLS", sc.tls);

            if (Build.VERSION.SDK_INT >= 26) {
                requireContext().startForegroundService(svc);
            } else {
                requireContext().startService(svc);
            }

            Toast.makeText(requireContext(),
                    "Starting background IRC as " + nick + " → " + channel,
                    Toast.LENGTH_SHORT).show();
        });

        // Chat only (không chạy service) + check nickname availability
        btnChatOnly.setOnClickListener(vw -> {
            String rawNick = getTextSafe(etNick);
            String nick    = NickUtils.sanitize(rawNick, 32);
            ServerChoice sc = readServerChoice();

            btnChatOnly.setEnabled(false);
            Toast.makeText(requireContext(), "Checking nickname on server…", Toast.LENGTH_SHORT).show();

            new NickAvailabilityChecker(sc.host, sc.port, sc.tls)
                    .check(nick, (inUse, message) -> {
                        btnChatOnly.setEnabled(true);

                        if (inUse) {
                            etNick.setError("Nickname in use on " + sc.host);
                            Toast.makeText(requireContext(),
                                    "Nickname already in use: " + nick + "\n" + message,
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        ChatFragment chat = ChatFragment.newInstance(nick);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, chat)
                                .addToBackStack(null)
                                .commit();
                    });
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
            case 1:  return new ServerChoice("irc.oftc.net", 6697, true);
            case 2:  return new ServerChoice("irc.rizon.net", 6697, true);
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
