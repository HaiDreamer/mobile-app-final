package vn.edu.usth.ircui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class UserInfoFullscreenDialog extends DialogFragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_CURRENT_USER = "current_user";
    private static final String ARG_SERVER_HOST = "server_host";
    private static final String ARG_IS_CURRENT_USER = "is_current_user";

    private String username;
    private String currentUser;
    private String serverHost;
    private boolean isCurrentUser;

    public static UserInfoFullscreenDialog newInstance(String username, String currentUser, String serverHost, boolean isCurrentUser) {
        UserInfoFullscreenDialog dialog = new UserInfoFullscreenDialog();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_CURRENT_USER, currentUser);
        args.putString(ARG_SERVER_HOST, serverHost);
        args.putBoolean(ARG_IS_CURRENT_USER, isCurrentUser);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogTheme);
        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME);
            currentUser = getArguments().getString(ARG_CURRENT_USER);
            serverHost = getArguments().getString(ARG_SERVER_HOST);
            isCurrentUser = getArguments().getBoolean(ARG_IS_CURRENT_USER);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        // Set dialog to take 3/4 of screen from bottom
        Window window = dialog.getWindow();
        if (window != null) {
            // Get screen height and set dialog height to 3/4
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;
            int dialogHeight = (int) (screenHeight * 0.75); // 3/4 of screen height
            
            // Set layout parameters
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = dialogHeight;
            params.gravity = android.view.Gravity.BOTTOM;
            params.y = 0; // Start from bottom
            params.dimAmount = 0.5f; // Add background dimming
            window.setAttributes(params);
            
            // Enable dimming
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_user_info_fullscreen, container, false);
        
        setupUserInfo(view);
        setupButtons(view);
        setupCloseButton(view);
        
        return view;
    }

    private void setupUserInfo(View view) {
        TextView usernameText = view.findViewById(R.id.username_text);
        TextView statusText = view.findViewById(R.id.status_text);
        TextView serverText = view.findViewById(R.id.server_text);
        TextView joinDateText = view.findViewById(R.id.join_date_text);
        TextView activityText = view.findViewById(R.id.activity_text);
        ImageView userAvatar = view.findViewById(R.id.user_avatar);
        
        usernameText.setText(username);
        statusText.setText(getString(R.string.online));
        serverText.setText(serverHost);
        joinDateText.setText(getString(R.string.today));
        activityText.setText(getString(R.string.active_now));
        
        // Set different avatar for current user
        if (isCurrentUser) {
            userAvatar.setImageResource(R.drawable.ic_person);
        } else {
            userAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    private void setupButtons(View view) {
        ImageButton chatButton = view.findViewById(R.id.chat_button);
        LinearLayout buttonContainer = view.findViewById(R.id.button_container);
        LinearLayout buttonLabels = view.findViewById(R.id.button_labels);
        
        if (isCurrentUser) {
            // Hide all buttons for current user
            buttonContainer.setVisibility(View.GONE);
            buttonLabels.setVisibility(View.GONE);
        } else {
            // Show only chat button for other users
            
            // Center the chat button
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) chatButton.getLayoutParams();
            params.gravity = android.view.Gravity.CENTER;
            chatButton.setLayoutParams(params);
            
            chatButton.setOnClickListener(v -> {
                // Navigate to DirectMessageFragment
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    
                    // Close the dialog first
                    dismiss();
                    
                    // Navigate to direct message
                    mainActivity.getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.container, 
                                    vn.edu.usth.ircui.feature_chat.ui.DirectMessageFragment.newInstance(currentUser, username, serverHost))
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
    }

    private void setupCloseButton(View view) {
        ImageButton closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> dismiss());
    }
}
