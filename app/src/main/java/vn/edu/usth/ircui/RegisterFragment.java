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

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {
    private FirebaseFirestore db;
    private Button btnRegister;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        db = FirebaseFirestore.getInstance();

        EditText etNickname = view.findViewById(R.id.et_register_nickname);
        EditText etUsername = view.findViewById(R.id.et_register_username);
        EditText etPassword = view.findViewById(R.id.et_register_password);
        EditText etVerifyPassword = view.findViewById(R.id.et_register_verify_password);

        btnRegister = view.findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            // All field required
            String nickname = etNickname.getText().toString().trim();
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String verifyPassword = etVerifyPassword.getText().toString().trim();


            // === Validation check ===

            // check empty field
            if (TextUtils.isEmpty(nickname) || TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(verifyPassword)) {
                Toast.makeText(getContext(), "Please fill all the blank", Toast.LENGTH_SHORT).show();
                return;
            }

            // check valid username
            if(username.length() < 6){
                Toast.makeText(getContext(), "Username must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!username.matches("^[a-zA-Z0-9]+$")) {
                Toast.makeText(getContext(), "Username can only contain letters and numbers", Toast.LENGTH_SHORT).show();
                return;
            }

            // check valid password
            if (password.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.matches(".*[A-Z].*")) {
                Toast.makeText(getContext(), "Password must contain at least uppercase letter", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.matches(".*[a-z].*")) {
                Toast.makeText(getContext(), "Password must contain at least lowercase letter", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.matches(".*[0-9].*")) {
                Toast.makeText(getContext(), "Password must contain at least number", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.matches(".*[!@#$%^&*].*")) {
                Toast.makeText(getContext(), "Password must contain at least special symbol (!@#$%^&*)", Toast.LENGTH_SHORT).show();
                return;
            }

            // check verify password
            if(!password.equals(verifyPassword)) {
                Toast.makeText(getContext(), "Verify password is different to the password", Toast.LENGTH_SHORT).show();
                return;
            }

            // create account for user after check all
            checkUsername(username, password, nickname);
        });

        return view;
    }

    // check existed account
    private void checkUsername(String username, String password, String nickname){
        // anti spam click button
        btnRegister.setEnabled(false);

        db.collection("Users").document(username).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if(document.exists()){      // username existed
                            Log.d("Firestore", "Username existed");
                            Toast.makeText(getContext(), "Username existed", Toast.LENGTH_SHORT).show();
                        } else {
                            // username gud to go
                            Log.d("Firestore", "Username ok");
                            createUser(username, password, nickname);
                        }
                    }
                    else{
                        // Other errors (No internet, etc...)
                        Log.w("Firestore", "Error check username", task.getException());
                        Toast.makeText(getContext(), "Can't check username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUser(String username, String password, String nickname){
        // create user object to store in Firestore
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("password", password);

        // save account to Users in db with username as UID
        db.collection("Users").document(username)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "User account for "+ username + " created");
                    Toast.makeText(getContext(), "Register success", Toast.LENGTH_SHORT).show();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).navigateToLoginFragment();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error creating user", e);
                    Toast.makeText(getContext(), "Register error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
