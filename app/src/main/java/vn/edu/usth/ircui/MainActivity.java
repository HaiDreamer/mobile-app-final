package vn.edu.usth.ircui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

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
    private String currentLang = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== Toolbar & AppBar edge-to-edge fix =====
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Độn đỉnh AppBar bằng chiều cao status bar / camera cutout
        AppBarLayout appBar = findViewById(R.id.appbar);
        ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    sb.top,                 // quan trọng: tránh title chui vào camera
                    v.getPaddingRight(),
                    v.getPaddingBottom()
            );
            return insets; // không consume để phần dưới vẫn tự xử lý insets
        });

        // Hiện/ẩn nút Back khi có/không có back stack
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            if (canBack) {
                // Dùng icon back của bạn (ic_arrow_back_24). Nếu chưa có, thay bằng icon khác tùy ý.
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24);
                toolbar.setNavigationOnClickListener(v ->
                        getOnBackPressedDispatcher().onBackPressed()
                );
            } else {
                toolbar.setNavigationIcon(null);
                toolbar.setNavigationOnClickListener(null);
                toolbar.setTitle("IRC UI"); // tiêu đề mặc định
            }
        });

        // ===== Màn hình đầu tiên: Login =====
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new LoginFragment())
                    .commit();
        }

        /*App notification permission
        * When app started -> ask for notification permission
        * Agree -> a welcome notification appear
        * Tap the notification -> open the app */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            } else {
                MessageNotification.showMsgNotification(
                        this,
                        "System",
                        "Welcome to IRC"
                );
            }
        }

        // Change Language Button
        Button btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) {
            // Set button text based on current language
            String current = getResources().getConfiguration().getLocales().get(0).getLanguage();
            btnLanguage.setText(current.equals("en") ? "EN" : "VI");

            btnLanguage.setOnClickListener(v -> {
                // Get current language
                String lang = getResources().getConfiguration().getLocales().get(0).getLanguage();
                String newLang = lang.equals("en") ? "vi" : "en";

                // Change language
                LocaleHelper.setLocale(MainActivity.this, newLang);

                // Update button text
                btnLanguage.setText(newLang.equals("en") ? "EN" : "VI");

                recreate();
            });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResult){
        super.onRequestPermissionsResult(requestCode, permission, grantResult);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION){
            if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED){
                MessageNotification.showMsgNotification(
                        this,
                        "System",
                        "Notification permission granted!"
                );
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Helper: điều hướng sang màn chat
    public void navigateToChat() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new ChatFragment())
                .addToBackStack(null)
                .commit();
    }
}
