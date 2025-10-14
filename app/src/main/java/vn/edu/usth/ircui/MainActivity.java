package vn.edu.usth.ircui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import vn.edu.usth.ircui.feature_chat.data.MessageNotification;


public class MainActivity extends AppCompatActivity {

    // ... (phần code cũ giữ nguyên)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            // Bắt đầu với WelcomeFragment
            ft.replace(R.id.container, new WelcomeFragment());
            ft.commit();
        }

        // ... (phần xin quyền notification giữ nguyên)
    }

    // ... (phần onRequestPermissionResult giữ nguyên)

    /**
     * NEW: Phương thức công khai để chuyển sang màn hình Chat.
     * Các fragment (Login, Register) sẽ gọi phương thức này.
     * @param username Tên người dùng để truyền cho ChatFragment.
     */
    public void navigateToChatFragment(String username) {
        ChatFragment chatFragment = ChatFragment.newInstance(username);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Thay thế fragment hiện tại bằng ChatFragment
        ft.replace(R.id.container, chatFragment);
        // Xóa tất cả các fragment trước đó khỏi back stack để người dùng không quay lại màn hình login/register
        getSupportFragmentManager().popBackStack(null, getSupportFragmentManager().POP_BACK_STACK_INCLUSIVE);
        ft.commit();
    }
}