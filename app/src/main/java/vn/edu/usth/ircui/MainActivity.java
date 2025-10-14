package vn.edu.usth.ircui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import vn.edu.usth.ircui.feature_chat.ui.GroupChatFragment;


import com.google.android.material.appbar.AppBarLayout;

import vn.edu.usth.ircui.feature_user.LocaleHelper;

/**
 * MainActivity với Drawer (ngăn kéo trái) hiển thị danh sách kênh.
 * Yêu cầu layout: activity_main.xml có:
 *  - DrawerLayout @id/drawerLayout
 *  - Toolbar @id/toolbar (trong AppBarLayout @id/appbar)
 *  - FrameLayout @id/container (nội dung chính)
 *  - FrameLayout @id/drawer_container (nội dung ngăn kéo)
 */
public class MainActivity extends AppCompatActivity
        implements ChannelListFragment.OnChannelSelected {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    private Button btnLanguage; // nếu bạn gắn từ layout nào đó

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp theme trước super.onCreate
        initializeAppTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ----- Toolbar + Insets
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AppBarLayout appBar = findViewById(R.id.appbar);
        if (appBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), sb.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // ----- Drawer + Toggle (hamburger)
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerToggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close
        );
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        // Nạp ChannelListFragment vào vùng ngăn kéo trái
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.drawer_container, new ChannelListFragment())
                .commit();

        // Lắng nghe back stack để chuyển icon Hamburger <-> Back
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateNavigationIcon);

        // Màn hình khởi đầu
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new WelcomeFragment())
                    .runOnCommit(this::updateUiForTopFragment)
                    .commit();
        }

        // Quyền thông báo Android 13+
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

    /** Khi chọn kênh từ ngăn kéo. */
    @Override
    public void onChannelSelected(String channel) {
        // TODO: thay bằng logic join/switch thật. Ví dụ dùng GroupChatFragment.newInstance(channel)
        Fragment chat = GroupChatFragment.newInstance(channel);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, chat)
                .addToBackStack(null)
                .commit();
        drawerLayout.closeDrawers();
    }

    /** Cho phép fragment gọi để vào màn hình chat và xóa back stack (ví dụ sau đăng nhập). */
    public void navigateToChatFragment(String username) {
        ChatFragment chatFragment = ChatFragment.newInstance(username);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, chatFragment);
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        ft.commit();
        updateNavigationIcon();
    }

    // ----- Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        boolean isAuthScreen = (f instanceof WelcomeFragment)
                || (f instanceof LoginFragment)
                || (f instanceof RegisterFragment);

        if (menu != null) {
            MenuItem userInfo = menu.findItem(R.id.action_user_info);
            MenuItem clearHistory = menu.findItem(R.id.action_clear_history);
            MenuItem logout = menu.findItem(R.id.action_logout);

            if (userInfo != null) userInfo.setVisible(!isAuthScreen);
            if (clearHistory != null) clearHistory.setVisible(!isAuthScreen);
            if (logout != null) logout.setVisible(!isAuthScreen);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Ưu tiên cho drawerToggle xử lý khi đang là hamburger
        if (drawerToggle.isDrawerIndicatorEnabled() && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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

    // ----- Permission callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ----- Drawer toggle sync
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    // ----- Helpers
    private void initializeAppTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void updateLanguageButtonText() {
        if (btnLanguage == null) return;
        String currentLang = LocaleHelper.getLanguage(this);
        btnLanguage.setText(currentLang.equals("vi") ? "VI" : "EN");
    }

    private void updateUiForTopFragment() {
        if (btnLanguage == null) return;
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        boolean onLogin = f instanceof LoginFragment;
        btnLanguage.setVisibility(onLogin ? android.view.View.VISIBLE : android.view.View.GONE);

        invalidateOptionsMenu(); // cập nhật hiển thị menu theo màn hình
    }

    /** Bật/tắt icon hamburger tùy theo có back stack hay không. */
    private void updateNavigationIcon() {
        boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (canBack) {
            // hiện mũi tên back, khóa mở ngăn kéo bằng icon
            drawerToggle.setDrawerIndicatorEnabled(false);
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        } else {
            // hiện hamburger, trả quyền cho drawerToggle
            toolbar.setNavigationOnClickListener(null);
            drawerToggle.setDrawerIndicatorEnabled(true);
            drawerToggle.syncState();
            toolbar.setTitle("IRC UI");
        }
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
                    updateNavigationIcon();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
