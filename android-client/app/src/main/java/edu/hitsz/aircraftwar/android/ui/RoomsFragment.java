package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import edu.hitsz.aircraftwar.android.MainActivity;

public class RoomsFragment extends Fragment {
    private static final String MODE_COOP = "COOP";
    private static final String MODE_PVP = "PVP";

    private TextView connectionView;
    private TextView roomIdView;
    private TextView modeView;
    private TextView roomStateView;
    private TextView player1View;
    private TextView player2View;
    private TextView hintView;
    private TextView errorView;
    private LinearLayout logContainer;
    private EditText roomInput;
    private Button connectButton;
    private Button randomButton;
    private Button createButton;
    private Button joinButton;
    private Button startButton;
    private Button disconnectButton;
    private Button modeCoopButton;
    private Button modePvpButton;

    private long roomId = 0L;
    private long player1Id = 0L;
    private long player2Id = 0L;
    private String selectedMode = MODE_COOP;
    private String roomMode = MODE_COOP;
    private String roomState = "未加入房间";
    private boolean authReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setFillViewport(true);
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        root.addView(UiUtils.createTitle(requireContext(), "联机房间"));
        root.addView(UiUtils.createCaption(requireContext(), "连接服务器后创建房间、输入房间号加入，或进入随机匹配。"));

        root.addView(createStatusPanel());
        root.addView(createModePanel());
        root.addView(createJoinPanel());
        root.addView(createActionPanel());
        root.addView(createLogPanel());

        Button back = UiUtils.createActionButton(requireContext(), "返回主菜单");
        back.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).setWsListener(null);
            ((MainActivity) requireActivity()).showMainMenu();
        });
        root.addView(back);

        renderState();
        return scrollView;
    }

    private View createStatusPanel() {
        LinearLayout panel = UiUtils.createPanel(requireContext());
        panel.addView(UiUtils.createSectionTitle(requireContext(), "房间状态"));

        connectionView = UiUtils.createChip(requireContext(), "未连接", UiUtils.colorNegative());
        panel.addView(connectionView);

        roomIdView = UiUtils.createBody(requireContext(), "房间号: --");
        UiUtils.setTopMargin(roomIdView, requireContext(), 12);
        panel.addView(roomIdView);

        roomStateView = UiUtils.createBody(requireContext(), "状态: 未加入房间");
        panel.addView(roomStateView);

        modeView = UiUtils.createBody(requireContext(), "模式: 合作");
        panel.addView(modeView);

        player1View = UiUtils.createBody(requireContext(), "玩家 1: --");
        UiUtils.setTopMargin(player1View, requireContext(), 10);
        panel.addView(player1View);

        player2View = UiUtils.createBody(requireContext(), "玩家 2: --");
        panel.addView(player2View);

        hintView = UiUtils.createCaption(requireContext(), "");
        UiUtils.setTopMargin(hintView, requireContext(), 10);
        panel.addView(hintView);

        errorView = UiUtils.createCaption(requireContext(), "");
        UiUtils.setTopMargin(errorView, requireContext(), 8);
        panel.addView(errorView);
        return panel;
    }

    private View createModePanel() {
        LinearLayout panel = UiUtils.createPanel(requireContext());
        panel.addView(UiUtils.createSectionTitle(requireContext(), "创建房间模式"));
        panel.addView(UiUtils.createCaption(requireContext(), "模式只在创建房间时生效；加入已有房间会跟随房主的模式。"));

        LinearLayout row = UiUtils.createRow(requireContext());
        UiUtils.setTopMargin(row, requireContext(), 12);
        modeCoopButton = UiUtils.createTabButton(requireContext(), "合作", true);
        modePvpButton = UiUtils.createTabButton(requireContext(), "对战", false);
        modeCoopButton.setOnClickListener(v -> selectMode(MODE_COOP));
        modePvpButton.setOnClickListener(v -> selectMode(MODE_PVP));
        row.addView(modeCoopButton);
        row.addView(modePvpButton);
        panel.addView(row);
        return panel;
    }

    private View createJoinPanel() {
        LinearLayout panel = UiUtils.createPanel(requireContext());
        panel.addView(UiUtils.createSectionTitle(requireContext(), "指定房间"));

        roomInput = UiUtils.createTextInput(requireContext(), "输入房间号，例如 1");
        roomInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        panel.addView(roomInput);

        joinButton = UiUtils.createActionButton(requireContext(), "加入房间");
        joinButton.setOnClickListener(v -> joinRoom());
        panel.addView(joinButton);
        return panel;
    }

    private View createActionPanel() {
        LinearLayout panel = UiUtils.createPanel(requireContext());
        panel.addView(UiUtils.createSectionTitle(requireContext(), "房间操作"));

        connectButton = UiUtils.createActionButton(requireContext(), "连接服务器");
        connectButton.setOnClickListener(v -> connectWs());
        panel.addView(connectButton);

        randomButton = UiUtils.createActionButton(requireContext(), "随机匹配");
        randomButton.setOnClickListener(v -> {
            clearError();
            resetRoom("匹配中");
            renderState();
            send("MATCH_RANDOM", 0L, null);
        });
        panel.addView(randomButton);

        createButton = UiUtils.createActionButton(requireContext(), "创建房间");
        createButton.setOnClickListener(v -> createRoom());
        panel.addView(createButton);

        startButton = UiUtils.createActionButton(requireContext(), "开始游戏");
        startButton.setOnClickListener(v -> startGame());
        panel.addView(startButton);

        disconnectButton = UiUtils.createSecondaryButton(requireContext(), "断开连接");
        disconnectButton.setOnClickListener(v -> ((MainActivity) requireActivity()).disconnectGameWs());
        panel.addView(disconnectButton);
        return panel;
    }

    private View createLogPanel() {
        LinearLayout panel = UiUtils.createPanel(requireContext(), 14);
        panel.addView(UiUtils.createSectionTitle(requireContext(), "事件记录"));
        panel.addView(UiUtils.createCaption(requireContext(), "仅显示最近的房间关键事件，便于人工测试。"));
        logContainer = new LinearLayout(requireContext());
        logContainer.setOrientation(LinearLayout.VERTICAL);
        UiUtils.setTopMargin(logContainer, requireContext(), 8);
        panel.addView(logContainer);
        return panel;
    }

    private void selectMode(String mode) {
        if (roomId > 0) {
            ((MainActivity) requireActivity()).toast("已在房间中，模式由当前房间决定");
            return;
        }
        selectedMode = mode;
        roomMode = mode;
        renderState();
    }

    private void connectWs() {
        MainActivity activity = (MainActivity) requireActivity();
        authReady = false;
        clearRoom();
        clearError();
        appendLog("正在连接服务器...");
        renderState();

        activity.setWsListener(new MainActivity.GameWsListener() {
            @Override
            public void onOpen() {
                appendLog("WS 已连接，等待认证完成");
                renderState();
            }

            @Override
            public void onMessage(String type, long roomIdFromMsg, long userId, @Nullable JSONObject payload) {
                handleWsMessage(type, roomIdFromMsg, payload);
            }

            @Override
            public void onError(String message) {
                showError(message);
                appendLog("错误: " + message);
            }

            @Override
            public void onClosed() {
                authReady = false;
                clearRoom();
                roomState = "连接已断开";
                appendLog("WS 已断开");
                renderState();
            }
        });
        activity.connectGameWs();
    }

    private void handleWsMessage(String type, long roomIdFromMsg, @Nullable JSONObject payload) {
        clearError();
        if (roomIdFromMsg > 0) {
            roomId = roomIdFromMsg;
        }

        switch (type) {
            case "AUTH_OK" -> {
                authReady = true;
                roomState = "已连接，未加入房间";
                appendLog("认证成功");
            }
            case "MATCH_WAITING" -> {
                resetRoom("匹配中");
                appendLog("正在等待另一位玩家");
            }
            case "MATCH_SUCCESS" -> {
                applyRoomPayload(payload, "房间已满");
                appendLog("匹配成功，房间号 " + roomId);
            }
            case "ROOM_CREATED" -> {
                applyRoomPayload(payload, "等待玩家加入");
                appendLog("已创建房间，房间号 " + roomId);
            }
            case "ROOM_JOINED" -> {
                applyRoomPayload(payload, "房间已满");
                appendLog("玩家已加入，房间号 " + roomId);
            }
            case "GAME_START" -> handleGameStart(roomIdFromMsg, payload);
            case "ERROR" -> {
                String reason = payload == null ? "服务器返回错误" : payload.optString("reason", "服务器返回错误");
                showError(reason);
                appendLog("服务器错误: " + reason);
            }
            default -> appendLog(type + " " + (payload == null ? "{}" : payload));
        }
        renderState();
    }

    private void createRoom() {
        clearError();
        try {
            JSONObject payload = new JSONObject();
            payload.put("gameMode", selectedMode);
            send("CREATE_ROOM", 0L, payload);
        } catch (Exception e) {
            send("CREATE_ROOM", 0L, null);
        }
    }

    private void joinRoom() {
        clearError();
        long targetRoomId = parseLong(roomInput.getText().toString().trim());
        if (targetRoomId <= 0) {
            showError("请输入有效房间号");
            return;
        }
        send("JOIN_ROOM", targetRoomId, null);
    }

    private void startGame() {
        if (roomId <= 0) {
            showError("请先创建、匹配或加入房间");
            return;
        }
        if (!canStartGame()) {
            showError("只有房主可在两名玩家到齐后开始游戏");
            return;
        }
        send("START_GAME", roomId, null);
    }

    private void handleGameStart(long roomIdFromMsg, @Nullable JSONObject payload) {
        long startRoomId = roomIdFromMsg;
        long seed = 0L;
        String mode = roomMode;
        long p1 = player1Id;
        long p2 = player2Id;
        int player1SkinId = 0;
        int player2SkinId = 0;

        if (payload != null) {
            startRoomId = payload.optLong("roomId", roomIdFromMsg);
            seed = payload.optLong("seed", 0L);
            mode = payload.optString("gameMode", MODE_COOP);
            p1 = payload.optLong("player1Id", 0L);
            p2 = payload.optLong("player2Id", 0L);
            player1SkinId = payload.optInt("player1SkinId", 0);
            player2SkinId = payload.optInt("player2SkinId", 0);
        }

        roomState = "进入游戏";
        appendLog("游戏开始");
        renderState();
        if (startRoomId > 0) {
            ((MainActivity) requireActivity()).showPvpGame(
                    startRoomId,
                    seed,
                    mode,
                    p1,
                    p2,
                    player1SkinId,
                    player2SkinId
            );
        }
    }

    private void applyRoomPayload(@Nullable JSONObject payload, String fallbackState) {
        if (payload != null) {
            roomId = payload.optLong("roomId", roomId);
            player1Id = payload.optLong("player1Id", player1Id);
            player2Id = payload.optLong("player2Id", player2Id);
            roomMode = payload.optString("gameMode", roomMode);
            roomState = translateRoomState(payload.optString("state", fallbackState));
        } else {
            roomState = fallbackState;
        }

        if (roomId > 0 && player1Id == 0 && currentUserId() > 0) {
            player1Id = currentUserId();
        }
        selectedMode = roomMode;
    }

    private String translateRoomState(String rawState) {
        return switch (rawState) {
            case "WAITING" -> player2Id == 0 ? "等待玩家加入" : "房间已满";
            case "IN_GAME" -> "游戏中";
            case "FINISHED" -> "已结束";
            default -> rawState == null || rawState.isBlank() ? "房间已更新" : rawState;
        };
    }

    private void send(String type, long targetRoomId, @Nullable JSONObject payload) {
        if (!authReady && !"AUTH".equals(type)) {
            showError("WebSocket 尚未认证，请等待 AUTH_OK");
            return;
        }
        boolean sent = ((MainActivity) requireActivity()).sendWs(type, targetRoomId, payload);
        if (!sent) {
            showError("请先连接服务器");
            return;
        }
        appendLog("发送 " + type + (targetRoomId > 0 ? " #" + targetRoomId : ""));
    }

    private void renderState() {
        if (connectionView == null) {
            return;
        }
        connectionView.setText(authReady ? "已连接" : "未连接");
        connectionView.setBackgroundColor(authReady ? UiUtils.colorPositive() : UiUtils.colorNegative());

        roomIdView.setText("房间号: " + (roomId > 0 ? String.valueOf(roomId) : "--"));
        roomStateView.setText("状态: " + roomState);
        modeView.setText("模式: " + modeText(roomId > 0 ? roomMode : selectedMode));
        player1View.setText("玩家 1: " + playerText(player1Id));
        player2View.setText("玩家 2: " + playerText(player2Id));
        hintView.setText(buildHint());

        UiUtils.applyTabStyle(modeCoopButton, MODE_COOP.equals(selectedMode));
        UiUtils.applyTabStyle(modePvpButton, MODE_PVP.equals(selectedMode));
        boolean canChooseMode = roomId <= 0;
        modeCoopButton.setEnabled(canChooseMode);
        modePvpButton.setEnabled(canChooseMode);

        connectButton.setEnabled(!authReady);
        randomButton.setEnabled(authReady);
        createButton.setEnabled(authReady && roomId <= 0);
        joinButton.setEnabled(authReady);
        startButton.setEnabled(canStartGame());
        startButton.setAlpha(canStartGame() ? 1.0f : 0.55f);
        disconnectButton.setEnabled(authReady);
    }

    private boolean canStartGame() {
        return authReady && roomId > 0 && player2Id > 0 && currentUserId() == player1Id;
    }

    private String buildHint() {
        if (!authReady) {
            return "先连接服务器；连接成功后才允许创建、加入或匹配。";
        }
        if (roomId <= 0) {
            return "可以创建房间、输入房间号加入，或使用随机匹配。";
        }
        if (player2Id == 0) {
            return "把房间号发给另一台客户端，等待对方加入。";
        }
        if (canStartGame()) {
            return "两名玩家已到齐，房主可以开始游戏。";
        }
        return "等待房主开始游戏。";
    }

    private String playerText(long userId) {
        if (userId <= 0) {
            return "--";
        }
        String suffix = userId == currentUserId() ? "（我）" : "";
        return "#" + userId + suffix;
    }

    private String modeText(String mode) {
        return MODE_PVP.equals(mode) ? "对战" : "合作";
    }

    private long currentUserId() {
        MainActivity activity = (MainActivity) requireActivity();
        return activity.getCurrentUser() == null ? 0L : activity.getCurrentUser().getUserId();
    }

    private void resetRoom(String state) {
        roomId = 0L;
        player1Id = 0L;
        player2Id = 0L;
        roomMode = selectedMode;
        roomState = state;
    }

    private void clearRoom() {
        resetRoom("未加入房间");
    }

    private void showError(String message) {
        if (errorView != null) {
            errorView.setText("提示: " + message);
        }
        ((MainActivity) requireActivity()).toast(message);
        renderState();
    }

    private void clearError() {
        if (errorView != null) {
            errorView.setText("");
        }
    }

    private void appendLog(String text) {
        if (logContainer == null) {
            return;
        }
        if (logContainer.getChildCount() >= 8) {
            logContainer.removeViewAt(0);
        }
        logContainer.addView(UiUtils.createCaption(requireContext(), text));
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public void onDestroyView() {
        ((MainActivity) requireActivity()).setWsListener(null);
        super.onDestroyView();
    }
}
