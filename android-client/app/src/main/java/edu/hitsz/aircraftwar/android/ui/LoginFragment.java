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

public class LoginFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        root.setGravity(android.view.Gravity.CENTER);

        var title = UiUtils.createTitle(requireContext(), "联网版飞机大战");
        var body = UiUtils.createBody(requireContext(), "先完成本地 Android 迁移骨架，后续再接好友、房间和排行榜接口。");
        EditText usernameInput = new EditText(requireContext());
        usernameInput.setHint("输入昵称");
        usernameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        usernameInput.setText("Player");
        usernameInput.setTextColor(0xFF111111);
        usernameInput.setHintTextColor(0xFF666666);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.topMargin = UiUtils.dp(requireContext(), 20);
        usernameInput.setLayoutParams(inputParams);

        var loginButton = UiUtils.createActionButton(requireContext(), "进入主菜单");
        loginButton.setOnClickListener(view ->
                ((MainActivity) requireActivity()).onLoginSubmitted(usernameInput.getText().toString())
        );

        root.addView(title);
        root.addView(body);
        root.addView(usernameInput);
        root.addView(loginButton);
        return root;
    }
}
