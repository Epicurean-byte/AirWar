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

        var create = UiUtils.createActionButton(requireContext(), "创建房间");
        create.setOnClickListener(v -> send("CREATE_ROOM", 0L, null));
        root.addView(create);

        var join = UiUtils.createActionButton(requireContext(), "加入房间");
        join.setOnClickListener(v -> {
            long targetRoomId = parseLong(roomInput.getText().toString().trim());
            if (targetRoomId <= 0) {
                ((MainActivity) requireActivity()).toast("请输入有效房间ID");
                return;
            }
            send("JOIN_ROOM", targetRoomId, null);
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
                    if (payload != null) {
                        startRoomId = payload.optLong("roomId", roomIdFromMsg);
                        seed = payload.optLong("seed", 0L);
                    }
                    if (startRoomId > 0) {
                        activity.showPvpGame(startRoomId, seed);
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
