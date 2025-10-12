package vn.edu.usth.irc;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import vn.edu.usth.irc.group.GroupChatFragment;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawer;
    private BottomNavigationView bottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_IRC);
        setContentView(R.layout.activity_main);

        // === Layout views ===
        drawer = findViewById(R.id.drawer);
        bottom = findViewById(R.id.bottomBar);
        NavigationView nav = findViewById(R.id.navChannels);

        // === Default fragment (Private message list) ===
        replace(new MessageListFragment());

        // === Bottom Navigation actions ===
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                replace(PlaceholderHomeFragment.newInstance());
                return true;
            } else if (id == R.id.nav_message) {
                replace(new MessageListFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                replace(new SettingsFragment());
                return true;
            }
            return false;
        });

        // === Channel Drawer menu actions (#topic) ===
        nav.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            drawer.closeDrawer(GravityCompat.START);

            String chId = "chat";
            String chName = "#Chat";

            if (id == R.id.ch_chat) {
                chId = "chat";
                chName = "#Chat";
            } else if (id == R.id.ch_java) {
                chId = "java";
                chName = "#Java";
            } else if (id == R.id.ch_ubuntu) {
                chId = "ubuntu";
                chName = "#Ubuntu";
            } else if (id == R.id.ch_cpp) {
                chId = "cpp";
                chName = "#C/C++";
            } else if (id == R.id.ch_anime) {
                chId = "anime";
                chName = "#Anime";
            } else if (id == R.id.ch_add) {
                // TODO: Dialog tạo kênh mới (future)
                return true;
            }

            // Mở group chat fragment tương ứng
            replace(GroupChatFragment.newInstance(chId, chName));
            return true;
        });
    }

    // === Mở drawer từ Fragment khác (nếu cần) ===
    public void openDrawer() {
        drawer.openDrawer(GravityCompat.START);
    }

    // === Hàm thay fragment ===
    private void replace(Fragment f) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragmentContainer, f);
        ft.commit();
    }
}
