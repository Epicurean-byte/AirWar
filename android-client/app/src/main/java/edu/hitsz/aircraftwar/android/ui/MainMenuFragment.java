package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.game.core.mode.Difficulty;

public class MainMenuFragment extends Fragment {
    private static final String ARG_PLAYER_NAME = "player_name";

    public static MainMenuFragment newInstance(String playerName) {
        Bundle args = new Bundle();
        args.putString(ARG_PLAYER_NAME, playerName);
        MainMenuFragment fragment = new MainMenuFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        String playerName = getArguments() == null ? "Player" : getArguments().getString(ARG_PLAYER_NAME, "Player");

        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        var title = UiUtils.createTitle(requireContext(), "主菜单");
        var welcome = UiUtils.createBody(requireContext(), "当前玩家: " + playerName);
        MainActivity activity = (MainActivity) requireActivity();

        Spinner difficultySpinner = new Spinner(requireContext());
        difficultySpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"EASY", "NORMAL", "HARD"}
        ));

        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        spinnerParams.topMargin = UiUtils.dp(requireContext(), 20);
        difficultySpinner.setLayoutParams(spinnerParams);

        LinearLayout audioPanel = UiUtils.createPanel(requireContext(), 14);
        audioPanel.addView(UiUtils.createSectionTitle(requireContext(), "音频"));
        TextView audioCaption = UiUtils.createCaption(requireContext(), "打开后启用背景音乐与全部音效，关闭后全局静音。");
        UiUtils.setTopMargin(audioCaption, requireContext(), 6);
        audioPanel.addView(audioCaption);
        LinearLayout audioRow = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(audioRow, requireContext(), 12);
        TextView audioLabel = UiUtils.createBody(requireContext(), "音乐与音效");
        SwitchCompat audioSwitch = new SwitchCompat(requireContext());
        audioSwitch.setChecked(activity.isAudioEnabled());
        audioSwitch.setText(audioSwitch.isChecked() ? "已开启" : "已关闭");
        audioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.setAudioEnabled(isChecked);
            audioSwitch.setText(isChecked ? "已开启" : "已关闭");
        });
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        audioLabel.setLayoutParams(labelParams);
        audioRow.addView(audioLabel);
        audioRow.addView(audioSwitch);
        audioPanel.addView(audioRow);

        var startButton = UiUtils.createActionButton(requireContext(), "开始单机战斗");
        startButton.setOnClickListener(view -> {
            String selected = String.valueOf(difficultySpinner.getSelectedItem());
            ((MainActivity) requireActivity()).showGame(Difficulty.valueOf(selected));
        });

        var friendsButton = UiUtils.createActionButton(requireContext(), "好友");
        friendsButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showFriends());

        var shopButton = UiUtils.createActionButton(requireContext(), "商城");
        shopButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showShop());

        var warehouseButton = UiUtils.createActionButton(requireContext(), "仓库");
        warehouseButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showWarehouse());

        var leaderboardButton = UiUtils.createActionButton(requireContext(), "排行榜");
        leaderboardButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showLeaderboard());

        var roomsButton = UiUtils.createActionButton(requireContext(), "对战房间");
        roomsButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showRooms());

        var settingsButton = UiUtils.createActionButton(requireContext(), "设置");
        settingsButton.setOnClickListener(view -> ((MainActivity) requireActivity()).showSettings());

        var logoutButton = UiUtils.createActionButton(requireContext(), "退出登录");
        logoutButton.setOnClickListener(view -> ((MainActivity) requireActivity()).logoutAndBackToLogin());

        root.addView(title);
        root.addView(welcome);
        root.addView(audioPanel);
        root.addView(difficultySpinner);
        root.addView(startButton);
        root.addView(friendsButton);
        root.addView(shopButton);
        root.addView(warehouseButton);
        root.addView(leaderboardButton);
        root.addView(roomsButton);
        root.addView(settingsButton);
        root.addView(logoutButton);
        return root;
    }
}
