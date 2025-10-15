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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        EditText etNickname = view.findViewById(R.id.et_register_nickname);
        // ... (các EditText khác)

        Button btnRegister = view.findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();

            // Kiểm tra xem nickname có được nhập hay không
            if (TextUtils.isEmpty(nickname)) {
                Toast.makeText(getContext(), "Please enter a nickname", Toast.LENGTH_SHORT).show();
                return;
            }

            // Thêm logic kiểm tra các trường khác nếu cần...

            // MODIFIED: Gọi phương thức trong MainActivity để chuyển màn hình
            // Lấy ra Activity đang chứa Fragment này và gọi phương thức của nó.
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToChooseServer(nickname);
            }
        });

        return view;
    }
}