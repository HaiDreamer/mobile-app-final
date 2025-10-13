package vn.edu.usth.ircui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;

import vn.edu.usth.ircui.feature_chat.data.MessageNotification;
import vn.edu.usth.ircui.feature_user.LocaleHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    public String currentLang = "en";

    private Button btnLanguage; // keep a reference so we can show/hide it

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== Toolbar & AppBar edge-to-edge fix =====
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

        // Back button icon only when there’s something in back stack
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
            // also refresh button visibility when stack changes
            updateUiForTopFragment();
        });

        // ===== First screen: Login =====
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new LoginFragment())
                    .runOnCommit(this::updateUiForTopFragment)
                    .commit();
        }

        // ===== Notification permission (Android 13+) =====
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

        // Language toggle button (only visible on LoginFragment)
        btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) {
            String current = getResources().getConfiguration().getLocales().get(0).getLanguage();
            btnLanguage.setText(current.equals("en") ? "EN" : "VI");

            btnLanguage.setOnClickListener(v -> {
                String lang = getResources().getConfiguration().getLocales().get(0).getLanguage();
                String newLang = lang.equals("en") ? "vi" : "en";
                LocaleHelper.setLocale(MainActivity.this, newLang);
                btnLanguage.setText(newLang.equals("en") ? "EN" : "VI");
                recreate();
            });
        }

        // ensure correct visibility at startup
        updateUiForTopFragment();
    }

    // Show language button only on LoginFragment.
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

    // Navigate to chat and refresh UI right after commit
    public void navigateToChat() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new ChatFragment())
                .addToBackStack(null)
                .runOnCommit(this::updateUiForTopFragment)
                .commit();
    }
}
