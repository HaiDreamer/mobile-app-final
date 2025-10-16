package vn.edu.usth.ircui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private FirebaseFirestore db;

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // initial Firestore
        db = FirebaseFirestore.getInstance();

        etUsername = view.findViewById(R.id.et_login_username);
        etPassword = view.findViewById(R.id.et_login_password);
        btnLogin = view.findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please fill all blank", Toast.LENGTH_SHORT).show();
                return;
            }

            // Anti spam click button
            btnLogin.setEnabled(false);
            loginUser(username, password);
        });

        return view;
    }

    private void loginUser(String username, String password){
        db.collection("Users").document(username).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();

                        if(document.exists()){     // existed account
                            String storedPassword = document.getString("password");

                            if(password.equals(storedPassword)){    // login success
                                Log.d("Firestore", "Login success");
                                Toast.makeText(getContext(), "Login success", Toast.LENGTH_SHORT).show();

                                // go to choose server screen
                                if(getActivity() instanceof MainActivity){
                                    ((MainActivity) getActivity()).navigateToChooseServer(username);
                                }
                            }
                            else{   // wrong password
                                Log.d("Firestore", "Wrong password");
                                Toast.makeText(getContext(), "Wrong password", Toast.LENGTH_SHORT).show();
                                btnLogin.setEnabled(true);
                            }
                        }
                        else{   // account not found
                            Log.d("Firestore", "Can't find account");
                            Toast.makeText(getContext(), "Account not found", Toast.LENGTH_SHORT).show();
                            btnLogin.setEnabled(true);  // re-enable button
                        }
                    }
                    else{   // error (No internet, etc...)
                        Log.w("Firestore", "Can't check", task.getException());
                        Toast.makeText(getContext(), "Login failed", Toast.LENGTH_SHORT).show();
                        btnLogin.setEnabled(true);  // re-enable button
                    }
                });
    }
}