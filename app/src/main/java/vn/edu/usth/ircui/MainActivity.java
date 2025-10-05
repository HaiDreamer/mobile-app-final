package vn.edu.usth.ircui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

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
    }

    // Helper: điều hướng sang màn chat
    public void navigateToChat() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new ChatFragment())
                .addToBackStack(null)
                .commit();
    }
}
