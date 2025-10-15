package vn.edu.usth.ircui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RegisterFragment extends Fragment {

    private EditText etNickname;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etVerifyPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        etNickname = view.findViewById(R.id.et_register_nickname);
        etUsername = view.findViewById(R.id.et_register_username);
        etPassword = view.findViewById(R.id.et_register_password);
        etVerifyPassword = view.findViewById(R.id.et_register_verify_password);

        Button btnRegister = view.findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String nickname = etNickname.getText() != null ? etNickname.getText().toString().trim() : "";
            String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String verify = etVerifyPassword.getText() != null ? etVerifyPassword.getText().toString().trim() : "";

            if (TextUtils.isEmpty(nickname)) {
                Toast.makeText(getContext(), "Please enter a nickname", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(username)) {
                Toast.makeText(getContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter a password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(verify)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Navigate to choose server using nickname as IRC nick
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToChooseServer(nickname);
            }
        });

        return view;
    }
}