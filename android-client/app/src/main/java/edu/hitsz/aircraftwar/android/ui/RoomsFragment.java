package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import edu.hitsz.aircraftwar.android.MainActivity;

public class RoomsFragment extends Fragment {
    private LinearLayout logContainer;
    private long roomId = 0L;
    private String gameMode = "COOP"; // 默认合作模式

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = UiUtils.createScreenColumn(requireContext());
        scrollView.addView(root);

        root.addView(UiUtils.createTitle(requireContext(), "联机房间"));

        EditText roomInput = new EditText(requireContext());
        roomInput.setHint("房间ID");
        roomInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        roomInput.setTextColor(0xFF111111);
        roomInput.setHintTextColor(0xFF666666);
        root.addView(roomInput);

        var connect = UiUtils.createActionButton(requireContext(), "连接WebSocket");
        connect.setOnClickListener(v -> connectWs());
        root.addView(connect);

        var random = UiUtils.createActionButton(requireContext(), "随机匹配");
        random.setOnClickListener(v -> send("MATCH_RANDOM", 0L, null));
        root.addView(random);

        // 游戏模式选择
        var modePanel = UiUtils.createPanel(requireContext(), 8);
        modePanel.setOrientation(LinearLayout.HORIZONTAL);
        var modeLabel = UiUtils.createCaption(requireContext(), "游戏模式:");
        modePanel.addView(modeLabel);
        
        var modeCoop = UiUtils.createActionButton(requireContext(), "合作");
        var modePvp = UiUtils.createActionButton(requireContext(), "对战");
        
        // 设置初始状态 - 合作模式选中
        modeCoop.setBackgroundColor(0xFF4CAF50); // 绿色背景表示选中
        modeCoop.setTextColor(0xFFFFFFFF); // 白色文字
        modePvp.setBackgroundColor(0xFFCCCCCC); // 灰色背景表示未选中
        modePvp.setTextColor(0xFF666666); // 深灰色文字
        
        modeCoop.setOnClickListener(v -> {
            if (!"COOP".equals(gameMode)) {
                gameMode = "COOP";
                // 更新按钮样式
                modeCoop.setBackgroundColor(0xFF4CAF50); // 绿色 - 选中
                modeCoop.setTextColor(0xFFFFFFFF);
                modePvp.setBackgroundColor(0xFFCCCCCC); // 灰色 - 未选中
                modePvp.setTextColor(0xFF666666);
                ((MainActivity) requireActivity()).toast("已选择合作模式");
            }
        });
        modePanel.addView(modeCoop);
        
        modePvp.setOnClickListener(v -> {
            if (!"PVP".equals(gameMode)) {
                gameMode = "PVP";
                // 更新按钮样式
                modePvp.setBackgroundColor(0xFFFF5722); // 橙红色 - 选中
                modePvp.setTextColor(0xFFFFFFFF);
                modeCoop.setBackgroundColor(0xFFCCCCCC); // 灰色 - 未选中
                modeCoop.setTextColor(0xFF666666);
                ((MainActivity) requireActivity()).toast("已选择对战模式");
            }
        });
        modePanel.addView(modePvp);
        root.addView(modePanel);

        var create = UiUtils.createActionButton(requireContext(), "创建房间");
        create.setOnClickListener(v -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("gameMode", gameMode);
                MainActivity activity = (MainActivity) requireActivity();
                int skinId = activity.getCurrentUser() == null ? 0 : activity.getCurrentUser().getEquippedSkinId();
                payload.put("skinId", skinId);
                send("CREATE_ROOM", 0L, payload);
            } catch (Exception e) {
                send("CREATE_ROOM", 0L, null);
            }
        });
        root.addView(create);

        var join = UiUtils.createActionButton(requireContext(), "加入房间");
        join.setOnClickListener(v -> {
            long targetRoomId = parseLong(roomInput.getText().toString().trim());
            if (targetRoomId <= 0) {
                ((MainActivity) requireActivity()).toast("请输入有效房间ID");
                return;
            }
            try {
                JSONObject payload = new JSONObject();
                MainActivity activity = (MainActivity) requireActivity();
                int skinId = activity.getCurrentUser() == null ? 0 : activity.getCurrentUser().getEquippedSkinId();
                payload.put("skinId", skinId);
                send("JOIN_ROOM", targetRoomId, payload);
            } catch (Exception e) {
                send("JOIN_ROOM", targetRoomId, null);
            }
        });
        root.addView(join);

        var start = UiUtils.createActionButton(requireContext(), "开始游戏");
        start.setOnClickListener(v -> {
            if (roomId <= 0) {
                ((MainActivity) requireActivity()).toast("请先匹配或加入房间");
                return;
            }
            send("START_GAME", roomId, null);
        });
        root.addView(start);

        var close = UiUtils.createActionButton(requireContext(), "断开连接");
        close.setOnClickListener(v -> ((MainActivity) requireActivity()).disconnectGameWs());
        root.addView(close);

        logContainer = new LinearLayout(requireContext());
        logContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(logContainer);

        var back = UiUtils.createActionButton(requireContext(), "返回主菜单");
        back.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).setWsListener(null);
            ((MainActivity) requireActivity()).showMainMenu();
        });
        root.addView(back);

        return scrollView;
    }

    private void connectWs() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setWsListener(new MainActivity.GameWsListener() {
            @Override
            public void onOpen() {
                appendLog("WS connected, AUTH sent");
            }

            @Override
            public void onMessage(String type, long roomIdFromMsg, long userId, @Nullable JSONObject payload) {
                if (roomIdFromMsg > 0) {
                    roomId = roomIdFromMsg;
                }
                appendLog(type + " room=" + roomIdFromMsg + " from=" + userId + " payload=" + payload);
                if ("GAME_START".equals(type)) {
                    long startRoomId = roomIdFromMsg;
                    long seed = 0L;
                    String gameMode = "COOP";
                    long player1Id = 0L;
                    long player2Id = 0L;
                    int player1SkinId = 0;
                    int player2SkinId = 0;
                    
                    if (payload != null) {
                        startRoomId = payload.optLong("roomId", roomIdFromMsg);
                        seed = payload.optLong("seed", 0L);
                        gameMode = payload.optString("gameMode", "COOP");
                        player1Id = payload.optLong("player1Id", 0L);
                        player2Id = payload.optLong("player2Id", 0L);
                        player1SkinId = payload.optInt("player1SkinId", 0);
                        player2SkinId = payload.optInt("player2SkinId", 0);
                    }
                    if (startRoomId > 0) {
                        activity.showPvpGame(startRoomId, seed, gameMode, player1Id, player2Id, player1SkinId, player2SkinId);
                    }
                }
            }

            @Override
            public void onError(String message) {
                appendLog("WS error: " + message);
            }

            @Override
            public void onClosed() {
                appendLog("WS closed");
            }
        });
        activity.connectGameWs();
    }

    private void send(String type, long roomId, @Nullable JSONObject payload) {
        boolean sent = ((MainActivity) requireActivity()).sendWs(type, roomId, payload);
        if (!sent) {
            ((MainActivity) requireActivity()).toast("请先连接WebSocket");
            return;
        }
        appendLog("send: " + type + " room=" + roomId);
    }

    private void appendLog(String text) {
        logContainer.addView(UiUtils.createBody(requireContext(), text));
    }

    private static long parseLong(String v) {
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public void onDestroyView() {
        MainActivity activity = (MainActivity) requireActivity();
        activity.setWsListener(null);
        super.onDestroyView();
    }
}
