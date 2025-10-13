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
                        "Ngôn ngữ đã được thay đổi",
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
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
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
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, SettingsFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
        else if (id == R.id.action_refresh) {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (id == R.id.action_user_info) {
            showUserInfo();
            return true;
        }
        else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        else if (id == R.id.action_clear_history) {
            clearChatHistory();
            return true;
        }
        else if (id == R.id.action_logout) {
            handleLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showUserInfo() {
        new AlertDialog.Builder(this)
                .setTitle("User Info")
                .setMessage("Username: Guest\nServer: Connected")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("USTH IRC Client v1.0\nDeveloped for USTH")
                .setPositiveButton("OK", null)
                .show();
    }

    private void clearChatHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat History")
                .setMessage("Are you sure you want to clear chat history?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, new LoginFragment())
                            .commit();
                })
                .setNegativeButton("No", null)
                .show();
    }
}