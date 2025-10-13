package vn.edu.usth.ircui;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class ChatActivity extends AppCompatActivity {
    protected void conCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_main);

        if (saveInstanceState == null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new ChatFragment())
                    .commit();
        }
    }
}
