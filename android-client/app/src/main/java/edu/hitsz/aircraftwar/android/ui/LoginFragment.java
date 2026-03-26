package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;
import edu.hitsz.aircraftwar.android.network.model.UserProfile;

public class LoginFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        root.setGravity(android.view.Gravity.CENTER);

        var title = UiUtils.createTitle(requireContext(), "联网飞机大战");
        var body = UiUtils.createBody(requireContext(), "输入账号密码后登录，若账号不存在可直接注册。");

        EditText usernameInput = new EditText(requireContext());
        usernameInput.setHint("用户名");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setTextColor(0xFF111111);
        usernameInput.setHintTextColor(0xFF666666);

        EditText passwordInput = new EditText(requireContext());
        passwordInput.setHint("密码");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setTextColor(0xFF111111);
        passwordInput.setHintTextColor(0xFF666666);

        EditText nicknameInput = new EditText(requireContext());
        nicknameInput.setHint("昵称(注册时使用)");
        nicknameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        nicknameInput.setTextColor(0xFF111111);
        nicknameInput.setHintTextColor(0xFF666666);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = UiUtils.dp(requireContext(), 14);
        usernameInput.setLayoutParams(inputParams);
        passwordInput.setLayoutParams(inputParams);
        nicknameInput.setLayoutParams(inputParams);

        var loginButton = UiUtils.createActionButton(requireContext(), "登录");
        loginButton.setOnClickListener(view -> doAuth(false, usernameInput, passwordInput, nicknameInput));

        var registerButton = UiUtils.createActionButton(requireContext(), "注册");
        registerButton.setOnClickListener(view -> doAuth(true, usernameInput, passwordInput, nicknameInput));

        root.addView(title);
        root.addView(body);
        root.addView(usernameInput);
        root.addView(passwordInput);
        root.addView(nicknameInput);
        root.addView(loginButton);
        root.addView(registerButton);
        return root;
    }

    private void doAuth(boolean register,
                        EditText usernameInput,
                        EditText passwordInput,
                        EditText nicknameInput) {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String nickname = nicknameInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            ((MainActivity) requireActivity()).toast("用户名和密码不能为空");
            return;
        }

        NetworkExecutor.run(() -> {
            try {
                MainActivity activity = (MainActivity) requireActivity();
                UserProfile user = register
                        ? activity.getApiClient().register(username, password, nickname.isEmpty() ? username : nickname)
                        : activity.getApiClient().login(username, password);
                activity.runOnUiThread(() -> activity.onUserAuthenticated(user));
            } catch (Exception e) {
                ((MainActivity) requireActivity()).toast(e.getMessage() == null ? "请求失败" : e.getMessage());
            }
        });
    }
}
