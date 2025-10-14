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

public class LoginFragment extends Fragment {

    // MODIFIED: Chỉ khai báo những biến cần thiết
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // MODIFIED: Ánh xạ tới các ID mới và đơn giản hơn
        etUsername = view.findViewById(R.id.et_login_username);
        etPassword = view.findViewById(R.id.et_login_password);
        btnLogin = view.findViewById(R.id.btn_login);

        // Giờ bạn có thể sử dụng các biến này một cách an toàn
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim(); // Có thể lấy pass để xác thực

            if (TextUtils.isEmpty(username)) {
                Toast.makeText(getContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi phương thức trong MainActivity để chuyển sang màn hình Chat
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToChatFragment(username);
            }
        });

        return view;
    }
}