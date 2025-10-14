package vn.edu.usth.ircui;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class FirebaseInit extends Application{
    @Override
    public void onCreate(){
        super.onCreate();
        // init Firebase SDK when app start
        FirebaseApp.initializeApp(this);
    }
}
