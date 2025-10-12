package vn.edu.usth.ircui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import vn.edu.usth.ircui.feature_chat.ui.GroupChatFragment;

/**
 * Uses the UPDATED activity_main.xml with DrawerLayout + BottomNavigation.
 * Default screen is the NEW LoginFragment (auth UI).
 */
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

        // === Default fragment: show Login (new UI) ===
        if (savedInstanceState == null) {
            replace(new LoginFragment());
        }

        // === Bottom Navigation actions ===
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // placeholder home could be shown here if needed
                return true;
            } else if (id == R.id.nav_message) {
                // A messages hub could be placed here
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
                chId = "chat";  chName = "#Chat";
            } else if (id == R.id.ch_java) {
                chId = "java";  chName = "#Java";
            } else if (id == R.id.ch_ubuntu) {
                chId = "ubuntu"; chName = "#Ubuntu";
            } else if (id == R.id.ch_cpp) {
                chId = "cpp";    chName = "#C/C++";
            } else if (id == R.id.ch_anime) {
                chId = "anime";  chName = "#Anime";
            } else if (id == R.id.ch_add) {
                // TODO: Dialog tạo kênh mới (future)
                return true;
            }

            replace(GroupChatFragment.newInstance(chId, chName));
            return true;
        });
    }

    public void openDrawer() {
        drawer.openDrawer(GravityCompat.START);
    }

    private void replace(Fragment f) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragmentContainer, f);
        ft.commit();
    }
}
