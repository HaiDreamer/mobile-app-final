package vn.edu.usth.ircui;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;

public class FirebaseInit extends Application{
    @Override
    public void onCreate(){
        super.onCreate();
        
        // Initialize theme before any UI is created
        initializeAppTheme();
        
        // init Firebase SDK when app start
        FirebaseApp.initializeApp(this);
    }
    
    private void initializeAppTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        int themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
}
