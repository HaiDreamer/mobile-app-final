package vn.edu.usth.ircui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import vn.edu.usth.ircui.feature_chat.data.MessageNotification;
import vn.edu.usth.ircui.feature_user.LocaleHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private Button btnLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initializeAppTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AppBarLayout appBar = findViewById(R.id.appbar);
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    sb.top,
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            if (canBack) {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
                toolbar.setNavigationOnClickListener(v ->
                        getOnBackPressedDispatcher().onBackPressed()
                );
            } else {
                toolbar.setNavigationIcon(null);
                toolbar.setNavigationOnClickListener(null);
                toolbar.setTitle("IRC UI");
            }
            updateUiForTopFragment();
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new LoginFragment())
                    .runOnCommit(this::updateUiForTopFragment)
                    .commit();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            } else {
                MessageNotification.showMsgNotification(
                        this, "System", "Welcome to IRC"
                );
            }
        }

        btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) {
            updateLanguageButtonText();

            btnLanguage.setOnClickListener(v -> {
                String currentLang = LocaleHelper.getLanguage(MainActivity.this);
                String newLang = currentLang.equals("en") ? "vi" : "en";

                LocaleHelper.setLocale(MainActivity.this, newLang);

                updateLanguageButtonText();

                Toast.makeText(MainActivity.this,
                        getString(R.string.language_changed_message),
                        Toast.LENGTH_SHORT).show();

                recreate();
            });
        }

        updateUiForTopFragment();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    private void initializeAppTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void updateLanguageButtonText() {
        if (btnLanguage == null) return;

        String currentLang = LocaleHelper.getLanguage(this);
        if (currentLang.equals("vi")) {
            btnLanguage.setText("VI");
        } else {
            btnLanguage.setText("EN");
        }
    }

    private void updateUiForTopFragment() {
        if (btnLanguage == null) return;
        androidx.fragment.app.Fragment f =
                getSupportFragmentManager().findFragmentById(R.id.container);
        boolean onLogin = f instanceof LoginFragment;
        btnLanguage.setVisibility(onLogin ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permission,
                                           @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permission, grantResult);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResult.length > 0
                    && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                MessageNotification.showMsgNotification(
                        this, "System", "Notification permission granted!"
                );
            } else {
                Toast.makeText(this, "Notification permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void navigateToChat() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new ChatFragment())
                .addToBackStack(null)
                .runOnCommit(this::updateUiForTopFragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Navigate to Settings
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, SettingsFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        else if (id == R.id.action_refresh) {
            // Handle refresh/reconnect
            Toast.makeText(this, getString(R.string.refreshing), Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.action_user_info) {
            // Handle user info
            showUserInfo();
            return true;
        }
        else if (id == R.id.action_about) {
            // Handle about
            showAboutDialog();
            return true;
        }
        else if (id == R.id.action_clear_history) {
            // Handle clear history
            clearChatHistory();
            return true;
        }
        else if (id == R.id.action_logout) {
            // Handle logout
            handleLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showUserInfo() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.user_info))
                .setMessage(getString(R.string.username) + ": " + getCurrentUsername() + "\n" +
                        getString(R.string.server) + ": " + getString(R.string.connected))
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.about))
                .setMessage("USTH IRC Client v1.0\n" + getString(R.string.developed_for_usth))
                .setPositiveButton("OK", null)
                .show();
    }

    private void clearChatHistory() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_chat_history))
                .setMessage(getString(R.string.confirm_clear_history))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    Toast.makeText(this, getString(R.string.chat_history_cleared), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.confirm_logout))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    // Navigate back to login
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, new LoginFragment())
                            .commit();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private String getCurrentUsername() {
        // This should return the actual current username
        // For now, return a placeholder
        return "Guest";
    }
}