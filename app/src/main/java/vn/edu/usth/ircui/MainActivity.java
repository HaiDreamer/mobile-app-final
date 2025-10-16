package vn.edu.usth.ircui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;

import vn.edu.usth.ircui.feature_chat.data.MessageNotification;
import vn.edu.usth.ircui.feature_user.LocaleHelper;

/**
 * MainActivity
 * -------------
 * Acts as the main container for all fragments:
 *  - Shows Welcome screen first (Login / Register / Guest)
 *  - Hosts ChatFragment after login or guest selection
 *  - Manages toolbar, app theme, permissions, and fragment navigation
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private Button btnLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Locale attach for runtime language switching
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1️⃣ Apply the saved Day/Night theme before super.onCreate
        initializeAppTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2️⃣ Setup toolbar and handle back navigation dynamically
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AppBarLayout appBar = findViewById(R.id.appbar);
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Update toolbar icon based on backstack
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            if (canBack) {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            } else {
                toolbar.setNavigationIcon(null);
                toolbar.setNavigationOnClickListener(null);
                toolbar.setTitle("IRC UI");
            }
            updateUiForTopFragment();
        });

        // 3️⃣ Load WelcomeFragment when app starts
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, new WelcomeFragment())
                    .runOnCommit(this::updateUiForTopFragment)
                    .commit();
        }

        // 4️⃣ Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }

        updateUiForTopFragment();
    }

    // =============================
    // 🔹 PUBLIC API FOR FRAGMENTS
    // =============================

    /**
     * Called by Login, Register, or Welcome (Guest mode)
     * Opens ChatFragment directly and clears backstack.
     */
    public void navigateToChatFragment(String username) {
        ChatFragment chatFragment = ChatFragment.newInstance(username);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, chatFragment);
        // Clear history (can't go back to login/welcome)
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        ft.commit();
    }

    /**
     * Called by LoginFragment → goes to server choosing screen (optional step)
     */
    public void navigateToChooseServer(String username) {
        ChooseServer chooseServerFragment = ChooseServer.newInstance(username);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, chooseServerFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    public void navigateToChoose_avt(String username) {
        Choose_avt chooseAvtFragment = new Choose_avt();

        Bundle bundle = new Bundle();
        bundle.putString("username", username);
        chooseAvtFragment.setArguments(bundle);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, chooseAvtFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    // =============================
    // 🔹 MENU & ACTIONS
    // =============================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu if needed
        // getMenuInflater().inflate(R.menu.main_menu, menu);
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
        } else if (id == R.id.action_refresh) {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_user_info) {
            showUserInfo();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_clear_history) {
            clearChatHistory();
            return true;
        } else if (id == R.id.action_logout) {
            handleLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // =============================
    // 🔹 PERMISSION CALLBACK
    // =============================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permission,
                                           @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permission, grantResult);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // =============================
    // 🔹 HELPER METHODS
    // =============================

    private void initializeAppTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void updateUiForTopFragment() {
        if (btnLanguage == null) return;
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        boolean onLogin = f instanceof LoginFragment;
        btnLanguage.setVisibility(onLogin ? android.view.View.VISIBLE : android.view.View.GONE);
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
                .setPositiveButton("Yes", (dialog, which) ->
                        Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show()
                )
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

    // Shortcut method: allows RegisterFragment to send user back to LoginFragment
    public void navigateToLoginFragment(){
        LoginFragment loginFragment = new LoginFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, loginFragment)
                .commit();
    }
}
