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
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.AppBarLayout;

import vn.edu.usth.ircui.feature_chat.data.MessageNotification;
import vn.edu.usth.ircui.feature_user.LocaleHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private Button btnLanguage;
    private DrawerLayout drawerLayout;
    private ChannelListFragment channelListFragment;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Locale attach for runtime language switching
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // THEME: apply saved Day/Night mode before super.onCreate
        initializeAppTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize drawer layout
        drawerLayout = findViewById(R.id.drawerLayout);
        
        // Initialize channel list fragment
        channelListFragment = new ChannelListFragment();
        FragmentTransaction drawerFt = getSupportFragmentManager().beginTransaction();
        drawerFt.replace(R.id.drawer_container, channelListFragment);
        drawerFt.commit();

        // TOOLBAR/BACK: set up app bar + back arrow on back stack changes
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

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
            
            // Debug logging
            String fragmentName = currentFragment != null ? currentFragment.getClass().getSimpleName() : "null";
            android.util.Log.d("MainActivity", "BackStackChanged - Fragment: " + fragmentName + ", canBack: " + canBack);
            
            // Check if we're on welcome screen (no navigation needed)
            boolean isWelcomeScreen = currentFragment instanceof WelcomeFragment;
            
            // Check if we're on login or register screens (need back to welcome)
            boolean isLoginOrRegister = currentFragment instanceof LoginFragment || 
                                      currentFragment instanceof RegisterFragment;
            
            // Check if we're on main chat fragments (should show menu icon)
            boolean isMainChatFragment = currentFragment instanceof ChatFragment || 
                                       currentFragment instanceof ChannelMessageFragment;
            
            if (isWelcomeScreen) {
                // Hide navigation icon on welcome screen (no back needed)
                toolbar.setNavigationIcon(null);
                toolbar.setNavigationOnClickListener(null);
                android.util.Log.d("MainActivity", "Setting navigation icon to null (welcome screen)");
            } else if (isLoginOrRegister && canBack) {
                // Show back arrow on login/register screens when there's back stack
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
                android.util.Log.d("MainActivity", "Setting navigation icon to back arrow (login/register with back stack)");
            } else if (isMainChatFragment) {
                // Always show menu icon on main chat fragments, even if there's back stack
                toolbar.setNavigationIcon(R.drawable.menu);
                toolbar.setNavigationOnClickListener(v -> toggleDrawer());
                android.util.Log.d("MainActivity", "Setting navigation icon to menu (chat fragment)");
            } else if (canBack) {
                // Show back arrow for other fragments with back stack
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
                android.util.Log.d("MainActivity", "Setting navigation icon to back arrow (canBack=true)");
            } else {
                // Show menu icon for other main fragments
                toolbar.setNavigationIcon(R.drawable.menu);
                toolbar.setNavigationOnClickListener(v -> toggleDrawer());
                android.util.Log.d("MainActivity", "Setting navigation icon to menu (default)");
            }
            toolbar.setTitle("IRC UI");
            updateUiForTopFragment();
        });

        // START: WelcomeFragment on first launch (kept from your version)
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, new WelcomeFragment())
                    .runOnCommit(this::updateUiForTopFragment)
                    .commit();
        }

        // NOTIFICATIONS: request runtime permission on Android 13+
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

    // PUBLIC API: fragments call this to jump to Chat and clear history
    public void navigateToChatFragment(String username, String serverHost, String channel) {
        // Save username, server, and current channel to SharedPreferences for later use
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit()
                .putString("current_username", username)
                .putString("current_server", serverHost)
                .putString("current_channel", channel)
                .apply();
        
        // Update channel list to include current channel
        updateCurrentChannel(channel);
        
        // Clear back stack first
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        
        // Then replace with chat fragment
        ChatFragment chatFragment = ChatFragment.newInstance(username, serverHost, channel);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, chatFragment);
        ft.runOnCommit(() -> {
            updateUiForTopFragment();
            // Force update toolbar after a short delay to ensure fragment is fully loaded
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                forceUpdateToolbar();
            }, 100);
        });
        ft.commit();
    }

    public void navigateToChooseServer(String username) {
        ChooseServer chooseServerFragment = ChooseServer.newInstance(username);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, chooseServerFragment);
        ft.addToBackStack(null); // Allow comeback if needs
        ft.commit();
    }

    // MENUS: inflate app bar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // MENUS: handle actions
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

    // NOTIFICATIONS: runtime permission result toast
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

    // helpers
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
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        boolean onLogin = f instanceof LoginFragment;
        btnLanguage.setVisibility(onLogin ? android.view.View.VISIBLE : android.view.View.GONE);
        
        // Disable drawer on auth screens (welcome, login, register)
        boolean isAuthScreen = f instanceof WelcomeFragment || 
                             f instanceof LoginFragment || 
                             f instanceof RegisterFragment;
        
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(isAuthScreen ? 
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED : 
                DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }
    
    private void forceUpdateToolbar() {
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        Toolbar toolbar = findViewById(R.id.toolbar);
        
        if (toolbar == null || currentFragment == null) return;
        
        // Check if we're on main chat fragments (should show menu icon)
        boolean isMainChatFragment = currentFragment instanceof ChatFragment || 
                                   currentFragment instanceof ChannelMessageFragment;
        
        if (isMainChatFragment) {
            // Force show menu icon on chat fragments
            toolbar.setNavigationIcon(R.drawable.menu);
            toolbar.setNavigationOnClickListener(v -> toggleDrawer());
            android.util.Log.d("MainActivity", "Force updated toolbar - showing menu icon for chat fragment");
        }
    }

    private void showUserInfo() {
        // Get current username from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String username = prefs.getString("current_username", "Guest");
        String server = prefs.getString("current_server", "Not connected");
        String channel = prefs.getString("current_channel", "None");
        
        String message = "Username: " + username + "\n" +
                        "Server: " + server + "\n" +
                        "Channel: " + channel;
        
        new AlertDialog.Builder(this)
                .setTitle("User Info")
                .setMessage(message)
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
    
    // Drawer methods
    public void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
    
    public void closeDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }
    
    public void openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START);
    }
    
    public ChannelListFragment getChannelListFragment() {
        return channelListFragment;
    }
    
    public void updateCurrentChannel(String channelName) {
        if (channelListFragment != null) {
            channelListFragment.setCurrentChannel(channelName);
        }
    }
    
    // PUBLIC API: navigate to ChannelMessageFragment
    public void navigateToChannelMessageFragment(String username, String serverHost, String channel) {
        // Save username, server, and current channel to SharedPreferences for later use
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit()
                .putString("current_username", username)
                .putString("current_server", serverHost)
                .putString("current_channel", channel)
                .apply();
        
        // Update channel list to include current channel
        updateCurrentChannel(channel);
        
        // Clear back stack first
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        
        // Then replace with channel message fragment
        ChannelMessageFragment channelMessageFragment = ChannelMessageFragment.newInstance(username, serverHost, channel);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, channelMessageFragment);
        ft.runOnCommit(() -> {
            updateUiForTopFragment();
            // Force update toolbar after a short delay to ensure fragment is fully loaded
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                forceUpdateToolbar();
            }, 100);
        });
        ft.commit();
    }
}
