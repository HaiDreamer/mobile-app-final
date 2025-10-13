package vn.edu.usth.ircui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import vn.edu.usth.ircui.feature_user.LocaleHelper;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireContext().getSharedPreferences("app_settings", 0);

        initAccountSection(view);
        initAppearanceSection(view);
        initLanguageSection(view);
        initActivityStatusSection(view);
        initNotificationSection(view);
        initChatSection(view);
        initAboutAndLogoutSection(view);

        return view;
    }

    private void initAccountSection(View view) {
        view.findViewById(R.id.accountManagement).setOnClickListener(v -> {
            showAccountManagement();
        });

        view.findViewById(R.id.profileSection).setOnClickListener(v -> {
            showProfileSettings();
        });
    }

    private void initAppearanceSection(View view) {
        Switch switchDarkMode = view.findViewById(R.id.switchDarkMode);
        TextView tvCurrentFontSize = view.findViewById(R.id.tvCurrentFontSize);

        // Dark mode
        boolean isDarkMode = sharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                == AppCompatDelegate.MODE_NIGHT_YES;
        switchDarkMode.setChecked(isDarkMode);

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int themeMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            sharedPreferences.edit().putInt("theme_mode", themeMode).apply();
            AppCompatDelegate.setDefaultNightMode(themeMode);
            requireActivity().recreate();
        });

        // Text size
        String currentFontSize = sharedPreferences.getString("font_size", "medium");
        tvCurrentFontSize.setText(getFontSizeDisplayName(currentFontSize));

        view.findViewById(R.id.fontSizeSection).setOnClickListener(v -> {
            showFontSizeDialog(tvCurrentFontSize);
        });
    }

    private void initLanguageSection(View view) {
        TextView tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage);

        // Take current language from SharedPreferences
        String currentLang = sharedPreferences.getString("app_language", "en");
        tvCurrentLanguage.setText(getLanguageDisplayName(currentLang));

        view.findViewById(R.id.languageSection).setOnClickListener(v -> {
            showLanguageDialog(tvCurrentLanguage);
        });
    }

    private void initActivityStatusSection(View view) {
        Switch switchOnlineStatus = view.findViewById(R.id.switchOnlineStatus);
        switchOnlineStatus.setChecked(true);

        switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("online_status", isChecked).apply();
            Toast.makeText(requireContext(),
                    isChecked ? "Đã bật trạng thái trực tuyến" : "Đã tắt trạng thái trực tuyến",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void initNotificationSection(View view) {
        Switch switchNotifications = view.findViewById(R.id.switchNotifications);
        Switch switchSound = view.findViewById(R.id.switchSound);
        Switch switchVibration = view.findViewById(R.id.switchVibration);

        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        boolean soundEnabled = sharedPreferences.getBoolean("sound_enabled", true);
        boolean vibrationEnabled = sharedPreferences.getBoolean("vibration_enabled", true);

        switchNotifications.setChecked(notificationsEnabled);
        switchSound.setChecked(soundEnabled);
        switchVibration.setChecked(vibrationEnabled);

        switchSound.setEnabled(notificationsEnabled);
        switchVibration.setEnabled(notificationsEnabled);

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply();
            switchSound.setEnabled(isChecked);
            switchVibration.setEnabled(isChecked);
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("sound_enabled", isChecked).apply();
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("vibration_enabled", isChecked).apply();
        });
    }

    private void initChatSection(View view) {
        Switch switchTimestamps = view.findViewById(R.id.switchTimestamps);
        Switch switchAutoConnect = view.findViewById(R.id.switchAutoConnect);
        Switch switchShowJoinLeave = view.findViewById(R.id.switchShowJoinLeave);

        boolean timestampsEnabled = sharedPreferences.getBoolean("timestamps_enabled", true);
        boolean autoConnectEnabled = sharedPreferences.getBoolean("auto_connect", false);
        boolean showJoinLeaveEnabled = sharedPreferences.getBoolean("show_join_leave", true);

        switchTimestamps.setChecked(timestampsEnabled);
        switchAutoConnect.setChecked(autoConnectEnabled);
        switchShowJoinLeave.setChecked(showJoinLeaveEnabled);

        switchTimestamps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("timestamps_enabled", isChecked).apply();
        });

        switchAutoConnect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_connect", isChecked).apply();
        });

        switchShowJoinLeave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("show_join_leave", isChecked).apply();
        });
    }

    private void initAboutAndLogoutSection(View view) {
        view.findViewById(R.id.aboutSection).setOnClickListener(v -> {
            showAboutDialog();
        });

        view.findViewById(R.id.logoutSection).setOnClickListener(v -> {
            handleLogout();
        });
    }

    private void showAccountManagement() {
        Toast.makeText(requireContext(), "Mở quản lý tài khoản", Toast.LENGTH_SHORT).show();
    }

    private void showProfileSettings() {
        Toast.makeText(requireContext(), "Mở cài đặt trang cá nhân", Toast.LENGTH_SHORT).show();
    }

    private void showFontSizeDialog(TextView tvCurrentFontSize) {
        String currentFontSize = sharedPreferences.getString("font_size", "medium");
        int checkedItem;
        switch (currentFontSize) {
            case "small": checkedItem = 0; break;
            case "large": checkedItem = 2; break;
            default: checkedItem = 1;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn cỡ chữ")
                .setSingleChoiceItems(new String[]{
                        getString(R.string.small),
                        getString(R.string.medium),
                        getString(R.string.large)
                }, checkedItem, (dialog, which) -> {
                    String fontSize;
                    switch (which) {
                        case 0: fontSize = "small"; break;
                        case 2: fontSize = "large"; break;
                        default: fontSize = "medium";
                    }
                    sharedPreferences.edit().putString("font_size", fontSize).apply();
                    tvCurrentFontSize.setText(getFontSizeDisplayName(fontSize));
                    dialog.dismiss();
                })
                .show();
    }

    private void showLanguageDialog(TextView tvCurrentLanguage) {
        String currentLang = sharedPreferences.getString("app_language", "en");
        int checkedItem = currentLang.equals("vi") ? 1 : 0;

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn ngôn ngữ")
                .setSingleChoiceItems(new String[]{
                        getString(R.string.language_english),
                        getString(R.string.language_vietnamese)
                }, checkedItem, (dialog, which) -> {
                    String lang = (which == 0) ? "en" : "vi";
                    sharedPreferences.edit().putString("app_language", lang).apply();
                    tvCurrentLanguage.setText(getLanguageDisplayName(lang));

                    // Áp dụng ngôn ngữ mới
                    LocaleHelper.setLocale(requireContext(), lang);

                    // Hiển thị thông báo
                    Toast.makeText(requireContext(),
                            "Đã thay đổi ngôn ngữ. Ứng dụng cần khởi động lại để áp dụng.",
                            Toast.LENGTH_LONG).show();

                    dialog.dismiss();
                })
                .show();
    }

    private String getFontSizeDisplayName(String fontSize) {
        switch (fontSize) {
            case "small": return getString(R.string.small);
            case "large": return getString(R.string.large);
            default: return getString(R.string.medium);
        }
    }

    private String getLanguageDisplayName(String lang) {
        switch (lang) {
            case "vi":
                return getString(R.string.language_vietnamese);
            default:
                return getString(R.string.language_english);
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Giới thiệu")
                .setMessage("USTH IRC Client v1.0\nDeveloped for USTH")
                .setPositiveButton("OK", null)
                .show();
    }

    private void handleLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Có", (dialog, which) -> {
                    requireActivity().getSupportFragmentManager().popBackStack();
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, new LoginFragment())
                            .commit();
                })
                .setNegativeButton("Không", null)
                .show();
    }
}