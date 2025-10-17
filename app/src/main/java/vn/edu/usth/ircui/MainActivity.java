package vn.edu.usth.ircui;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import vn.edu.usth.ircui.feature_chat.ui.GroupChatFragment;
import vn.edu.usth.ircui.feature_chat.ui.DirectMessageFragment;
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
    private DrawerLayout drawerLayout;
    private ChannelListFragment channelListFragment;
    private RightDrawerFragment rightDrawerFragment;
    private GestureDetector gestureDetector;

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

        // Play startup sound effect
        playStartupSound();

        // Initialize drawer layout
        drawerLayout = findViewById(R.id.drawerLayout);
        
        // Initialize channel list fragment (left drawer)
        channelListFragment = new ChannelListFragment();
        FragmentTransaction drawerFt = getSupportFragmentManager().beginTransaction();
        drawerFt.replace(R.id.drawer_container, channelListFragment);
        drawerFt.commit();
        
        // Initialize right drawer fragment
        rightDrawerFragment = new RightDrawerFragment();
        FragmentTransaction rightDrawerFt = getSupportFragmentManager().beginTransaction();
        rightDrawerFt.replace(R.id.right_drawer_container, rightDrawerFragment);
        rightDrawerFt.commit();
        
        // Setup swipe gesture detection for right drawer
        setupSwipeGestureDetection();
        
        // Test fragment detection
        testFragmentDetection();

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
            androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
            
            // Show/hide swipe area based on current fragment
            updateSwipeAreaVisibility(currentFragment);
            
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
                
                // Also hide the entire AppBar for welcome screen
                if (appBar != null) {
                    appBar.setVisibility(android.view.View.GONE);
                    // Remove layout behavior to eliminate spacing
                    android.widget.FrameLayout container = findViewById(R.id.container);
                    if (container != null) {
                        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) container.getLayoutParams();
                        params.setBehavior(null);
                        container.setLayoutParams(params);
                    }
                    android.util.Log.d("MainActivity", "Hiding AppBar and removing layout behavior in BackStackChangedListener for WelcomeFragment");
                }
            } else if (isLoginOrRegister && canBack) {
                // Show back arrow on login/register screens when there's back stack
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
                android.util.Log.d("MainActivity", "Setting navigation icon to back arrow (login/register with back stack)");
                
                // Show AppBar for login/register screens
                if (appBar != null) {
                    appBar.setVisibility(android.view.View.VISIBLE);
                    // Restore layout behavior
                    android.widget.FrameLayout container = findViewById(R.id.container);
                    if (container != null) {
                        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) container.getLayoutParams();
                        params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                        container.setLayoutParams(params);
                    }
                    android.util.Log.d("MainActivity", "Showing AppBar and restoring layout behavior in BackStackChangedListener for Login/Register");
                }
            } else if (isMainChatFragment) {
                // Always show menu icon on main chat fragments, even if there's back stack
                toolbar.setNavigationIcon(R.drawable.menu);
                toolbar.setNavigationOnClickListener(v -> toggleDrawer());
                android.util.Log.d("MainActivity", "Setting navigation icon to menu (chat fragment)");
                
                // Show AppBar for chat fragments
                if (appBar != null) {
                    appBar.setVisibility(android.view.View.VISIBLE);
                    // Restore layout behavior
                    android.widget.FrameLayout container = findViewById(R.id.container);
                    if (container != null) {
                        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) container.getLayoutParams();
                        params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                        container.setLayoutParams(params);
                    }
                    android.util.Log.d("MainActivity", "Showing AppBar and restoring layout behavior in BackStackChangedListener for Chat");
                }
            } else if (canBack) {
                // Show back arrow for other fragments with back stack
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
                toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
                android.util.Log.d("MainActivity", "Setting navigation icon to back arrow (canBack=true)");
                
                // Show AppBar for other fragments with back stack
                if (appBar != null) {
                    appBar.setVisibility(android.view.View.VISIBLE);
                    // Restore layout behavior
                    android.widget.FrameLayout container = findViewById(R.id.container);
                    if (container != null) {
                        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) container.getLayoutParams();
                        params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                        container.setLayoutParams(params);
                    }
                    android.util.Log.d("MainActivity", "Showing AppBar and restoring layout behavior in BackStackChangedListener for other fragments with back stack");
                }
            } else {
                // Show menu icon for other main fragments
                toolbar.setNavigationIcon(R.drawable.menu);
                toolbar.setNavigationOnClickListener(v -> toggleDrawer());
                android.util.Log.d("MainActivity", "Setting navigation icon to menu (default)");
                
                // Show AppBar for other main fragments
                if (appBar != null) {
                    appBar.setVisibility(android.view.View.VISIBLE);
                    // Restore layout behavior
                    android.widget.FrameLayout container = findViewById(R.id.container);
                    if (container != null) {
                        androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                            (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) container.getLayoutParams();
                        params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                        container.setLayoutParams(params);
                    }
                    android.util.Log.d("MainActivity", "Showing AppBar and restoring layout behavior in BackStackChangedListener for other main fragments");
                }
            }
            toolbar.setTitle("IRC");
            updateUiForTopFragment();
        });

        // Load WelcomeFragment when app starts or restore fragment after recreate
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container, new WelcomeFragment())
                    .runOnCommit(this::updateUiForTopFragment)
                    .commit();
        } else {
            // After recreate (e.g., theme change), ensure toolbar is updated
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                updateUiForTopFragment();
                forceUpdateToolbar();
            }, 100);
        }

        // 4Request notification permission (Android 13+)
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
        // Use default server and channel for backward compatibility
        String defaultServer = "irc.libera.chat";
        String defaultChannel = "#usth-ircui";
        
        // Save username, server, and current channel to SharedPreferences for later use
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit()
                .putString("current_username", username)
                .putString("current_server", defaultServer)
                .putString("current_channel", defaultChannel)
                .apply();
        
        // Update right drawer with new user info
        if (rightDrawerFragment != null) {
            rightDrawerFragment.updateCurrentUserInfo(username, defaultServer);
        }
        
        // Update channel list to include current channel
        updateCurrentChannel(defaultChannel);
        
        ChatFragment chatFragment = ChatFragment.newInstance(username, defaultServer, defaultChannel);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, chatFragment);
        // Clear history (can't go back to login/welcome)
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        ft.runOnCommit(() -> {
            updateUiForTopFragment();
            // Force update toolbar after a short delay to ensure fragment is fully loaded
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                forceUpdateToolbar();
            }, 100);
        });
        ft.commit();
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
        
        // Update right drawer with new user info
        if (rightDrawerFragment != null) {
            rightDrawerFragment.updateCurrentUserInfo(username, serverHost);
        }
        
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

    // MENU & ACTIONS

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu if needed
        // getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_right_drawer) {
            // Open right drawer
            openRightDrawer();
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

    public void updateUiForTopFragment() {
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        
        // Handle language button visibility (only if btnLanguage exists)
        if (btnLanguage != null) {
            boolean onLogin = f instanceof LoginFragment;
            btnLanguage.setVisibility(onLogin ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        // Disable drawer on auth screens (welcome, login, register)
        boolean isAuthScreen = f instanceof WelcomeFragment || 
                             f instanceof LoginFragment || 
                             f instanceof RegisterFragment;
        
        if (drawerLayout != null) {
            drawerLayout.setDrawerLockMode(isAuthScreen ? 
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED : 
                DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        
        // Hide/show AppBar based on current fragment
        com.google.android.material.appbar.AppBarLayout appBar = findViewById(R.id.appbar);
        android.widget.FrameLayout container = findViewById(R.id.container);
        
        if (appBar != null && container != null) {
            if (f instanceof WelcomeFragment) {
                // Hide AppBar completely on welcome screen
                appBar.setVisibility(android.view.View.GONE);
                // Remove layout behavior to eliminate spacing
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                    (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) container.getLayoutParams();
                params.setBehavior(null);
                container.setLayoutParams(params);
                android.util.Log.d("MainActivity", "Hiding AppBar and removing layout behavior for WelcomeFragment");
            } else {
                // Show AppBar for all other screens
                appBar.setVisibility(android.view.View.VISIBLE);
                // Restore layout behavior
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                    (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) container.getLayoutParams();
                params.setBehavior(new com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior());
                container.setLayoutParams(params);
                android.util.Log.d("MainActivity", "Showing AppBar and restoring layout behavior for " + (f != null ? f.getClass().getSimpleName() : "null"));
            }
        }
    }
    
    private void forceUpdateToolbar() {
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        Toolbar toolbar = findViewById(R.id.toolbar);
        
        if (toolbar == null || currentFragment == null) return;
        
        // Check fragment types
        boolean isWelcomeScreen = currentFragment instanceof WelcomeFragment;
        boolean isLoginOrRegister = currentFragment instanceof LoginFragment || 
                                  currentFragment instanceof RegisterFragment;
        boolean isMainChatFragment = currentFragment instanceof ChatFragment || 
                                   currentFragment instanceof ChannelMessageFragment;
        boolean isSettingsFragment = currentFragment instanceof SettingsFragment;
        boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        
        if (isWelcomeScreen) {
            // No navigation icon on welcome screen
            toolbar.setNavigationIcon(null);
            toolbar.setNavigationOnClickListener(null);
            android.util.Log.d("MainActivity", "Force updated toolbar - no icon for welcome screen");
        } else if (isLoginOrRegister && canBack) {
            // Show back arrow on login/register screens when there's back stack
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            android.util.Log.d("MainActivity", "Force updated toolbar - back arrow for login/register with back stack");
        } else if (isMainChatFragment) {
            // Always show menu icon on main chat fragments
            toolbar.setNavigationIcon(R.drawable.menu);
            toolbar.setNavigationOnClickListener(v -> toggleDrawer());
            android.util.Log.d("MainActivity", "Force updated toolbar - menu icon for chat fragment");
        } else if (isSettingsFragment || canBack) {
            // Show back arrow for settings or other fragments with back stack
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
            android.util.Log.d("MainActivity", "Force updated toolbar - back arrow for settings or other fragments with back stack");
        } else {
            // Show menu icon for other main fragments
            toolbar.setNavigationIcon(R.drawable.menu);
            toolbar.setNavigationOnClickListener(v -> toggleDrawer());
            android.util.Log.d("MainActivity", "Force updated toolbar - menu icon for other main fragments");
        }
        
        toolbar.setTitle("IRC");
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
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.confirm_logout))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    // Reset SharedPreferences to Guest
                    resetUserPreferencesToGuest();
                    
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, new WelcomeFragment())
                            .runOnCommit(this::updateUiForTopFragment)
                            .commit();
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
    
    private void resetUserPreferencesToGuest() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit()
                .putString("current_username", getString(R.string.guest))
                .putString("current_server", "irc.libera.chat")
                .putString("current_channel", "#usth-ircui")
                .apply();
        
        // Update right drawer to show Guest
        if (rightDrawerFragment != null) {
            rightDrawerFragment.updateCurrentUserInfo(getString(R.string.guest), "irc.libera.chat");
        }
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
    
    // Right drawer methods
    public void toggleRightDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }
    
    public void closeRightDrawer() {
        drawerLayout.closeDrawer(GravityCompat.END);
    }
    
    public void openRightDrawer() {
        android.util.Log.d("MainActivity", "Opening right drawer...");
        drawerLayout.openDrawer(GravityCompat.END);
    }
    
    public ChannelListFragment getChannelListFragment() {
        return channelListFragment;
    }
    
    public RightDrawerFragment getRightDrawerFragment() {
        return rightDrawerFragment;
    }
    
    private void updateSwipeAreaVisibility(androidx.fragment.app.Fragment currentFragment) {
        LinearLayout swipeArea = findViewById(R.id.right_swipe_area);
        if (swipeArea == null) return;
        
        String fragmentName = currentFragment != null ? currentFragment.getClass().getSimpleName() : "null";
        
        // Show swipe area only for chat-related screens
        boolean isChatScreen = currentFragment != null && 
            (currentFragment instanceof ChatFragment || 
             currentFragment instanceof GroupChatFragment ||
             currentFragment instanceof DirectMessageFragment ||
             currentFragment instanceof ChannelMessageFragment ||
             fragmentName.contains("Chat") ||
             fragmentName.contains("Channel"));
        
        android.util.Log.d("MainActivity", "Update swipe area - Fragment: " + fragmentName + ", isChatScreen: " + isChatScreen);
        
        swipeArea.setVisibility(isChatScreen ? android.view.View.VISIBLE : android.view.View.GONE);
    }
    
    private void testFragmentDetection() {
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        String fragmentName = currentFragment != null ? currentFragment.getClass().getSimpleName() : "null";
        android.util.Log.d("MainActivity", "INITIAL FRAGMENT DETECTION - Fragment: " + fragmentName);
        
        // Also log all fragments in backstack
        int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
        android.util.Log.d("MainActivity", "BackStack count: " + backStackCount);
    }
    
    private void setupSwipeGestureDetection() {
        // Create GestureDetector for better swipe detection
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                // Only enable swipe gesture for chat screens
                androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
                String fragmentName = currentFragment != null ? currentFragment.getClass().getSimpleName() : "null";
                
                boolean isChatScreen = currentFragment != null && 
                    (currentFragment instanceof ChatFragment || 
                     currentFragment instanceof GroupChatFragment ||
                     currentFragment instanceof DirectMessageFragment ||
                     currentFragment instanceof ChannelMessageFragment ||
                     fragmentName.contains("Chat") ||
                     fragmentName.contains("Channel"));
                
                android.util.Log.d("MainActivity", "GestureDetector - Fragment: " + fragmentName + ", isChatScreen: " + isChatScreen);
                
                if (!isChatScreen) {
                    return false; // Don't handle swipe for non-chat screens
                }
                
                float deltaX = e2.getX() - e1.getX();
                float deltaY = e2.getY() - e1.getY();
                
                android.util.Log.d("MainActivity", "Fling detected - deltaX: " + deltaX + ", deltaY: " + deltaY + ", velocityX: " + velocityX);
                
                // Check if it's a left swipe (from right to left)
                if (deltaX < -100 && Math.abs(deltaY) < 500 && velocityX < -100) {
                    android.util.Log.d("MainActivity", "Left swipe detected! Opening right drawer...");
                    
                    // Open right drawer with haptic feedback
                    openRightDrawer();
                    return true;
                }
                
                return false;
            }
        });
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Only handle swipe for chat screens
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container);
        String fragmentName = currentFragment != null ? currentFragment.getClass().getSimpleName() : "null";
        
        boolean isChatScreen = currentFragment != null && 
            (currentFragment instanceof ChatFragment || 
             currentFragment instanceof GroupChatFragment ||
             currentFragment instanceof DirectMessageFragment ||
             currentFragment instanceof ChannelMessageFragment ||
             fragmentName.contains("Chat") ||
             fragmentName.contains("Channel"));
        
        if (isChatScreen && gestureDetector != null) {
            android.util.Log.d("MainActivity", "dispatchTouchEvent - Fragment: " + fragmentName + ", isChatScreen: " + isChatScreen);
            if (gestureDetector.onTouchEvent(ev)) {
                return true; // Swipe gesture was handled
            }
        }
        
        // Let other views handle the touch event normally
        return super.dispatchTouchEvent(ev);
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
        
        // Update right drawer with new user info
        if (rightDrawerFragment != null) {
            rightDrawerFragment.updateCurrentUserInfo(username, serverHost);
        }
        
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

    // Shortcut method: allows RegisterFragment to send user back to LoginFragment
    public void navigateToLoginFragment(){
        LoginFragment loginFragment = new LoginFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, loginFragment)
                .commit();
    }

    // Play startup sound effect
    private void playStartupSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.startup_sfx);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error playing startup sound", e);
        }
    }
}
