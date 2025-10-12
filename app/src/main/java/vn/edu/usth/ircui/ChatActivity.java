package vn.edu.usth.ircui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        // Your updated layout file name stayed "activity_main", and it contains a view @id/fragmentContainer
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null){
            getSupportFragmentManager()
                    .beginTransaction()
                    // Use the new container id from the updated activity_main.xml
                    .replace(R.id.fragmentContainer, new ChatFragment())
                    .commit();
        }
    }
}
