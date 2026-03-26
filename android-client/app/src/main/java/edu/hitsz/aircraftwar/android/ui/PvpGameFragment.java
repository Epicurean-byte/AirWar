package edu.hitsz.aircraftwar.android.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.hitsz.aircraftwar.android.MainActivity;
import edu.hitsz.aircraftwar.android.game.PvpBattleView;
import edu.hitsz.aircraftwar.android.network.NetworkExecutor;

public class PvpGameFragment extends Fragment {
    private static final String ARG_ROOM_ID = "room_id";
    private static final String ARG_SEED = "seed";

    private final AtomicBoolean settled = new AtomicBoolean(false);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long roomId;
    private long seed;
    private long myUserId;

    private PvpBattleView battleView;
    private TextView statusView;
    private long myScore = 0L;
    private long myCoins = 0L;

    private final Runnable autoFireTask = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) {
                return;
            }
            ((MainActivity) requireActivity()).sendWs("FIRE", roomId, null);
            handler.postDelayed(this, 250L);
        }
    };

    public static PvpGameFragment newInstance(long roomId, long seed) {
        Bundle args = new Bundle();
        args.putLong(ARG_ROOM_ID, roomId);
        args.putLong(ARG_SEED, seed);
        PvpGameFragment fragment = new PvpGameFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        roomId = getArguments() == null ? 0L : getArguments().getLong(ARG_ROOM_ID, 0L);
        seed = getArguments() == null ? 0L : getArguments().getLong(ARG_SEED, 0L);

        MainActivity activity = (MainActivity) requireActivity();
        myUserId = activity.getCurrentUser() == null ? 0L : activity.getCurrentUser().getUserId();

        FrameLayout root = new FrameLayout(requireContext());
        battleView = new PvpBattleView(requireContext());
        battleView.setMyUserId(myUserId);
        battleView.setInputListener(new PvpBattleView.InputListener() {
            @Override
            public void onMove(float x, float y) {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("x", x);
                    payload.put("y", y);
                    activity.sendWs("MOVE", roomId, payload);
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onFire() {
                activity.sendWs("FIRE", roomId, null);
            }
        });
        root.addView(battleView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        int margin = UiUtils.dp(requireContext(), 12);

        statusView = UiUtils.createBody(requireContext(), "PVP房间#" + roomId + " seed=" + seed + " 连接中...");
        statusView.setBackgroundColor(0x66333333);
        statusView.setTextColor(0xFFFFFFFF);
        statusView.setPadding(margin, UiUtils.dp(requireContext(), 8), margin, UiUtils.dp(requireContext(), 8));
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusParams.gravity = Gravity.TOP | Gravity.START;
        statusParams.setMargins(margin, margin, margin, margin);
        root.addView(statusView, statusParams);

        Button exitButton = new Button(requireContext());
        exitButton.setText("退出联机");
        exitButton.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).sendWs("GAME_OVER", roomId, null);
            settlePvp();
            ((MainActivity) requireActivity()).showMainMenu();
        });
        FrameLayout.LayoutParams exitParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        exitParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        exitParams.setMargins(margin, margin, margin, margin);
        root.addView(exitButton, exitParams);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) requireActivity();
        activity.setWsListener(new MainActivity.GameWsListener() {
            @Override
            public void onOpen() {
                statusView.setText("PVP房间#" + roomId + " 已连接");
            }

            @Override
            public void onMessage(String type, long msgRoomId, long userId, @Nullable JSONObject payload) {
                if (msgRoomId != 0 && msgRoomId != roomId) {
                    return;
                }
                if ("BATTLE_STATE".equals(type) && payload != null) {
                    onBattleState(payload);
                    return;
                }
                if ("GAME_OVER".equals(type) && payload != null) {
                    onGameOver(payload);
                    return;
                }
                if ("OPPONENT_DISCONNECTED".equals(type)) {
                    activity.toast("对手断线，比赛结束");
                    settlePvp();
                    activity.showMainMenu();
                    return;
                }
                if ("ERROR".equals(type) && payload != null) {
                    activity.toast("WS错误: " + payload.optString("reason", "unknown"));
                }
            }

            @Override
            public void onError(String message) {
                activity.toast("WS错误: " + message);
            }

            @Override
            public void onClosed() {
                activity.toast("WS已断开");
            }
        });
        handler.postDelayed(autoFireTask, 250L);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(autoFireTask);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(autoFireTask);
        MainActivity activity = (MainActivity) requireActivity();
        activity.setWsListener(null);
        battleView = null;
        super.onDestroyView();
    }

    private void onBattleState(JSONObject payload) {
        JSONArray playersJson = payload.optJSONArray("players");
        JSONArray enemiesJson = payload.optJSONArray("enemies");
        if (playersJson == null || enemiesJson == null || battleView == null) {
            return;
        }

        List<PvpBattleView.PlayerState> players = new ArrayList<>();
        for (int i = 0; i < playersJson.length(); i++) {
            JSONObject item = playersJson.optJSONObject(i);
            if (item == null) continue;
            PvpBattleView.PlayerState p = new PvpBattleView.PlayerState();
            p.userId = item.optLong("userId", 0L);
            p.x = (float) item.optDouble("x", 0.0);
            p.y = (float) item.optDouble("y", 0.0);
            p.hp = item.optInt("hp", 0);
            p.score = item.optLong("score", 0L);
            p.coins = item.optLong("coins", 0L);
            if (p.userId == myUserId) {
                myScore = p.score;
                myCoins = p.coins;
            }
            players.add(p);
        }

        List<PvpBattleView.EnemyState> enemies = new ArrayList<>();
        for (int i = 0; i < enemiesJson.length(); i++) {
            JSONObject item = enemiesJson.optJSONObject(i);
            if (item == null) continue;
            PvpBattleView.EnemyState e = new PvpBattleView.EnemyState();
            e.id = item.optInt("id", 0);
            e.type = item.optInt("type", 0);
            e.x = (float) item.optDouble("x", 0.0);
            e.y = (float) item.optDouble("y", 0.0);
            e.hp = item.optInt("hp", 0);
            enemies.add(e);
        }

        battleView.updateState(players, enemies);
        statusView.setText("PVP房间#" + roomId + " score=" + myScore + " coins=" + myCoins);
    }

    private void onGameOver(JSONObject payload) {
        long winnerUserId = payload.optLong("winnerUserId", 0L);
        MainActivity activity = (MainActivity) requireActivity();
        if (winnerUserId == myUserId) {
            activity.toast("你赢了");
        } else {
            activity.toast("你输了");
        }

        JSONArray playersJson = payload.optJSONArray("players");
        if (playersJson != null) {
            for (int i = 0; i < playersJson.length(); i++) {
                JSONObject p = playersJson.optJSONObject(i);
                if (p == null) continue;
                if (p.optLong("userId", 0L) == myUserId) {
                    myScore = p.optLong("score", myScore);
                    myCoins = p.optLong("coins", myCoins);
                    break;
                }
            }
        }

        settlePvp();
        activity.showMainMenu();
    }

    private void settlePvp() {
        MainActivity activity = (MainActivity) requireActivity();
        if (activity.getCurrentUser() == null || !settled.compareAndSet(false, true)) {
            return;
        }
        long userId = activity.getCurrentUser().getUserId();
        long score = myScore;
        long coins = myCoins;
        NetworkExecutor.run(() -> {
            try {
                activity.getApiClient().settlePvp(roomId, userId, score, coins);
                activity.toast("联机结算已上报 score=" + score + " coins=" + coins);
            } catch (Exception e) {
                activity.toast("联机结算失败: " + (e.getMessage() == null ? "unknown" : e.getMessage()));
            }
        });
    }
}
